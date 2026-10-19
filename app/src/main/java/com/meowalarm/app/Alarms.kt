package com.meowalarm.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.net.Uri
import android.webkit.JavascriptInterface
import android.util.Log
import java.util.Calendar
import org.json.JSONArray
import org.json.JSONObject

/** UI can observe ringing, but the alarm must not depend on a live WebView. */
object Ring {
    @Volatile var active = false
    @Volatile var listener: (() -> Unit)? = null
}

data class AlarmEntry(
    val id: Int, val hour: Int, val minute: Int, val enabled: Boolean,
    val days: Int, val label: String, val sound: String, val soundOverride: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("h", hour).put("m", minute)
        .put("on", enabled).put("days", days)
        .put("label", label).put("sound", sound).put("soundOverride", soundOverride)

    companion object {
        fun fromJson(o: JSONObject) = AlarmEntry(
            o.optInt("id", 100).coerceAtLeast(100),
            o.optInt("h", 7).coerceIn(0, 23),
            o.optInt("m", 0).coerceIn(0, 59),
            o.optBoolean("on", true),
            o.optInt("days", 127).coerceIn(0, 127),
            o.optString("label", "Wake up").take(50),
            MeowAudio.normalizeMeow(o.optString("sound", "meow_1")).takeIf { MeowAudio.isMeow(it) } ?: "meow_1",
            // Old alarm entries contained a sound even when there was no override toggle.
            // Only keep legacy imported sounds as specific to that alarm.
            if (o.has("soundOverride")) o.optBoolean("soundOverride", false)
            else o.optString("sound") == "custom"
        )
    }
}

object Alarms {
    private const val PREFS = "meow"
    private const val KEY_ALARMS = "alarms_v2"
    private const val SNOOZE_ID = 2
    private const val FIRST_ID = 100
    fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun read(c: Context): MutableList<AlarmEntry> {
        val p = prefs(c)
        val raw = p.getString(KEY_ALARMS, null)
        if (raw == null) {
            // Preserve existing users' single alarm during v2 migration.
            val old = AlarmEntry(FIRST_ID, p.getInt("h", 7).coerceIn(0, 23),
                p.getInt("m", 0).coerceIn(0, 59), p.getBoolean("on", false),
                127, "Wake up", "meow_1")
            write(c, listOf(old))
            return mutableListOf(old)
        }
        return try {
            val arr = JSONArray(raw)
            val legacyGlobal = MeowAudio.normalizeMeow(p.getString("default_meow_sound", "real") ?: "real")
            MutableList(arr.length()) {
                val json = arr.getJSONObject(it)
                val entry = AlarmEntry.fromJson(json)
                // Preserve old per-alarm selections when they differ from the old global default.
                if (!json.has("soundOverride"))
                    entry.copy(soundOverride = entry.sound == "custom" || entry.sound != legacyGlobal)
                else entry
            }.distinctBy { it.id }.toMutableList()
        } catch (ex: Exception) {
            Log.e("MeowAlarm", "Invalid stored alarm list", ex)
            mutableListOf()
        }
    }

    @Synchronized
    fun write(c: Context, entries: List<AlarmEntry>) {
        val array = JSONArray()
        entries.forEach { array.put(it.toJson()) }
        prefs(c).edit().putString(KEY_ALARMS, array.toString()).apply()
    }

    private fun alarmIntent(c: Context, id: Int): PendingIntent {
        val intent = Intent(c, AlarmService::class.java)
            .setAction(AlarmService.ACTION_RING).putExtra("id", id)
        return PendingIntent.getForegroundService(c, id, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun showIntent(c: Context): PendingIntent = PendingIntent.getActivity(
        c, 90,
        Intent(c, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun canSchedule(c: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        return c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    fun setAt(c: Context, at: Long, id: Int): Boolean {
        if (!canSchedule(c)) {
            Log.w("MeowAlarm", "Exact alarm permission unavailable: id=$id")
            return false
        }
        return try {
            val am = c.getSystemService(AlarmManager::class.java)
            am.setAlarmClock(AlarmManager.AlarmClockInfo(at, showIntent(c)), alarmIntent(c, id))
            true
        } catch (ex: Exception) {
            Log.e("MeowAlarm", "Could not schedule alarm $id", ex)
            false
        }
    }

    fun cancel(c: Context, id: Int) {
        runCatching { c.getSystemService(AlarmManager::class.java).cancel(alarmIntent(c, id)) }
            .onFailure { Log.e("MeowAlarm", "Cannot cancel alarm $id", it) }
    }

    fun schedule(c: Context, entry: AlarmEntry): Boolean {
        cancel(c, entry.id)
        if (!entry.enabled) return true
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, entry.hour)
            set(Calendar.MINUTE, entry.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Days mask uses Calendar.DAY_OF_WEEK: Sunday bit 0 ... Saturday bit 6.
        for (offset in 0..7) {
            val day = cal.get(Calendar.DAY_OF_WEEK)
            if (cal.timeInMillis > now + 1000 && (entry.days == 0 || entry.days and (1 shl (day - 1)) != 0)) {
                return setAt(c, cal.timeInMillis, entry.id)
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return false
    }

    fun rescheduleAll(c: Context) = read(c).forEach { schedule(c, it) }

    @Synchronized
    fun save(c: Context, source: JSONObject): AlarmEntry {
        val entries = read(c)
        val existing = source.optInt("id", -1)
        val id = if (existing >= FIRST_ID && entries.any { it.id == existing }) existing
                 else (entries.maxOfOrNull { it.id } ?: FIRST_ID - 1) + 1
        val entry = AlarmEntry.fromJson(JSONObject(source.toString()).put("id", id))
        val index = entries.indexOfFirst { it.id == id }
        if (index >= 0) entries[index] = entry else entries.add(entry)
        write(c, entries)
        schedule(c, entry)
        return entry
    }

    @Synchronized
    fun remove(c: Context, id: Int): Boolean {
        val entries = read(c)
        if (!entries.removeAll { it.id == id }) return false
        write(c, entries)
        cancel(c, id)
        runCatching { MeowAudio.alarmFile(c, id).delete() }
        // If this alarm is waiting for a snooze, do not let it ring after deletion.
        if (prefs(c).getInt("active_alarm_id", -1) == id) cancelSnooze(c)
        return true
    }

    fun onTriggered(c: Context, alarmId: Int) {
        val p = prefs(c)
        val id = if (alarmId == SNOOZE_ID) p.getInt("active_alarm_id", FIRST_ID) else alarmId
        p.edit().putInt("active_alarm_id", id).apply()
        if (alarmId >= FIRST_ID) {
            val entry = read(c).firstOrNull { it.id == alarmId } ?: return
            if (entry.days == 0) {
                val updated = read(c).map { if (it.id == alarmId) it.copy(enabled = false) else it }
                write(c, updated)
            } else schedule(c, entry)
        }
    }

    fun effectiveSound(c: Context, entry: AlarmEntry?): String {
        val fallback = MeowAudio.normalizeMeow(prefs(c).getString("default_meow_sound", "meow_1") ?: "meow_1")
        val global = fallback.takeIf { MeowAudio.isMeow(it) && it != "alarm_custom" } ?: "meow_1"
        return if (entry?.soundOverride == true && MeowAudio.isMeow(entry.sound)) entry.sound else global
    }

    fun active(c: Context): AlarmEntry? {
        val id = prefs(c).getInt("active_alarm_id", FIRST_ID)
        return read(c).firstOrNull { it.id == id }
    }

    @Synchronized
    fun setAlarmImportedAudio(c: Context, id: Int) {
        val entries = read(c)
        val index = entries.indexOfFirst { it.id == id }
        if (index >= 0) {
            entries[index] = entries[index].copy(sound = "alarm_custom", soundOverride = true)
            write(c, entries)
        }
    }

    fun cancelSnooze(c: Context) = cancel(c, SNOOZE_ID)
    fun scheduleSnooze(c: Context, seconds: Int): Boolean {
        cancelSnooze(c)
        return setAt(c, System.currentTimeMillis() + seconds.coerceIn(1, 3600) * 1000L, SNOOZE_ID)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        when (i.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> Alarms.rescheduleAll(c)
        }
    }
}

class Bridge(private val c: Context) {
    @JavascriptInterface fun getAlarms(): String {
        val result = JSONArray()
        Alarms.read(c).forEach { result.put(it.toJson()) }
        return result.toString()
    }

    @JavascriptInterface fun saveAlarm(json: String): String = try {
        Alarms.save(c, JSONObject(json)).toJson().toString()
    } catch (ex: Exception) {
        Log.e("MeowAlarm", "Cannot save alarm", ex)
        "{}"
    }

    @JavascriptInterface fun deleteAlarm(id: Int): Boolean = Alarms.remove(c, id)
    @JavascriptInterface fun getSettings(): String = Alarms.prefs(c).getString("scene_settings", "{}") ?: "{}"
    @JavascriptInterface fun setSettings(json: String) {
        val input = JSONObject(json)
        // Each time is limited so corrupt UI values never create huge delays.
        val bounds = mapOf("petSec" to (1..60), "snoozeSec" to (10..1800),
            "smokeDelaySec" to (0..300), "smokeDurationSec" to (1..120),
            "foodDelaySec" to (1..300), "feedSec" to (1..60))
        val output = JSONObject()
        for ((key, range) in bounds) if (input.has(key))
            output.put(key, input.optInt(key).coerceIn(range.first, range.last))
        Alarms.prefs(c).edit().putString("scene_settings", output.toString()).apply()
    }

    @JavascriptInterface fun openPermissionSetup() {
        (c as? MainActivity)?.let { a -> a.runOnUiThread { a.showPermissionSetup() } }
    }

    @JavascriptInterface fun canShowFullScreenAlarm(): Boolean {
        if (Build.VERSION.SDK_INT < 34) return true
        return c.getSystemService(android.app.NotificationManager::class.java).canUseFullScreenIntent()
    }

    @JavascriptInterface fun areAlarmNotificationsEnabled(): Boolean {
        val nm = c.getSystemService(android.app.NotificationManager::class.java)
        if (!nm.areNotificationsEnabled()) return false
        val channel = nm.getNotificationChannel("meow_fullscreen_alarm_v2")
        return channel == null || channel.importance >= android.app.NotificationManager.IMPORTANCE_HIGH
    }

    @JavascriptInterface fun openAlarmDisplaySettings() {
        (c as? Activity)?.runOnUiThread {
            try {
                val i = if (Build.VERSION.SDK_INT >= 34 && !canShowFullScreenAlarm()) {
                    Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:${c.packageName}"))
                } else {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, c.packageName)
                }
                c.startActivity(i)
            } catch (ex: Exception) {
                Log.e("MeowAlarm", "Unable to open alarm display settings", ex)
            }
        }
    }

    @JavascriptInterface fun canSetExactAlarms(): Boolean = Alarms.canSchedule(c)
    @JavascriptInterface fun requestExactAlarmPermission() {
        val a = c as? Activity ?: return
        if (Build.VERSION.SDK_INT >= 31 && !Alarms.canSchedule(c)) {
            a.runOnUiThread {
                runCatching {
                    a.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:${c.packageName}")))
                }.onFailure { Log.e("MeowAlarm", "Exact alarm settings unavailable", it) }
            }
        }
    }

    @JavascriptInterface fun pickTime(h: Int, m: Int) {
        (c as? MainActivity)?.let { activity ->
            activity.runOnUiThread { activity.showAlarmTimePicker(h, m) }
        }
    }

    @JavascriptInterface fun ringIn(sec: Int) = Alarms.scheduleSnooze(c, sec)
    @JavascriptInterface fun silence() { c.stopService(Intent(c, AlarmService::class.java)) }
    @JavascriptInterface fun dismiss() { silence(); Alarms.cancelSnooze(c) }
    @JavascriptInterface fun isRinging(): Boolean = Ring.active
    @JavascriptInterface fun getDefaultSound(): String = Alarms.effectiveSound(c, null)
    @JavascriptInterface fun setDefaultSound(sound: String) {
        if (MeowAudio.isMeow(sound) && sound != "alarm_custom") {
            Alarms.prefs(c).edit().putString("default_meow_sound", MeowAudio.normalizeMeow(sound)).apply()
        }
    }
    @JavascriptInterface fun getDefaultPurr(): String =
        (Alarms.prefs(c).getString("default_purr_sound", "purr_1") ?: "purr_1")
            .takeIf { MeowAudio.isPurr(it) } ?: "purr_1"
    @JavascriptInterface fun setDefaultPurr(sound: String) {
        if (MeowAudio.isPurr(sound)) Alarms.prefs(c).edit().putString("default_purr_sound", sound).apply()
    }
    @JavascriptInterface fun getCatName(): String =
        Alarms.prefs(c).getString("cat_name", "گربه") ?: "گربه"
    @JavascriptInterface fun setCatName(name: String) {
        Alarms.prefs(c).edit().putString("cat_name", name.trim().take(24).ifEmpty { "گربه" }).apply()
    }
    @JavascriptInterface fun getCatColor(): String =
        Alarms.prefs(c).getString("cat_color", "gray") ?: "gray"
    @JavascriptInterface fun setCatColor(value: String) {
        if (value in setOf("gray", "orange", "black", "white", "calico"))
            Alarms.prefs(c).edit().putString("cat_color", value).apply()
    }
    @JavascriptInterface fun hasCustomMeow(): Boolean =
        MeowAudio.customFile(c).isFile && MeowAudio.customFile(c).length() > 0
    @JavascriptInterface fun hasCustomPurr(): Boolean =
        MeowAudio.customPurrFile(c).isFile && MeowAudio.customPurrFile(c).length() > 0
    @JavascriptInterface fun hasAlarmMeow(id: Int): Boolean =
        id >= 100 && MeowAudio.alarmFile(c, id).let { it.isFile && it.length() > 0 }
    @JavascriptInterface fun chooseCustomMeow() {
        (c as? MainActivity)?.let { a -> a.runOnUiThread { a.selectCustomAudio("meow") } }
    }
    @JavascriptInterface fun chooseCustomPurr() {
        (c as? MainActivity)?.let { a -> a.runOnUiThread { a.selectCustomAudio("purr") } }
    }
    @JavascriptInterface fun chooseAlarmMeow(id: Int) {
        if (Alarms.read(c).any { it.id == id })
            (c as? MainActivity)?.let { a -> a.runOnUiThread { a.selectCustomAudio("alarm", id) } }
    }
    @JavascriptInterface fun previewMeow(type: String) {
        (c as? MainActivity)?.let { a -> a.runOnUiThread { a.previewAudio("meow", type) } }
    }
    @JavascriptInterface fun previewAlarmMeow(id: Int) {
        if (hasAlarmMeow(id)) (c as? MainActivity)?.let { a ->
            a.runOnUiThread { a.previewAudio("alarm", "alarm_custom", id) }
        }
    }
    @JavascriptInterface fun previewPurr(type: String) {
        (c as? MainActivity)?.let { a -> a.runOnUiThread { a.previewAudio("purr", type) } }
    }
    @JavascriptInterface fun startPurr() {
        (c as? MainActivity)?.let { a -> a.runOnUiThread { a.startPurr() } }
    }
    @JavascriptInterface fun stopPurr() {
        (c as? MainActivity)?.let { a -> a.runOnUiThread { a.stopPurr() } }
    }
}
