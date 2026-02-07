# Automated Version Management

This document explains the automated version management system implemented for the Blurr Android app.

## Overview

The app now automatically increments version numbers **only for release builds**, not debug builds. This eliminates the need to manually update version codes and names before each release.

## How It Works

### Files Involved

1. **`version.properties`** - Stores current version numbers
2. **`app/build.gradle.kts`** - Modified to read versions from properties file and includes auto-increment task

### Version Storage

The `version.properties` file contains:
```properties
VERSION_CODE=13           # Integer that increments by 1 each release
VERSION_NAME=1.0.13      # Semantic version (x.y.z) - patch number increments
```

### Auto-Increment Logic

- **Version Code**: Increments by 1 (13 → 14 → 15...)
- **Version Name**: Increments patch version (1.0.13 → 1.0.14 → 1.0.15...)

### Build Behavior

| Build Type | Version Increment | Command |
|------------|------------------|---------|
| **Debug** | ❌ No increment | `./gradlew assembleDebug` |
| **Release** | ✅ Auto increment | `./gradlew assembleRelease` |
| **Release Bundle** | ✅ Auto increment | `./gradlew bundleRelease` |

## Usage

### Building Debug (No Version Change)
```bash
./gradlew assembleDebug
# Version stays the same - no increment
```

### Building Release (Auto Increment)
```bash
./gradlew assembleRelease
# Automatically increments version before building
# Updates version.properties file
```

### Manual Version Increment (if needed)
```bash
./gradlew incrementVersion
# Manually increment version without building
```

## What Changed

### Before
```kotlin
defaultConfig {
    versionCode = 13
    versionName = "1.0.13"
}
```

### After
```kotlin
defaultConfig {
    versionCode = versionProps.getProperty("VERSION_CODE", "13").toInt()
    versionName = versionProps.getProperty("VERSION_NAME", "1.0.13")
}
```

## Benefits

1. **No Manual Updates**: Version numbers update automatically for releases
2. **Debug Safety**: Debug builds don't change version numbers
3. **Git Trackable**: Version changes are committed with the release
4. **Semantic Versioning**: Follows standard x.y.z pattern
5. **Rollback Safe**: Can manually edit version.properties if needed

## Troubleshooting

### If Version Doesn't Increment
- Ensure you're running a release build (`assembleRelease` or `bundleRelease`)
- Check that `version.properties` file exists and is writable

### If Build Fails
- Verify `version.properties` has valid integer values
- Check that file permissions allow writing to `version.properties`

### Manual Reset
To manually set version numbers, edit `version.properties`:
```properties
VERSION_CODE=20
VERSION_NAME=1.1.0
```

## Technical Details

The implementation uses:
- Gradle task registration for `incrementVersion`
- Task dependencies to hook into release builds only
- Properties file I/O for persistent version storage
- Semantic versioning logic for automatic patch increments