package com.meowalarm.app

import android.app.Activity
import android.app.TimePickerDialog
import android.app.NotificationManager
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient

open class MainActivity : Activity() {
    private lateinit var web: WebView
    private var firstRunPermissionLaunched = false
    private var previewPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val customSoundRequest = 4701
    private var pendingAudioKind = "meow"
    private var pendingAlarmId = -1
    private var purrPlayer: MediaPlayer? = null
    private var purrFade: Runnable? = null
    private var previewTimeout: Runnable? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        web = WebView(this)
        setContentView(web)
        web.setBackgroundColor(0xFF342B40.toInt())
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // A new WebView is still loading when Android launches the full-screen
                // notification. Re-send the ringing state *after* JS has initialized.
                web.evaluateJavascript("window.refreshAlarmPermissionUI&&window.refreshAlarmPermissionUI()", null)
                if (Ring.active) {
                    updateAlarmWindow(true)
                    syncRingJs()
                } else if (this@MainActivity !is RingingActivity) {
                    maybeLaunchPermissionSetup()
                }
            }
        }
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = false
        }
        web.addJavascriptInterface(Bridge(this), "Android")
        web.loadUrl("file:///android_asset/index.html")
    }

    /** The ONLY editable clock: invoked from the alarm editor. */
    fun showAlarmTimePicker(h: Int, m: Int) {
        TimePickerDialog(this, { _, hour, minute ->
            if (::web.isInitialized) {
                web.evaluateJavascript("window.onNativeTimePicked($hour,$minute)", null)
            }
        }, h.coerceIn(0, 23), m.coerceIn(0, 59), true).show()
    }

    fun selectCustomAudio(kind: String, alarmId: Int = -1) {
        if (kind !in setOf("meow", "purr", "alarm")) return
        if (kind == "alarm" && alarmId < 100) return
        pendingAudioKind = kind
        pendingAlarmId = alarmId
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("audio/*")
        @Suppress("DEPRECATION")
        startActivityForResult(intent, customSoundRequest)
    }

    private fun importResult(kind: String, id: Int, ok: Boolean, message: String) {
        if (!::web.isInitialized) return
        val quoted = JSONObject.quote(message)
        val callback = if (kind == "alarm")
            "window.onAlarmMeowImported&&window.onAlarmMeowImported($id,$ok,$quoted)"
        else if (kind == "purr")
            "window.onCustomPurrImported&&window.onCustomPurrImported($ok,$quoted)"
        else "window.onCustomMeowImported&&window.onCustomMeowImported($ok,$quoted)"
        web.evaluateJavascript(callback, null)
    }

    @Deprecated("Activity result API retained to avoid additional dependencies")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != customSoundRequest || resultCode != RESULT_OK) return
        val kind = pendingAudioKind
        val alarmId = pendingAlarmId
        val uri = data?.data ?: return
        val pending = File(filesDir, "new_${kind}_${alarmId}.pending")
        try {
            // Copy to private app storage; no SAF URI permissions are needed after import.
            contentResolver.openInputStream(uri)?.use { source ->
                pending.outputStream().use { destination ->
                    val buffer = ByteArray(8192)
                    var count = 0L
                    while (true) {
                        val n = source.read(buffer)
                        if (n < 0) break
                        if (n == 0) continue
                        count += n
                        require(count <= 25L * 1024 * 1024) { "حداکثر حجم فایل ۲۵ مگابایت است" }
                        destination.write(buffer, 0, n)
                    }
                    require(count > 0) { "فایل خالی است" }
                }
            } ?: error("نمی‌توان فایل انتخاب‌شده را خواند")
            MeowAudio.validateImportedFile(pending)
            val destination = when (kind) {
                "purr" -> MeowAudio.customPurrFile(this)
                "alarm" -> {
                    require(Alarms.read(this).any { it.id == alarmId }) { "آلارم حذف شده است" }
                    MeowAudio.alarmFile(this, alarmId)
                }
                else -> MeowAudio.customFile(this)
            }
            Files.move(pending.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            if (kind == "alarm") Alarms.setAlarmImportedAudio(this, alarmId)
            importResult(kind, alarmId, true, "فایل ذخیره و با موفقیت بررسی شد")
        } catch (ex: Exception) {
            Log.e("MeowAlarm", "Sound import failed: $kind", ex)
            importResult(kind, alarmId, false, ex.message ?: "پخش فایل روی این گوشی پشتیبانی نمی‌شود")
        } finally {
            pending.delete()
        }
    }

    private fun stopPreview() {
        previewTimeout?.let { handler.removeCallbacks(it) }
        previewTimeout = null
        previewPlayer?.let { runCatching { it.stop() }; runCatching { it.release() } }
        previewPlayer = null
    }

    fun previewAudio(kind: String, sound: String, alarmId: Int = -1) {
        stopPreview()
        try {
            val player = if (kind == "purr")
                MeowAudio.openPurr(this, sound, AudioAttributes.USAGE_MEDIA, loop = false)
            else MeowAudio.open(this, sound, AudioAttributes.USAGE_MEDIA, loop = false, alarmId = alarmId)
            player.setVolume(if (kind == "purr") 0.7f else 1f, if (kind == "purr") 0.7f else 1f)
            player.setOnCompletionListener { stopPreview() }
            player.setOnErrorListener { _, what, extra ->
                Log.e("MeowAlarm", "Sound preview error: $kind/$sound $what/$extra")
                stopPreview()
                true
            }
            previewPlayer = player
            player.start()
            val timeout = Runnable { stopPreview() }
            previewTimeout = timeout
            handler.postDelayed(timeout, 20000)
        } catch (ex: Exception) {
            Log.e("MeowAlarm", "Preview failed for $kind/$sound", ex)
            stopPreview()
            if (::web.isInitialized) web.evaluateJavascript(
                "window.onMeowPreviewError&&window.onMeowPreviewError(${JSONObject.quote(ex.message ?: "پخش صدا ناموفق بود")})", null
            )
        }
    }

    fun startPurr() {
        if (purrPlayer != null) return
        try {
            val key = getSharedPreferences("meow", MODE_PRIVATE)
                .getString("default_purr_sound", "purr_1") ?: "purr_1"
            val player = MeowAudio.openPurr(this, key, AudioAttributes.USAGE_MEDIA, loop = true)
            player.setVolume(0f, 0f)
            player.setOnErrorListener { _, what, extra ->
                Log.e("MeowAlarm", "Purr playback error $what/$extra")
                stopPurr()
                true
            }
            purrPlayer = player
            player.start()
            // A gentle ramp avoids the harsh start of short, looping recordings.
            val fade = object : Runnable {
                var step = 0
                override fun run() {
                    if (purrPlayer !== player) return
                    step++
                    val gain = (step / 10f * 0.55f).coerceAtMost(0.55f)
                    runCatching { player.setVolume(gain, gain) }
                    if (step < 10) handler.postDelayed(this, 55)
                }
            }
            purrFade = fade
            handler.post(fade)
        } catch (ex: Exception) {
            Log.e("MeowAlarm", "Unable to start purr", ex)
            stopPurr()
        }
    }

    fun stopPurr() {
        purrFade?.let { handler.removeCallbacks(it) }
        purrFade = null
        purrPlayer?.let { runCatching { it.stop() }; runCatching { it.release() } }
        purrPlayer = null
    }

    private fun updateAlarmWindow(ringing: Boolean) {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(ringing)
            setTurnScreenOn(ringing)
        } else {
            @Suppress("DEPRECATION")
            val flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            if (ringing) window.addFlags(flags) else window.clearFlags(flags)
        }
        if (ringing) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun maybeLaunchPermissionSetup() {
        // Never cover an active alarm with a first-run settings screen.
        if (firstRunPermissionLaunched || Ring.active || isFinishing) return
        if (!Alarms.prefs(this).getBoolean(PermissionSetupActivity.DONE_KEY, false)) {
            firstRunPermissionLaunched = true
            startActivity(Intent(this, PermissionSetupActivity::class.java))
        }
    }

    /** Reopen the permission walkthrough from MeowAlarm settings at any time. */
    fun showPermissionSetup() {
        if (!Ring.active && this !is RingingActivity)
            startActivity(Intent(this, PermissionSetupActivity::class.java))
    }

    private fun syncRingJs() {
        if (!::web.isInitialized) return
        val js = if (Ring.active) "window.nativeRing&&window.nativeRing()"
                 else "window.nativeStopped&&window.nativeStopped()"
        runCatching { web.evaluateJavascript(js, null) }
            .onFailure { Log.e("MeowAlarm", "WebView ringing callback failed", it) }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        runCatching { updateAlarmWindow(Ring.active); if (Ring.active) syncRingJs() }
            .onFailure { Log.e("MeowAlarm", "onNewIntent failed", it) }
    }

    override fun onResume() {
        super.onResume()
        Ring.listener = { runOnUiThread {
            runCatching { updateAlarmWindow(Ring.active); syncRingJs() }
                .onFailure { Log.e("MeowAlarm", "Ring listener failed", it) }
        } }
        runCatching {
            updateAlarmWindow(Ring.active)
            if (Ring.active) syncRingJs()
            if (::web.isInitialized) web.evaluateJavascript(
                "window.refreshAlarmPermissionUI&&window.refreshAlarmPermissionUI()", null)
        }
    }

    override fun onPause() {
        stopPurr()
        Ring.listener = null
        if (!Ring.active) window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }

    override fun onDestroy() {
        stopPurr()
        stopPreview()
        if (::web.isInitialized) web.destroy()
        super.onDestroy()
    }
}
