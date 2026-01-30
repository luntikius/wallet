# Wallet Design System

A centralized design system module for the Wallet Android app, built with Jetpack Compose and Material 3.

## Overview

The design system provides:
- **Theme System**: Black & white color scheme with blue accent (#FF0077b6)
- **Typography**: Complete Material 3 type scale + semantic text styles
- **Color System**: 8 pastel pass color palettes with WCAG AA contrast validation
- **Spacing System**: Consistent 4dp-based spacing scale
- **Component Library**: 11 reusable UI components wrapping Material 3
- **Animation Tokens**: Standardized animation specs for pass card transitions

## Quick Start

### Using the Theme

```kotlin
import com.luntikius.wallet.designsystem.theme.WalletTheme

@Composable
fun MyApp() {
    WalletTheme {
        // Your app content
    }
}
```

### Accessing Design Tokens

**Spacing:**
```kotlin
Box(modifier = Modifier.padding(MaterialTheme.spacing.medium))
```

**Pass Colors:**
```kotlin
val passColors = MaterialTheme.walletColors
val palette = passColors[0] // First color palette
```

**Semantic Text Styles:**
```kotlin
Text(
    text = "Primary text",
    style = MaterialTheme.textStyles.bodyPrimary
)
Text(
    text = "Secondary text",
    style = MaterialTheme.textStyles.bodySecondary
)
```

## Components

### Buttons
- `WalletFilledButton` - Primary action button
- `WalletOutlinedButton` - Secondary action button
- `WalletTextButton` - Tertiary/low-emphasis button
- `WalletIconButton` - Icon-only button

### Dialogs
- `WalletAlertDialog` - Alert dialog with icon, title, and actions

### Input
- `WalletTextField` - Standard text field
- `WalletOutlinedTextField` - Outlined text field variant
- `WalletTransparentTextField` - Transparent text field for colored backgrounds

### Menus
- `WalletDropdownMenu` - Dropdown menu container
- `WalletMenuItem` - Menu item with icon and text

### Navigation
- `WalletTopAppBar` - Top app bar with title and actions

### Feedback
- `EmptyState` - Empty state with icon and message
- `WalletSnackbar` - Status-aware snackbar (success/error/loading)
- `WalletCircularProgressIndicator` - Loading spinner

### Pickers
- `ColorPicker` - Horizontal scrolling color palette selector

## Adding Custom Fonts

1. Place font files in `design-system/src/main/res/font/`:
   ```
   font/
   ├── inter_regular.ttf
   ├── inter_medium.ttf
   ├── inter_semibold.ttf
   └── inter_bold.ttf
   ```

2. Define FontFamily in `foundation/typography/FontFamilies.kt`:
   ```kotlin
   val InterFontFamily = FontFamily(
       Font(R.font.inter_regular, FontWeight.Normal),
       Font(R.font.inter_medium, FontWeight.Medium),
       Font(R.font.inter_semibold, FontWeight.SemiBold),
       Font(R.font.inter_bold, FontWeight.Bold)
   )
   ```

3. Update `theme/Typography.kt` to use the custom font:
   ```kotlin
   bodyLarge = TextStyle(
       fontFamily = InterFontFamily,
       fontWeight = FontWeight.Normal,
       fontSize = 16.sp,
       ...
   )
   ```

## Adding New Components

1. Create component file in appropriate subdirectory:
   ```
   components/
   └── [category]/
       └── WalletNewComponent.kt
   ```

2. Follow naming convention: `Wallet[ComponentName]`

3. Wrap Material 3 component with design system styling:
   ```kotlin
   @Composable
   fun WalletNewComponent(
       modifier: Modifier = Modifier,
       // Component-specific parameters
   ) {
       Material3Component(
           modifier = modifier,
           colors = /* theme colors */,
           shape = MaterialTheme.shapes.medium,
           // Other Material 3 parameters
       )
   }
   ```

4. Add @Preview composables for light/dark themes

5. Document with KDoc comments

## Architecture

### Foundation Layer
- **color/**: Color system (palettes, tokens, utilities)
- **typography/**: Typography (font families, text styles)
- **spacing/**: Spacing scale
- **animation/**: Animation specifications

### Theme Layer
- **WalletTheme.kt**: Main theme composable
- **ColorScheme.kt**: Material 3 color schemes
- **Typography.kt**: Material 3 type scale
- **Shape.kt**: Shape definitions
- **Dimensions.kt**: Size constants

### Component Layer
- **button/**: Button components
- **dialog/**: Dialog components
- **input/**: Text field components
- **menu/**: Menu and dropdown components
- **navigation/**: Navigation components
- **feedback/**: Empty state, snackbar, progress indicators
- **picker/**: Color picker

## Design Decisions

### Color Scheme
- **Base**: Black & White (high contrast for financial apps)
- **Primary/Accent**: #FF0077b6 (blue)
- **Error**: Red
- **No Dynamic Color**: Fixed color schemes for consistency

### Typography
- **Scale**: Complete Material 3 typography (Display, Headline, Title, Body, Label)
- **Semantic Styles**: Primary/secondary variants instead of manual opacity
- **Font**: FontFamily.Default (system font) - extensible for custom fonts

### Spacing
- **System**: 4dp-based grid
- **Scale**: 9 levels from 0dp to 48dp
- **Access**: Via CompositionLocal (`MaterialTheme.spacing`)

### Components
- **Pattern**: Thin wrappers around Material 3 components
- **Scope**: Only includes currently-used components
- **Naming**: `Wallet` prefix for all components
- **Theming**: All components theme-aware via MaterialTheme

## Testing

Run design-system tests:
```bash
./gradlew :design-system:test
```

Build design-system module:
```bash
./gradlew :design-system:build
```

Lint checks:
```bash
./gradlew :design-system:ktlintCheck
./gradlew :design-system:detekt
```

## License

Proprietary - Wallet App
