package com.meowalarm.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.File

/** All alarm and purr sounds are local. Playback never needs the Internet or the WebView. */
object MeowAudio {
    private const val CUSTOM_FILENAME = "custom_meow_audio" // Existing user's global import is retained.
    private const val CUSTOM_PURR_FILENAME = "custom_purr_audio"

    fun customFile(c: Context): File = c.getFileStreamPath(CUSTOM_FILENAME)
    fun customPurrFile(c: Context): File = c.getFileStreamPath(CUSTOM_PURR_FILENAME)
    fun alarmFile(c: Context, id: Int): File = c.getFileStreamPath("alarm_meow_${id}.audio")

    fun normalizeMeow(value: String): String = when (value) {
        "real" -> "meow_1"  // Migration from the original built-in real recording
        "cute" -> "meow_2"  // Migration from the original synthetic cute recording
        else -> value
    }
    fun isMeow(value: String): Boolean = normalizeMeow(value) in (1..6).map { "meow_$it" } ||
        value == "custom" || value == "alarm_custom"
    fun isPurr(value: String): Boolean = value in (1..3).map { "purr_$it" } || value == "custom"

    private fun player(c: Context, file: File?, resourceName: String?, usage: Int, loop: Boolean): MediaPlayer {
        val output = MediaPlayer()
        try {
            output.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(usage).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            if (file != null) {
                require(file.isFile && file.length() > 0) { "Missing imported audio" }
                output.setDataSource(file.absolutePath)
            } else {
                require(!resourceName.isNullOrBlank()) { "Missing resource name" }
                val id = c.resources.getIdentifier(resourceName, "raw", c.packageName)
                require(id != 0) { "Missing built-in audio: $resourceName" }
                c.resources.openRawResourceFd(id)?.use {
                    output.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                } ?: error("Unable to open $resourceName")
            }
            output.isLooping = loop
            output.prepare()
            require(output.duration > 0) { "Audio decoder returned empty audio" }
            return output
        } catch (ex: Exception) {
            runCatching { output.release() }
            throw ex
        }
    }

    fun open(c: Context, value: String, usage: Int, loop: Boolean, alarmId: Int = -1): MediaPlayer {
        val sound = normalizeMeow(value)
        require(isMeow(sound)) { "Unknown meow preset: $sound" }
        return when (sound) {
            "custom" -> player(c, customFile(c), null, usage, loop)
            "alarm_custom" -> {
                require(alarmId >= 100) { "A saved alarm is required for personal sound" }
                player(c, alarmFile(c, alarmId), null, usage, loop)
            }
            else -> player(c, null, sound, usage, loop)
        }
    }

    fun openPurr(c: Context, sound: String, usage: Int, loop: Boolean): MediaPlayer {
        require(isPurr(sound)) { "Unknown purr preset: $sound" }
        return if (sound == "custom") player(c, customPurrFile(c), null, usage, loop)
        else player(c, null, sound, usage, loop)
    }

    /** Verify the Android decoder can play the imported file before replacing any previous import. */
    fun validateImportedFile(file: File) {
        require(file.isFile && file.length() > 0) { "فایل خالی است" }
        val check = MediaPlayer()
        try {
            check.setDataSource(file.absolutePath)
            check.prepare()
            require(check.duration > 0) { "فرمت صوتی پشتیبانی نمی‌شود" }
        } finally {
            runCatching { check.release() }
        }
    }
}
