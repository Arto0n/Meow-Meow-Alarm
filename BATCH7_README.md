# MeowAlarm — Batch 7 Cozy 2D Deluxe

**This is a source patch for the last confirmed pre-pixel Batch 4 app, not an APK.** It preserves the native alarm engine and only replaces the game scene, sound selection asset routing, profile storage and per-alarm routine metadata.

## What's included
- Illustrated cozy room background (`room.webp`) + time-sensitive window tint.
- Male/female animated **vector** character; five color variants of a clearer animated **vector** cat.
- Two-choice onboarding; profile persisted in Android SharedPreferences (`cozy_profile_v7`).
- Five per-alarm routines: Lazy Morning (pet), Breakfast (pet + feed), Playtime (feather), Grumpy Cat (pet + find toy + feed), Custom (pet + play + blanket + feed).
- Manual swipe/drag/hold interaction; simple taps alone cannot complete petting or feeding.
- Activity volume control and additional sound effects. Default cute/realistic/custom meow selection retained.
- Installation script for **real** Wikimedia cat-meow, kitten-meow, purr, human sigh, and human yawn recordings. Local fallback audio included.
- All existing alarm editing, deleting, testing, scheduling, and custom audio import remain available.

## Apply safely on Windows PowerShell
Run in the main repo root (`C:\Users\NP4040902\Desktop\MeowAlarm-FIXED-GitHub-APK-ready`):

```powershell
cd "C:\Users\NP4040902\Desktop\MeowAlarm-FIXED-GitHub-APK-ready"
git status
# Continue only if no uncommitted work needs saving.
git pull --ff-only origin main
Expand-Archive -Path "$env:USERPROFILE\Downloads\MeowAlarm-Batch7-COZY2D-DELUXE-PATCH.zip" -DestinationPath . -Force
powershell -NoProfile -ExecutionPolicy Bypass -File .\install-cozy-audio.ps1
# Check the download output: only SUCCESS confirms each new recording was obtained.
git add app tests install-cozy-audio.ps1 AUDIO_CREDITS.md BATCH7_README.md
git commit -m "Add Cozy 2D Deluxe character cat and routines"
git push origin main
```

If `git pull --ff-only` refuses, stop and send the output, rather than force-pushing. If individual sound downloads fail, the app still builds using existing audio and included synthesized fallback SFX; the new real sound(s) have **not** been installed until their download succeeds.

GitHub Actions: https://github.com/Arto0n/MeowAlarm3/actions . Download `MeowAlarm-debug` artifact on success and install the APK.

## How to test
1. First launch: choose human + cat; switch choices in Settings, close/reopen and confirm they persist.
2. Edit an alarm and select Breakfast routine; use its five-second test; swipe the cat repeatedly, then drag food bag over bowl and hold. Confirm it dismisses.
3. Try Playtime, Grumpy, Lazy and Custom; custom imported sound must still work.
4. Test with screen locked. Test Samsung and Xiaomi separately; Xiaomi crash behavior is **not** declared fixed.
5. The main alarm sound must remain audible if the WebView animation fails. Keep your ordinary Clock alarm as backup until tested.

## Architecture notes
- Android `AlarmService.kt` is unmodified. Main sound playback stays native.
- `Alarms.kt` adds an optional `preset` field to each alarm with a safe default (`breakfast`) and a sanitized global profile bridge.
- New scene and interactions are in `cozy2d.js`/`cozy2d.css`; old SVG remains in DOM hidden for backward compatibility.
- Time-of-day is derived from local device `Date` in WebView (morning/afternoon/sunset/night).
- New sound installer recordings are **public domain/CC0**, with links in `AUDIO_CREDITS.md`.

## Validation and limitations
Passed: original Batch 2/3/4 source tests, Batch 7 source checks, responsive browser checks at 320, 390, 520 pixels, automated swipe/drag Breakfast sequence, and successful simulated Playtime entry. These do not prove Android compilation, performance, visual quality on a real phone, or Xiaomi stability.

The house image is a generated illustration used as an attractive static backdrop with animated SVG characters overlaid. These characters move smoothly but are **not** fully rigged professional 2D characters (such as Spine/Rive). This is a usable lightweight personal-app iteration, not a claim of studio-grade character animation.
