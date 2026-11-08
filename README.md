# NEXA Browser

**Fast. Private. Yours.**

NEXA Browser is a lightweight Android WebView browser focused on fast startup, touch-friendly navigation, privacy controls, and a maintainable native Kotlin codebase.

## Features
- Real WebView browsing
- Smart URL/search handling
- Tabs and private tabs
- Bookmarks and local history
- Downloads through Android DownloadManager
- File upload chooser
- JavaScript, DOM storage and media support
- Desktop-site toggle
- Find in page, share, bookmarks and basic privacy clearing
- Edge-to-edge Material 3 UI
- System light/dark appearance
- English/Bengali-ready string resources
- GitHub Actions debug and release workflows

## Requirements
- Android Studio Ladybug or newer
- JDK 17
- Android SDK 35
- Gradle 8.7+

## Build
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

## GitHub Actions
Push the project to GitHub, then open **Actions → Android Build → Run workflow**. The workflow runs lint, tests and a debug build and uploads `NEXA-Browser-debug.apk` as an artifact.

## Release signing
Configure these repository secrets:
- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Then run **Actions → Android Release → Run workflow**.

## Privacy
NEXA stores browser history/bookmarks/settings locally. Private tabs are not written to the history manager. Incognito is not anonymity: websites, networks and other platform-level observers can still identify or monitor activity.

## Architecture
The project intentionally keeps the dependency surface small: Kotlin + AndroidX + Material 3 + Android WebView. Browser state is separated into small managers for tabs, history, bookmarks and settings.

## Screenshots
Add screenshots from your device here.

## License
Choose a license appropriate for your distribution before publishing.
