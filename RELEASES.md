# Osyster - Releases

> **Project:** Osyster
> **Version:** 0.2.0
> **Last Updated:** 2026-09-19

| Version | Release Date | Key Focus |
| :--- | :--- | :--- |
| [v0.2.0](#v020) | 2026-09-19 | Home-screen data and music widgets, expanded hardware diagnostics, Screen Time, modernized navigation and settings, and developer Stay Awake control |
| [v0.1.0](#v010) | 2026-09-06 | First public release: offline system monitoring, Bento Grid dashboard, native kernel telemetry, App Stopper, network traffic tracking, and Material 3 Expressive UI |

---

# v0.2.0

**Release Date:** September 19, 2026

**Previous public release:** v0.1.0

**Development range included:** v0.1.1 through v0.2.0

**Known issues and roadmap:** Track active issues and ongoing engineering tasks in [TASKS.md](TASKS.md).

Osyster v0.2.0 expands the private system monitor into a broader device utility suite. This release adds home-screen widgets, deeper hardware inspection, local Screen Time insights, streamlined navigation and settings, and an optional developer Stay Awake control while preserving the app's offline architecture.

## Highlights

- **Data Usage Widgets**: Place daily, monthly, or combined cellular and Wi-Fi totals on the home screen, refresh them directly, and jump into the Network dashboard.
- **Music Widgets**: Choose a 3x3 artwork disc or 5x1 capsule player with track details, high-resolution artwork, live playback state, and player-supported play, skip, shuffle, and repeat controls.
- **Stable Multi-Player Handling**: Music widgets follow one active media session at a time, keep controls and artwork attached to that player, and no longer prefer a specific music app.
- **Developer Stay Awake**: Toggle Android's plugged-in Stay awake setting from a compact dashboard row after a guided one-time ADB permission grant.
- **Screen Time Insights**: Review today's usage, a seven-day history, hourly activity, daily goals, and per-app foreground time using Android's local usage history.
- **Expanded Hardware Diagnostics**: Inspect display, SoC and GPU, storage, battery, cameras, live sensors, connectivity, DRM, and security details from dedicated pages.
- **Modernized Navigation and Motion**: Reach every subsystem from the Overview ribbon and three-pillar dock with spring transitions, tactile feedback, smoothed gauges, and rolling metric animations.
- **Settings and Portability**: Use the redesigned Settings hub, export or restore preferences locally, review bundled licenses, and choose System, Light, Dark, or OLED presentation.

## Home-Screen Widgets

### Data Usage

- Daily and monthly widgets show separate cellular and Wi-Fi totals.
- The combined widget presents both time periods in one view.
- Refresh controls update the displayed totals, while tapping the widget opens Network details.
- Android Usage Access is required for historical totals. Widgets show a setup state when access is unavailable.

### Music Playback

- The 3x3 disc widget emphasizes album artwork; the 5x1 capsule adds track and artist text.
- Artwork is loaded from the active media session at a resolution suited to the widget and cached only on the device.
- Playback state updates immediately, while artwork processing runs away from the media callback path.
- Shuffle and repeat use the selected player's media-session capabilities. Controls are hidden when a player does not expose support.
- Optional Notification Listener access is used only to discover active media sessions and render local music controls.

## Device and Usage Experience

- Dedicated hardware pages expose GPU drivers, CPU topology, storage, display, battery, cameras, sensors, connectivity, and DRM capabilities.
- The sensor oscilloscope plots live multi-channel readings with dynamic scaling and an interactive sensor selector.
- Screen Time adds daily and weekly summaries, hourly distribution, goals, trends, and app-level foreground usage.
- The Overview dashboard now includes device identity, uptime, deep sleep, Quick Reach shortcuts, and direct subsystem cards.
- Settings adds local preference backup and restore, open-source notices, legal documents, theme controls, and telemetry refresh choices.
- Onboarding and Settings present live access status and explain which statistics depend on Android Usage Access or optional Notification Listener access.
- Privacy controls add optional screenshot and screen-recording protection for Osyster, while temperature formatting now includes Kelvin alongside Celsius and Fahrenheit.
- Centralized motion physics provide reduced-motion-aware transitions, touch feedback, rolling metrics, and smoother telemetry gauges throughout the app.

## Developer Stay Awake Setup

Android protects the global plugged-in Stay awake setting. To authorize the installed release build once, connect the device through ADB and run:

```bash
adb shell pm grant dev.qtremors.osyster android.permission.WRITE_SECURE_SETTINGS
```

After the grant, enable **Stay awake** from the Overview dashboard. Android will keep the screen on while connected to AC, USB, wireless, or dock power and allow normal sleep when unplugged. Android Studio debug builds use the package `dev.qtremors.osyster.debug` instead.

## Upgrade and Compatibility Notes

- Android 7.0 or newer remains supported.
- No root access is required.
- Existing preferences and managed App Stopper entries are retained during an in-place upgrade.
- Data widgets depend on launcher widget support and Android Usage Access.
- Music behavior depends on the active player exposing a standards-compatible media session and the requested playback modes.
- Osyster still declares no internet permission. Media metadata, artwork, diagnostics, and usage information remain on the device.

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
