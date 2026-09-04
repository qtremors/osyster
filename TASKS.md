# Osyster - Tasks

> **Project:** Osyster
>
> **Version:** 0.0.3
>
> **Last Updated:** 2026-09-04

---

## 1. System Monitoring & Kernel Telemetry

- [ ] **Frequency Scaling & Governors:**
  - [ ] Add real-time CPU frequency scaling governor inspector (interactive, schedutil, performance, powersave).
  - [ ] Support per-cluster frequency tracking across big.LITTLE / DynamIQ topologies.
- [ ] **Storage I/O Diagnostics:**
  - [ ] Parse `/proc/diskstats` for live read/write bandwidth throughput metrics.
  - [ ] Add disk latency benchmarks for internal UFS/eMMC and external SD cards.
- [ ] **Network Interface Telemetry:**
  - [ ] Monitor real-time local network interface bytes (`/proc/net/dev`) for Wi-Fi and mobile networks rootless.
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
  - [ ] Sort by RSS memory, thread counts, or CPU jiffy consumption.
- [ ] **Thread-Level Inspection:**
  - [ ] Drill down into individual threads within `/proc/[pid]/task/`.
- [ ] **Batch Process Actions:**
  - [ ] Multi-select process termination for runaway background tasks.

---

## 4. Bento Grid UI & Personalization

- [ ] **Customizable Bento Layouts:**
  - [ ] Enable drag-and-drop tile reordering.
  - [ ] Support resizable bento cards (1x1, 2x1, 2x2, full-width).
- [ ] **True OLED Theme:**
  - [ ] Add pitch-black background token (`#000000`) for maximum power savings on OLED displays.
- [ ] **Home Screen System Widgets:**
  - [ ] Build Jetpack Glance home screen widgets matching the Bento Grid aesthetic.

---

## 5. Automation & Notifications

- [ ] **Thermal Throttling Alerts:**
  - [ ] Trigger opt-in local notifications when CPU core temperatures exceed critical thresholds.
- [ ] **Battery Health & Charge Alarms:**
  - [ ] Add notifications for custom battery charge limits (e.g., 80% stop notifications to extend lifespan).
  - [ ] Log charge cycles and estimate battery wear degradation over time.
