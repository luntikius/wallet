# Wallet App

An Android wallet application built with Kotlin and Jetpack Compose. Supports multiple pass formats including Apple Wallet (PKPass) with modern Material 3 design.

## Features

- Multi-format pass support (PKPass, extensible for Google Wallet)
- Modern UI with Jetpack Compose and Material 3
- Edge-to-edge display with proper inset handling
- Drag-and-drop pass reordering
- Pull-to-refresh with automatic updates
- QR code and barcode generation (QR, PDF417, Code128, etc.)
- Dark/light mode with dynamic color support
- Room database for local storage

## Development

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 17
- Android SDK 36 (API level 36)
- Kotlin 2.0.21

### Local Setup

1. Clone the repository:
   ```bash
   git clone https://sourcecraft.dev/<your-username>/wallet.git
   cd wallet
   ```

2. Open in Android Studio:
   - File → Open → Select the `wallet` directory
   - Wait for Gradle sync to complete

3. Run on emulator or device:
   - Select a device/emulator from the toolbar
   - Click Run (▶️) or press `Shift + F10`

### Building Locally

#### Debug Build
```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

#### Release Build (requires signing config)

**Option 1: Using local.properties (recommended for local development)**

Add the following to `local.properties` (this file is gitignored):
```properties
KEYSTORE_FILE=/absolute/path/to/your/wallet-upload-key.jks
KEYSTORE_PASSWORD=your-password
KEY_ALIAS=wallet-upload
KEY_PASSWORD=your-password
```

Then build:
```bash
# Build signed AAB
./gradlew bundleRelease

# Build signed APK
./gradlew assembleRelease

# Verify signature
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/*.aab
```

**Option 2: Using environment variables (for CI/CD)**
```bash
# Set environment variables for signing
export KEYSTORE_FILE=/absolute/path/to/your/wallet-upload-key.jks
export KEYSTORE_PASSWORD=your-password
export KEY_ALIAS=wallet-upload
export KEY_PASSWORD=your-password

# Build signed AAB
./gradlew bundleRelease
```

### Running Tests

```bash
# Run unit tests
./gradlew test

# Run specific test class
./gradlew test --tests com.luntikius.wallet.ExampleUnitTest

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest

# Run lint checks
./gradlew lint
```

## CI/CD Pipeline

This project uses **Yandex Sourcecraft** for automated builds and **Google Play Store** publishing. The pipeline is configured in `.sourcecraft/ci.yaml`.

### Architecture

- **Secrets Management**: Yandex Cloud Lockbox for secure keystore and credentials
- **Build System**: Gradle with R8 code shrinking and ProGuard obfuscation
- **Signing**: Google Play App Signing (Google manages production keys)
- **Artifacts**: Signed AAB (Android App Bundle) for optimal Play Store delivery

### Build Validation (Automatic)

Every pull request to `main` triggers automated validation:

1. **Lint checks** - Code quality and potential issues
2. **Unit tests** - Verify core functionality
3. **Debug APK build** - Ensure the app builds successfully

**View results**: Navigate to the Sourcecraft CI/CD tab to see real-time logs and test results.

**Artifacts**: Debug APK and test reports are available for download after the workflow completes.

### Release Process (Manual)

To publish a new release to Google Play:

#### 1. Create Release Branch

```bash
git checkout main
git pull sourcecraft main
git checkout -b release/v1.x.x
```

#### 2. Configure Release (Optional)

Edit `.sourcecraft/ci.yaml` to customize the release:

```yaml
env:
  VERSION_BUMP: patch  # Options: major, minor, patch, none
  RELEASE_TRACK: internal  # Options: internal, alpha, beta, production
```

**Version Bump Strategy:**
- `major`: Breaking changes (1.0.0 → 2.0.0)
- `minor`: New features (1.0.0 → 1.1.0)
- `patch`: Bug fixes (1.0.0 → 1.0.1)
- `none`: No version change (for testing)

#### 3. Push to Trigger Build

```bash
git push sourcecraft release/v1.x.x
```

This triggers the `release-internal` workflow, which:
1. Fetches secrets from Yandex Lockbox
2. Bumps version number (if configured)
3. Builds signed AAB with R8 optimization
4. Uploads to Google Play Store
5. Commits version bump back to main

#### 4. Monitor Workflow

- Go to **Sourcecraft CI/CD** tab
- Click on the running workflow
- View real-time logs for each cube
- Expected duration: ~15-20 minutes (first run), ~10-12 minutes (subsequent)

#### 5. Verify in Google Play Console

- Go to: [Google Play Console](https://play.google.com/console) → Wallet App
- Navigate to: **Release → Testing → Internal testing** (or your configured track)
- Verify the new release appears
- Add testers and distribute

#### 6. Merge Release Branch (Optional)

After successful build:
```bash
git checkout main
git merge release/v1.x.x
git push sourcecraft main
```

### Release Tracks

Google Play offers multiple testing tracks before public release:

| Track | Purpose | Audience | Typical Testers |
|-------|---------|----------|-----------------|
| **internal** | Initial validation | Internal team | 1-100 testers |
| **alpha** | Early testing | Alpha testers | 100-1000 testers |
| **beta** | Broader testing | Beta testers | 1000+ testers |
| **production** | Public release | All users | Everyone |

**Best Practice**: Always test in lower tracks before promoting to production.

#### Promoting Between Tracks

To promote a release:
1. Verify it works in current track
2. Create new release branch: `git checkout -b release/v1.x.x-beta`
3. Edit `.sourcecraft/ci.yaml`: `RELEASE_TRACK: beta`
4. Push and trigger workflow
5. Monitor and verify in Google Play Console

## Contributing

We welcome contributions! Please follow these guidelines:

### Branch Naming Conventions

- `feature/*` - New features (e.g., `feature/dark-mode`)
- `bugfix/*` - Bug fixes (e.g., `bugfix/crash-on-startup`)
- `release/*` - Release branches (triggers CI/CD)
- `hotfix/*` - Emergency production fixes

### Pull Request Process

1. **Fork and create branch**:
   ```bash
   git checkout -b feature/amazing-feature
   ```

2. **Make changes and commit**:
   ```bash
   git add .
   git commit -m "Add amazing feature"
   ```

3. **Push to your fork**:
   ```bash
   git push origin feature/amazing-feature
   ```

4. **Create pull request** on Sourcecraft:
   - Navigate to your fork → Pull Requests → New
   - Base: `main` (upstream)
   - Compare: `feature/amazing-feature` (your branch)
   - Fill in description and submit

5. **Ensure CI/CD passes**:
   - The `build-validation` workflow runs automatically
   - Fix any lint errors or test failures
   - Push additional commits to update the PR

6. **Code review**:
   - Wait for maintainer review
   - Address feedback if requested
   - Once approved, maintainer will merge

### Code Quality Requirements

All pull requests must:
- ✅ Pass lint checks (`./gradlew lintDebug`)
- ✅ Pass unit tests (`./gradlew testDebugUnitTest`)
- ✅ Build successfully (`./gradlew assembleDebug`)
- ✅ Follow Kotlin coding conventions
- ✅ Include relevant documentation updates

### Coding Conventions

- **Language**: Kotlin (official code style)
- **Formatting**: Use Android Studio auto-formatter (`Ctrl+Alt+L` / `Cmd+Opt+L`)
- **Naming**:
  - Classes: `PascalCase`
  - Functions: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- **Architecture**: Follow existing patterns (Repository, ViewModel, Compose UI)
- **Comments**: Document complex logic, avoid obvious comments

## Architecture

### Overview

The app follows modern Android architecture best practices:

- **UI Layer**: Jetpack Compose with Material 3
- **ViewModel Layer**: StateFlow for reactive state management
- **Repository Layer**: Single source of truth, abstracts data sources
- **Data Layer**: Room database with Flow-based queries
- **Parser Layer**: Registry pattern for multi-format pass support

### Multimodal Pass Format Support

The app is architected to support multiple pass formats through a **Parser Registry pattern**:

```
PassParser Interface → ParserRegistry → Format-specific parsers
                                      ↓
                              Pass entity (Room)
```

**Currently supported**: PKPass (Apple Wallet)

**To add a new format**:
1. Implement `PassParser` interface in `data/parser/`
2. Register parser in `ParserRegistry`
3. Add enum value to `PassFormat.kt`

See [CLAUDE.md](CLAUDE.md) for detailed architecture documentation.

### Key Components

- **MainActivity** (`MainActivity.kt`) - Single activity, Compose entry point
- **PassViewModel** (`ui/PassViewModel.kt`) - State management for passes
- **PassRepository** (`data/repository/PassRepository.kt`) - Data operations
- **PassDao** (`data/dao/PassDao.kt`) - Room database queries
- **PKPassParser** (`data/parser/PKPassParser.kt`) - Apple Wallet parser
- **PassGridScreen** (`ui/screen/PassGridScreen.kt`) - Grid display
- **PassDetailScreen** (`ui/screen/PassDetailScreen.kt`) - Detail view with barcode

### Dependencies

Managed via Gradle Version Catalog (`gradle/libs.versions.toml`):

- **UI**: Jetpack Compose, Material 3, Coil (image loading)
- **Data**: Room, Gson, Retrofit, OkHttp
- **Async**: Kotlin Coroutines, Flow
- **Navigation**: Jetpack Compose Navigation
- **Utilities**: ZXing (barcodes), QRose (QR codes), Reorderable (drag-drop)
- **Background**: WorkManager

## Project Structure

```
app/src/main/java/com/luntikius/wallet/
├── MainActivity.kt                    # Entry point
├── data/
│   ├── dao/                          # Room database access
│   ├── database/                     # Database configuration
│   ├── model/                        # Data models (Pass, PassFormat, etc.)
│   ├── parser/                       # Pass parsers (PKPass, etc.)
│   └── repository/                   # Repository pattern implementation
└── ui/
    ├── navigation/                   # Navigation configuration
    ├── screen/                       # Compose screens
    ├── theme/                        # Material 3 theme
    └── PassViewModel.kt              # State management

.sourcecraft/
└── ci.yaml                           # CI/CD pipeline configuration

app/
├── build.gradle.kts                  # Build configuration
└── proguard-rules.pro                # R8/ProGuard rules
```

## Security

### Secret Management

- **Keystore**: NEVER commit `.jks`, `.keystore`, or `keystore.properties` files
- **Passwords**: Store in Yandex Cloud Lockbox, never in code or git
- **Service Accounts**: Use minimal required permissions
- **CI/CD**: Access secrets via service connections, never log sensitive data

### Signing Keys

- **Upload Key**: You generate and manage (backup securely!)
- **App Signing Key**: Google manages (with secure backups)
- **Backup**: Store keystore in 2+ secure locations (password manager, encrypted storage)

**If you lose your upload key**: Google can reset it (you'll generate a new one)
**If Google loses app signing key**: Google has redundant backups

## Troubleshooting

### Common Issues

#### Build fails with "Keystore not found"
- **Cause**: Signing configuration not set
- **Fix**: Add keystore properties to `local.properties` or export environment variables (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`)

#### R8 compilation error
- **Cause**: Missing ProGuard rules
- **Fix**: Add `-keep` rules in `app/proguard-rules.pro` for problematic classes

#### Workflow doesn't trigger
- **Cause**: Branch name doesn't match filter
- **Fix**: Ensure branch name starts with `release/` for releases, or create PR to `main` for validation

#### Version code must be greater
- **Cause**: `VERSION_BUMP=none` or version not incremented
- **Fix**: Set `VERSION_BUMP` to `patch`/`minor`/`major`, or manually edit `gradle.properties`

#### Google Play upload fails "Permission denied"
- **Cause**: Service account lacks permissions
- **Fix**: In Play Console → Setup → API access, grant "Manage releases" permission

### Getting Help

- **Issues**: Report bugs at [GitHub Issues](https://github.com/anthropics/claude-code/issues)
- **Documentation**: See [CLAUDE.md](CLAUDE.md) for architecture details
- **Sourcecraft**: [Documentation](https://sourcecraft.dev/portal/docs/en/)
- **Google Play**: [API Reference](https://developers.google.com/android-publisher)

## License

[Your license here - e.g., MIT, Apache 2.0]

## Acknowledgments

Built with:
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material 3](https://m3.material.io/)
- [Yandex Sourcecraft](https://sourcecraft.dev/)
- [Google Play](https://play.google.com/console)
