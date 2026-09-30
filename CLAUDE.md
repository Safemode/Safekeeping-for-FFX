# My Android App

## Build
./gradlew assembleDebug

## Run Tests
./gradlew test

## Stack
- Kotlin
- Jetpack Compose
- MVVM Architecture
- Manual dependency injection via `di/AppContainer.kt` (no Hilt/Dagger - the app is small enough that a DI framework would be overhead)

## Conventions
- All UI in Compose, no XML layouts
- ViewModels should not reference Android framework classes directly
- Use StateFlow for UI state