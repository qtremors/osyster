# Osyster - Releases

> **Project:** Osyster
> **Version:** 0.1.0
> **Last Updated:** 2026-09-06

| Version | Release Date | Key Focus |
| :--- | :--- | :--- |
| [v0.1.0](#v010) | 2026-09-06 | First public release: offline system monitoring, Bento Grid dashboard, native kernel telemetry, App Stopper, network traffic tracking, and Material 3 Expressive UI |

---

# v0.1.0

**Release Date:** September 6, 2026

**Previous public release:** None (Initial Release)

**Development range included:** v0.0.1 through v0.1.0

**Known issues & roadmap:** Track active issues and ongoing engineering tasks in [TASKS.md](TASKS.md).

Osyster v0.1.0 is the first public release of Osyster, an offline Android system monitor and OS utility suite. It combines native Linux kernel diagnostics, hardware telemetry, process monitoring, offline network usage statistics, and application management into a responsive Material 3 Expressive interface.

## Highlights

- **Completely Private & Offline**: Zero network permissions, no accounts, no advertisements, no tracking SDKs, and volatile in-memory diagnostic processing.
- **Interactive Bento Grid Dashboard**: High-density landing screen presenting real-time CPU load, live RAM and SWAP allocations, CPU temperatures, running process counts, battery status, and network transfer speeds.
- **Native Kernel Diagnostics & Telemetry**: Core-by-core processor counters from `/proc/stat`, cluster clock frequencies, SoC thermal sensor monitoring, and real-time sparkline trend graphs.
- **Memory & SWAP Matrix**: Granular RAM capacity breakdowns (used, available, buffers, and cached) from `/proc/meminfo` with support for both 4 KB and 16 KB kernel page sizes.
- **Active Tasks & Process Manager**: Process exploration across visible OS tasks with command line inspection, UID ownership resolution, RSS memory metrics, instant search, and direct App Info shortcuts.
- **Offline Network Usage Monitoring**: Real-time throughput gauges, interactive daily/weekly/monthly timeline charts, interface-specific filters (Mobile vs. Wi-Fi), and per-application data breakdowns via Android NetworkStatsManager.
- **App Stopper Utility**: Managed application grid with configurable 4x to 6x density, active vs. stopped state indicators, ghost entries for uninstalled packages, and direct system force-stop workflows.
- **Material 3 Expressive Design**: Floating dock navigation shell with spring-animated tabs, tactile rotary haptics, expressive wavy progress indicators, dynamic color schemes, and System, Light, Dark, and OLED themes.

## Features & Architecture

### Bento Grid Dashboard

- **High-Density Widget Matrix**: Modular dashboard presenting overview cards for CPU Load, Active RAM, SWAP Usage, CPU and Battery Thermal status, Running Process count, Network Throughput, and Battery Power.
- **Dynamic Battery Telemetry**: Live readout of battery percentage, millivolt voltage levels, charging status, health states, and battery temperature with dynamic Celsius/Fahrenheit units.
- **Configurable Refresh Intervals**: Adjustable sampling rates supporting Fast (0.5s), Balanced (1.0s), and Eco (2.0s) modes to conserve battery when desired.
- **Direct Navigation Shortcuts**: Quick one-tap access from bento cards into dedicated Telemetry, Tasks, Network, and Settings subpages.

### Native Kernel Telemetry & CPU Diagnostics

- **Jiffy-Based CPU Counters**: Real-time delta jiffy calculation parsed directly from `/proc/stat` to measure active versus idle processor ticks without frequency estimation bias.
- **Topology & Frequencies**: Cluster clock frequencies monitored across multi-core big.LITTLE and DynamIQ processor architectures.
- **SoC Thermal Zones**: Core temperatures queried from verified CPU and SoC sensor thermal zones in `/sys/class/thermal/`.
- **Sparkline Visualizations**: Custom Canvas-rendered sparkline graphs tracking real-time load trends over rolling temporal windows.
- **Graceful Sandbox Indicators**: Explicit "Restricted by OS" badges when OEM security configurations restrict access to `/proc/stat`.

### Memory & SWAP Matrix

- **Granular Allocation Breakdown**: Direct parsing of `/proc/meminfo` exposing total RAM, active memory, available memory, kernel buffers, and page cache.
- **16 KB Page Compatibility**: Accurate memory accounting supporting modern 16 KB kernel page alignments alongside standard 4 KB page devices.
- **SWAP Telemetry**: Real-time tracking of zRAM and SWAP capacity, used memory, and free reserves.
- **Wavy Progress Indicators**: Visual progress meters rendered with Material 3 Expressive wavy indicators for intuitive capacity assessment.

### Active Tasks & Process Management

- **Process Discovery**: Real-time scan of `/proc` directory structure identifying active PIDs, command names, and UID ownership.
- **Resource Footprints**: Precise RSS (Resident Set Size) memory allocation accounting for each running process.
- **Full-Text Filter & Search**: Instant query filtering by process name and process ID.
- **Kernel Thread Filtering**: Toggleable display filter to isolate user-space applications from kernel worker threads.
- **Compliant Force-Stop Operations**: Direct shortcuts to Android system App Info screens, enabling safe, platform-compliant application termination.
- **Pull-to-Refresh Gestures**: Smooth swipe gestures to force immediate process table rescans.

### Network Usage & Traffic Monitoring

- **Live Bandwidth Gauges**: Real-time download and upload transfer rates queried from system network statistics.
- **Historical Consumption Tracking**: Granular data usage totals across daily, weekly, and monthly cycles via Android NetworkStatsManager.
- **Interactive Timeline Charts**: Dual-color bar charts separating download and upload traffic with TalkBack accessibility semantics.
- **Interface-Specific Chips**: Independent filtering for Mobile Data and Wi-Fi connections.
- **Per-Application Usage Breakdown**: Sorted listing of individual application consumption with direct application settings shortcuts.
- **Curved Usage Visualizations**: Capsule pill metric gauges and curved circular segments providing immediate usage context.

### App Stopper OS Utility

- **Managed Application Grid**: High-density grid display with selectable 4-column, 5-column, or 6-column layouts saved to persistent storage.
- **Stopped-State Detection**: Active applications displayed in full color with animated badges; stopped or force-stopped applications rendered in dimmed grayscale.
- **Fast App Info Handoff**: Tapping any monitored application launches its system App Info page for quick force-stop operations, updating state immediately upon return.
- **Full-Height App Picker**: Bottom sheet with real-time package search and system application toggles for curating monitored apps.
- **Contextual Management Menu**: Long-press actions offering App Info, app launch, Play Store navigation, and list removal.
- **Ghost Application Sync**: Preserves uninstalled applications as ghost entries with Play Store reinstall shortcuts, keeping dashboard counts synchronized.

### Interface, Motion & Theming

- **Material 3 Expressive System**: Built with modern M3 Expressive tokens, surface container palettes, and fluid motion curves.
- **Standardized Floating Dock**: Consistent 296 dp floating dock navigation container featuring spring-animated expanding pills and docked search.
- **3-Tab Navigation Architecture**: Focused primary tabs (Dashboard, Telemetry, Tasks) connected by a gesture-driven horizontal pager with predictive back support.
- **Tactile Rotary Haptics**: Rotary bucketed feedback during pager swipes and crisp virtual key clicks on tab selection.
- **Flexible Theming Engine**: Four display themes (System Default, Light, Dark Slate Navy, and OLED Pure Black) paired with customizable accent palettes.
- **Accessibility & Internationalization**: Minimum 48 dp touch targets, high-contrast text ratios, comprehensive TalkBack semantics, and centralized XML string resources.

### Privacy, Security & Platform Compliance

- **Zero Network Permissions**: The application manifest completely omits `android.permission.INTERNET`, ensuring zero outbound data transmission.
- **Volatile Diagnostic State**: Telemetry samples and real-time statistics exist in volatile memory only and are never saved to a local database.
- **Foreground-Restricted Polling**: Recurring ViewModel telemetry automatically pauses when screens are hidden or the application is sent to the background.
- **Hardened Build Packaging**: Cleartext traffic explicitly disabled and backup rules configured to exclude preferences and diagnostic caches.
- **Transparent Permissions**: Usage Access is requested strictly for NetworkStatsManager queries, and Query All Packages is utilized solely for local installed app enumeration.
