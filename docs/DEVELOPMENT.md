# Developer guide

## Checkout

```powershell
git clone https://github.com/Arto0n/Meow-Meow-Alarm.git
cd Meow-Meow-Alarm
```

Before the GitHub rename takes effect, use the old `MeowAlarm3` URL.

## Build via GitHub Actions (recommended)

1. Push a commit to `main`, or start the `Build MeowAlarm APK` workflow manually.
2. Wait for **Build debug APK** and **Upload APK artifact** to succeed.
3. Download artifact **MeowAlarm-debug** and extract **app-debug.apk**.

The build workflow uses Ubuntu, JDK 17, Android SDK 34, Build Tools 34.0.0 and Gradle 8.9.

## Local build

Install JDK 17, Android SDK Platform 34, Build Tools 34.0.0 and Gradle 8.9, then:

```powershell
gradle --no-daemon clean assembleDebug
```

APK path: `app/build/outputs/apk/debug/app-debug.apk`.

> The repository may not have `gradlew` / a Gradle wrapper. If `gradle` isn't installed, use GitHub Actions instead.

## Tests

```powershell
python tests/verify_sources.py
```

Some source tests use `node --check`; install Node.js if you run them locally. Additional tests live in `tests/` and may require browser automation dependencies; read each test before running it.

## Updating the project

- Keep changes scoped: native scheduling and playback live under `app/src/main/java/com/meowalarm/app/`.
- The WebView UI is in `app/src/main/assets/index.html`.
- Bundled audio lives in `app/src/main/res/raw/`.
- Preserve `.github/workflows/build-apk.yml` and core build files.
- Test both a foreground preview and a real scheduled alarm while the screen is locked.
- Use ordinary commits and `git pull --ff-only` or a carefully reviewed rebase; don't force-push for routine updates.

## Versioning & signing

`app/build.gradle.kts` controls `versionCode` and `versionName`.

Debug APKs are not a substitute for a consistent production signing key. If the project moves beyond personal use, establish a release signing process and decide on an explicit media/code license.
