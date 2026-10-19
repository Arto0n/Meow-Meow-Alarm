# Requirements & supported devices

## Android runtime

- **Minimum:** Android 8.0 (API 26), as configured by `minSdk = 26`.
- **Compile and target:** API 34 (Android 14) in the latest locally available project build files.
- Alarm functions depend on Android allowing precise scheduling, notifications (on applicable versions), lock-screen/full-screen alarm presentation, and background alarm execution. The operating system or vendor may restrict these functions.
- Xiaomi/MIUI, Samsung/One UI, and other Android distributions require real-device tests. A single APK is intended for supported Android devices, but identical behavior on all devices is not guaranteed.
- No internet connection or user account is required for bundled alarm sounds.

## Development environment

- JDK **17**
- Android SDK Platform **34**
- Android Build Tools **34.0.0**
- Android Gradle Plugin **8.5.2**
- Kotlin Android Gradle Plugin **1.9.24**
- Gradle **8.9** (used by the CI workflow; there may not be a Gradle wrapper in the repository)
- Python **3** for provided tests
- Node.js for the JavaScript syntax portion of source checks
- Git for change tracking

## Functional requirements

- Add, edit, delete, enable/disable and repeat multiple alarms.
- Save an optional sound override for each alarm; otherwise use the global default meow.
- Support imported custom meow and purr audio in the application's private storage.
- Provide selectable built-in meows and purrs, a cat name and cat appearance.
- Trigger alarm playback with native Android components, independent of whether the WebView scene is currently visible.
- Provide a reliable way to stop/dismiss the alarm even if an animation fails.
- Guide the user through platform permissions at first run without requesting unrelated permissions or interrupting a ringing alarm.

## Explicit limitations

- The shipped APK is built as a **debug** variant through GitHub Actions, not a production-signed release.
- No cloud synchronization or login is provided.
- These requirements describe project intent and existing implementations; they are not a claim of certification or successful testing on every phone.
