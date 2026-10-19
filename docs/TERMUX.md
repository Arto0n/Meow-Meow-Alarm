# Optional / legacy Termux build

The repository retains `setup-termux.sh` and `build-termux.sh` as an optional route for users developing directly on an Android device.

The primary **supported and regularly used** build path is GitHub Actions. The Termux scripts are legacy project tooling and should be reviewed before running on a new Android/Termux version. The earlier `README_TERMUX.md` referenced a discontinued archive name and shouldn't be followed verbatim.

If you want to try the legacy path:

1. Install an up-to-date Termux distribution.
2. Keep the project in a writable directory that allows tool execution (not directly from a `noexec` shared storage location).
3. Inspect `setup-termux.sh` and `build-termux.sh` before running.
4. Run `bash setup-termux.sh` then `bash build-termux.sh` only if their assumptions fit your setup.

For a more reliable APK build, use [GitHub Actions](DEVELOPMENT.md).
