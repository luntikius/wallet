# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

An Android wallet application built with Kotlin and Jetpack Compose. The project uses modern Android development practices with Material 3 design system and edge-to-edge display support.

**Package:** `com.luntikius.wallet`
**Min SDK:** 33 (Android 13)
**Target SDK:** 36
**Compile SDK:** 36

## Build System

This project uses Gradle with Kotlin DSL and the Version Catalog feature for dependency management.

### Common Commands

**Build the app:**
```bash
./gradlew build
```

**Install debug build on connected device:**
```bash
./gradlew installDebug
```

**Run unit tests:**
```bash
./gradlew test
```

**Run a specific test class:**
```bash
./gradlew test --tests com.luntikius.wallet.ExampleUnitTest
```

**Run instrumented tests (requires emulator or device):**
```bash
./gradlew connectedAndroidTest
```

**Run a specific instrumented test:**
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.luntikius.wallet.ExampleInstrumentedTest
```

**Clean build artifacts:**
```bash
./gradlew clean
```

**Lint checks:**
```bash
./gradlew lint
```

## Architecture

### UI Layer
- **Compose-based UI**: All UI components built with Jetpack Compose
- **Single Activity**: `MainActivity` serves as the entry point, using Compose for all screens
- **Material 3**: Uses Material Design 3 with dynamic color support (Android 12+)
- **Edge-to-Edge**: App uses edge-to-edge display with proper inset handling via Scaffold

### Theme System
Located in `app/src/main/java/com/luntikius/wallet/ui/theme/`:
- `Theme.kt`: Contains `WalletTheme` composable that handles dark/light mode and dynamic colors
- `Color.kt`: Color definitions for the app
- `Type.kt`: Typography configuration

The theme supports:
- Dark and light color schemes
- Dynamic color extraction from system (Android 12+)
- System theme following via `isSystemInDarkTheme()`

## Dependency Management

Dependencies are managed via Version Catalog in `gradle/libs.versions.toml`. To add a new dependency:
1. Add the version to `[versions]` section
2. Add the library to `[libraries]` section
3. Reference it in `app/build.gradle.kts` using `libs.` prefix

## Code Organization

- **Main source:** `app/src/main/java/com/luntikius/wallet/`
- **Tests:** `app/src/test/java/com/luntikius/wallet/`
- **Instrumented tests:** `app/src/androidTest/java/com/luntikius/wallet/`
- **Resources:** `app/src/main/res/`

## Java/Kotlin Configuration

- **Java Version:** 11 (source and target compatibility)
- **JVM Target:** 11
- **Kotlin Version:** 2.0.21
