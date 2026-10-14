package com.meowalarm.app

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.*
import android.util.Log

/** Native alarm audio runs independently of the WebView and of phone lock state. */
class AlarmService : Service() {
    companion object {
        const val ACTION_RING = "com.meowalarm.app.action.RING"
        const val ACTION_STOP = "com.meowalarm.app.action.STOP"
        private const val CHANNEL_ID = "alarm_ring"
        private const val NOTIFICATION_ID = 1001
    }

    private var meowPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            AlarmDiagnostics.mark(this, "stop_requested")
            stopRing()
            stopSelf()
            return START_NOT_STICKY
        }
        if (Ring.active) return START_NOT_STICKY
        AlarmDiagnostics.mark(this, "trigger_received")
        val id = intent?.getIntExtra("id", 2) ?: 2
        Alarms.onTriggered(this, id)
        try {
            AlarmDiagnostics.mark(this, "before_foreground")
            goForeground()
            AlarmDiagnostics.mark(this, "foreground_started")
            Ring.active = true
            AlarmDiagnostics.mark(this, "before_meow")
            startSound()
            AlarmDiagnostics.mark(this, "before_ui_callback")
            runCatching { Ring.listener?.invoke() }
                .onFailure { Log.e("MeowAlarm", "Alarm UI listener failed", it) }
            AlarmDiagnostics.mark(this, "ui_callback_returned")
        } catch (ex: Exception) {
            AlarmDiagnostics.mark(this, "startup_exception_${ex.javaClass.simpleName}")
            Log.e("MeowAlarm", "Alarm service startup failed", ex)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun goForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Meow alarm", NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(null, null)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
        }

        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            ),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("میو میو! ⏰")
            .setContentText("گربه رو ناز کن تا ساکت بشه")
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(Notification.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
        } else {
            startForeground(NOTIFICATION_ID, n)
        }
    }

    private fun startSound() {
        if (meowPlayer != null) return
        val selected = Alarms.active(this)?.sound ?: "real"
        if (!playSelected(selected) && selected != "cute") {
            Log.w("MeowAlarm", "Selected sound $selected failed; using bundled cute meow")
            playSelected("cute")
        }
        AlarmDiagnostics.mark(this, "meow_started_or_fallback")
        AlarmDiagnostics.mark(this, "before_vibration")
        // A vibration exception must never stop the audio.
        try {
            val vibrator = getSystemService(Vibrator::class.java)
            if (vibrator?.hasVibrator() == true) {
                val effect = VibrationEffect.createWaveform(longArrayOf(0, 400, 300), 0)
                if (Build.VERSION.SDK_INT >= 31) vibrator.vibrate(
                    effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
                else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(effect)
                }
            }
        } catch (ex: Exception) {
            Log.e("MeowAlarm", "Vibration failed; audio continues", ex)
        }
        AlarmDiagnostics.mark(this, "vibration_started_or_skipped")
    }

    private fun playSelected(sound: String): Boolean {
        var candidate: MediaPlayer? = null
        return try {
            val player = MeowAudio.open(this, sound, AudioAttributes.USAGE_ALARM, loop = true)
            candidate = player
            player.setVolume(1f, 1f)
            player.setOnErrorListener { failed, what, extra ->
                Log.e("MeowAlarm", "$sound meow playback error ($what, $extra)")
                if (meowPlayer === failed) meowPlayer = null
                runCatching { failed.release() }
                if (Ring.active && sound != "cute") handler.post {
                    if (Ring.active && meowPlayer == null) playSelected("cute")
                }
                true
            }
            player.start()
            meowPlayer = player
            candidate = null
            true
        } catch (ex: Exception) {
            Log.e("MeowAlarm", "Unable to play $sound meow", ex)
            false
        } finally {
            runCatching { candidate?.release() }
        }
    }

    private fun stopRing() {
        handler.removeCallbacksAndMessages(null)
        meowPlayer?.let { runCatching { it.stop() }; runCatching { it.release() } }
        meowPlayer = null
        try {
            getSystemService(Vibrator::class.java)?.cancel()
        } catch (ex: Exception) {
            Log.w("MeowAlarm", "Unable to cancel vibration", ex)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        Ring.active = false
        runCatching { Ring.listener?.invoke() }
            .onFailure { Log.e("MeowAlarm", "UI callback failed", it) }
    }

    override fun onDestroy() {
        AlarmDiagnostics.mark(this, "service_destroyed")
        stopRing()
        super.onDestroy()
    }
}
