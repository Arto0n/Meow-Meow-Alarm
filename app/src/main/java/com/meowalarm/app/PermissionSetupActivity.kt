package com.meowalarm.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * One-time, user-controlled alarm permission walkthrough.
 * Android special-access grants cannot be forced. Each step opens the system
 * permission UI, and onResume checks the *real* OS state again.
 * This activity MUST NOT interrupt an already ringing alarm.
 */
class PermissionSetupActivity : Activity() {
    companion object {
        const val DONE_KEY = "permission_setup_v1_done"
        private const val REQUEST_NOTIFICATIONS = 7101
        private const val CHANNEL = "meow_fullscreen_alarm_v2"
    }

    private val background = Color.rgb(51, 43, 64)
    private val cream = Color.rgb(255, 241, 217)
    private val ink = Color.rgb(56, 43, 59)
    private val mauve = Color.rgb(115, 79, 103)
    private val green = Color.rgb(62, 113, 81)
    private lateinit var screen: ScrollView
    private lateinit var column: LinearLayout
    private var lastExactAllowed = false
    private var ready = false

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        if (Ring.active) { finish(); return }
        window.statusBarColor = background
        window.navigationBarColor = background
        window.decorView.systemUiVisibility = 0
        lastExactAllowed = Alarms.canSchedule(this)
        ensureAlarmChannel()
        screen = ScrollView(this).apply {
            setBackgroundColor(background)
            isFillViewport = true
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(24), dp(18), dp(28))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        screen.addView(column)
        setContentView(screen)
        ready = true
        draw()
    }

    override fun onResume() {
        super.onResume()
        if (Ring.active) { finish(); return }
        val exactNow = Alarms.canSchedule(this)
        if (!lastExactAllowed && exactNow) {
            // Re-enable alarms that were saved before exact access was granted.
            runCatching { Alarms.rescheduleAll(this) }
                .onFailure { Log.e("MeowAlarm", "Could not restore pending alarms", it) }
        }
        lastExactAllowed = exactNow
        if (ready) draw()
    }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density + 0.5f).toInt()

    private fun rounded(color: Int, radius: Int = 16): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    private fun text(value: String, size: Float = 15f, color: Int = ink, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.RIGHT
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            setLineSpacing(dp(3).toFloat(), 1.06f)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

    private fun button(label: String, color: Int, onClick: () -> Unit) =
        Button(this).apply {
            text = label
            textSize = 14f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(color, 10)
            setOnClickListener { onClick() }
        }

    private fun section(title: String, description: String, ok: Boolean?, optional: Boolean = false,
                        actionText: String? = null, action: (() -> Unit)? = null) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(cream)
        }
        val heading = when (ok) {
            true -> "✓  $title — فعال"
            false -> "●  $title — نیاز به فعال‌سازی"
            null -> "◌  $title — بررسی دستی"
        }
        box.addView(text(heading, 16f, if (ok == true) green else ink, true))
        box.addView(text(description, 13f, mauve).apply {
            setPadding(0, dp(7), 0, dp(5))
        })
        if (optional) box.addView(text("اختیاری؛ برای بعضی گوشی‌ها توصیه می‌شود.", 12f, mauve))
        if (actionText != null && action != null && ok != true) {
            box.addView(button(actionText, Color.rgb(160, 84, 108), action).apply {
                val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
                p.topMargin = dp(8)
                layoutParams = p
            })
        }
        val outer = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        outer.bottomMargin = dp(12)
        column.addView(box, outer)
    }

    private fun ensureAlarmChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Meow alarm",
            NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            description = "MeowAlarm ringing notification and lock-screen display"
        })
    }

    private fun notificationsAllowed(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return false
        val nm = getSystemService(NotificationManager::class.java)
        if (!nm.areNotificationsEnabled()) return false
        val channel = nm.getNotificationChannel(CHANNEL)
        return channel == null || channel.importance >= NotificationManager.IMPORTANCE_HIGH
    }

    private fun fullscreenAllowed(): Boolean = Build.VERSION.SDK_INT < 34 ||
        getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    private fun batteryUnrestricted(): Boolean =
        getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

    private fun goTo(intent: Intent, alternative: Intent = appDetails()) {
        try { startActivity(intent) }
        catch (error: Exception) {
            Log.w("MeowAlarm", "Permission settings unavailable: ${intent.action}", error)
            runCatching { startActivity(alternative) }
        }
    }

    private fun appDetails(): Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:$packageName"))

    private fun openNotificationSettings() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !Alarms.prefs(this).getBoolean("notification_prompt_already_shown", false)) {
            // A denied/revoked permission is handled by app Settings on later taps;
            // requesting the same OS dialog in a loop does not help the user.
            Alarms.prefs(this).edit().putBoolean("notification_prompt_already_shown", true).apply()
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
            return
        }
        val nm = getSystemService(NotificationManager::class.java)
        val channel = nm.getNotificationChannel(CHANNEL)
        val intent = if (channel != null && channel.importance < NotificationManager.IMPORTANCE_HIGH) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL)
        } else Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        goTo(intent)
    }

    private fun openExactSettings() {
        if (Build.VERSION.SDK_INT >= 31 && !Alarms.canSchedule(this))
            goTo(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:$packageName")))
    }

    private fun openFullScreenSettings() {
        if (Build.VERSION.SDK_INT >= 34 && !fullscreenAllowed())
            goTo(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                Uri.parse("package:$packageName")))
    }

    private fun finishSetup() {
        Alarms.prefs(this).edit().putBoolean(DONE_KEY, true).apply()
        finish()
    }

    private fun draw() {
        column.removeAllViews()
        column.addView(text("🐱 میو آلارم", 25f, Color.WHITE, true))
        column.addView(text("راه‌اندازی آلارم روی صفحهٔ قفل", 19f, Color.WHITE, true).apply {
            setPadding(0, dp(8), 0, dp(4))
        })
        column.addView(text(
            "این مرحله فقط یک‌بار نشان داده می‌شود. برای اینکه هنگام قفل بودن گوشی، صدای میو و صفحهٔ نوازش با هم ظاهر شوند، مجوزهای زیر را بررسی کن. تأیید نهایی هر مجوز با خودت است.",
            13f, Color.rgb(255, 228, 215)).apply { setPadding(0, 0, 0, dp(16)) })

        val notif = notificationsAllowed()
        val exact = Alarms.canSchedule(this)
        val fullscreen = fullscreenAllowed()
        section("اعلان‌های آلارم", "برای نشان‌دادن اعلان مهم، صدای آلارم و بازکردن صفحهٔ نوازش ضروری است.", notif,
            actionText = "فعال‌سازی اعلان‌ها", action = { openNotificationSettings() })
        section("آلارم دقیق", "برای زنگ‌خوردن در همان ساعت تنظیم‌شده ضروری است. روی بعضی نسخه‌های اندروید از قبل فعال است.", exact,
            actionText = "فعال‌سازی آلارم دقیق", action = { openExactSettings() })
        section("نمایش تمام‌صفحه روی قفل", "برای بازشدن مستقیم گربه وقتی نمایشگر خاموش یا قفل است. در اندروید ۱۴ و جدیدتر قابل مدیریت است.", fullscreen,
            actionText = "مجوز نمایش تمام‌صفحه", action = { openFullScreenSettings() })
        section("محدودیت باتری", "اگر گوشی برنامه را در پس‌زمینه متوقف می‌کند، از تنظیمات باتری برای MeowAlarm گزینهٔ بدون محدودیت را انتخاب کن.",
            batteryUnrestricted(), optional = true,
            actionText = "تنظیمات باتری", action = {
                goTo(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            })
        val isXiaomi = Build.MANUFACTURER.contains("xiaomi", ignoreCase = true) ||
            Build.BRAND.contains("redmi", ignoreCase = true) ||
            Build.BRAND.contains("poco", ignoreCase = true)
        if (isXiaomi) section("شیائومی / ردمی / پوکو",
            "در تنظیمات برنامه، Autostart را فعال کن؛ Battery saver را روی No restrictions بگذار؛ و اجازهٔ نمایش اعلان روی Lock screen را فعال کن. این مجوزها دکمهٔ استاندارد اندروید ندارند.",
            null, optional = true, actionText = "بازکردن تنظیمات برنامه", action = { goTo(appDetails()) })

        val allRequired = notif && exact && fullscreen
        val guide = if (allRequired) "✓ مجوزهای اصلی آماده‌اند!" else
            "برای کارکرد کامل روی صفحهٔ قفل، مجوزهای اصلیِ غیرفعال را فعال کن."
        column.addView(text(guide, 15f, if (allRequired) Color.rgb(191, 247, 210) else Color.WHITE, true).apply {
            setPadding(0, dp(6), 0, dp(12))
        })
        if (!allRequired) column.addView(button("فعال‌سازی مورد ضروری بعدی", green) {
            when {
                !notif -> openNotificationSettings()
                !exact -> openExactSettings()
                !fullscreen -> openFullScreenSettings()
            }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT))
        column.addView(button("بررسی دوباره", mauve) { draw() }.apply {
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            p.topMargin = dp(10)
            layoutParams = p
        })
        if (allRequired) column.addView(button("✓ ورود به برنامه", green) { finishSetup() }.apply {
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            p.topMargin = dp(10)
            layoutParams = p
        })
        else column.addView(button("فعلاً ورود بدون همهٔ مجوزها", mauve) {
            AlertDialog.Builder(this)
                .setTitle("ممکن است آلارم کامل کار نکند")
                .setMessage("بدون مجوزهای اصلی، ممکن است آلارم دیر اجرا شود یا صفحهٔ نوازش هنگام قفل بودن نمایش داده نشود. می‌خواهی فعلاً ادامه بدهی؟")
                .setNegativeButton("بازگشت", null)
                .setPositiveButton("ادامه", { _, _ -> finishSetup() })
                .show()
        }.apply {
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            p.topMargin = dp(10)
            layoutParams = p
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS && ready) draw()
    }
}
