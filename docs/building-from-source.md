# Building from Source

This guide is for developers who want to clone Finxel and build it themselves, rather than installing the prebuilt APK from Releases. If you just want to **use** the app, see [Getting Started](getting-started.md) instead.

## Requirements

- **Android Studio** (recent stable version) or the Android command-line SDK tools.
- **JDK 17** (or whatever version Android Studio bundles/requires for this project's Android Gradle Plugin version).
- Android SDK Platform matching the app's `compileSdk` / `targetSdk`, plus the API 28 platform for the app's `minSdk`.
- Git.

## 1. Clone the repository

```bash
git clone https://github.com/ahmadrezagh671/Finxel.git
cd Finxel
```

## 2. Handle the Firebase dependency

Finxel uses **Firebase Analytics** (see [THIRD-PARTY-NOTICES.md](../THIRD-PARTY-NOTICES.md#firebase-analytics)) for anonymous usage stats. The Firebase/Google Services Gradle plugin requires a `google-services.json` file to build. Since that file contains project-specific keys, it isn't committed to this repository, and you have two options:

### Option A: Add your own `google-services.json` (keep analytics)

1. Create a free project at the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app to it using this project's applicationId: `com.ahmadrezagh671.finxel`.
3. Download the `google-services.json` file Firebase generates for you.
4. Place it at `app/google-services.json`.
5. Build normally. Analytics data from your build will go to **your own** Firebase project, not the original developer's (as noted in the third-party notices).

### Option B: Remove Firebase Analytics entirely (no `google-services.json` needed)

If you don't want to set up Firebase at all, strip the dependency out instead:

1. Remove the Google Services plugin application, usually a line like:
   ```kotlin
   id("com.google.gms.google-services")
   ```
   from the top-level and/or `app/build.gradle(.kts)`.
2. Remove the plugin classpath/dependency declaration in the top-level `build.gradle(.kts)` (or the `libs.versions.toml` entry, if version catalogs are used).
3. Remove the Firebase BOM and `firebase-analytics` dependency lines from `app/build.gradle(.kts)`.
4. Remove any code that calls Firebase Analytics APIs (e.g. `FirebaseAnalytics.getInstance(...)`, logged events) so the project still compiles. Search the codebase for `firebase` / `FirebaseAnalytics` to find every call site.
5. Sync Gradle and build. The app will build and run identically, just without any analytics collection.

> Whichever option you pick, the app builds and runs fine without any of your personal data being required, `google-services.json` only carries Firebase project identifiers, never user data.

## 3. Build the APK

From Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

From the command line:

```bash
./gradlew assembleDebug
```

for a debug build, or

```bash
./gradlew assembleRelease
```

for a release build (release builds require your own signing configuration, Android Studio's **Build → Generate Signed Bundle / APK** wizard is the easiest way to set that up if you don't already have one).

The output APK will be under `app/build/outputs/apk/`.

## 4. Install it

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

or drag the APK onto a running emulator, or copy it to a device and install it manually as described in [Getting Started](getting-started.md#2-allow-installing-from-this-source).

## Notes on the Sora Editor dependency

The in-app config JSON editor uses [Sora Editor](https://github.com/Rosemoe/sora-editor) as an unmodified library dependency (LGPL-2.1, see [THIRD-PARTY-NOTICES.md](../THIRD-PARTY-NOTICES.md#sora-editor)). It's pulled in automatically via Gradle, no extra setup is needed to build with it.
