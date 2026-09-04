<p align="center">
  <a href="https://qtremors.github.io/osyster/">
    <img src="assets/Osyster.png" alt="Osyster Logo" width="120"/>
  </a>
</p>

<h1 align="center"><a href="https://qtremors.github.io/osyster/">Osyster</a></h1>

<p align="center">
  A private, modern Android system monitor and OS utility suite.
</p>

<p align="center">
  <a href="https://github.com/qtremors/osyster/releases/latest">
    <img src="https://img.shields.io/github/v/release/qtremors/osyster?label=Download%20APK&color=00E6FF&logo=android&logoColor=black" alt="Download APK" height="32">
  </a>
</p>

<p align="center">
  <a href="https://github.com/qtremors/osyster/releases"><img src="https://img.shields.io/github/downloads/qtremors/osyster/total?label=Total%20Downloads&color=00E6FF" alt="Total Downloads"></a>
  <a href="https://github.com/qtremors/osyster/releases"><img src="https://img.shields.io/github/downloads/qtremors/osyster/latest/total?label=Latest%20Downloads&color=00E6FF" alt="Latest Downloads"></a>
</p>
<p align="center">
  <img src="https://img.shields.io/badge/Android-7.0%2B-34A853?logo=android" alt="Android 7.0+">
  <img src="https://img.shields.io/badge/License-TSL-red" alt="License">
</p>

> [!NOTE]
> **Privacy first:** Osyster does not request `android.permission.INTERNET`. All system telemetry, process stats, and hardware metrics stay on your device.

## Why Osyster

Osyster is an offline Android system monitor and OS utility suite built for speed, privacy, and a clean native interface. It has no ads, trackers, accounts, network access, or hidden data collection.

## Download

Download the latest APK from [GitHub Releases](https://github.com/qtremors/osyster/releases) and install it on any device running Android 7.0 (Nougat, API 24) or newer.

Osyster requires no root access for its core diagnostic suite: processor jiffies, memory allocations, device specifications, and battery telemetry are read directly from accessible Android sysfs and procfs endpoints.

## Features

- **Private and offline:** No ads, accounts, trackers, data collection, or internet permission.
- **Interactive Bento Grid:** High-density landing dashboard presenting overall CPU load, live RAM allocation, Swap status, CPU temperatures, running process counts, and battery status at a glance.
- **Native Kernel Diagnostics:** Core-by-core processor load computed directly from `/proc/stat` active vs total delta jiffies, active CPU cluster frequencies, thermal zones, and real-time sparkline trend graphs drawn via custom Canvas paths.
- **RAM & SWAP Matrix:** Comprehensive memory allocation breakdowns (total RAM, used RAM, available, buffers, and cache) parsed directly from `/proc/meminfo` with visual progress gauges.
- **Active Tasks & Process Manager:** Scans the `/proc` directory structure to discover active running PIDs, matching them to process command names and RSS memory footprints. Includes full-text search and explicit process SIGKILL termination triggers.
- **Device & Hardware Specifications:** Detailed breakdown of hardware manufacturer, device model, board configurations, processor platform, supported ABIs, Android OS versions, API levels, security patches, and bootloaders.
- **Real-Time Battery Telemetry:** Monitors battery percentage, millivolt voltage levels, temperatures, health status, charging states, and connected power sources.
- **Sleek Slate Tech Interface:** Dark Slate Navy (`#0C1115`), Surface Slate (`#141C22`), Electric Cyan Neon (`#00E6FF`), Warm Amber (`#FFB300`), and Coral Rose (`#FF5252`) tailored for modern high-refresh OLED displays.

## Community and support

- Join the [Discord community](https://discord.gg/QgUjuNj9U8).
- Report bugs or request features through [GitHub Issues](https://github.com/qtremors/osyster/issues).
- Review changes in the [changelog](CHANGELOG.md).
- Read the [privacy policy](PRIVACY.md).

## Credits

Osyster is built by [Tremors](https://github.com/qtremors) with Kotlin and the Android platform. Thanks to the maintainers of:

- [AndroidX](https://developer.android.com/jetpack/androidx), [Jetpack Compose](https://developer.android.com/compose), [Material 3](https://m3.material.io/), and [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [Kotlin](https://kotlinlang.org/), [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines), [Kotlin Serialization](https://github.com/Kotlin/kotlinx.serialization), and [Immutable Collections](https://github.com/Kotlin/kotlinx.collections.immutable)
- [MaterialKolor](https://github.com/jordond/MaterialKolor) and [Graphics Shapes](https://developer.android.com/develop/ui/compose/graphics/draw/shapes)
- [Tailwind CSS](https://tailwindcss.com/), [Lucide](https://lucide.dev/), [Simple Icons](https://simpleicons.org/), [Roboto](https://fonts.google.com/specimen/Roboto), and [Outfit](https://fonts.google.com/specimen/Outfit) for the project website and visual presentation

The app's **Settings -> About -> Open Source Licenses** screen lists its runtime libraries and their licenses. Each project remains the property of its respective authors and is used under its own license.

## For developers

Architecture, project structure, technology choices, setup, build commands, testing, release signing, and maintenance guidance live in [DEVELOPMENT.md](DEVELOPMENT.md).

## License

Osyster is source-available under the **Tremors Source License (TSL)**. Viewing, forking, and derivative works require attribution; commercial use requires written permission.

Read [LICENSE.md](LICENSE.md) or the [web version](https://qtremors.github.io/license).

---

<p align="center">
  Made by <a href="https://github.com/qtremors">Tremors</a>
</p>
