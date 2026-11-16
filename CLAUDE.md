# My Android App

## Build
./gradlew assembleDebug

## Release
Release builds are shrunk and optimized by R8 but not obfuscated (`-dontobfuscate` in
`app/proguard-rules.pro`), so crash logs show real class and method names. R8 still renumbers lines
and inlines small methods, so exact source lines need that build's mapping. Every signed release
build (Android Studio's "Generate Signed Bundle / APK") writes
`release-mappings/safekeeping-for-ffx-<version>-mapping.zip`; unsigned `assembleRelease` runs, like
CI, leave it alone.
The zip is kept private, not attached to GitHub releases: users never need it, and only the
maintainer retraces crash logs. `release-mappings/` is gitignored, so keep a backup of it, one zip per
released version, since a crash log can only be retraced with the mapping from its exact build. Play
gets its mapping from the .aab automatically.

## Run Tests
./gradlew test

Database migration tests are instrumented (they need a real SQLite), so CI does not run them. Run
them on an emulator or device whenever the Room schema changes, before releasing:
./gradlew connectedDebugAndroidTest

## Stack
- Kotlin
- Jetpack Compose
- MVVM Architecture
- Manual dependency injection via `di/AppContainer.kt` (no Hilt/Dagger - the app is small enough that a DI framework would be overhead)

Item, fiend, progress-key and grid-node ids are database keys, frozen in `app/src/test/frozen-ids`.
After adding reference data, run `./gradlew test -PupdateFrozenIds` to append the new ids. A frozen
id that disappears means saved progress would be orphaned - don't just delete the line.

## Conventions
- All UI in Compose, no XML layouts
- ViewModels should not reference Android framework classes directly
- Use StateFlow for UI state