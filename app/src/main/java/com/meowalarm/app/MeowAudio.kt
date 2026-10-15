package com.meowalarm.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.File

/**
 * One audio route for previews and alarms. No AudioTrack synthesizer: the cute
 * sound is a local WAV file, and custom sound imports are validated up front.
 */
object MeowAudio {
    const val CUSTOM_FILENAME = "custom_meow_audio"

    fun customFile(c: Context): File = c.getFileStreamPath(CUSTOM_FILENAME)

    fun open(c: Context, sound: String, usage: Int, loop: Boolean): MediaPlayer {
        require(sound in setOf("real", "cute", "custom")) { "Unknown sound: $sound" }
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(
                AudioAttributes.Builder().setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            )
            when (sound) {
                "custom" -> {
                    val custom = customFile(c)
                    require(custom.isFile && custom.length() > 0) { "Custom sound is not imported" }
                    custom.inputStream().use { player.setDataSource(it.fd) }
                }
                "cute", "real" -> {
                    val resourceId = if (sound == "cute") {
                        c.resources.getIdentifier("cute_cat_v7", "raw", c.packageName)
                            .takeIf { it != 0 } ?: R.raw.cute_cat_meow
                    } else c.resources.getIdentifier("real_cat_meow", "raw", c.packageName)
                    require(resourceId != 0) { "Missing bundled $sound sound" }
                    c.resources.openRawResourceFd(resourceId)?.use { descriptor ->
                        player.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                    } ?: error("Cannot open bundled $sound sound")
                }
            }
            player.isLooping = loop
            player.prepare()
            require(player.duration > 0) { "Audio decoder returned an empty sound" }
            return player
        } catch (failure: Exception) {
            runCatching { player.release() }
            throw failure
        }
    }

    /** Reject unreadable or unsupported files before showing an import-success message. */
    fun validateImportedFile(file: File) {
        require(file.isFile && file.length() > 0) { "The selected audio file is empty" }
        val check = MediaPlayer()
        try {
            file.inputStream().use { check.setDataSource(it.fd) }
            check.prepare()
            require(check.duration > 0) { "Unsupported or empty audio file" }
        } finally {
            runCatching { check.release() }
        }
    }
}
