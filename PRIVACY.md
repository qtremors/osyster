# Privacy Policy for Osyster

**Last Updated:** 2026-09-06

Osyster is built with a privacy-first, local-only architecture. This policy explains how the application handles your system and device information.

## 1. No Data Collection

Osyster **does not send** personal information, usage statistics, device identifiers, or telemetry to the developer or an external service. It reads diagnostics locally and saves preferences, managed package names, and cached app labels on your device. No account is required.

## 2. Offline by Design

The Android application does not declare `android.permission.INTERNET` and does not make internet requests. Network usage screens read records maintained locally by Android.

Links to GitHub, Discord, the project website, or Play Store open another application, which may use the internet under its own privacy policy. The project website loads external fonts/scripts and public GitHub statistics; the app's offline behavior does not apply to the website.

## 3. No Advertisements or Trackers

The application contains **zero advertisements** and **zero third-party analytics or tracking SDKs**.

## 4. Local System Diagnostics and Hardware Telemetry

Osyster reads native Linux kernel interfaces and Android system broadcasts strictly on-device to present live diagnostics in the Bento Grid interface:
- **Processor Telemetry:** Reads accessible CPU counters and frequency files. Android/vendor restrictions limit availability; the current frequency-ratio load fallback is not measured utilization.
- **Memory & Swap:** Parsed directly from `/proc/meminfo` to report RAM and SWAP allocations.
- **Task & Process Details:** Discovered via `/proc/[pid]` entries to display active tasks, command names, and RSS memory usage.
- **Thermals & Battery:** Read via Android system intents (`Intent.ACTION_BATTERY_CHANGED`) and accessible thermal sysfs nodes.
- **Device Information:** Android Build APIs provide manufacturer, model, hardware, and OS details.
- **Network Usage:** Usage access permits historical per-app/device network queries; network-state permission supports connection detection. Optional phone permission supports carrier metadata and older mobile-query fallbacks. Below Android 10, mobile queries may read a subscriber ID locally; Osyster does not save or transmit it.
- **Installed Apps:** Package visibility supports app names, icons, stopped-state checks, and App Stopper selection.
- **Notifications:** Osyster does not request notification permission; status readouts and thermal-alert notifications are not implemented.

Diagnostic snapshots and short chart histories remain in process memory rather than a saved telemetry database. Recurring telemetry polling stops when its screen is hidden or the app is not resumed. Some collector failures print stack traces to local Android logs; no logs are uploaded.

SharedPreferences save settings, onboarding completion, selected App Stopper packages, cached labels, and grid columns. Cached labels preserve ghost entries after uninstall. Removing a managed entry removes its saved package and label; clearing app storage removes saved preferences and managed-app information. Backup and device-transfer rules exclude SharedPreferences. About copy actions place the selected version or device summary on the Android clipboard at your request.

## 5. Local Process Control

App Stopper and Tasks open Android's App Info screen, where you can choose to force stop an application. Removing a managed entry does not uninstall or stop the app. On Android 13 and earlier, Tasks also offers Android's background-process management API. That action is hidden on Android 14 and newer. Arbitrary PID termination displays guidance instead of executing a shell kill command.

## 6. Source Availability

Osyster's source code is publicly available for inspection. You are welcome to audit the implementation yourself on [GitHub](https://github.com/qtremors/osyster).

## 7. Changes to This Policy

This policy may be updated as features change. Osyster's app diagnostics remain local; the date above identifies the latest revision.

---
[Back to Home](https://qtremors.github.io/osyster/)
