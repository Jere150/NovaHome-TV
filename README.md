# NovaHome TV (v1.0 Stable)

<p align="center">
  <strong>Ultra-lightweight, hardware-accelerated, and privacy-focused Android TV launcher written in 100% pure Java 17.</strong><br>
  <em>Pure Java 17 &bull; Native Android UI &bull; AndroidX RecyclerView &amp; DynamicAnimation &bull; No Jetpack Compose &bull; No heavy runtime frameworks</em>
</p>

<p align="center">
  <a href="README.md"><strong>English</strong></a> &bull; 
  <a href="README.es.md"><strong>Español</strong></a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Release-v1.0--Stable-brightgreen?style=flat-square" alt="Release">
  <img src="https://img.shields.io/badge/Android%20TV-8.0%2B%20(API%2026%2B)-blue?style=flat-square" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Pure%20Java%2017-orange?style=flat-square" alt="Language">
  <img src="https://img.shields.io/badge/Dependencies-Minimal%20AndroidX%20(No%20Compose)-success?style=flat-square" alt="Dependencies">
  <img src="https://img.shields.io/badge/RAM%20Usage-19.5%20--%2028.6%20MB%20Private%20Dirty-success?style=flat-square" alt="RAM">
  <img src="https://img.shields.io/badge/License-MIT-lightgrey?style=flat-square" alt="License">
</p>

---

## Visual Showcase

| Minimalist Home & Dock | Expanded Application Drawer |
|:---:|:---:|
| <img src="docs/screenshots/01_home_dock.webp" width="480" alt="Home Dock"/> | <img src="docs/screenshots/02_app_drawer.webp" width="480" alt="App Drawer"/> |
| *Focused cards on clean dark glassmorphism dock.* | *Continuous 3-row grid with fluid GPU-accelerated navigation.* |
| Art Mode (Uncluttered) | Cinematic OLED Screensaver |
| <img src="docs/screenshots/03_art_mode.webp" width="480" alt="Art Mode"/> | <img src="docs/screenshots/04_screensaver.webp" width="480" alt="Screensaver"/> |
| *Clean ambient backdrop with minimal clock and dynamic date.* | *Ultra-lightweight floating clock with anti-burn-in protection.* |

---

## Key Highlights

* **Lean Native AndroidX Architecture:** Built 100% in pure Java 17 using native Android APIs and lean modular AndroidX support (`recyclerview:1.3.2` and `dynamicanimation:1.0.0`). Completely free of heavy runtime layers (no Jetpack Compose, no Glide, no Lottie, no telemetry or advertising SDKs).
* **Exceptional Memory Efficiency:** Operates between **19.5 MB – 28.6 MB Private Dirty RAM** across all typical usage states, freeing up maximum memory for demanding streaming apps and games.
* **High-Refresh Display Architecture (60 Hz – 144 Hz ready):** Hardware-accelerated GPU animations (`translationY`, `alpha`, spring physics) engineered to synchronize with native panel refresh rates (tested at locked 60 FPS on Mi Box 4, designed to scale up to 120/144 Hz on high-refresh TV panels).
* **Demand-Driven Dock:** A clean install starts completely uncluttered with 0 pinned icons. Pin up to 6 favourite apps on demand with an intuitive 6/6 interactive replacement dialog.
* **Millimeter-Accurate HSV Color Picker:** Native Canvas-drawn 2D HSV matrix with dedicated horizontal saturation bar and fine-grained D-Pad step control (50 steps per axis).
* **Pixel-Aligned Floating Quick Settings:** The settings window dynamically synchronizes its right edge with the dock capsule down to the exact subpixel.
* **Bilingual Core & Extended Localization:** Fully audited and maintained natively in English and Spanish, with dynamic locale-aware date/clock formatting. Also retains 23 inherited localization resource sets from upstream (`res/values-*`) across standard Leanback TV strings.
* **Privacy by Design:** Zero analytics, zero telemetry, zero advertising SDKs, and zero background network polling.

---

## Remote Control Navigation (D-Pad)

| Action | Remote Key |
|---|---|
| **Navigate apps** | `D-Pad Left / Right / Up / Down` |
| **Open focused app** | `Center Button (OK)` |
| **Expand application drawer** | `D-Pad Down` (from collapsed dock) |
| **Collapse drawer to dock** | `D-Pad Up` or `Back Button` |
| **Tile context menu** | `Hold OK (Long Press)` on any tile |
| **Move tile within dock** | Select *Move* in inline menu, then use `D-Pad Left / Right` |
| **Open Quick Settings** | `D-Pad Up` (from top row) or select settings gear icon |

---

## Memory Footprint & Real-Device Benchmarks

Tested on **Xiaomi Mi Box 4** (Model MDZ-16-AB, 2 GB RAM, Android TV 9 / API 28) measured via Android native memory inspector (`dumpsys meminfo com.novahome.tv`):

| Operational State | Private Dirty RAM | Total PSS | Dalvik Heap | Measurement Conditions |
|---|---|---|---|---|
| **Idle / Home Screen** | **~19.5 MB** | ~26 MB | ~1.1 MB | Clean boot, minimalist dock collapsed (3 pinned cards). |
| **Active App Browsing** | **~20.7 MB** | ~27 MB | ~1.3 MB | Continuous navigation across 3-row expanded app drawer. |
| **Quick Settings Open** | **~21.6 MB** | ~28 MB | ~1.4 MB | Real-time HSV picker, slider dragging, and theme changes. |
| **Peak Usage / Stress** | **< 28.6 MB** | ~35 MB | ~2.1 MB | Rapid continuous scrolling and repetitive dialog toggling. |

---

## Hardware Tier Performance & Compatibility

NovaHome TV dynamically adjusts its rendering pipeline according to available system memory via `isLowRamDevice()`:

| Hardware Tier | Benchmark Devices | Typical Hardware Specs | Behavior & Performance with NovaHome TV |
|---|---|---|---|
| **Entry / Budget Tier** | Haier 32" Smart TV, budget Noblex, entry TCL sets | $\le 1$ GB RAM, quad-core Cortex-A53 MediaTek / Realtek SoC, Mali-470 / 450 GPU | **Full Revitalization:** Stock Google TV / Android TV launcher saturates device memory. NovaHome runs at < 28 MB RAM with a lightweight 150 ms animation profile and direct software rendering to eradicate GPU composition tearing. |
| **Mid-Range / Standard Tier** | Xiaomi Mi Box 4 / 4K, Chromecast with Google TV HD/4K, Fire TV Stick 4K | 1.5 GB – 2 GB RAM, Amlogic S905X / S905L SoC, penta-core Mali-450 GPU | **Rock-solid 60 FPS:** Smooth multitasking at 4K resolution. The 2 GB RAM headroom prevents the OS from killing the launcher process when switching between demanding streaming services. |
| **Upper Mid-Range / Premium Tier** | TCL 55P8L (QD-Mini LED), Sony Bravia XR, Philips The One / OLED | 3 GB RAM or more, MediaTek Pentonic 700 (MT9653) Cortex-A73, 120 Hz / 144 Hz display panel | **Ultimate Performance:** Native ultra-high refresh rate activation (`configureHighRefreshRate()`) at 120 Hz / 144 Hz with zero VSync tearing. 3 GB RAM and fast UFS storage guarantee console-grade instant app transitions. |

---

## System Requirements

* **OS:** Android TV / Google TV 8.0 Oreo (API level 26) or higher.
* **Resolution:** 720p, 1080p Full HD, 4K UHD.
* **Input:** Standard Android TV D-Pad remote control.

---

## Installation

> [!NOTE]
> **System Memory & Optimization Note:** NovaHome TV operates independently consuming under 28 MB of RAM with zero telemetry. However, to reclaim 200–400 MB of system RAM on your TV and prevent the stock Google launcher from running in the background, disabling the stock launcher via ADB is recommended.

### Option A: Install Pre-Built Release APK

1. Download **`NovaHome-v1.0.apk`** from [GitHub Releases](https://github.com/Jere150/NovaHome-TV/releases/tag/v1.0).
2. Enable **Developer Options** and **Network / USB Debugging** on your Android TV.
3. Connect your computer to your TV over ADB:
   ```bash
   adb connect <YOUR_TV_IP>:5555
   ```
4. Install the release package:
   ```bash
   adb install -r NovaHome-v1.0.apk
   ```

---

### Option B: Build and Install from Source

To compile from source and deploy to your Android TV:

```bash
# Clone the repository
git clone https://github.com/Jere150/NovaHome-TV.git
cd NovaHome-TV

# Build the debug APK with Gradle wrapper
./gradlew assembleDebug

# Install the compiled APK directly to your connected TV
adb install -r -d app/build/outputs/apk/debug/app-debug.apk
```

#### Automated One-Step Deploy Scripts
You can also build and install automatically in a single command:
* **Linux / macOS:**
  ```bash
  TV_HOST=192.168.1.50 ./build-and-install.sh
  ```
* **Windows (PowerShell):**
  ```powershell
  .\build-and-install.ps1 -TvHost 192.168.1.50
  ```

---

### Option C: Direct Installation via USB Drive or Transfer App (No Computer Required)

If you prefer not to use a computer or ADB command line:

1. Download **`NovaHome-v1.0.apk`** directly from [GitHub Releases](https://github.com/Jere150/NovaHome-TV/releases/tag/v1.0) on your phone or computer.
2. Transfer the APK file to your Android TV using either method:
   * **USB Flash Drive:** Copy the APK to a USB pendrive and connect it to your TV's USB port.
   * **Wireless Transfer App:** Send the file over Wi-Fi using apps like *Send Files to TV (SFTV)* or *LocalSend* installed on both your smartphone and TV.
3. Open any file explorer on your TV (e.g. *AnExplorer*, *FX File Explorer*, or *File Commander*).
4. Select the APK and click **Install**.
   *(If prompted by Android TV, grant the file explorer permission to "Install unknown apps" under TV Settings > Security & Restrictions).*

---

### Screensaver & Art Mode Setup

NovaHome TV includes a built-in low-power **Art Mode** and **cinematic screensaver** engine to prevent burn-in on OLED and LCD displays.

* **Native Behavior:** NovaHome maintains the screen active while its internal screensaver is visible.
* **Coexistence with Google Backdrop:** On certain Android TV / Google TV devices, the OS system screensaver may force-activate if remote inactivity is detected. To ensure uninterrupted playback of NovaHome's screensaver and Art Mode, you can disable the system daydream from:
  * *TV Settings -> Device Preferences -> Screensaver -> Turn off (or increase idle timer)*.
  * Or by executing the following ADB command:
    ```bash
    adb shell settings put secure screensaver_enabled 0
    ```

---

## License & Third-Party Attribution

NovaHome TV is released under the [MIT License](LICENSE).

### Upstream Attribution & Credits
NovaHome TV originated as a customized evolution derived from [CleanYourTV](https://github.com/Gathin23/cleanyourtv) by [Gathin](https://github.com/Gathin23) (licensed under MIT).

* **Base Foundation:** Initial Leanback activity scaffolding, fundamental D-pad navigation structure, and asset configurations.
* **NovaHome TV Contributions & Custom Engineering:**
  * Completely revamped pinned dock system (demand-driven 0 to 6 items with 6/6 interactive replacement dialog).
  * Symmetrical fixed-width 6-tile dock capsule rendering in custom Canvas (`DockDecoration`).
  * Floating Quick Settings panel dynamically aligned with the dock's right edge.
  * Native 2D Canvas HSV color picker with dedicated saturation slider and fine-stepped D-pad matrix.
  * Real-time dock, glow, and settings color synchronization engine.
  * Ambient screensaver and Art Mode system with anti-burn-in clock protection.
  * Automatic bilingual localization (English / Spanish) with dynamic date formatting.
  * Strict memory footprint optimization (< 28.6 MB Private Dirty RAM) in pure Java 17 with lean AndroidX UI architecture.
* **Author & Lead Maintainer:** [Jere150](https://github.com/Jere150).
