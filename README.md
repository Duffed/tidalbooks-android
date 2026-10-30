# TIDAL Books for Android

An unofficial Android app for listening to audiobook albums on TIDAL. Requires Android 8 or later and a TIDAL account with an eligible subscription.

## Features

- Local library with saved listening progress, search, sorting, and status filters
- Bookmarks, position history, and a sleep timer with fade-out and rewind
- Background playback, system media controls, and configurable home screen widgets
- Material Expressive and TIDAL themes, with German and English interfaces
- JSON backup and restore of your library, progress, and bookmarks

## Get started

Download the APK from [Releases](https://github.com/Duffed/tidalbooks-releases/releases). In Settings, connect your TIDAL account and confirm the device code on TIDAL's website. Return to the app, then paste an album link or share an album from TIDAL to TIDAL Books.

Listening progress is saved automatically. The app resumes at your saved position without starting playback automatically. Audio downloads, playback speed adjustment, and library sync are not supported.

## Build

Open the project in Android Studio with JDK 17, Android SDK Platform 35, and Build Tools 35.0.0. Set the SDK path through `ANDROID_HOME` or `sdk.dir` in an untracked `local.properties` file.

```sh
./gradlew :app:assembleDebug
```

On Windows, use `gradlew.bat`. The APK is generated in `app/build/outputs/apk/debug/`. Use `-PseparateApp=true` to build a separate test app with its own data. Updating an existing installation requires the same signing key.

Checks:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

The last command requires an Android device or emulator.

## Integration and privacy

Playback uses the TIDAL Player SDK. Device login uses the shared client profile distributed with tidalapi 0.8.11; attribution and its license are in [third_party](third_party/README.txt). This unofficial integration may change or stop working. TIDAL Books is not affiliated with or endorsed by TIDAL.

Your library stays on your device. Account tokens are encrypted using Android Keystore and excluded from exported backups. The app has no server or analytics of its own; the TIDAL SDK may send operational and playback events to TIDAL. Export a backup before uninstalling or clearing app data.

## Support development

TIDAL Books is free. You can support development with a [voluntary contribution via PayPal](https://paypal.me/rest0ck).
