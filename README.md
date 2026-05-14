# Wallet

An Android app for managing digital passes — Apple Wallet (PKPass) and custom barcode passes — built
with Kotlin and Jetpack Compose.

## Screenshots

| Pass Grid                     | Pass Detail                       | Custom Pass Builder                 | Theme                           |
|-------------------------------|-----------------------------------|-------------------------------------|---------------------------------|
| ![Grid](docs/readme/grid.png) | ![Detail](docs/readme/detail.png) | ![Builder](docs/readme/builder.png) | ![Theme](docs/readme/theme.png) |

## Features

- **Import PKPass files** — open `.pkpass` files from any app or file manager; the app registers as
  an `ACTION_VIEW` handler
- **Scan to import** — point the camera at a QR code containing a pass URL; the pass is downloaded
  and previewed before adding
- **Custom passes** — scan any barcode and save it as a custom pass with a name and color
- **Pass grid** — drag-and-drop to reorder; swipe during drag to reveal a delete zone
- **Pass detail** — front/back flip animation; back side shows fields, auto-refresh toggle, share,
  and delete
- **Barcode rendering** — QR, PDF417, Aztec, Code128, and more, generated on-device
- **Background refresh** — passes with a PKPass web service URL update automatically once a day over
  Wi-Fi
- **PKPass localization** — displays field values in the device locale when the pass includes
  `.lproj` translations
- **Dark/light theme** — follows system setting; pass card colors adapt for contrast in both modes

## Supported Pass Formats

| Format                    | Source                          | Barcode types                                                                                         | Images                                       | Localization           | Background refresh  | Pass categories                                          |
|---------------------------|---------------------------------|-------------------------------------------------------------------------------------------------------|----------------------------------------------|------------------------|---------------------|----------------------------------------------------------|
| **PKPass** (Apple Wallet) | `.pkpass` file or QR scan       | QR, PDF417, Aztec, Code128                                                                            | Yes (icon, logo, strip, background; @1x–@3x) | Yes (`.lproj` strings) | Yes (webServiceURL) | Boarding pass, Event ticket, Coupon, Store card, Generic |
| **Custom**                | Camera scan / Code from gallery | QR, Code 128, Code 39, Code 93, Codabar, Data Matrix, EAN-13, EAN-8, ITF, UPC-A, UPC-E, PDF417, Aztec | No                                           | No                     | No                  | Loyalty card                                             |

## Architecture

The app uses a **Parser Registry** pattern so new pass formats can be added without touching
existing code: implement `PassParser`, register it in `ParserRegistry`, add a `PassFormat` value.
PKPass is the only format currently supported.

Dependency injection is handled by Koin (`di/KoinModules.kt`). Each screen has its own focused
ViewModel — `PassGridViewModel`, `PassPreviewViewModel`, `CustomPassBuilderViewModel` — rather than
one god-object.

Pass assets (images extracted from `.pkpass` archives) live in the app's internal storage under
`files/passes/`. The database stores paths, not blobs.

---

# Development

## CI/CD

PRs to `main` run lint (ktlint + detekt + Android Lint), unit tests, and a debug build in parallel.
Results are posted as PR comments.

Releases are triggered by pushing a `release/**` branch or via manual dispatch in the GitHub Actions
UI. The workflow bumps the version, builds a signed AAB, uploads it to Google Play, and creates a
GitHub Release.

**Required secrets for releases:** `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD`, `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`.
