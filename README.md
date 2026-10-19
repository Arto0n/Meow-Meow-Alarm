# 🐱 Meow Meow Alarm

<div align="center">

A cozy, interactive Android alarm clock where a cat wakes you up with meows, purrs, and a playful bedtime-to-breakfast routine.

**Choose your language / زبان مورد نظر را انتخاب کنید**

[🇬🇧 English Documentation](./Readme-En.md) · [🇮🇷 مستندات فارسی](./Readme-Fa.md)

<img src="./docs/images/preview-collage.png" alt="Meow Meow Alarm preview collage" width="100%" />

</div>

---

## Quick Overview

**Meow Meow Alarm** is an Android alarm app built around a cozy 2D cat-and-human scene.
Instead of a plain ringtone-only alarm, the app can open an interactive wake-up screen where the user pets the cat, hears meows and purrs, and manages alarms in a cute, game-like interface.

### Main sections
- **Cozy Room** – the main interactive wake-up scene
- **Alarms** – create, edit, test, enable, and delete multiple alarms
- **Settings** – manage meows, purrs, cat name, cat color, and timing behavior

### Open the full docs
- [Readme-En.md](./Readme-En.md)
- [Readme-Fa.md](./Readme-Fa.md)

---

## Preview Images

### Cozy Room
<img src="./docs/images/preview-room.png" alt="Cozy room preview" width="360" />

### Alarms
<img src="./docs/images/preview-alarms.png" alt="Alarms preview" width="360" />

### Settings
<img src="./docs/images/preview-settings.png" alt="Settings preview" width="360" />

---

## Repository Structure

```text
app/                     Android app source
app/src/main/assets/     HTML / CSS / JavaScript UI
app/src/main/java/       Android Kotlin host code
app/src/main/res/raw/    Built-in audio files
.github/workflows/       GitHub Actions APK build
tests/                   Validation and smoke tests
```

---

## Status

- Android project is buildable through GitHub Actions
- The current visual direction uses the original 2D cozy style
- Pixel-art and 3D prototype directions are intentionally not part of the active product path

---

## Notes

- Some internal test controls still exist in the app for development and verification.
- The repository may contain custom audio assets. Review usage rights before redistributing them.
