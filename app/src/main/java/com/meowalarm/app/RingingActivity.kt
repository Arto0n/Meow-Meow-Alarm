package com.meowalarm.app

import android.os.Build
import android.os.Bundle
import android.view.WindowManager

/** Dedicated lock-screen-capable entry point for an actual ringing alarm.
 *  It reuses MainActivity's existing WebView scene, so petting and feeding
 *  work exactly as they do when MeowAlarm is opened normally.
 */
class RingingActivity : MainActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // These flags must be installed before the WebView begins loading.
        if (Ring.active) {
            if (Build.VERSION.SDK_INT >= 27) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        super.onCreate(savedInstanceState)
    }
}
