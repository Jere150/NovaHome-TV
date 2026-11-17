#!/usr/bin/env bash
# NovaHome TV Build & Install Script (Linux / macOS)
# Builds the launcher APK with Gradle and installs it directly to your Android TV over ADB.
#
# Usage:
#   TV_HOST=192.168.1.50 ./build-and-install.sh
#   (or simply ./build-and-install.sh if device is already connected via adb)

set -euo pipefail
cd "$(dirname "$0")"

echo ">>> Building NovaHome TV (Debug APK)..."
./gradlew assembleDebug

APK="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK" ]; then
    echo "ERROR: Build completed but APK was not found at $APK" >&2
    exit 1
fi

if [ -n "${TV_HOST:-}" ]; then
    echo ">>> Connecting to Android TV at $TV_HOST:5555..."
    adb connect "$TV_HOST:5555"
fi

echo ">>> Installing NovaHome TV ($APK)..."
adb install -r -d "$APK"

# Resolve target Application ID from gradle.properties or default to com.novahome.tv
APP_ID="com.novahome.tv"
if [ -f "gradle.properties" ]; then
    CUSTOM_ID=$(grep -E '^[[:space:]]*launcherApplicationId[[:space:]]*=' gradle.properties | cut -d'=' -f2 | tr -d '[:space:]' || true)
    if [ -n "$CUSTOM_ID" ]; then
        APP_ID="$CUSTOM_ID"
    fi
fi

echo ">>> Starting NovaHome TV ($APP_ID) on device..."
adb shell am start -n "$APP_ID/com.gathin.tvhome.MainActivity"
echo ">>> Done! NovaHome TV is ready."
