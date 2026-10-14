package com.meowalarm.app

import android.Manifest
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

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var previewPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val customSoundRequest = 4701

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        ProfileStore.init(this) // Must run before the WebView migrates the alarm list.
        web = WebView(this)
        setContentView(web)
        web.setBackgroundColor(0xFF342B40.toInt())
        web.webViewClient = WebViewClient()
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = false
        }
        web.addJavascriptInterface(Bridge(this), "Android")
        web.loadUrl("file:///android_asset/index.html")
        askPermissions()
    }

    /** The ONLY editable clock: invoked from the alarm editor. */
    fun showAlarmTimePicker(h: Int, m: Int) {
        TimePickerDialog(this, { _, hour, minute ->
            if (::web.isInitialized) {
                web.evaluateJavascript("window.onNativeTimePicked($hour,$minute)", null)
            }
        }, h.coerceIn(0, 23), m.coerceIn(0, 59), true).show()
    }

    fun selectCustomMeow() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("audio/*")
        @Suppress("DEPRECATION")
        startActivityForResult(intent, customSoundRequest)
    }

    private fun importResult(ok: Boolean, message: String) {
        if (!::web.isInitialized) return
        web.evaluateJavascript(
            "window.onCustomMeowImported&&window.onCustomMeowImported($ok,${JSONObject.quote(message)})", null
        )
    }

    @Deprecated("Activity result API retained to avoid additional dependencies")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != customSoundRequest || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val pending = File(filesDir, "custom_meow_audio.pending")
        try {
            // Store a private copy for offline ringing. Never delete the last good import
            // if the newly selected file is unreadable or unsupported on this phone.
            contentResolver.openInputStream(uri)?.use { source ->
                pending.outputStream().use { dest ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val size = source.read(buffer)
                        if (size < 0) break
                        if (size == 0) continue
                        total += size
                        require(total <= 25L * 1024 * 1024) { "فایل صوتی نباید بیشتر از ۲۵ مگابایت باشد" }
                        dest.write(buffer, 0, size)
                    }
                    require(total > 0) { "فایل صوتی خالی است" }
                }
            } ?: error("نمی‌توان فایل انتخاب‌شده را خواند")
            MeowAudio.validateImportedFile(pending)
            Files.move(pending.toPath(), MeowAudio.customFile(this).toPath(),
                StandardCopyOption.REPLACE_EXISTING)
            importResult(true, "فایل قابل پخش است و آمادهٔ آلارم شد")
        } catch (ex: Exception) {
            Log.e("MeowAlarm", "Unable to import or decode sound", ex)
            importResult(false, ex.message ?: "فرمت صوتی این فایل روی گوشی پشتیبانی نمی‌شود")
        } finally {
            pending.delete()
        }
    }

    private fun stopPreview() {
        handler.removeCallbacksAndMessages(null)
        previewPlayer?.let { runCatching { it.stop() }; runCatching { it.release() } }
        previewPlayer = null
    }

    fun previewMeow(type: String) {
        stopPreview()
        try {
            val player = MeowAudio.open(this, type, AudioAttributes.USAGE_MEDIA, loop = false)
            player.setVolume(1f, 1f)
            player.setOnCompletionListener { stopPreview() }
            player.setOnErrorListener { _, what, extra ->
                Log.e("MeowAlarm", "Preview error $what/$extra for $type")
                stopPreview()
                true
            }
            previewPlayer = player
            player.start()
            handler.postDelayed({ stopPreview() }, 9000)
        } catch (ex: Exception) {
            Log.e("MeowAlarm", "Meow preview failed for $type", ex)
            stopPreview()
            if (::web.isInitialized) web.evaluateJavascript(
                "window.onMeowPreviewError&&window.onMeowPreviewError(${JSONObject.quote(ex.message ?: "پخش آزمایشی صدا ناموفق بود")})", null
            )
        }
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

    private fun askPermissions() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        if (Build.VERSION.SDK_INT >= 34) {
            val nm = getSystemService(NotificationManager::class.java)
            if (!nm.canUseFullScreenIntent()) {
                runCatching { startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:$packageName"))) }
            }
        }
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
        runCatching { updateAlarmWindow(Ring.active); if (Ring.active) syncRingJs() }
    }

    override fun onPause() {
        Ring.listener = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }

    override fun onDestroy() {
        stopPreview()
        if (::web.isInitialized) web.destroy()
        super.onDestroy()
    }
}
