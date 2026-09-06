# Osyster - Tasks

> **Project:** Osyster
>
> **Version:** 0.1.0
>
> **Last Updated:** 2026-09-06

---

## 1. System Monitoring & Kernel Telemetry

- [ ] **Frequency Scaling & Governors:**
  - [ ] Add real-time CPU frequency scaling governor inspector (interactive, schedutil, performance, powersave).
  - [ ] Support per-cluster frequency tracking across big.LITTLE / DynamIQ topologies.
- [ ] **Storage I/O Diagnostics:**
  - [ ] Parse `/proc/diskstats` for live read/write bandwidth throughput metrics.
  - [ ] Add disk latency benchmarks for internal UFS/eMMC and external SD cards.
- [ ] **Network Interface Telemetry:**
  - [ ] Add separate live Wi-Fi and mobile rates where Android exposes the counters; aggregate live speed and historical interface filters already exist.
  - [ ] Add per-interface live bandwidth meters and active socket monitors.
- [ ] **GPU & Graphics Telemetry:**
  - [ ] Query Adreno/Mali GPU load and clock states where vendor sysfs paths are accessible.
  - [ ] Add FPS overlay meter for real-time frame drop diagnostics.

---

## 2. Baked-in OS Utilities & Power Tools

- [ ] **Dynamic Quick Settings Tiles:**
  - [ ] Build a CPU load quick tile with live percentage readout.
  - [ ] Build a one-tap RAM release / background process trim tile.
  - [ ] Add quick reboot, soft reboot, and recovery shortcut tiles.
- [ ] **Per-App Volume Mixer:**
  - [ ] Create an independent volume slider overlay for concurrent media streams.
- [ ] **System-Wide Clipboard History:**
  - [ ] Implement a local, encrypted clipboard history database with instant search and auto-clearing.
- [ ] **Sensor Diagnostics & Calibration:**
  - [ ] Add a comprehensive sensor bench: accelerometer, gyroscope, magnetometer, barometer, and proximity sensor tests.
- [ ] **App Freezer & Background Limiter:**
  - [ ] Implement dormant app suspension tools to reduce idle battery drain.

---

## 3. Process Management & Resource Controls

- [ ] **Advanced Task Filtering & Sorting:**
  - [ ] Filter tasks by User, System, and Foreground/Background states.
  - [ ] Add selectable sort modes for memory, thread counts, or CPU usage where available; the list already sorts by descending RSS.
- [ ] **Thread-Level Inspection:**
  - [ ] Drill down into individual threads within `/proc/[pid]/task/`.
- [ ] **Batch Process Actions:**
  - [ ] Multi-select process termination for runaway background tasks.

---

## 4. Bento Grid UI & Personalization

- [ ] **Customizable Bento Layouts:**
  - [ ] Enable drag-and-drop tile reordering.
  - [ ] Support resizable bento cards (1x1, 2x1, 2x2, full-width).
- [ ] **Home Screen System Widgets:**
  - [ ] Build Jetpack Glance home screen widgets matching the Bento Grid aesthetic.

---

## 5. Automation & Notifications

- [ ] **Thermal Throttling Alerts:**
  - [ ] Trigger opt-in local notifications when CPU core temperatures exceed critical thresholds.
- [ ] **Battery Health & Charge Alarms:**
  - [ ] Add notifications for custom battery charge limits (e.g., 80% stop notifications to extend lifespan).
  - [ ] Log charge cycles and estimate battery wear degradation over time.

---

## 6. Audit Remediation Backlog

Completed implementation entries have been removed. Remaining localization work and device verification are listed below; these items are not a committed release scope.

### Accessibility

- [ ] **A11Y-0004 - Remaining Device Accessibility Verification** `[Medium]`
  - **Location:** `ui/`
  - **Problem:** Device touch targets, insets, and large-text layouts still need manual verification.
  - **Fix:** Verify device accessibility, including the new chart period selector.
  - **Verification:** Check large text, TalkBack, keyboard use, and navigation insets.

### Internationalization & Localization

- [ ] **I18N-0001 - Remaining Hardcoded User-Facing Strings** `[Medium]`
  - **Location:** `monitor/, ui/viewmodel/, ui/NetworkDashboard.kt`
  - **Problem:** Most labels use resources, but battery statuses, chart descriptions, date/app labels, and error toasts remain hardcoded. Only English resources exist.
  - **Fix:** Extract remaining strings and avoid claiming translated UI until translations exist.
  - **Verification:** Inspect all user-facing strings and verify locale-sensitive formatting.

### Testing & Quality Assurance

- [ ] **TEST-0003 - Remaining Device Release Verification** `[Medium]`
  - **Location:** `ui/, monitor/`
  - **Problem:** Physical-device checks for permission revocation, foreground-only polling, OS-restricted telemetry, and 16 KB devices remain unverified.
  - **Fix:** Run the signed release on supported Android versions and exercise the permission, navigation, and telemetry flows.
  - **Verification:** Verify fresh install and upgrade, permissions, rapid filter changes, background/resume, App Info actions, and a 16 KB device.
