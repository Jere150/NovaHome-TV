# NovaHome TV v1.0.0 Stable

Welcome to the official launch of **NovaHome TV (v1.0.0 Stable)** — an ultra-lightweight, hardware-accelerated, and privacy-first application launcher crafted specifically for Android TV and Google TV devices.

---

### Project Overview

NovaHome TV was engineered from the ground up to eliminate launcher lag, intrusive ads, and bloated dependencies on TV devices. Written in **100% pure Java 17**, it offers a fluid, minimalist, and responsive Leanback experience while operating between **19.5 MB – 28.6 MB Private Dirty RAM** across all typical usage scenarios.

---

### Key Features & Capabilities

* **Lean Native Java 17 Architecture:** Pure Java 17 with minimal AndroidX modular support (`recyclerview` and `dynamicanimation`). 0 Compose runtime bloat, 0 telemetry. Clean and lean ~3.3 MB APK package.
* **Minimalist Glassmorphism Dock:** Dynamic floating dock capsule drawn in native Canvas with real-time ambient glow matching the active application.
* **Continuous 3-Row App Drawer:** Seamless vertical expansion providing smooth browsing across all installed apps with instant focus retention.
* **Demand-Driven Dock:** A clean install starts with an uncluttered screen (0 pinned apps). Pin up to 6 favourites at will, with an intuitive 6/6 card replacement dialog when full.
* **Millimeter-Accurate HSV Color Picker:** Native Canvas-drawn 2D HSV matrix with dedicated horizontal saturation bar and fine-grained D-Pad step control (50 steps per axis).
* **Pixel-Aligned Floating Quick Settings:** Real-time settings window dynamically synchronizes its right edge with the dock capsule down to the exact subpixel.
* **Cinematic Screensaver & Art Mode:** Ultra-low-overhead ambient clock and screensaver engine designed to prevent OLED / LED panel burn-in.
* **Automatic Bilingual Support:** Built-in automatic localization for English and Spanish, adapting dynamic dates, clocks, and dialogs to system language.

---

### Performance & Hardware Footprint

Tested on **Xiaomi Mi Box 4** (Model MDZ-16-AB, 2 GB RAM, Android TV 9 / API 28) via Android native inspector (`dumpsys meminfo com.novahome.tv`):

| Operational State / Metric | Result | Measurement Conditions |
|---|---|---|
| **Idle / Home Screen** | **~19.5 MB** | Clean boot, minimalist dock collapsed (3 pinned cards). |
| **Active App Browsing** | **~20.7 MB** | Continuous navigation across 3-row expanded app drawer. |
| **Quick Settings Open** | **~21.6 MB** | Real-time HSV picker, slider dragging, and theme changes. |
| **Peak Usage / Stress** | **< 28.6 MB** | Rapid continuous scrolling and repetitive dialog toggling. |
| **Dalvik Heap Dirty** | **~1.1 MB – 2.1 MB** | Ultra-compact garbage-collected memory footprint. |
| **Framerate Stability** | **60 FPS locked** | GPU-accelerated spring animations and Leanback views. |
| **APK File Size** | **3.31 MB** | Standalone APK without heavy runtime bundles. |
| **External Dependencies** | **Minimal AndroidX** | Pure Java 17 with `recyclerview:1.3.2` & `dynamicanimation:1.0.0`. |

---

### Upstream Attribution

NovaHome TV originated as an independent custom re-engineering based on the open-source Leanback architecture of [CleanYourTV](https://github.com/Gathin23/cleanyourtv) by [Gathin](https://github.com/Gathin23) (MIT Licensed).

* **Author & Lead Maintainer:** [Jere150](https://github.com/Jere150).

---

### Assets

* **`NovaHome-v1.0.0.apk`** — Production-ready APK package compatible with Android TV 8.0 (API 26) through Android TV 14+.
* **Source code (zip / tar.gz)** — Complete clean source tree.
