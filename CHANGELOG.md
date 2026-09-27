# Osyster Changelog

> **Project:** Osyster
> **Version:** 0.2.8
> **Last Updated:** 2026-09-27

---

## [0.2.8] - 2026-09-27

- **Website**: Rebuilt as plain HTML, CSS, and JavaScript with no outside requests except GitHub stats, covering device pages, tools, power tools, design, credits, and an FAQ.
- **README**: Rewritten for the new app, with every feature, the power tools, and the privacy promise.
- **Privacy Policy**: Lists every permission and special access, what each is used for, and what Osyster stores.
- **Developer Guide**: Modules, startup, the power tools layer, the design system, rules, tests, and release steps.
- **Tasks**: A backlog for the next versions, including what the old app had that the new design brings back.

---

## [0.2.7] - 2026-09-26

- **Power Setup**: Unlock power tools with a one-time ADB command, Shizuku, or root. With Shizuku or root, Osyster grants itself settings access once, so settings tools keep working when neither is running.
- **Power Saving**: Battery saver on or off, the level that turns it on by itself, and which limits it applies. Reset puts back your phone's defaults.
- **Scale and Animations**: All animation speeds at once, font sizes past the Settings slider, and display size in five steps (Shizuku or root).
- **Status Bar Icons**: Hide icons you never look at and show the battery percentage. One tap shows them all again.
- **System Tweaks**: Developer options, USB and wireless debugging, window blur, tap to wake, pop-up notifications, touch indicators, and Private DNS with your own provider.
- **Shut-Up!**: Open banking apps that refuse to run with developer options on. Debugging turns off first, and Restore in the notification puts back exactly what you had. Pin any of them to your home screen.
- **Standby Apps**: See and change each app's standby bucket (Shizuku or root).
- **App Freezing**: Freeze apps you rarely use and unfreeze them in one tap or all at once (Shizuku or root).
- **New Tiles**: Private DNS, USB debugging, Developer options, Window blur, Tap to wake, Freeze, and Restart SystemUI (root).
- **App Stopper**: With Shizuku or root, Stop works in one tap and Stop all appears.
- **Fewer, Honest Tools**: Per-app volume and Night light per app are gone. Both needed hidden system interfaces or background app watching, which Osyster does not use.

---

## [0.2.6] - 2026-09-25

- **Welcome**: First launch walks through what Osyster is, its privacy promises, and the optional access a few tools use. Every step can be skipped.
- **What's New**: After an update, a short sheet lists the highlights once.
- **Search**: Search from Home for any device page, tool, setting, or device fact, including values like your chip name or Android version.
- **Music Widgets**: Disc and capsule music players for the home screen, with album art and play, pause, and skip controls.
- **Flashlight Pulse**: Blink the flashlight for new notifications while the screen is off. Ongoing notifications are skipped and bursts blink only once. Notification text is never read.
- **About**: Version, developer, source code, releases, issues, privacy, license, and device in one place.
- **Open Source Licenses**: Credits for every library and font, with each full license text.
- **Privacy Page**: What Osyster reads, what it never reads, and why it has no internet access.
- **Backup**: Export your theme, Home layout, app stopper list, and tool options to a file, and import them on another phone.
- **About This Page**: Long-press any device page or tool for a plain explanation of what it shows and what it needs.
- **Launcher Shortcuts**: Long-press the Osyster icon to open Data usage, Stay awake, Flashlight, or Screen time directly.

---

## [0.2.5] - 2026-09-24

- **Stay Awake**: Keep the screen on for 15 minutes, 30 minutes, 1 hour, 2 hours, or until you stop it, with a countdown notification and a Stop button. Works with Osyster closed.
- **Stay Awake While Charging**: Turn Android's developer setting on or off from Osyster. Until it is allowed, the page shows the one-time ADB command to copy and a shortcut to Developer options.
- **Flashlight**: A big on and off switch, plus a brightness slider and 25, 50, 75, and 100 percent presets on phones that support brightness levels (Android 13 and newer).
- **Activity Launcher**: Browse any app's screens, open the ones Android allows, and add them to your home screen.
- **Link Actions**: Set Osyster as your default browser and every link asks which app should open it, with copy and share. Try any link from the tool page.
- **Quick Settings Tiles**: Stay awake (each tap adds time), Awake while charging, Flashlight at your last brightness, RAM in use, and Data today. On Android 13 and newer, add any of them with one tap from the Quick Settings tiles page.

---

## [0.2.4] - 2026-09-23

- **Data Usage**: See data used today, over the last 7 days, or this month, split into mobile and Wi-Fi, downloaded and uploaded, with a daily chart and every app's usage. Tap an app to open its App info.
- **Live Speed**: Data usage shows the current download and upload speed.
- **Data Usage Widgets**: Today, This month, and Combined home-screen widgets with a refresh button. Tapping one opens Data usage in Osyster.
- **Screen Usage**: Screen time for each of the last 7 days, an hourly chart, and every app's time and number of opens. Tap a day in the chart to look at it.
- **App Stopper**: Keep a list of apps you often stop. Each shows whether Android has it stopped, and Stop takes you straight to its App info. Removed apps stay listed until you remove them.
- **Usage Access**: Data and screen usage explain why they need Usage access, open the right settings page, and load as soon as you come back.
- **Smoother Back Gesture**: Swiping back now moves the page with your finger instead of shrinking it into a card, and the bottom bar no longer collapses into a circle on the way back.

---

## [0.2.3] - 2026-09-22

- **Display Page**: Resolution, screen size, aspect ratio, density, current and supported refresh rates, HDR formats and brightness, wide color gamut, cutout, and rounded corners.
- **Screen Tester**: Full-screen colors for dead or stuck pixels, a gray ramp for banding, and a multi-touch drawing test, opened from the Display page.
- **Cameras Page**: Every camera's megapixels, aperture, focal length, stabilization, flash, Camera2 support level, zoom range, ISO range, and capabilities. No camera is ever opened.
- **Sensors Page**: Every sensor grouped into motion, position, environment, and other. Tap one to watch its live readings; it stops the moment you close the sheet.
- **Network Page**: Connection type, internet check, metered and VPN state, live download and upload speed, local addresses, Wi-Fi band, link speed, signal, standard, and security, plus carrier, SIM state, network type, and roaming.
- **Bluetooth Page**: Adapter state, Low Energy features, LE Audio, and Auracast support, with the lowest Bluetooth version those features prove.
- **Media and DRM Page**: Widevine security level, version, and HDCP, ClearKey support, and every audio and video codec marked hardware or software.
- **Certificates Page**: The system and user certificate authorities your phone trusts, with issuer and expiry.
- **Search Long Pages**: Media, Certificates, and Sensors have a search field that filters rows as you type and opens folded results.
- **Ask Only When Needed**: The Wi-Fi name needs location access and mobile details need phone access. Each is asked only from its own section, with a sentence on why.
- **Share Device Report**: Share a plain-text summary of your phone from Home. It leaves out network addresses, the Wi-Fi name, and long lists.

---

## [0.2.2] - 2026-09-21

- **Live Hero Card**: Home now shows CPU, RAM, storage, and battery live. When Android blocks overall CPU load, the card shows the fastest core's clock instead and says so.
- **System Page**: Android version, API level, security patch, Google Play system update, the Android version the phone launched with, build details, kernel, baseband, bootloader, runtime, uptime, and deep sleep.
- **Device Page**: Model, manufacturer, codename, device type, chip, board, supported ABIs, and the hardware features the phone declares, such as NFC, UWB, eSIM, IR blaster, and fingerprint.
- **CPU and GPU Page**: Every cluster's clock range and governor, live clock speed for each core, GPU vendor, renderer, OpenGL ES and Vulkan versions, thermal status, thermal headroom, and CPU temperature where readable.
- **Memory and Storage Page**: Live RAM and swap use, low-memory point, and every storage volume Android exposes.
- **Battery Page**: Live level, charging state, power source, time to full, health, temperature, voltage, charge cycles on Android 14 and newer, current, and power, with a shortcut to Android's battery usage by app.
- **Honest Values**: Anything Android does not report says why (restricted, not supported, or not reported) instead of showing zero.
- **Copy Any Value**: Long-press a value to copy it.
- **Arrange Home**: Long-press a page to pin it to the top, move it within its group, or hide it. Hidden pages can be brought back from the bottom of Home. Tools can be pinned to Home from the Tools tab.

---

## [0.2.1] - 2026-09-20

- **Fresh Start**: Osyster is being rebuilt from the ground up with a calmer, more consistent design. This first build brings the new look and layout; device pages and tools return over the next updates.
- **New Home**: A hero card with your phone's name, Android version, security patch, and chip, followed by every device page grouped into About this phone, Performance, Hardware, Connectivity, and Media and security.
- **Tools Tab**: Usage, utilities, and power tools are listed in one place. Power tools say up front whether they need a one-time ADB grant, Shizuku, or root.
- **Floating Toolbar**: Switch between Home, Tools, and Settings from a floating bar that turns into a Back button with the page title on detail pages.
- **Appearance Settings**: Choose System, Light, or Dark theme, pure black for OLED screens, wallpaper colors or one of six presets, and turn haptic feedback or motion down.
- **Credits and License**: Osyster is now open source under the GNU General Public License v3. Settings lists the projects it builds on.
- **Android 8.0 or newer**: The minimum Android version is now 8.0 (Oreo).

---

## [0.2.0] - 2026-09-19

- **Settings Diagnostics Redesign**: Modernized the Diagnostics and Telemetry settings section with compact horizontal segmented selectors for refresh intervals and temperature units, eliminating vertical clutter and integrating seamlessly into unified card containers.
- **Touch Target Shape Clipping**: Fixed touch feedback, click indication, and ripple bounds across theme mode selectors, screen time history charts, expandable lists, and dashboard action chips to strictly conform to their rounded component boundaries.
- **Showcase and Website Telemetry Cleanup**: Streamlined website preview graphics to feature live Screen Time, Data Usage, and Network Speedometer telemetry while removing OS restriction badges and unrepresentative process manager claims.

---

## [0.1.9] - 2026-09-19

- **Stay Awake Control**: Added a compact dashboard toggle with one-time ADB permission setup and plugged-in screen control.

---

## [0.1.8] - 2026-09-19

- **Music Widgets**: Added 3x3 disc and 5x1 capsule players with high-resolution artwork, stable multi-player session selection, live playback state, and compatible play, skip, shuffle, and repeat controls.

---

## [0.1.7] - 2026-09-19

- **Data Usage Widgets**: Added Day, Month, and Combined home-screen widgets with cellular and Wi-Fi totals, refresh actions, and direct Network dashboard access.

---

## [0.1.6] - 2026-09-18

- **Unified Physics & Motion Tokens**: Established centralized spring physics specifications (`TouchBounceSpring`, `MorphSpring`, `NavigationSpring`, `GaugeSmoothSpring`, `WeightSpring`, `DisabledShakeSpring`) alongside non-consuming `Modifier.pressBounce`, tactile `Modifier.expressiveClickable`, and system-wide reduced motion compliance.
- **Expressive Navigation & Fluid Transitions**: Upgraded `NavHost` screen routes to spring-driven horizontal slide and parallax exit transitions, paired with smooth spring motion for the settings drawer and subpage navigators.
- **Telemetry Gauge Smoothing & Rolling Tickers**: Applied critically damped spring smoothing across all arc gauges, wavy progress bars, hero sleep distribution, and memory allocation segments, paired with rolling digit tickers for CPU, Memory, and Battery metrics.
- **Fluid Components & Tactile Haptics**: Added fluid weight expansion to tab rows and button groups, shake rejection physics on disabled actions, tactile virtual key haptics, and standardized connected container shapes (24.dp outer, 6.dp inner).

---

## [0.1.5] - 2026-09-18

- **Digital Wellbeing & Screen Time Tracker**: Added local screen time tracking featuring daily usage hero summary, animated digit ticker, daily target goal with linear wavy progress indicator, yesterday trend comparison, 7-day interactive history chart, 24-hour distribution breakdown, and per-app foreground time metrics.
- **Onboarding Statistics & Permissions Flow**: Added a dedicated Statistics Experience step to onboarding with selectable options ("Start Fresh" vs "Populate with System Data"), pros and cons comparisons, and redesigned permission cards featuring connected containers, live permission status updates, and collapsible app stability controls.
- **Interactive Permission Bottom Sheet**: Added a modal permission sheet accessible from screen time warnings with live status checking for Usage Access, Notifications, and Battery Optimization.
- **Bento & Quick Reach Screen Time Access**: Added today's screen time overview Bento card and instant jump ribbon shortcuts directly from the main dashboard.
- **Material 3 Expressive UI & Motion**: Implemented connected container groupings with morphing corner radii, smooth scale touch bounces, rolling digit tickers, wavy progress gauges, and segmented toggle groups across tabs and permission lists.

---

## [0.1.4] - 2026-09-12

- **Modern Settings Hub**: Redesigned settings with a fluid overscroll hero header, prominent app identity branding, split version and package pill, device hardware chip, and grouped external action buttons for issues, source code, releases, notices, privacy, and licenses.
- **Simplified Pull-Down Indicator**: Streamlined the top dashboard pull-down pill to consistently display the settings icon and a clean "Settings" label without dynamic helper text.
- **Preferences Backup & Restore**: Added offline export and import of application preferences via JSON files, complete with a structured confirmation preview dialog before applying changes.
- **Open Source Notices & Dependency Catalog**: Added a dedicated Open Source Notices screen showcasing all bundled libraries, versions, repository links, and license identifiers.
- **Bundled Legal Document Viewer**: Added a dedicated legal document viewer with selectable monospace typography for browsing full open source and project licenses directly inside the app.
- **Tactile Motion & Component Styling**: Added squishy bounce click feedback across interactive controls, spring-animated theme mode selector, morphing accent palette selector, and cohesive container card grouping.

---

## [0.1.3] - 2026-09-12

- **Direct-Reach Overview Hub**: Redesigned master dashboard into a direct-reach control center with instant 1-tap access to all subsystems.
- **Hero Device Identity & Deep Sleep**: Added a hero device card showing model identity, Android version, security patch, uptime, and deep sleep telemetry.
- **Quick Reach Action Ribbon**: Added a scrollable action ribbon for instant thumb access to CPU, GPU, RAM, Storage, Network, Apps, Sensors, Battery, Cameras, DRM, and Specs.
- **Three-Pillar Navigation Dock**: Streamlined the floating bottom dock into 3 core pillars: Overview, Network, and Apps.
- **Dedicated Standalone Pages**: Replaced category tabs with independent, dedicated pages for GPU, Storage, Battery, Display, System, Cameras, Sensors, and DRM.
- **Direct-Return Back Navigation**: Restored direct back navigation to the dashboard from Network and Apps, paired with floating dock back controls on all subpages.
- **Tab-First Nested Swipe Navigation**: Upgraded inner tabs to nested pagers so horizontal swipes switch between available tabs before traversing parent pages.
- **Clean Screen Tops**: Removed redundant top back buttons and page headers in favor of the floating bottom dock, and eliminated top header clutter on the Overview Dashboard.

---

## [0.1.2] - 2026-09-12

- **Streamlined 4-Pillar Navigation**: Reorganized navigation into 4 balanced, equal-access destinations on the floating bottom dock: Overview (Bento), Monitor (CPU, RAM, Network, Battery), Apps (Processes and App Stopper), and Hardware (Device Specs and Sensors).
- **Consolidated Monitor Hub**: Integrated real-time CPU, RAM and SWAP, Network traffic, and Battery telemetry into a unified 4-tab monitor with connected button controls.
- **Unified Application Manager**: Combined Active Processes and App Stopper under a single Apps hub, adding inline search and an accessible in-screen application management button.
- **Pull-to-Reveal Settings Overlay**: Replaced the morphing dock button with an Acqua-inspired pull-down gesture that reveals a sleek settings capsule with progressive haptic feedback and slides Settings down from the top.
- **Clean Symmetrical Dock**: Removed floating action button clutter from the bottom dock to ensure consistent and balanced navigation across all screens.
- **Modular UI Architecture**: Refactored the UI layer into domain-specific packages (overview, monitor, apps, hardware) with backwards-compatible composable aliases.

---

## [0.1.1] - 2026-09-12

- **Modular Hardware Diagnostics**: Expanded device specifications into segmented views for System, Device & Display, SoC & GPU clusters, Storage, Battery, Camera, Live Sensors, Connectivity, and Media DRM.
- **Interactive Sensor Oscilloscope**: Added live multi-channel waveform visualization for device sensors with dynamic range scaling, real-time value inspector, and configurable sensor selector.
- **Camera & Media DRM Inspection**: Added camera optical capabilities (facing, megapixels, resolution, aperture, OIS/EIS support) and DRM security levels (Widevine L1/L3, ClearKey, HDCP encryption).
- **SoC Topology & Off-Screen GPU Querying**: Detailed CPU cluster core counts, ranges, and frequency scaling governors alongside OpenGL ES and Vulkan driver versions.
- **Privacy & Unit Controls**: Added Kelvin temperature unit conversion and an optional screen capture blocking setting (FLAG_SECURE) to safeguard diagnostic telemetry from screenshots and screen recordings.

---

## [0.1.0] - 2026-09-06

- **Accurate Diagnostics**: Removed frequency-based CPU utilization estimates, isolated CPU counter baselines, corrected memory accounting on 16 KB devices, and restricted thermal readings to identified CPU/SoC sensors.
- **Foreground Monitoring & Fresh Data**: Paused recurring telemetry on hidden screens and in the background, prevented stale query results, refreshed current network usage, and exposed incomplete network reads.
- **Permission Clarity & Accessible Controls**: Removed unused notification permission requests, clarified usage access, hid unsupported background-kill actions on Android 14+, improved accent contrast, and added accessible chart-period and website FAQ controls.
- **Streamlined Artifact Packaging**: Standardized output APK naming across debug and release builds to use clean version-based filenames without redundant variant suffixes.
- **Documentation & Website Accuracy**: Updated feature, privacy, and setup information to match current behavior; removed completed tasks while retaining unfinished fixes.

---

## [0.0.9] - 2026-09-06

- **Comprehensive UI Internationalization**: Centralized user-facing labels, metrics, dialogs, and accessibility descriptions across all dashboards into XML string resources, with locale-sensitive number and byte formatting.
- **Jetpack ViewModel Architecture & Smooth Scrolling**: Migrated state management across all core screens to dedicated ViewModels, decoupled UI previews, and introduced asynchronous LRU icon caching for fluid list performance.
- **Sandbox Compliance & Telemetry Precision**: Modeled CPU and thermal states with explicit "Restricted by OS" indicators, resolved process UID ownership, and replaced raw shell process killing with compliant background process management.
- **Interactive Controls & Personalization**: Added pull-to-refresh gestures on Task and Network screens, configurable update intervals with Eco mode, dynamic temperature units (Celsius/Fahrenheit), and kernel thread filtering.
- **Accessibility & Contrast Calibration**: Implemented screen reader semantics for the network usage timeline chart and dock navigation tabs, expanded minimum touch targets to 48 dp, and calibrated surface contrast for Dark and OLED themes.
- **Platform Hardening & Build Optimization**: Enforced cleartext traffic restrictions, excluded sensitive diagnostic state from backups, eliminated unused runtime dependencies, and established variant-specific APK naming.

---

## [0.0.8] - 2026-09-04

- **Material 3 Expressive System & Tokens**: Integrated expressive motion scheme, complete surface container palettes, and morphing loading indicators.
- **Touch & Hover Shape Alignment**: Bounded ripples and hover feedback across cards, dock navigation pills, and lists to exact corner geometries.
- **Expressive Wavy Progress Gauges**: Converted circular and linear indicators across CPU, RAM, SWAP, Battery, and Network to expressive wavy bars and rings.
- **Connected Toggle Button Switcher**: Modernized Telemetry tab switching to the Material 3 Expressive connected button group standard.
- **App Stopper Ghost Apps & Count Sync**: Preserved uninstalled apps as ghost entries with Play Store reinstall shortcuts and live dashboard count updates.
- **Predictive Back & Dock Motion**: Added tactile gesture previews, sequential back navigation, and smooth floating dock enter/exit transitions.

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
