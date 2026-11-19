# FocusLock

An offline, native Android app-usage limiter built with Kotlin, Jetpack Compose, Material 3, Room and DataStore. It uses Android Usage Access to reconcile daily foreground time and an Accessibility Service to redirect launches of apps whose daily allowance has expired.

## Requirements

- Android Studio Ladybug or newer with Android SDK Platform 35 installed
- JDK 17 (Android Studio's bundled JBR is suitable)

Set `sdk.dir` in `local.properties` if Android Studio has not created it.

## Build

```powershell
./gradlew.bat assembleDebug
./gradlew.bat bundleRelease
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Enforcement model

FocusLock is a personal-use blocker. Android grants third-party apps no absolute guarantee against force-stop, uninstall, Safe Mode, accessibility revocation, or a user choosing to bypass a redirect. While Accessibility and Usage Access are enabled, FocusLock detects launches of limited apps and immediately shows the local blocking screen. The service never blocks the launcher, Settings, emergency dialer, package installer, system UI, or FocusLock itself.
