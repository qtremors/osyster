# Osyster - Tasks

> **Project:** Osyster
>
> **Version:** 0.2.0
>
> **Last Updated:** 2026-09-19

---

## 1. System Monitoring & Kernel Telemetry

- [x] **Frequency Scaling & Governors:**
  - [x] Add real-time CPU frequency scaling governor inspector (interactive, schedutil, performance, powersave).
  - [x] Support per-cluster frequency tracking across big.LITTLE / DynamIQ topologies.
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
- [x] **Sensor Diagnostics & Calibration:**
  - [x] Add a comprehensive sensor bench: accelerometer, gyroscope, magnetometer, barometer, and proximity sensor tests.
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
- [x] **Home Screen System Widgets:**
  - [x] Build home screen widgets matching the Bento Grid aesthetic (Day, Month, and 2x2 Combined Data Usage widgets).
  - [x] Implement 3x3 circular and 5x1 pill music widgets with media-session controls, local album artwork caching, and optional Notification Listener integration.

---

## 5. Automation & Notifications

- [ ] **Thermal Throttling Alerts:**
  - [ ] Trigger opt-in local notifications when CPU core temperatures exceed critical thresholds.
- [ ] **Battery Health & Charge Alarms:**
  - [ ] Add notifications for custom battery charge limits (e.g., 80% stop notifications to extend lifespan).
  - [ ] Log charge cycles and estimate battery wear degradation over time.

---

## 6. Audit Remediation Backlog

Completed implementation entries have been removed. The remaining items are prioritized remediation work, not a committed release scope.

### Architecture / Maintainability Tasks

- [ ] **ARCH-0001 - Reconcile Parallel Navigation Models and Dead Destinations** `[Medium]`
  - **Location:** `osyster-app/app/src/main/java/dev/qtremors/osyster/MainActivity.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/navigation/AppRoutes.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/monitor/MonitorDashboard.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/hardware/HardwareDashboard.kt`
  - **Problem:** `AppRoutes` declares destinations such as `DeviceInfo`, `Settings`, `About`, and `Licenses` that are not registered in the `NavHost`, while settings use a separate overlay enum and several deprecated dashboard wrappers remain unreachable. This creates two navigation models and route contracts that can compile but fail if passed to the default `navController.navigate(route)` branch.
  - **Impact:** Navigation changes are difficult to reason about, dead screens and aliases continue to drift, and a newly reused route can become a runtime navigation failure.
  - **Fix:** Define one authoritative destination model, register every supported route or remove it, move the settings subpages into that model where appropriate, and delete obsolete wrappers after callers and documentation are migrated.
  - **Verification:** Add a navigation graph test that resolves every public route, exercise back and predictive-back behavior from each reachable destination, and confirm no deprecated wrapper or unregistered route remains referenced.

- [ ] **ARCH-0002 - Establish Feature Ownership, Package Boundaries, and File Naming Rules** `[Medium]`
  - **Location:** `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/viewmodel/` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/monitor/` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/hardware/` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/apps/` `osyster-app/app/src/main/java/dev/qtremors/osyster/MainActivity.kt`
  - **Problem:** The single app module mixes layer-based and feature-based organization: feature ViewModels are separated into a global package, settings models depend on Compose presentation types, and files named `*Dashboard`, `*Screen`, or `*Components` expose multiple unrelated public declarations and compatibility aliases. Several feature files exceed 1,000 lines and combine route wiring, state collection, charts, dialogs, rows, formatting, and previews, so neither ownership nor the expected location of a change is predictable.
  - **Impact:** Feature changes span distant packages, increase merge and review conflicts, expose accidental APIs, and make later module extraction or parallel ownership risky because compile-time dependency direction is not defined.
  - **Fix:** Record a lightweight architecture convention and reorganize incrementally around feature-owned slices containing route, screen, state holder, models, and private components. Isolate genuinely shared models, platform gateways, preferences, and design-system code; use one primary public declaration per predictably named file; standardize `Route` for stateful wiring and `Screen` for stateless UI; remove obsolete aliases; and extract Gradle modules only where the resulting dependency graph creates a measurable build, reuse, or ownership benefit.
  - **Verification:** Generate and review the package/module dependency graph, confirm feature code does not depend on another feature's UI internals, make non-contract declarations `internal` or `private`, run focused tests after each move, and verify a new feature or telemetry field can be implemented without editing unrelated feature packages.

- [ ] **ARCH-0003 - Introduce a Composition Root and Injectable Platform Gateways** `[Medium]`
  - **Location:** `osyster-app/app/src/main/java/dev/qtremors/osyster/MainActivity.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/viewmodel/` `osyster-app/app/src/main/java/dev/qtremors/osyster/monitor/` `osyster-app/app/src/main/java/dev/qtremors/osyster/settings/OsysterPreferences.kt`
  - **Problem:** Most state holders are `AndroidViewModel`s that locate system services, singleton preferences, dispatchers, and global `object` monitors themselves. Construction policy is therefore spread across the UI, shared mutable caches and platform side effects are hidden behind static calls, and existing JVM tests primarily cover parsers or already-built state rather than complete ViewModel flows.
  - **Impact:** Permission, time, filesystem, lifecycle, and vendor-specific behavior is difficult to substitute deterministically; feature changes can create duplicate collectors or service lookups; and tests must depend on Android or avoid the highest-risk orchestration logic.
  - **Fix:** Add one application composition root using explicit factories or a proportionate DI solution, define narrow interfaces for telemetry sources, package/permission access, preferences, time, and dispatchers, and inject them into plain ViewModels or state holders. Keep Android implementations at the platform boundary and make ownership and lifetime of caches and hot flows explicit.
  - **Verification:** Instantiate every ViewModel in local JVM tests with fake gateways and a test dispatcher, cover success/restricted/error/cancellation and lifecycle restart flows without global reset hooks, and assert the production graph creates one intended owner for each shared repository or stream.

- [ ] **ARCH-0004 - Standardize Typed Telemetry Availability and Failure Contracts** `[High]`
  - **Location:** `osyster-app/app/src/main/java/dev/qtremors/osyster/monitor/TelemetryResult.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/monitor/SystemMonitor.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/monitor/DeviceHardwareMonitor.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/monitor/NetworkMonitor.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/monitor/ScreenUsageHelper.kt`
  - **Problem:** CPU sampling uses `TelemetryResult`, but other collectors communicate unavailable data through nullable values, empty lists, `hasErrors`, hardcoded strings such as `unknown` or `Permission required`, and broad exceptions that are frequently swallowed. These representations conflate a legitimate empty result with unsupported hardware, denied access, transient failure, and parse defects, while also moving presentation text into data models.
  - **Impact:** Screens cannot explain or recover from failures consistently, regressions can silently look like real zero or empty data, and every new telemetry source requires bespoke error and formatting branches across monitors, ViewModels, and composables.
  - **Fix:** Define a shared typed result contract that distinguishes available, empty, restricted, unsupported, and failed states with safe diagnostic context and recovery metadata. Return raw typed domain values from collectors, map platform exceptions at gateway boundaries, localize formatting in presentation code, and use intentional logging for unexpected failures without exposing device-sensitive data.
  - **Verification:** Add contract tests for each result state across hardware, process, network, and screen-time sources; inject permission loss, unreadable files, unsupported APIs, malformed input, and transient exceptions; and confirm each screen renders a distinct localized state without sentinel strings or silent fallback to valid-looking data.

### UI / UX Tasks

- [ ] **UI-0001 - Add Window-Adaptive Navigation and Content Layouts** `[Medium]`
  - **Location:** `osyster-app/app/src/main/java/dev/qtremors/osyster/MainActivity.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/navigation/OsysterDock.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/overview/OverviewDashboard.kt` `osyster-app/app/build.gradle.kts`
  - **Problem:** The app always renders a phone-style three-page horizontal pager and single-column detail screens. `OsysterDock` only hides labels from `LocalConfiguration.screenWidthDp`, and the configured Material adaptive libraries are not used to provide rails, panes, or window-size-aware content.
  - **Impact:** Tablets, foldables, landscape, split-screen, and desktop windows receive stretched phone layouts instead of useful workspace adaptation, and configuration-based width reads can be stale for resized windows.
  - **Fix:** Drive the shell from current window metrics and adaptive window size classes, use an appropriate navigation suite or rail on wider windows, introduce multi-pane layouts for overview/detail surfaces, and remove unused adaptive dependencies if the product intentionally remains compact-only.
  - **Verification:** Run Compose previews and device tests at compact, medium, and expanded widths, including fold posture, landscape, split-screen, freeform resize, keyboard navigation, and font scale 2.0.

### Accessibility

- [ ] **A11Y-0004 - Remaining Device Accessibility Verification** `[Medium]`
  - **Location:** `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/` `osyster-app/app/src/main/java/dev/qtremors/osyster/MainActivity.kt`
  - **Problem:** No instrumented accessibility suite exists, and device behavior for focus order, system-bar and IME insets, switch access, keyboard input, RTL, and large-text reflow remains unverified across the app shell and modal surfaces.
  - **Impact:** Regressions that are not visible in phone-sized previews can block navigation or obscure controls for assistive-technology, large-text, and non-touch users.
  - **Fix:** Establish a repeatable accessibility QA matrix and automate semantics assertions for the primary dock, onboarding, settings, permission, search, dialog, and bottom-sheet flows.
  - **Verification:** Run TalkBack Accessibility Scanner, switch access, hardware keyboard, RTL, gesture and three-button navigation, font scales 1.3/1.5/2.0, and landscape/split-screen checks on the supported API range.

- [ ] **A11Y-0005 - Make Interactive Charts and Dense Controls Operable Without Precise Touch** `[High]`
  - **Location:** `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/screentime/ScreenTimeDashboard.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/apps/AppStopperDashboard.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/monitor/NetworkDashboard.kt` `osyster-app/app/src/main/res/layout/widget_music_3x3.xml` `osyster-app/app/src/main/res/layout/widget_music_5x1.xml` `osyster-app/app/src/main/java/dev/qtremors/osyster/widget/music/MusicWidgetUpdater.kt`
  - **Problem:** The seven-day and 24-hour screen-time bars are unlabeled `clickable` layout nodes, with 24 hourly targets compressed into one 90 dp row. The App Stopper density chooser is a 36 dp custom clickable with no label or role. The network chart has an accessible summary and menu, but its direct canvas selection exposes no selected-state semantics. Music widget controls remain only 32 to 40 dp where the layouts could provide larger pointer targets.
  - **Impact:** TalkBack, switch access, keyboard users, launcher accessibility users, and users with motor impairments cannot reliably discover, identify, or activate important filters, chart data, and playback controls.
  - **Fix:** Add roles, selected/state descriptions, localized action labels, focus behavior, and custom accessibility actions or an equivalent accessible list. Keep pointer targets at least 48 dp where host constraints allow, route dense visual bars through a separately focusable selector, and update each widget control's content description and state whenever its `RemoteViews` are rendered.
  - **Verification:** Add Compose semantics tests for every chart period and density choice, add `RemoteViews` accessibility assertions for every media state, and confirm TalkBack and switch-access users can select, clear, control playback, and hear the current value without using coordinate-based taps.

### Internationalization & Localization

- [ ] **I18N-0001 - Remaining Hardcoded User-Facing Strings** `[Medium]`
  - **Location:** `osyster-app/app/src/main/java/dev/qtremors/osyster/monitor/SystemMonitor.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/monitor/ScreenUsageHelper.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/viewmodel/NetworkViewModel.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/settings/PreferencesBackupManager.kt` `osyster-app/app/src/main/res/values/strings.xml`
  - **Problem:** Battery, hardware, duration, network date, chart semantics, backup preview, toast, and fallback app labels are still assembled from hardcoded English. Lint reports non-observable locale reads, configuration-stale `LocalContext.getString` calls, a default-locale format, and quantity strings that should use plurals.
  - **Impact:** Locale changes can leave stale mixed-language UI, quantities and formatting are grammatically incorrect in many languages, and the full lint gate remains blocked.
  - **Fix:** Move display text and quantities into resources, keep domain state typed rather than preformatted, format dates/numbers/bytes through the active configuration locale, use `pluralStringResource`, and make composables observe locale changes through configuration-aware APIs.
  - **Verification:** Run `lintDebug` with no locale/configuration errors, switch locale at runtime, and test RTL plus representative plural, date, decimal, duration, and file-size formats.

### Security / Privacy Tasks

- [ ] **SEC-0001 - Separate Media Listener Access From Unused Notification and Doze Requests** `[High]`
  - **Location:** `osyster-app/app/src/main/AndroidManifest.xml` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/onboarding/OnboardingScreen.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/onboarding/OnboardingPages.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/onboarding/PermissionBottomSheet.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/widget/music/MusicNotificationListenerService.kt` `osyster-app/app/src/main/res/values/strings.xml` `PRIVACY.md`
  - **Problem:** Music widgets now have a separately presented and disclosed Notification Listener use case, but the app still declares and requests unrelated `POST_NOTIFICATIONS`, treats it as required for the permission sheet's ready action, describes screen-time alerts and monitor status that do not exist, and directly requests a battery-optimization exemption without scheduled background work. Lint flags the direct exemption flow as a Play policy risk.
  - **Impact:** Users can still confuse the broad notification-reading grant with permission to post notifications, denial of an unused permission blocks the ready action, and store review can reject the Doze exemption flow.
  - **Fix:** Keep Notification Listener access optional and requested only for music widgets with precise disclosure, remove `POST_NOTIFICATIONS` and the direct Doze-exemption request until a concrete feature needs them, remove their readiness gating and obsolete claims, and handle every denial or revocation without forcing Settings.
  - **Verification:** Inspect the merged release manifest, complete onboarding and Settings flows with each access independently denied or revoked, confirm music widgets degrade safely without listener access, confirm no posting or Doze prompt appears, run lint, and reconcile all in-app and privacy-policy wording with actual behavior.

- [ ] **SEC-0002 - Eliminate Unsupported Subscriber-ID Access and Unnecessary Phone Permission** `[High]`
  - **Location:** `osyster-app/app/src/main/AndroidManifest.xml` `osyster-app/app/src/main/java/dev/qtremors/osyster/monitor/NetworkMonitor.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/monitor/NetworkDashboard.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/viewmodel/NetworkViewModel.kt` `PRIVACY.md`
  - **Problem:** Mobile usage calls the deprecated `TelephonyManager.subscriberId`, which current SDK contracts protect with privileged access, while the UI claims ordinary `READ_PHONE_STATE` is required for per-app cellular statistics. Lint reports a blocking `MissingPermission` error, and the only carrier-name helper is unused.
  - **Impact:** The app requests sensitive phone access without a dependable benefit, legacy mobile queries can fail or show generic partial-data errors, and release lint cannot pass.
  - **Fix:** Remove subscriber-ID and dead carrier lookup code, redesign network queries around supported `NetworkStatsManager` behavior, and model mobile history as unavailable on platform/device combinations that cannot expose it. Retain a phone permission only if a documented, supported, user-visible capability strictly requires it.
  - **Verification:** Confirm the merged manifest no longer requests unnecessary phone access, run `lintDebug`, and test Mobile, Wi-Fi, and All filters on API 24-28 and current single-SIM, dual-SIM, Wi-Fi-only, denied, and revoked-permission devices.

- [ ] **SEC-0003 - Resolve Broad Package-Visibility Release Policy** `[High]`
  - **Location:** `osyster-app/app/src/main/AndroidManifest.xml` `osyster-app/app/src/main/java/dev/qtremors/osyster/monitor/AppStopperMonitor.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/monitor/NetworkMonitor.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/apps/AppStopperDashboard.kt` `PRIVACY.md`
  - **Problem:** The release APK declares `QUERY_ALL_PACKAGES` and enumerates installed applications for App Stopper and UID labels. Google Play treats installed-app inventory as sensitive and permits broad visibility only for qualifying core purposes with a declaration and accurate prominent disclosure; the repository contains no release decision or narrower fallback.
  - **Impact:** A Play-distributed release can be rejected or removed, while removing the permission without redesign would silently degrade app selection and attribution.
  - **Fix:** Decide and document the distribution/policy path. If broad visibility is essential and eligible, prepare the Play declaration and in-product disclosure; otherwise replace it with launcher queries, targeted `<queries>`, and usage-derived packages, and disable unsupported system-app enumeration explicitly.
  - **Verification:** Inspect the merged manifest for the chosen variant, test App Stopper and network attribution on Android 11+ with restricted visibility, and complete Play policy/pre-launch review or the equivalent distribution checklist.

- [ ] **SEC-0004 - Isolate Data Widget Refresh Actions From Exported Providers** `[High]`
  - **Location:** `osyster-app/app/src/main/AndroidManifest.xml` `osyster-app/app/src/main/java/dev/qtremors/osyster/widget/DailyDataUsageWidgetReceiver.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/widget/MonthlyDataUsageWidgetReceiver.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/widget/CombinedDataUsageWidgetReceiver.kt`
  - **Problem:** The three data app-widget providers are exported and advertise app-private refresh actions in their manifest filters. Any installed app can therefore start their network-usage queries. Music controls have been moved to an explicit non-exported receiver and obsolete third-party media broadcasts were removed.
  - **Impact:** Untrusted apps can still trigger background network-usage work without user interaction.
  - **Fix:** Keep only the system-required app-widget update surface exported, route data refresh controls through an explicit non-exported receiver or another caller-authenticated component, validate widget IDs and action ownership, and use immutable explicit `PendingIntent`s with unique identities.
  - **Verification:** Inspect the merged manifest, send every data refresh action both implicitly and explicitly from a separate test app or `adb`, confirm external sends cannot refresh data, and verify launcher updates and every data widget button still work on API 24 through the current target.

- [ ] **SEC-0005 - Clear Persisted Media Metadata When It Is No Longer Needed** `[High]`
  - **Location:** `osyster-app/app/src/main/java/dev/qtremors/osyster/widget/music/MusicNotificationListenerService.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/widget/music/MusicWidgetManager.kt` `osyster-app/app/src/main/res/xml/backup_rules.xml` `osyster-app/app/src/main/res/xml/data_extraction_rules.xml` `osyster-app/app/src/main/res/values/strings.xml` `PRIVACY.md`
  - **Problem:** The listener now ignores unrelated notifications, the prompt and privacy policy disclose local media handling, SharedPreferences are excluded from backup, and artwork lives in the non-backed-up cache. However, the last title, artist, package, modes, and cached artwork remain after all music widgets are removed or listener access is revoked.
  - **Impact:** Private listening metadata can remain locally longer than the feature needs it.
  - **Fix:** Track active music-widget instances and clear persisted metadata and artwork after the final instance is removed, listener access is revoked, or the user chooses a clear action.
  - **Verification:** Inspect cloud-backup and device-transfer contents, remove all widgets and revoke access to confirm cleanup, and test that unrelated notification content is never persisted or logged.

### Performance Tasks

- [ ] **PERF-0001 - Centralize and Cache Static Hardware Probes** `[High]`
  - **Location:** `osyster-app/app/src/main/java/dev/qtremors/osyster/MainActivity.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/viewmodel/BentoViewModel.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/viewmodel/DeviceInfoViewModel.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/monitor/SystemMonitor.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/monitor/DeviceHardwareMonitor.kt`
  - **Problem:** `DeviceInfoViewModel` is created at the app root and eagerly inventories system, display, CPU clusters, EGL/GPU, storage, cameras, sensors, connectivity, and DRM. `BentoViewModel` independently repeats most of that work. Every CPU sample also redetects the CPU model, maximum frequencies, and thermal-zone candidates, including at the 500 ms interval.
  - **Impact:** Cold start and first render contend for duplicate file, package, camera, EGL, and DRM work, while high-frequency static sysfs/procfs reads waste CPU, storage operations, battery, and thermal headroom.
  - **Fix:** Introduce a single repository that caches immutable device capabilities and discovered sysfs paths, refreshes only values that can change, lazily loads detail-only data, and shares snapshots between overview and hardware screens. Keep dynamic frequency, utilization, temperature, and battery samples separate from static metadata.
  - **Verification:** Add cold/warm startup and CPU-dashboard macrobenchmarks, trace file/probe counts at 500 ms and 5 s intervals, and verify hidden screens perform no duplicate hardware inventory or polling.

- [ ] **PERF-0002 - Bound and Coalesce Widget Rendering and Refresh Work** `[High]`
  - **Location:** `osyster-app/app/src/main/java/dev/qtremors/osyster/MainActivity.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/widget/DataUsageWidgetUpdater.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/widget/music/MusicWidgetManager.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/widget/music/MusicWidgetUpdater.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/widget/music/MusicNotificationListenerService.kt`
  - **Problem:** Music artwork is now sampled, bounded to 1024 px, rendered from widget options, and decoded off the callback thread, but palette generation and `RemoteViews` rendering can still repeat for rapid callbacks. Data widgets launch unstructured receiver coroutines and duplicate day/month queries across individual and combined widgets without timeout or coalescing.
  - **Impact:** Rapid media callbacks can still amplify render work, background receivers can exceed their execution window, and multiple data widgets multiply CPU, battery, and query cost.
  - **Fix:** Coalesce music renders by immutable track/artwork state and share one bounded day/month snapshot across active data widgets. Use bounded `goAsync` work only for short queries and schedule durable work through the platform job API or WorkManager when it can exceed the receiver window.
  - **Verification:** Stress cold and warm updates with 4K artwork, low-memory devices, slow usage queries, multiple instances, rapid media callbacks, process death, and widget resizing; record main-thread, allocation, `RemoteViews` bitmap usage, query-count, and broadcast-duration traces and confirm every update stays below platform limits.

### Reliability Tasks

- [ ] **REL-0001 - Correct Screen-Time Event Processing and Cache Identity** `[High]`
  - **Location:** `osyster-app/app/src/main/java/dev/qtremors/osyster/monitor/ScreenUsageHelper.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/viewmodel/ScreenTimeViewModel.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/screentime/ScreenTimeDashboard.kt`
  - **Problem:** `fetchDailySummaries()` first caches today's result with `includeHourly = false`; the immediately following `fetchDetailedUsageToday(includeHourly = true)` returns that same cache entry, so the 24-hour chart is empty on normal first load. The cache is not keyed by day or detail level, null-package events are discarded before screen on/off handling, and repeated init/resume/refresh loads are not coordinated as latest-wins work.
  - **Impact:** A primary screen-time visualization is predictably missing, midnight and concurrent refreshes can publish stale data, and event edge cases can miscount foreground time.
  - **Fix:** Cache a complete immutable result keyed by query range/day and derive reduced views from it, process system events before package filtering, define the cross-midnight/open-session policy, and serialize or cancel superseded ViewModel requests with explicit loading/error state.
  - **Verification:** Add synthetic `UsageEvents` tests for first load, hourly/no-hourly requests, null-package screen events, sessions crossing midnight, DST changes, day rollover, cancellation, permission revocation, and rapid date/refresh changes; then verify the hourly chart on device.

- [ ] **REL-0002 - Make Start Fresh Screen-Time History Durable or Remove the Mode** `[High]`
  - **Location:** `osyster-app/app/src/main/java/dev/qtremors/osyster/settings/OsysterPreferences.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/monitor/ScreenUsageHelper.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/viewmodel/ScreenTimeViewModel.kt` `osyster-app/app/src/main/res/values/strings.xml`
  - **Problem:** The onboarding option promises to record usage from now on, but the app stores neither an opt-in timestamp nor daily usage. When `preferSystemUsageHistory` is false, every non-today day is always returned as an empty result, so the seven-day history never accumulates after day rollover and today's value still includes usage from before onboarding.
  - **Impact:** The recommended onboarding choice cannot deliver its stated behavior, producing permanently blank history and undermining trust in usage statistics.
  - **Fix:** Persist a start anchor and durable daily aggregates with migration, retention, timezone-change, and reconciliation rules, or remove Start Fresh and accurately explain that all displayed history comes from Android's usage log.
  - **Verification:** Test fresh install before and after midday, midnight rollover, seven consecutive days, timezone/DST changes, reboot/process death, upgrade, preference toggle, and clearing app data; confirm history and disclosure match the chosen model.

- [ ] **REL-0003 - Normalize Battery Current Using the Android API Contract** `[High]`
  - **Location:** `osyster-app/app/src/main/java/dev/qtremors/osyster/monitor/SystemMonitor.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/monitor/BatteryDashboard.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/hardware/BatteryDiagnosticsDashboard.kt` `osyster-app/app/src/test/java/dev/qtremors/osyster/SystemMonitorTest.kt`
  - **Problem:** `BATTERY_PROPERTY_CURRENT_NOW` is defined in microamperes, but the collector divides only values whose absolute magnitude exceeds 10,000 and does not handle `Int.MIN_VALUE`, the unsupported sentinel for this target SDK. Low currents can be displayed 1,000 times too large and unsupported devices can show `-2147483648 mA`.
  - **Impact:** A core diagnostics value is materially incorrect and can mislead battery-health or charging decisions.
  - **Fix:** Treat the unsupported sentinel as unavailable, always convert supported microampere values to milliamperes with a documented rounding/sign policy, and model unavailable separately from a real zero current.
  - **Verification:** Add unit tests for positive, negative, low, zero, and sentinel readings, then compare displayed values with `dumpsys battery` or a trusted device reading while charging and discharging on multiple vendors.

- [ ] **REL-0004 - Stop Live Sensor Collection When the Sensor UI Is Not Resumed** `[High]`
  - **Location:** `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/viewmodel/DeviceInfoViewModel.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/hardware/SensorsDashboard.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/MainActivity.kt`
  - **Problem:** Selecting a sensor registers the activity-scoped `DeviceInfoViewModel` as a listener, but navigation away, app backgrounding, and loss of UI subscribers do not unregister it. The listener stops only when another sensor is selected or the activity-scoped ViewModel is cleared.
  - **Impact:** Sensor callbacks and history allocations can continue after the screen is hidden, wasting battery and CPU and retaining an operation the user believes ended.
  - **Fix:** Tie registration to the resumed sensor screen and current selection, expose an idempotent stop action, unregister in lifecycle/disposal paths, and avoid activity-wide ownership unless sharing is necessary.
  - **Verification:** Instrument a fake or test sensor manager to assert balanced register/unregister calls, then use device sensor and battery profiling while navigating away, pressing Home, locking the device, rotating, and returning.

- [ ] **REL-0005 - Align Process Management With Android Sandbox Capabilities** `[High]`
  - **Location:** `osyster-app/app/src/main/java/dev/qtremors/osyster/monitor/SystemMonitor.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/viewmodel/ProcessViewModel.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/apps/ProcessDashboard.kt` `osyster-app/app/src/main/AndroidManifest.xml`
  - **Problem:** The Active Processes screen scans every numeric `/proc` directory but silently drops entries whose files Android prevents a third-party app from reading, then presents the remainder as the running-process total without a partial-data state. It also offers `killBackgroundProcesses` for other packages below API 34 even though Android advises third-party apps not to control other app processes and limits the API to the caller's own package from API 34.
  - **Impact:** Users can mistake a sandbox-limited subset for a complete task list, and a destructive-looking control can be ineffective, short-lived, or counterproductive while requiring `KILL_BACKGROUND_PROCESSES`.
  - **Fix:** Define a capability-aware process data contract, label visible/self-only results accurately, expose permission or platform limitations instead of swallowing them, remove cross-app process killing and its manifest permission, and retain system App Info as the supported user-mediated control.
  - **Verification:** Test visible process counts and states on stock API 24, 29, 33, 34, and current devices from multiple vendors, confirm inaccessible entries produce an honest limited-data state, verify no cross-app kill control remains, and inspect the merged manifest for removal of `KILL_BACKGROUND_PROCESSES`.

- [ ] **REL-0006 - Complete Cross-Player Music Session Verification** `[High]`
  - **Location:** `osyster-app/app/src/main/java/dev/qtremors/osyster/widget/music/MusicNotificationListenerService.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/widget/music/MusicWidgetManager.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/widget/music/MusicWidgetUpdater.kt` `osyster-app/app/src/test/java/dev/qtremors/osyster/widget/music/MusicWidgetTest.kt`
  - **Problem:** Session selection is now token-based and sticky, callbacks are limited to the selected player, Apple Music fallback commands and implicit legacy media broadcasts are removed, and repeat/shuffle use one advertised or compatibility command with live mode callbacks. Pure policy tests cover core selection order, but Android media controllers are final system objects and command rejection, session destruction, reconnect timing, and vendor-specific compatibility still lack device or instrumented coverage.
  - **Impact:** A player with a non-standard custom action and no readable compatibility state may still report a mode differently, and rare session timing differences remain undetected without multi-device evidence.
  - **Fix:** Add an injectable controller adapter, then cover command rejection, missing capabilities, notification removal, session replacement/destruction, listener reconnect, and the existing bounded mode reconciliation with instrumented or fake-adapter tests.
  - **Verification:** Run the full off/all/one repeat and shuffle cycles while rapidly switching among Apple Music, Spotify, and another standards-compliant player; confirm metadata, art, play state, and controls never cross sessions, then repeat after listener revocation, process death, and session recreation.

### Data / Storage / Platform Tasks

- [ ] **STORAGE-0001 - Validate and Apply Preference Backups Atomically** `[Medium]`
  - **Location:** `osyster-app/app/src/main/java/dev/qtremors/osyster/settings/PreferencesBackupManager.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/settings/OsysterPreferences.kt` `osyster-app/app/src/main/java/dev/qtremors/osyster/ui/settings/BackupRestoreSection.kt` `osyster-app/app/src/test/java/dev/qtremors/osyster/PreferencesBackupTest.kt`
  - **Problem:** Import reads an unbounded document fully into memory, accepts any `schemaVersion`, silently skips invalid enum values, then applies individual preferences one by one while still reporting success. The preview is built from raw unvalidated strings and does not guarantee the state that will be committed.
  - **Impact:** A malformed, oversized, future-version, or partially invalid backup can cause memory pressure or a silent partial restore with misleading confirmation.
  - **Fix:** Bound input size and nesting, validate app identity, supported schema, enums, and numeric ranges into one typed candidate, render the preview from that candidate, and commit the complete validated state atomically with explicit migration and error reporting.
  - **Verification:** Add tests for oversized input, malformed JSON, unknown/future schema, invalid enums/ranges, unknown fields, old-schema migration, stream failure, cancellation, and all-or-nothing restore; manually verify SAF providers and configuration change during preview.

### Testing & Quality Assurance

- [ ] **BUILD-0002 - Restore Screen-Time Compatibility on Android 7** `[Critical]`
  - **Location:** `osyster-app/app/src/main/java/dev/qtremors/osyster/monitor/ScreenUsageHelper.kt` `osyster-app/app/build.gradle.kts` `osyster-app/gradle/libs.versions.toml`
  - **Problem:** `ScreenUsageHelper` uses `java.time` APIs that require API 26, but the app advertises API 24 as its minimum and does not enable core library desugaring. Full lint reports 13 blocking `NewApi` errors across `ZoneId`, `Instant`, and `ZonedDateTime` usage.
  - **Impact:** Opening or refreshing screen-time features can crash on Android 7.0 and 7.1 even though those versions are declared supported.
  - **Fix:** Enable and configure core library desugaring for the required `java.time` APIs, or replace the calls with an API 24-compatible time implementation. Keep timezone, DST, and day-boundary behavior explicit and shared with screen-time aggregation.
  - **Verification:** Run full lint with no `NewApi` findings, add API 24/25 tests for conversion and day boundaries, and smoke-test the minified release APK's screen-time dashboard on Android 7.0 and 7.1.

- [ ] **BUILD-0001 - Make Full Lint and Tests a Release Gate** `[High]`
  - **Location:** `osyster-app/app/build.gradle.kts` `osyster-app/build.gradle.kts` `osyster-app/app/build/reports/lint-results-debug.html`
  - **Problem:** `lintDebug` currently fails with 31 errors and 224 warnings, while `assembleRelease` still produces a signed, minified APK because only vital lint runs during assembly. `verifyOsysterBuildConventions` checks only that catalog sections and version regexes exist and does not depend on lint or tests.
  - **Impact:** A distributable artifact can be produced despite confirmed min-SDK, permission, locale, and configuration-awareness errors.
  - **Fix:** Resolve the behavioral lint findings tracked above, add a release-readiness task that depends on full lint, unit tests, and the relevant instrumented checks, and document that task as the only release artifact path. Keep suppressions narrow and justified rather than baselining correctness errors.
  - **Verification:** From a clean checkout, run the release-readiness task and confirm it fails on an injected lint/test regression, passes without a baseline hiding current errors, and produces the same signed R8/resource-shrunk APK verified by `apksigner`.

- [ ] **TEST-0003 - Remaining Device Release Verification** `[Medium]`
  - **Location:** `osyster-app/app/src/androidTest/` `osyster-app/app/src/test/` `osyster-app/app/build.gradle.kts`
  - **Problem:** The project configures Compose UI and Espresso dependencies but has no `androidTest` sources or benchmark module. JVM tests pass, but permission revocation, process recreation, foreground-only polling, sensor cleanup, accessibility semantics, min-SDK behavior, R8 behavior, and 16 KB devices have no automated or completed device evidence; no device was attached during this audit.
  - **Impact:** The highest-risk platform and lifecycle flows can regress while unit tests and a release assembly remain green.
  - **Fix:** Add a focused instrumented suite and release matrix for onboarding/permission denial and revocation, navigation/back restoration, network and screen-time error states, sensor lifecycle, backup SAF flows, App Info handoffs, widget discovery/update/control/resize flows, accessibility, and representative large data. Add macrobenchmarks for startup, overview, process lists, charts, and cold widget rendering; replace widget constant-only tests with behavior tests over `RemoteViews`, receiver lifecycle, media session selection, and failure recovery.
  - **Verification:** Run JVM and instrumented tests against API 24/25, 29, 33, and current target devices, including a 16 KB image and minified release APK; record fresh install, upgrade, denied/revoked access, offline, background/resume, rotation, process death, launcher restart, widget add/remove/resize, dark mode, large font, RTL, and rapid-input results.
