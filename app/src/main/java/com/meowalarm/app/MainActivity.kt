package com.meowalarm.app

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.app.NotificationManager
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
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
    private var previewTrack: AudioTrack? = null
    private val handler = Handler(Looper.getMainLooper())
    private val customSoundRequest = 4701

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
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

    @Deprecated("Activity result API retained to avoid additional dependencies")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != customSoundRequest || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            // Copy to private storage: sound must still work after device reboot and offline.
            val file = getFileStreamPath("custom_meow_audio")
            contentResolver.openInputStream(uri)?.use { source ->
                file.outputStream().use { dest ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val n = source.read(buffer)
                        if (n < 0) break
                        total += n
                        require(total <= 10L * 1024 * 1024) { "Audio file exceeds 10 MB" }
                        dest.write(buffer, 0, n)
                    }
                    require(total > 0) { "Empty audio file" }
                }
            } ?: error("Cannot read selected audio")
            web.evaluateJavascript("window.onCustomMeowImported&&window.onCustomMeowImported(true)", null)
        } catch (ex: Exception) {
            getFileStreamPath("custom_meow_audio").delete()
            Log.e("MeowAlarm", "Unable to import sound", ex)
            web.evaluateJavascript("window.onCustomMeowImported&&window.onCustomMeowImported(false)", null)
        }
    }

    private fun stopPreview() {
        handler.removeCallbacksAndMessages(null)
        previewPlayer?.let { runCatching { it.stop() }; runCatching { it.release() } }
        previewPlayer = null
        previewTrack?.let { runCatching { it.stop() }; runCatching { it.release() } }
        previewTrack = null
    }

    fun previewMeow(type: String) {
        stopPreview()
        try {
            val file = getFileStreamPath("custom_meow_audio")
            val resource = resources.getIdentifier("real_cat_meow", "raw", packageName)
            if (type == "cute" || (type == "real" && resource == 0) || (type == "custom" && !file.exists())) {
                val samples = Meow.make(22050)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder()
                        .setSampleRate(22050).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC).build()
                check(track.state == AudioTrack.STATE_INITIALIZED)
                track.write(samples, 0, samples.size)
                track.play()
                previewTrack = track
            } else {
                val player = MediaPlayer()
                player.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
                if (type == "custom") player.setDataSource(file.absolutePath)
                else {
                    resources.openRawResourceFd(resource)?.use {
                        player.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                    } ?: error("No real meow recording available")
                }
                player.setOnCompletionListener { stopPreview() }
                player.prepare()
                player.start()
                previewPlayer = player
            }
            handler.postDelayed({ stopPreview() }, 4500)
        } catch (ex: Exception) {
            Log.e("MeowAlarm", "Meow preview failed", ex)
            stopPreview()
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
