# Alarm-related Android permissions

Meow Meow Alarm asks only for capabilities that support alarms and their presentation.

| Capability | Why it matters | Typical platform behavior |
| --- | --- | --- |
| Exact alarms | Deliver wake-up alarms at the selected time | Android 12+ applies special rules; `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` depend on OS version and app role |
| Notifications | Show an ongoing ringing alarm with an action to open it | Android 13+ can require user permission (`POST_NOTIFICATIONS`); exemption behavior can vary |
| Full-screen intent | Display the interaction screen over the lock screen when allowed | Android 14+ may require a separate user setting (`USE_FULL_SCREEN_INTENT`) |
| Foreground service | Keep the alarm sound working when the UI is not in front | Android versions apply service-type and launch restrictions |
| Battery optimization / Autostart | Help prevent some device vendors from stopping background work | Often manufacturer-specific; user controls the setting |
| Boot completed | Re-schedule saved alarms after phone restarts | `RECEIVE_BOOT_COMPLETED` |

**Android permissions cannot be forcibly accepted by an app.** The correct behavior is to request them at an appropriate time, explain the impact of denial, and guide the user to Android settings when needed.

The app's permission walkthrough must never obscure an alarm that is currently ringing.

## Device setup checklist

1. Open the app and complete its first-run alarm permission setup.
2. Enable alarms/reminders access where your phone provides that control.
3. Enable notifications and full-screen alarm behavior where available.
4. On Xiaomi or other aggressive battery-management devices, review battery restriction and Autostart settings.
5. Create an alarm a couple of minutes ahead, close the app, lock the screen and verify you can dismiss it without manually opening the app.

See [troubleshooting](TROUBLESHOOTING.md).
