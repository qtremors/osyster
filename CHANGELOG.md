# Osyster Changelog

> **Project:** Osyster
> **Version:** 0.0.7
> **Last Updated:** 2026-09-04

---

## [0.0.7] - 2026-09-04

- **App Stopper Grid & Stopped Detection**: Added high-density 4 to 6 column grid with persistent column settings, displaying active apps in full color and stopped apps in grayscale with dimmed opacity.
- **Direct System App Info Force Stop**: Tapping any monitored application launches its system App Info page for quick force-stop operations, updating state immediately upon return.
- **Docked Search & Top Bar Density Switcher**: Integrated inline search into the bottom floating dock with software keyboard tracking, alongside a top-bar 4x to 6x grid density dropdown.
- **Contextual Hold-to-Manage Actions**: Added long-press bottom sheet with options to open App Info, launch the app, open Google Play Store, or remove from the list.
- **Full-Height App Picker**: Redesigned app selection sheet expanding to full height with search and system apps toggle.
- **Dynamic Floating Dock Integration**: Adapted Osyster's floating dock for App Stopper with back navigation, marquee title, docked search, and Add Applications FAB.
- **Persistent Managed Storage**: Saved curated apps and column preferences to offline SharedPreferences across reboots.
- **Task Dashboard Shortcuts**: Added direct App Info force-stop shortcuts to running process cards on the Tasks dashboard.

---

## [0.0.6] - 2026-09-04

- **Network & Wi-Fi Data Usage Monitoring**: Added offline network traffic monitoring with real-time transfer speeds, adaptive dashboard Bento card with active connection detection (Wi-Fi and Mobile Data), interactive daily/weekly/monthly timeline bar charts with dual-color download and upload indicators, curved pill-segmented data usage circle with 4-stat metric grid, and per-application consumption breakdown with interface-specific chip filtering, search, and direct application settings shortcuts.
- **Standardized Floating Dock Dimensions**: Aligned dock geometry across main navigation and all subpages with uniform 296 dp width, elevation, and centered title balance.
- **Capsule Pill Metric Gauges**: Refined application breakdown bars, storage meters, and timeline activity slots into smooth rounded capsule pill segments.
- **Battery Temperature Telemetry on Dashboard**: Added live battery thermal metrics to the Battery Power Bento card, featuring a header temperature badge and combined status readout formatted in the user's preferred temperature unit (Celsius or Fahrenheit).
- **Expressive Wavy Progress for Dashboard RAM**: Replaced the linear bar in the Active RAM Bento block with a Material 3 Expressive wavy progress indicator.

---

## [0.0.5] - 2026-09-04

- **Streamlined 2-Page Onboarding Flow**: Added a focused first-run onboarding experience with an overview page highlighting offline privacy and kernel telemetry, followed by an elevated permissions page with feature dependency unlock lists for notifications and usage access.
- **Floating Dock Navigation Shell & Subpage Alignment**: Replaced the static bottom navigation bar with a floating dock container featuring spring-animated expanding pill indicators, docked contextual actions, and vertically centered back navigation with title marquee labels on subpages.
- **Streamlined 3-Tab Architecture**: Consolidated core monitoring screens into three focused destinations (Dashboard, Telemetry, and Tasks) with a unified CPU/RAM segmented switcher in Telemetry.
- **Predictive Back Navigation & Gestures**: Connected the core dashboards via a gesture-driven horizontal pager with predictive back navigation returning to the Bento dashboard with depth scaling.
- **Tactile Rotary Haptics**: Implemented rotary bucketed haptic feedback during page swipe transitions and crisp virtual key feedback on tab switches, integrated with user haptic preferences.

---

## [0.0.4] - 2026-09-04

- **Settings Screen & Reactive Preferences**: Added Material 3 Expressive settings screen backed by an offline-only SharedPreferences manager and Kotlin StateFlow stream, supporting theme mode (System, Light, Dark, OLED), accent palette swatches, telemetry sampling frequency, temperature units, and process filters.
- **About & System Specifications Screen**: Added dedicated About screen with project identity banner, copyable version and device hardware diagnostics, developer profile, privacy policy, and release channels.
- **Licenses & Credits Screen**: Added open-source software licenses screen and credits section cataloging runtime dependencies and upstream contributors with direct repository links.
- **Navigation Chrome Integration**: Added top bar settings action button, adaptive subpage top app bars with back navigation, and subpage bottom bar suppression.
- **Brand Identity & Icon Compatibility**: Integrated high-resolution rasterized Osyster brand logo for the About header, resolved adaptive icon Compose rendering crash, and standardized version display.

---

## [0.0.3] - 2026-09-04

- **Type-Safe Route Serialization**: Migrated navigation graph and bottom navigation bar to type-safe `@Serializable` destination contracts (`AppRoutes`).
- **Predictive Back Navigation**: Enabled Android 13+ predictive back gesture callback handling in application manifest.
- **Edge-to-Edge Chrome**: Configured activity edge-to-edge system bar layout with adaptive navigation padding.
- **Release Signing & Verification**: Added release keystore configuration with verified signing resolution, R8 minification, and build convention tasks.
- **Semantic Design Tokens**: Added semantic typography extensions (`titleLargeBold`, `filename`, `storageMetric`, `dangerLabel`) and cleaned XML resources.
- **System Font Optimization**: Standardized on native system typography, reducing APK footprint.

---

## [0.0.2] - 2026-09-04

### Added
- **Repository Parity & Licensing**:
  - Added Tremors Source License (TSL) Version 1.0 in `LICENSE.md` establishing source-available distribution and attribution terms.
  - Added comprehensive `PRIVACY.md` detailing the offline-only architecture, complete absence of network permissions, zero telemetry collection, and local volatile memory boundaries for system telemetry.
- **Task Roadmap & Engineering Documentation**:
  - Added `TASKS.md` tracking development across kernel diagnostics, baked-in OS utilities, process controls, and bento UI features.
  - Overhauled `README.md` and `DEVELOPMENT.md` with complete architectural documentation, mathematical kernel delta jiffy formulas, memory token parsing, and verification guides.
- **Slate Tech Documentation Website**:
  - Created a dedicated single-page site in `docs/` with the Slate Tech Bento design, live animated Canvas sparkline graph, GitHub release download counters, responsive feature cards, developer profile, and FAQ accordion.
- **Build Engineering & Toolchain**:
  - Updated Gradle wrapper to 9.5.0 and Android Gradle Plugin to 9.3.2.
  - Bumped project versionCode to 2 and versionName to 0.0.2.

---

## [0.0.1] - 2026-07-06

### Added
- **Interactive Bento Grid Dashboard**:
  - Implemented an original bento-layout dashboard displaying widgets for CPU load, RAM allocation, Swap status, CPU temperatures, running process count, and battery status.
  - Added a custom-drawn `OysterArcGauge` canvas ring for CPU visualization.
- **Gradle Build Suffixes & Placeholders**:
  - Configured `debug` build type with `applicationIdSuffix = ".debug"`, `versionNameSuffix = "-debug"`, and `manifestPlaceholders["appLabel"] = "Osyster Debug"`.
  - Configured `release` build type with `manifestPlaceholders["appLabel"] = "Osyster"`.
- **AndroidManifest Registration**:
  - Declared `MainActivity` inside the manifest with the `android.intent.action.MAIN` launcher filter.
  - Mapped app and activity labels to `${appLabel}` to load custom labels dynamically.
- **Custom UI Architecture**:
  - Developed custom dashboards styled with standard Material 3 progress indicators and sliders, without referencing or copying layouts from external apps.
- **Memory & Swap Monitor**:
  - Parsed RAM capacity details (Total, Used, Free, Cache, Buffer) and SWAP allocations from `/proc/meminfo`.
  - Visualized active memory distributions using standard clean progress indicators.
- **Active Tasks & Process Manager**:
  - Added a search-enabled active tasks list scanning the `/proc` directory.
  - Built a task details bottom sheet with resource info (PID, user, RSS memory).
  - Implemented task termination triggers via system SIGKILL signals.
- **System Information & Battery Diagnostics**:
  - Added build, board, processor platform, manufacturer, and ABI data collectors.
  - Registered real-time battery voltage, charging status, health, and temp sensors.
