# Osyster - Releases

> **Project:** Osyster
> **Version:** 0.4.0
> **Last Updated:** 2026-11-10

| Version | Release Date | Key Focus |
| :--- | :--- | :--- |
| [v0.4.0](#v040) | 2026-11-10 | Bluetooth remote, barcode scanner and code creator, notification and battery history, app manager with permissions, measure tools, music tiles and battery widgets, and a new website |
| [v0.3.0](#v030) | 2026-11-09 | Ground-up rebuild: new Home dashboard, focused device pages, data and screen usage with charts, power tools with ADB, Shizuku, or root, utilities, widgets, tiles, and GPLv3 |
| [v0.2.0](#v020) | 2026-09-19 | Home-screen data and music widgets, expanded hardware diagnostics, Screen Time, modernized navigation and settings, and developer Stay Awake control |
| [v0.1.0](#v010) | 2026-09-06 | First public release: offline system monitoring, Bento Grid dashboard, native kernel telemetry, App Stopper, network traffic tracking, and Material 3 Expressive UI |

---

# v0.4.0

**Release Date:** November 10, 2026

**Previous public release:** v0.3.0

**Development range included:** v0.3.1 through v0.4.0

**Known issues and roadmap:** Track open work in [TASKS.md](TASKS.md).

Osyster v0.4.0 adds six new tools: a Bluetooth remote, a barcode scanner and code creator, notification history, battery history, an app manager, and measure tools. It also brings new home-screen widgets and a new website. Everything still runs on your phone, with no internet permission.

## Highlights

- **Bluetooth Remote**: Use your phone as a remote, touchpad, keyboard, or gamepad for an Android TV, computer, or tablet. Nothing to install on the other device.
- **Barcode Scanner and Creator**: Read QR codes and barcodes from the camera or a picture, and make your own to save or share.
- **Notification History**: Keeps every notification in full, even ones you dismissed or the app deleted. Off until you turn it on.
- **Battery History**: Your battery level over the last month, each charge, drain, and temperature, plus a battery widget.
- **App Manager**: Every installed app with what it's allowed to use and how risky that is, and a page showing which apps can use your camera, microphone, location, contacts, and more.
- **Measure**: A bubble level, compass, ruler at true size, and sound meter.

## New Tools

### Bluetooth Remote (Android 9 and newer)

- **Remote**: D-pad or swipe pad, Home, Back, power, volume, channels, a number pad, media keys, and color keys. Hold a key to repeat it.
- **Fits the device**: TVs get the full remote; computers get arrows, Enter, Esc, and the Windows key. The choice is saved for each device.
- **Touchpad and air mouse**: Move, tap, scroll, and drag with your fingers, or point the phone to move the pointer.
- **Keyboard**: Type with any phone keyboard, swipe typing included, with Ctrl, Alt, Shift, Win, and keys a phone lacks, like Esc, Tab, and F1 to F12.
- **Gamepad**: Two sticks, the D-pad, face and shoulder buttons, for TV and computer games.
- **Computer actions**: Lock, sleep, restart, shut down, take a screenshot, and open Task Manager. Some work only on Windows.
- **Widget and volume keys**: Media keys on the home screen, and the phone's volume keys can control the TV or computer.

### Barcode Scanner and Code Creator

- **Scan**: QR, Data Matrix, Aztec, PDF 417, EAN, UPC, Code 128, 93, and 39, Codabar, and ITF, with torch and zoom. Works offline.
- **From a picture**: Pick a photo or screenshot, or share one to Osyster. If nothing reads, frame the code and try again.
- **Act on it**: Open links, call, text, email, open maps, and add contacts and events, all through your other apps. Copy a Wi-Fi password.
- **Search sites**: Add your own sites to look up product codes in your browser.
- **History**: Every scan is kept on your phone, with rename and delete.
- **Create**: QR codes for text, links, Wi-Fi, contacts, events, messages, and more, filled in with a form, plus every common barcode. Pick colors and round dots, then save as PNG or SVG or share.

### Notification History

- Keeps the full text, lists, pictures, buttons, and your replies. Chats open as a conversation.
- Shows whether you dismissed it, opened it, or the app removed it.
- Search, filter by app, and choose how long to keep it, from one week to one year. Leave out any app, and delete one entry, one app, or everything.
- Saves in small batches, so it uses almost no battery. Stays on the phone and out of Android backup.

### Battery History

- The battery level over a day, week, or month, with each charge listed: from and to what level, how long, the charger, and the highest temperature.
- Average drain, temperature range, health, and charge cycles where Android reports them.
- One reading every half hour, kept 30 days. You can turn it off or clear it.
- A 2x2 battery widget with the level, temperature, and the last day as a small chart.

### App Manager

- Every app with its version, where it came from, its size, and its risk.
- Each app's page shows its permissions as allowed, not allowed, or special access, plus its components and signing certificate.
- Uninstall, save its APK, open App info, or see its data and screen time.
- The permissions page shows how many apps are allowed each kind of access. Tap one to see which apps.

### Measure

- A bubble level that taps when flat, a compass, a ruler in centimeters or inches, and a sound meter.
- Sensors run only while a tool is open. The microphone is never recorded.

## Widgets and Shortcuts

- **Music tiles**: One widget that shows the album art with play controls at 3x3, or two buttons at 3x2, and switches as you resize it.
- **Music apps**: Choose which apps the music widgets follow, so a video or game never takes over.
- **Data widget setup**: Pick the period and what to show, with a live preview. The data widgets fit a 2x1 cell again and load properly.
- **Private DNS widget**: A 2x1 switch on the home screen.
- **Launcher shortcuts**: Long-press the icon to scan a code, scan a picture, or create a code. Your launcher can show the ones you use most first.

## Everything Else

- **Split screen**: Two panes follow the app's window, so they work in split screen and on foldables.
- **New buttons and menus**: Buttons change shape when pressed, and long-press menus are grouped and clearer.
- **Rounded text fields** everywhere, to match the rest of the app.
- **No more What's New sheet**: Updates open straight into the app.
- **Privacy**: The privacy policy and the in-app privacy screen list the camera, microphone, and Bluetooth, and how each is used.
- **Website and docs**: A new website, screenshots, and simpler README and privacy policy.

## Upgrade and Compatibility Notes

- Your settings and data from v0.3.0 carry over.
- The Data today and Data this month widgets are now one Data usage widget, set up when you place it.
- The music grid and music tiles widgets are now one widget.
- If you paired a TV or computer with the remote before, pair it again once so the power, sleep, and gamepad keys work.
- Osyster still has no internet permission. Everything it reads stays on the phone.

---

# v0.3.0

**Release Date:** November 9, 2026

**Previous public release:** v0.2.0

**Development range included:** v0.2.1 through v0.3.0

**Known issues and roadmap:** Track active issues and ongoing engineering tasks in [TASKS.md](TASKS.md).

Osyster v0.3.0 is a ground-up rebuild. Every screen has a new, calmer design: three tabs (Home, Tools, and Settings) and a floating bar that turns into a Back button. Device information is spread across focused pages. Usage, utilities, and a new set of power tools now share one Tools tab. Osyster is now open source under the GNU General Public License v3 and still has no internet access.

## Highlights

- **New Home**: A live dashboard with CPU, RAM, storage, battery, screen time, and data today. Each tile has its own graphic and turns amber or red as a reading gets high. Below it, every device page is grouped into sections.
- **Edit Home**: Reorder, show, or hide dashboard tiles, pinned tools, and device pages from one sheet.
- **Device Pages**: System, Device, CPU and GPU, Memory and storage, Battery, Display with a screen tester, Cameras, Sensors with live readings, Network, Bluetooth, Media and DRM, and Certificates.
- **Data and Screen Usage**: A download and upload ring, charts by hour or day, live speed, mobile and Wi-Fi filters, and a date picker. Each app gets its own page with charts.
- **Power Tools**: Power saving, Scale and animations, Status bar icons, System tweaks, Shut-Up!, and Standby apps. Unlock them with a one-time ADB command, Shizuku, or root.
- **Utilities**: Stay awake, Flashlight with brightness and notification pulse, App stopper with freezing, Activity launcher, Link actions, home-screen widgets, and Quick Settings tiles.
- **Search Everything**: Find any device page, tool, setting, or device fact from the top bar of Home. Every page with a list searches itself from the same place.
- **Themes**: System, Light, Dark, and pure Black, with wallpaper colors or six presets.

## Home and Device Pages

- The dashboard tiles open their pages: CPU, RAM, storage, and battery go to their device pages, while screen time and data go to their tools.
- The data tile follows your active connection and splits download from upload.
- Every value Android does not report says why (restricted, not supported, or not reported) instead of showing zero. Long-press any value to copy it.
- Wi-Fi name and mobile details are asked for only from their own sections, with a sentence on why.
- Share a plain-text device report that leaves out network addresses and the Wi-Fi name.
- On tablets and unfolded foldables, tabs stay on the left and pages open on the right.

## Tools

### Usage

- **Data usage**: 7 days, Today, or This month, with Mobile, All, or Wi-Fi. Step between periods or jump to any day. Search the app list from the top bar.
- **Screen usage**: Today, the last 7 days, an hourly chart, and every app's time and opens.
- **App pages**: Tapping an app in Data usage shows its data. Tapping one in Screen usage shows its screen time. Tap the app's icon for App info.
- **App stopper**: A grid of the apps you stop often. Uninstalled apps stay listed under their name until you remove them. With Shizuku or root, Stop takes one tap, Stop all appears, and apps can be frozen.

### Utilities

- **Stay awake**: Keep the screen on for a set time or until you stop it, with a countdown notification, or while charging through Android's own setting.
- **Flashlight**: Brightness levels on Android 13 and newer, plus a short blink for new notifications while the screen is off. Notification text is never read.
- **Activity launcher**: Open any screen an app allows, and pin it to the home screen.
- **Link actions**: Choose which app opens each link, with copy and share.
- **Widgets**: Glance list with level bars, Music disc with sharp album art and capsule, and three resizable data usage widgets with a spinning refresh. They follow your theme and, on Android 12 and newer, your wallpaper colors.
- **Quick Settings tiles**: Stay awake, Awake while charging, Flashlight, RAM, Data today, Color picker, Private DNS, USB debugging, Developer options, Window blur, Tap to wake, Freeze, and Restart SystemUI (root).

### Power Tools

- **Power saving**: Battery saver, the level that turns it on, and which limits it applies.
- **Scale and animations**: Animation speed, font sizes beyond the Settings slider, and display size (Shizuku or root).
- **Status bar icons**: Hide icons and show the battery percentage.
- **System tweaks**: Developer options, debugging, window blur, wake gestures, pop-up notifications, touch indicators, and Private DNS.
- **Shut-Up!**: Open banking apps that refuse to run while developer options are on, and restore your settings afterwards.
- **Standby apps**: See and change each app's standby bucket (Shizuku or root).

## Power Tools Setup

Most power tools need Android's secure settings permission. Grant it once with ADB:

```bash
adb shell pm grant dev.qtremors.osyster android.permission.WRITE_SECURE_SETTINGS
```

With Shizuku or root, Osyster can grant this to itself from Power setup. Standby apps, display size, and freezing need Shizuku or root while you use them. Debug builds use the package `dev.qtremors.osyster.debug` instead.

## Upgrade and Compatibility Notes

- Android 8.0 or newer is now required. v0.2.0 supported Android 7.0.
- Root is never required. Shizuku and root are optional and only unlock extra power tools.
- The process list, per-app volume, Night light per app, and Screen Time daily goals are gone. They need hidden system interfaces or access Android no longer gives apps, and Osyster does not use either.
- Settings and the App stopper list from v0.2.0 do not carry over, because the app was rebuilt with new storage. Use Export and Import in Settings to move settings between phones from this version on.
- Osyster still declares no internet permission. Everything it reads stays on the phone.

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
