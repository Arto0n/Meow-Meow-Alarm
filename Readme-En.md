# 🐱 Meow Meow Alarm — English Documentation

<div align="center">
<img src="./docs/images/preview-collage.png" alt="Meow Meow Alarm preview collage" width="100%" />
</div>

## What is Meow Meow Alarm?

**Meow Meow Alarm** is a cozy, interactive Android alarm app.
It combines a soft 2D art style, cat-themed audio, and a playful wake-up routine instead of a plain traditional alarm screen.

When the alarm rings, the app can show an interactive scene with the cat and the human character, making the user engage with the alarm instead of just hearing a sound.

---

## Core Idea

The app is built around three main experiences:

1. **Interactive wake-up scene**
2. **Flexible alarm management**
3. **Deep meow / purr / cat customization**

---

## Feature Highlights

### 1) Interactive alarm experience
- Cozy 2D room scene with the original cat and human character
- Interactive wake-up screen instead of a simple dismiss dialog
- Pet-the-cat interaction as part of the alarm flow
- Cat meows, purrs, and animated scene behavior
- Lock-screen / full-screen alarm behavior when permissions are enabled
- Screen wake-up flow designed to open the interactive alarm scene directly

### 2) Alarm management
- Create multiple alarms
- Enable / disable alarms individually
- Edit existing alarms
- Delete saved alarms
- Set custom labels for alarms
- Repeat alarms on selected weekdays
- View the next upcoming alarm
- Built-in alarm test controls
- 5-second quick test for development / checking behavior

### 3) Audio system
#### Meow presets
- Multiple built-in meow presets
- Neutral naming style: **Meow 1, Meow 2, Meow 3, ...**
- Universal default meow for all alarms
- Per-alarm meow override
- Per-alarm custom audio file support

#### Purr presets
- Multiple built-in purr presets
- Neutral naming style: **Purr 1, Purr 2, Purr 3, ...**
- Custom purr file support
- Purr preview buttons
- Cleaner, more balanced purr playback

### 4) Cat customization
- Custom cat name
- App text updates to use the selected cat name in relevant places
- Built-in cat color presets
- Current planned colors include:
  - Gray / tabby
  - Orange
  - Black
  - White
  - Calico / tricolor

### 5) Story / timing customization
- Adjustable interaction timing
- Story pacing presets (for example: calm / classic / fast)
- Configurable values for wake-up routine steps
- Timing controls for actions such as:
  - petting duration
  - snooze period / story step delays
  - smoking timing
  - food arrival timing
  - food pouring timing

### 6) Permissions & Android integration
- First-run permission setup flow
- Notification permission guidance
- Exact alarm guidance
- Full-screen notification guidance
- Lock-screen alarm guidance
- Battery optimization guidance
- Xiaomi / restrictive-device guidance
- Shortcut to re-check permissions later from inside the app

### 7) Reliability / platform behavior
- Designed for offline use after assets are available inside the app
- GitHub Actions build flow for APK export
- Fixes included for alarm deletion, custom sound selection, and interactive lock-screen behavior
- Structured to work across different Android phones, while still depending on phone-specific permission settings

---

## Preview

### Cozy Room / Interactive Scene
<img src="./docs/images/preview-room.png" alt="Cozy room preview" width="420" />

### Alarm Management
<img src="./docs/images/preview-alarms.png" alt="Alarm management preview" width="420" />

### Meows, Purrs, and Cat Settings
<img src="./docs/images/preview-settings.png" alt="Settings preview" width="420" />

---

## Why this project is different

Unlike a standard alarm app, **Meow Meow Alarm** is designed to feel personal, warm, and slightly playful.
It treats the wake-up process as a tiny interactive routine rather than just a ringtone.

It is especially focused on:
- cute presentation
- memorable alarm behavior
- cat-themed interaction
- user-controlled audio customization
- practical Android alarm reliability

---

## Current Design Direction

The active design direction is:
- the original cozy **2D art style**
- the original cat-and-man scene
- practical improvements to reliability and customization

The project intentionally moved away from:
- pixel-art redesigns
- 3D prototype directions

---

## Requirements

### For users
- Android phone
- Notifications enabled
- Alarm / full-screen permissions enabled where required by the device
- Battery restrictions relaxed for the app on restrictive phones (especially Xiaomi / MIUI / HyperOS if needed)

### For developers
- Git
- Java 17
- Android SDK
- Gradle (via wrapper / project setup)
- GitHub Actions or Android Studio for building

See also:
- [`docs/REQUIREMENTS.md`](./docs/REQUIREMENTS.md)
- [`docs/ANDROID_PERMISSIONS.md`](./docs/ANDROID_PERMISSIONS.md)
- [`docs/TROUBLESHOOTING.md`](./docs/TROUBLESHOOTING.md)

---

## Building the APK

The repository includes a GitHub Actions workflow for building the APK.

Typical flow:
1. Push your changes to the repository
2. Open the **Actions** tab on GitHub
3. Run or inspect the Android build workflow
4. Download the APK artifact from the successful run

If you are building locally, use the Android project under `app/` with the configured Gradle setup.

---

## Repository Layout

```text
app/                     Android app source
app/src/main/assets/     HTML / CSS / JavaScript UI
app/src/main/java/       Android Kotlin host code
app/src/main/res/raw/    Built-in audio files
.github/workflows/       GitHub Actions APK build workflow
tests/                   Validation and smoke tests
docs/                    Documentation
```

---

## Important Notes

- Some internal test controls remain in the app intentionally for development and validation.
- These may be removed later in a final user-facing release.
- Audio files may have separate usage or redistribution considerations.
- If you rename the repository to **Meow-Meow-Alarm**, make sure to update badges, links, and Git remote URLs.

---

## Suggested GitHub About Section

**Name:** Meow Meow Alarm  
**Description:** A cozy interactive Android alarm clock with cats, meows, purrs, custom alarms, and playful wake-up routines.  
**Topics:** `android`, `kotlin`, `alarm-clock`, `cat`, `meow`, `android-app`, `offline`, `notifications`

---

## License / Rights

A final open-source license decision should be made carefully, especially if the repository includes user-provided or externally sourced audio files.

See: [`docs/RIGHTS.md`](./docs/RIGHTS.md)
