# 🐱 Meow Meow Alarm

**A cozy, interactive alarm clock for Android. Wake up to a cat's meow—and help your sleepy companion settle down.**

[🇮🇷 فارسی](Readme-Fa.md) · **🇬🇧 English** · [Project home](README.md)

[![Build APK](https://github.com/Arto0n/Meow-Meow-Alarm/actions/workflows/build-apk.yml/badge.svg)](https://github.com/Arto0n/Meow-Meow-Alarm/actions/workflows/build-apk.yml)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white)
![Personal project](https://img.shields.io/badge/project-personal%20%26%20in%20development-F0AD4E)

> **Development project:** Please verify actual scheduled alarms on your specific phone before relying on Meow Meow Alarm as your only wake-up alarm. Background and lock-screen restrictions vary across Android devices.

## 🎬 Preview

![Settings and cozy 2D interface preview](docs/assets/settings-preview.png)

*Development preview of the original cozy, hand-drawn 2D app; the appearance on your Android phone may differ.*

## ✨ What it does

Meow Meow Alarm combines an everyday Android alarm clock with a playful, interactive cat scene. When an alarm goes off, your cat calls for attention. The user can interact with the cat to snooze or finish the wake-up routine.

- **Multiple alarms:** Create, edit, delete, enable/disable, label, and repeat alarms on selected days.
- **Original cozy 2D design:** An interactive room with a cat and a human character—not pixel art or 3D.
- **Six built-in meows:** `Meow 1` through `Meow 6`, with preview playback.
- **Global default or per-alarm sound:** Alarms use the global meow unless an individual override is enabled. User-imported meows are supported.
- **Three built-in purrs:** `Purr 1` through `Purr 3`, plus a custom purr option and previews.
- **Make the cat yours:** Choose a cat appearance and give your cat a name, which appears in interface messages.
- **Interactive alarm:** Petting, snooze, feeding, and the existing character animations.
- **Adjustable interactions:** Settings for the timing of cat-related actions and the human's routine.
- **Lock-screen ringing:** Native Android audio and a full-screen alarm notification where Android permits it.
- **First-run permissions:** An onboarding helper for notifications, exact alarms, and full-screen alarm access; device-specific battery settings may still need manual adjustment.
- **Offline:** Bundled alarms and purrs do not need an internet connection.

The **5-second test** and other developer-facing controls are intentionally retained while the app is still being tested.

## 📱 Download and install the APK

No ready-made Play Store release is provided here. The project builds a **debug APK** via GitHub Actions:

1. Open [Build APK workflow](https://github.com/Arto0n/Meow-Meow-Alarm/actions/workflows/build-apk.yml).
2. Open the latest successful run. If needed, select **Run workflow** on the `main` branch.
3. Under **Artifacts**, download **`MeowAlarm-debug`**. GitHub may require you to sign in.
4. Extract the downloaded ZIP and transfer **`app-debug.apk`** to your Android phone.
5. Install the APK from a source you trust, then complete the first-launch permission checklist.
6. Test the 5-second preview **and** a scheduled alarm with the phone locked and the app closed.

> **Debug signing:** Android may reject an in-place update if the APK was signed with a different debug key. Uninstalling the old app can erase saved alarms and settings, so keep a record of them first.

## 🔔 Android permissions and reliability

Alarms depend on OS and manufacturer policies. Meow Meow Alarm may need access to:

| Capability | Purpose |
| --- | --- |
| Exact alarms / Alarms & reminders | Schedule an alarm for the chosen time |
| Notifications | Show an alarm notification while ringing |
| Full-screen notifications | Open the interactive alarm screen over the lock screen where allowed |
| Background operation | Keep alarm playback working outside the visible app |
| Battery settings / Autostart | Help prevent some phones, particularly Xiaomi/MIUI devices, from suppressing alarm behavior |

Android does **not** allow apps to force users to accept special permissions. The setup screen guides users to the appropriate OS settings instead. For specific versions and common issues, see [Android permissions](docs/ANDROID_PERMISSIONS.md) and [troubleshooting](docs/TROUBLESHOOTING.md).

## 🧰 Requirements

| Component | Version |
| --- | --- |
| Android runtime | Android 8.0+ / API 26+ |
| Compile / target SDK | Android API 34 |
| JDK | 17 |
| Android SDK platform / build tools | 34 / 34.0.0 |
| Gradle | 8.9 (CI) |
| Android Gradle Plugin | 8.5.2 |
| Kotlin Gradle Plugin | 1.9.24 |
| Optional checks | Python 3, Node.js |

Full details: [requirements](docs/REQUIREMENTS.md) and [development guide](docs/DEVELOPMENT.md).

## 🛠️ Build from source

Clone the project (after the planned GitHub repository rename):

```bash
git clone https://github.com/Arto0n/Meow-Meow-Alarm.git
cd Meow-Meow-Alarm
```

Until the rename has been completed, use [`Arto0n/MeowAlarm3`](https://github.com/Arto0n/MeowAlarm3).

With JDK 17, SDK 34, Build Tools 34.0.0 and Gradle 8.9 installed:

```bash
gradle --no-daemon clean assembleDebug
```

Result: `app/build/outputs/apk/debug/app-debug.apk`.

The repository may not contain a Gradle wrapper (`gradlew`). GitHub Actions is therefore the simplest build method. To run the basic source checks:

```bash
python tests/verify_sources.py
```

## 📁 Repository structure

```text
.github/workflows/build-apk.yml   Automatic debug APK build
app/                              Kotlin Android app, WebView UI and sounds
tests/                            Source and interface checks
docs/                             Requirements, permissions and troubleshooting
Readme-Fa.md                      Persian documentation
Readme-En.md                      English documentation
README.md                         Language selection and short overview
build.gradle.kts                  Android build plugin configuration
settings.gradle.kts               Gradle project configuration
```

**Tech stack:** Kotlin + Android SDK (alarm scheduling, audio service, notifications and permissions); HTML/CSS/JavaScript inside WebView (original cozy 2D interface); Gradle and GitHub Actions for builds.

## 🗺️ Project status

Meow Meow Alarm is a **personal project under active development**. Real-device reliability—especially full-screen alarm presentation and manufacturer-specific restrictions—matters more than experimental visual redesigns. The project intentionally retains its original hand-drawn 2D style. The APK is not a certified alarm-clock replacement and is not a production-signed Play Store release.

## 🔒 License, media and privacy

No open-source license has been selected for the repository. Being publicly accessible on GitHub does not automatically grant reuse or redistribution rights. Cat-meow, purr and visual files may have separate rights that should be verified before redistribution or commercial use. Read [source and media rights](docs/RIGHTS.md).

The core app is intended to work offline without accounts. See the source for any permissions or local persistence behavior; this document is not a formal independent privacy audit.

---

*Made for sleepy humans and very demanding cats.* 🐾

[فارسی](Readme-Fa.md) · [Back to README](README.md)
