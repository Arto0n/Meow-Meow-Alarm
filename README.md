# 🐱 Meow Meow Alarm

**A cozy, interactive Android alarm clock — wake up to a cat's meow, then help it settle down.**

**آلارم میومیو** — یک آلارم تعاملی با گربهٔ دوست‌داشتنی، صدای میو و نوازش.

[![Build APK](https://github.com/Arto0n/Meow-Meow-Alarm/actions/workflows/build-apk.yml/badge.svg)](https://github.com/Arto0n/Meow-Meow-Alarm/actions/workflows/build-apk.yml)
![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white)
![Status](https://img.shields.io/badge/status-personal%20project-orange)

> **Personal project · Work in progress.** This is an Android alarm app, not an officially certified alarm replacement. Test it on your phone before relying on it to wake you up.

## Preview

![Meow Meow Alarm settings and cozy UI preview](docs/assets/settings-preview.png)

*Interface preview from a development browser build. Layout may differ slightly on Android.*

## Features

- **Multiple alarms:** create, edit, enable/disable, repeat, and delete individual alarms.
- **Interactive wake-up:** a cozy hand-drawn 2D room with a cat and human character; petting, snoozing, and feeding interactions.
- **Meow sound library:** six built-in meows (Meow 1–6), plus an option to import your own audio.
- **Default + per-alarm sound:** choose a global meow; optionally override it for a specific alarm.
- **Purring sounds:** three built-in purrs (Purr 1–3), plus custom purr import.
- **Personalize your cat:** set a name and choose a color/appearance.
- **Alarm on the lock screen:** Android foreground alarm playback and full-screen alarm notification when the operating system permits it.
- **First-run permission guide:** helps configure the permissions needed for reliable alarm delivery.
- **Offline alarm audio:** bundled sounds don't require internet access to ring.

## Install / download

1. Open [**GitHub Actions — Build APK**](https://github.com/Arto0n/Meow-Meow-Alarm/actions/workflows/build-apk.yml).
2. Open the most recent **successful** build, or use **Run workflow** on `main`.
3. Download the `MeowAlarm-debug` **artifact** from the run page (GitHub sign-in may be required).
4. Extract the artifact ZIP and install `app-debug.apk` on Android.
5. Follow the in-app alarm/notification permission setup. Test both the 5-second preview and a scheduled alarm with the screen locked.

> This workflow produces a **debug APK**. It's not a Play Store release; debug signing keys may differ across GitHub runs. If Android refuses an update because of a signature mismatch, back up your alarm settings before uninstalling the previous APK.

## Requirements

| Item | Requirement |
| --- | --- |
| Android | Android 8.0 / API 26 or newer (`minSdk 26`) |
| App SDK target | Android 14 / API 34 (project configuration) |
| For local builds | JDK 17, Android SDK Platform 34, Build Tools 34.0.0, Gradle 8.9 |
| Android build plugins | AGP 8.5.2, Kotlin 1.9.24 |
| For source checks | Python 3 and Node.js (for JavaScript syntax tests) |

See [requirements & compatibility](docs/REQUIREMENTS.md) and [developer build guide](docs/DEVELOPMENT.md).

## Technology

- **Kotlin** — Android alarm scheduling, notification/foreground service, storage, and permissions.
- **HTML / CSS / JavaScript in Android WebView** — cozy 2D visuals, controls, and interactions.
- **Gradle + GitHub Actions** — debug APK builds.
- **Local storage and local audio files** — no account required for the core alarm experience.

## Repository layout

```text
.github/workflows/build-apk.yml   GitHub Actions debug APK workflow
app/                              Android application, UI and sounds
tests/                            Source checks and interface tests
docs/                             Requirements, setup, troubleshooting
build.gradle.kts                  Android plugin versions
settings.gradle.kts               Gradle project settings
gradle.properties                  Build settings
```

The optional `build-termux.sh` and `setup-termux.sh` scripts are retained for the legacy Termux build path. See [Termux notes](docs/TERMUX.md).

## Permissions & phone compatibility

Android 12+, Android 13+, and Android 14+ have different restrictions for exact alarms, notifications, and full-screen intent access. Xiaomi/MIUI devices may also restrict background and autostart behavior. Permission grants are **user-controlled**, not forced by the app. See [Android permissions](docs/ANDROID_PERMISSIONS.md) and [troubleshooting](docs/TROUBLESHOOTING.md).

## Status & planned work

This is a personal project under active iteration. Priorities: keep alarm delivery reliable on real devices, polish the original non-pixel 2D UI, and improve sound selection. Test controls may remain in development versions.

## Source and media rights

Source and media rights are **not automatically granted** merely because this repository is public. No open-source license has been selected yet. Several audio files were supplied for this personal project; redistribution rights should be confirmed before issuing an open-source or public commercial release. See [rights notes](docs/RIGHTS.md).

---

Made with care for sleepy humans and demanding cats. 🐾
