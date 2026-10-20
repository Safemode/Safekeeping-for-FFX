# My Android App

## Build
./gradlew assembleDebug

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

## Conventions
- All UI in Compose, no XML layouts
- ViewModels should not reference Android framework classes directly
- Use StateFlow for UI state