<p align="center">
  <img src="assets/Osyster.png" alt="Osyster Logo" width="120"/>
</p>

<h1 align="center">Osyster</h1>

<p align="center">
  An Advanced & Powerful System Monitoring and Management tool for Android.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-0.0.1-blueviolet" alt="Version">
  <img src="https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Compose_BOM-2026.05.00-4285F4?logo=jetpackcompose" alt="Compose BOM">
  <img src="https://img.shields.io/badge/Android-7.0%2B-34A853?logo=android" alt="Android 7.0+">
  <img src="https://img.shields.io/badge/License-Private-red" alt="License">
</p>

> [!NOTE]
> **Privacy Model** Osyster operates locally on your device. It does not request internet permissions and does not contain telemetry, trackers, or external ads.

---

## Why Osyster

Osyster is designed to give you deep, real-time insights into your Android device's hardware, processor performance, memory allocation, battery diagnostics, and background processes. It presents complex system logs and stats through an original, premium **Bento Grid Dashboard** interface built entirely with Jetpack Compose.

---

## Features

| Feature | Description |
|---------|-------------|
| **Bento Grid Dashboard** | A highly polished landing dashboard showing overall CPU load, live RAM utilization, SWAP status, CPU temperatures, running process count, and battery status at a glance. |
| **CPU Engine Monitor** | Core-by-core processor load (parsed from `/proc/stat`), active CPU cluster frequencies, system thermal zones, and real-time sparkline trend graphs drawn via custom Canvas paths. |
| **RAM & SWAP Details** | Comprehensive memory allocation breakdowns (total RAM, used RAM, available, buffers, and cache) parsed directly from `/proc/meminfo` with visual progress gauges. |
| **Active Tasks & Process Manager** | Scans the `/proc` directory structure to query active running PIDs, matching them to process command names and RSS memory values. Includes full text search and process SIGKILL termination triggers. |
| **Device Specifications** | Grouped card details reporting hardware manufacturer, device model, board configurations, processor platforms, supported ABIs, Android OS versions, API levels, security patches, and bootloaders. |
| **Battery Diagnostics** | Tracks battery level percentages, real-time millivolt voltage levels, temperatures, overall health status, charging states, and connected power sources. |
| **Sleek Dark Mode** | Premium Slate Navy, Neon Cyan, and Coral Amber HSL-tailored colors built around the modern Material 3 design system. |

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| **Language** | Kotlin 2.2.10 |
| **Android Gradle Plugin** | 9.2.1 |
| **UI** | Jetpack Compose BOM 2026.05.00, Material 3 1.5.0-alpha19 |
| **Navigation** | Navigation Compose |
| **Data Engine** | Native `/proc/stat`, `/proc/meminfo`, and `/proc/[pid]` file scanners |
| **Android Support** | SDK 24+ (Android 7.0 or newer) |

---

## Quick Start

### Build Commands

Run Gradle commands from the `osyster-app/` directory (`gradlew.bat` may be used instead of `./gradlew` on Windows):

```bash
# Build the debug APK
./gradlew :app:assembleDebug

# Build the release APK
./gradlew :app:assembleRelease
```

Build outputs will be generated under:
- Debug: `app/build/outputs/apk/debug/app-debug.apk` (packaged as `dev.qtremors.osyster.debug` with name "Osyster Debug")
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk` (packaged as `dev.qtremors.osyster` with name "Osyster")

### Debugging & Installation

Install the compiled APK on a USB-connected device via adb:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
