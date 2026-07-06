# Changelog

All notable changes to the **Osyster** project will be documented in this file.

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
