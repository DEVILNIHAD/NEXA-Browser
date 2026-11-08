# NEXA Browser

**Fast. Private. Yours.**

NEXA Browser is a lightweight Android WebView browser built with Kotlin, AndroidX and Material 3. It is designed for fast startup, simple navigation, low dependency overhead and easy GitHub Actions builds.

## Included

- Real Android WebView browsing
- Smart URL/search resolution
- Multiple live tabs
- Private tabs with history suppression and reduced cookie persistence
- Back / forward / reload via normal WebView navigation
- Bookmarks and local history
- Android DownloadManager downloads
- Web file chooser
- Camera / microphone / geolocation permission prompts
- Desktop-site toggle
- Find in page
- Share page
- Light / dark / system theme
- English and Bengali resources
- Safe SSL-error handling
- Clear browsing data
- GitHub Actions lint, tests and APK artifacts

## Requirements

- Android Studio with Android SDK 35
- JDK 17
- Gradle 8.9

## Build

```bash
./gradlew assembleDebug
```

If your environment does not have the Gradle wrapper distribution cached, the included bootstrap script downloads Gradle 8.9 from services.gradle.org.

APK:

`app/build/outputs/apk/debug/app-debug.apk`

## GitHub Actions

Push to `main` and the **Android Build** workflow runs automatically.

Manual build:

**GitHub → Actions → Android Build → Run workflow**

The workflow runs:

1. Lint
2. Unit tests
3. Debug APK build
4. Artifact upload

Artifact:

`NEXA-Browser-debug.apk`

## Signed release

Add these repository secrets:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Then run:

**GitHub → Actions → Android Release → Run workflow**

The release workflow uploads:

`NEXA-Browser-release.apk`

## Privacy

History and bookmarks are stored locally. Private tabs do not add visits to browser history and disable third-party cookies for their WebView session. Private browsing is not anonymity: websites, networks, DNS providers, and device-level observers can still observe activity.

## Security

NEXA does not request broad storage permissions, does not accept SSL certificate errors, disables cleartext traffic by default, limits WebView file access, and requests camera/microphone/location permissions only when a website requests them.

## Architecture

The project deliberately uses a small native dependency set:

- Kotlin
- AndroidX
- Material 3
- Android WebView
- SharedPreferences for small local browser state

The browser UI is native Android; the homepage is lightweight HTML rendered inside the WebView.

## Limitations

Android WebView behavior varies by the WebView provider installed on the device. Some desktop-browser features such as full browser-level content blocking, complete reader mode, extension support and per-tab isolated WebView storage require substantially more infrastructure and are not falsely represented as fully implemented here.

## License

Choose and add a license before publishing the project publicly.
