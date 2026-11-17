# NovaHome TV Build & Install Script (Windows PowerShell)
# Builds the launcher APK with Gradle and installs it directly to your Android TV over ADB.
#
# Usage:
#   .\build-and-install.ps1 -TvHost 192.168.1.50
#   (or simply .\build-and-install.ps1 if your device is already connected via adb)

param(
    [string]$TvHost = $env:TV_HOST
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

Write-Host ">>> Building NovaHome TV (Debug APK)..." -ForegroundColor Cyan
if (Test-Path ".\gradlew.bat") {
    .\gradlew.bat assembleDebug
} else {
    gradle assembleDebug
}

if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed."
}

$apk = "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) {
    throw "Build completed but APK was not found at $apk"
}

if ($TvHost) {
    Write-Host ">>> Connecting to Android TV at $TvHost:5555..." -ForegroundColor Cyan
    adb connect "$($TvHost):5555"
}

Write-Host ">>> Installing NovaHome TV ($apk)..." -ForegroundColor Cyan
adb install -r -d $apk
if ($LASTEXITCODE -ne 0) {
    throw "ADB installation failed. Make sure your TV has USB/Network debugging enabled."
}

# Resolve target Application ID from gradle.properties or default to com.novahome.tv
$appId = "com.novahome.tv"
if (Test-Path "gradle.properties") {
    $prop = Select-String -Path "gradle.properties" -Pattern "launcherApplicationId\s*=\s*(.+)" | ForEach-Object { $_.Matches[0].Groups[1].Value.Trim() }
    if ($prop) { $appId = $prop }
}

Write-Host ">>> Starting NovaHome TV ($appId) on device..." -ForegroundColor Green
adb shell am start -n "$appId/com.gathin.tvhome.MainActivity"
Write-Host ">>> Done! NovaHome TV is ready." -ForegroundColor Green
