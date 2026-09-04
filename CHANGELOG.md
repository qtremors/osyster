# Changelog

All notable changes to the **Osyster** project will be documented in this file.

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
