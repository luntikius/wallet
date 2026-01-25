#!/bin/bash
set -e

echo "Building signed AAB..."

# Check required environment variables
if [ -z "$KEYSTORE_PASSWORD" ] || [ -z "$KEY_ALIAS" ] || [ -z "$KEY_PASSWORD" ]; then
  echo "Error: Required environment variables not set"
  echo "Need: KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD"
  exit 1
fi

# Use /workspace in CI, current directory for local testing
WORKSPACE_DIR="${WORKSPACE_DIR:-/workspace}"
if [ ! -d "$WORKSPACE_DIR" ] || [ ! -w "$WORKSPACE_DIR" ]; then
  WORKSPACE_DIR="."
fi

# Check keystore exists
if [ ! -f "$WORKSPACE_DIR/release.jks" ]; then
  echo "Error: Keystore not found at $WORKSPACE_DIR/release.jks"
  exit 1
fi

# Version bump logic
if [ "${VERSION_BUMP:-patch}" != "none" ]; then
  echo "Bumping version (type: ${VERSION_BUMP:-patch})..."

  CURRENT_VERSION=$(grep "^VERSION_NAME=" gradle.properties | cut -d'=' -f2)
  CURRENT_CODE=$(grep "^VERSION_CODE=" gradle.properties | cut -d'=' -f2)

  IFS='.' read -ra VERSION_PARTS <<< "$CURRENT_VERSION"
  MAJOR=${VERSION_PARTS[0]}
  MINOR=${VERSION_PARTS[1]}
  PATCH=${VERSION_PARTS[2]}

  case "${VERSION_BUMP:-patch}" in
    major) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
    minor) MINOR=$((MINOR + 1)); PATCH=0 ;;
    patch) PATCH=$((PATCH + 1)) ;;
  esac

  NEW_VERSION="$MAJOR.$MINOR.$PATCH"
  NEW_CODE=$((CURRENT_CODE + 1))

  sed -i.bak "s/^VERSION_CODE=.*/VERSION_CODE=$NEW_CODE/" gradle.properties
  sed -i.bak "s/^VERSION_NAME=.*/VERSION_NAME=$NEW_VERSION/" gradle.properties
  rm gradle.properties.bak || true

  echo "Version bumped to $NEW_VERSION (code $NEW_CODE)"
else
  echo "Skipping version bump (VERSION_BUMP=none)"
fi

# Set keystore environment variables for Gradle (use absolute path)
KEYSTORE_PATH="$(cd "$WORKSPACE_DIR" && pwd)/release.jks"
export KEYSTORE_FILE="$KEYSTORE_PATH"

echo "Using keystore: $KEYSTORE_FILE"

# Build signed AAB
echo "Building release AAB..."
chmod +x gradlew
./gradlew clean bundleRelease --no-daemon --stacktrace

# Verify AAB was created
if [ ! -f app/build/outputs/bundle/release/app-release.aab ]; then
  echo "Error: AAB file not found"
  exit 1
fi

echo "AAB build completed successfully:"
ls -lh app/build/outputs/bundle/release/

echo "Build complete!"
