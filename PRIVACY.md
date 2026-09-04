# Privacy Policy for Osyster

**Last Updated:** 2026-09-04

Osyster is built with a privacy-first, local-only architecture. This policy explains how the application handles your system and device information.

## 1. No Data Collection

Osyster **does not collect, store, or transmit** any personal information, usage statistics, device identifiers, or telemetry from your device.

## 2. Offline by Design

Osyster is designed to operate entirely offline. The application does not declare the `android.permission.INTERNET` permission in its manifest. As a result, the Android operating system prevents Osyster from establishing network sockets or transmitting files, telemetry, or system metrics over the internet.

## 3. No Advertisements or Trackers

The application contains **zero advertisements** and **zero third-party analytics or tracking SDKs**.

## 4. Local System Diagnostics and Hardware Telemetry

Osyster reads native Linux kernel interfaces and Android system broadcasts strictly on-device to present live diagnostics in the Bento Grid interface:
- **Processor Telemetry:** Read from `/proc/stat` and per-core scaling files to calculate instantaneous CPU load and frequencies.
- **Memory & Swap:** Parsed directly from `/proc/meminfo` to report RAM and SWAP allocations.
- **Task & Process Details:** Discovered via `/proc/[pid]` entries to display active tasks, command names, and RSS memory usage.
- **Thermals & Battery:** Read via Android system intents (`Intent.ACTION_BATTERY_CHANGED`) and standard thermal sysfs nodes.

All metrics are processed transiently in volatile memory and are never logged, exported, or shared.

## 5. Local Process Control

When you request task termination in the Process Manager, Osyster sends a standard Linux `SIGKILL` signal to the targeted process using local JVM Runtime execution. This operation is initiated exclusively by you, runs entirely on your device, and is never logged externally.

## 6. Source Availability

Osyster's source code is publicly available for inspection. You are welcome to audit the implementation yourself on [GitHub](https://github.com/qtremors/osyster).

## 7. Changes to This Policy

This policy may be updated as new features and system utilities are introduced. However, the foundational commitments: **absolute privacy, offline-only architecture, and zero data collection**, will remain unchanged.

---
[Back to Home](https://qtremors.github.io/osyster/)
