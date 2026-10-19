# Troubleshooting

## The alarm makes noise, but no cat interaction screen appears

- Enable notifications and full-screen alarm permission (if shown by Android).
- Verify the phone allows lock-screen alarm presentation.
- On Xiaomi, enable the app's Autostart and review background/battery restrictions.
- Test a scheduled alarm with the screen locked, not only the short preview.

## Alarm doesn't ring when the app is closed

- Confirm exact alarm permission and notifications where required.
- Check the selected sound and **Alarm volume** in Android settings.
- On some devices, battery management can still interfere; test the phone model directly.

## GitHub Actions fails at `sdkmanager`

- Check that the workflow uses the correct Android SDK manager path and SDK packages.
- Preserve the working workflow rather than replacing it with a completely different SDK setup action.

## GitHub Actions fails during `compileDebugKotlin`

- Read the **first `e:` compiler error** in the build log, including Kotlin file and line number.
- Fix the precise source error and push a small commit; do not delete project files to work around a compiler error.

## APK won't install over the old version

A new debug signature can be incompatible with an existing install. Before uninstalling, document or back up alarm settings. Uninstalling can erase the app's local data.

## Local Git working tree has old ZIP notes or scripts

Untracked local files are **not automatically published to GitHub**. Use `git status` to review them. The repository cleanup scripts deliberately do not delete untracked files; archive them separately if desired.

## Capturing a crash log

For a reproducible Android crash, use Android Debug Bridge (`adb logcat`) on a connected device with USB debugging enabled, then reproduce the issue. Remove personal information before sharing logs publicly. A browser test alone does not prove alarm reliability on hardware.
