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

---

## 6. Audit Remediation Backlog

### Security & Privacy

- [ ] **SEC-0001 - Unprivileged Process Termination via Raw Shell Execution** `[Critical]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/ui/ProcessDashboard.kt#L322-330`
  - **Problem:** The process dashboard attempts to terminate arbitrary processes using `Runtime.getRuntime().exec("kill -9 $pid")`. On standard Android devices, the SELinux sandbox prevents untrusted applications from signaling processes owned by other UIDs.
  - **Impact:** The command fails silently with permission denied or, if a PID collision occurs with Osyster's own process, terminates Osyster unexpectedly.
  - **Fix:** Remove raw shell kill execution. Use `ActivityManager.killBackgroundProcesses(packageName)` for third-party background applications, or display an informative dialog indicating that arbitrary process termination requires root or Shizuku/ADB permissions.
  - **Verification:** Attempt to terminate an external process on an unrooted device with Android 10 or newer. Verify that the app does not crash or self-terminate, and that appropriate user guidance is displayed.

- [ ] **SEC-0002 - Deprecated Subscriber ID Query Triggering SecurityException on API 29+** `[High]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/monitor/NetworkMonitor.kt#L479-493`
  - **Problem:** `NetworkMonitor` invokes `TelephonyManager.getSubscriberId()` to identify mobile network subscriptions. Starting in Android 10 (API 29), this method requires the privileged `READ_PRIVILEGED_PHONE_STATE` permission, which non-system applications cannot obtain.
  - **Impact:** Throws an uncaught `SecurityException` if permissions are inspected strictly, or fails to resolve subscriber data on modern devices.
  - **Fix:** Guard the call with `Build.VERSION.SDK_INT < Build.VERSION_CODES.Q`. For API 29 and higher, identify network interfaces using `SubscriptionManager` and standard carrier metadata without querying subscriber identifiers.
  - **Verification:** Run mobile network diagnostics on Android 10, 13, and 14 emulators. Verify no `SecurityException` is logged and network traffic mapping functions correctly.

- [ ] **SEC-0003 - Unrestricted Backup Configuration and Unspecified Cleartext Traffic Policy** `[Medium]`
  - **Location:** `osyster-app/app/src/main/AndroidManifest.xml#L17-27`, `osyster-app/app/src/main/res/xml/backup_rules.xml#L1-13`
  - **Problem:** The application manifest does not explicitly set `android:usesCleartextTraffic="false"`, and `backup_rules.xml` does not explicitly exclude sensitive diagnostic logs or preferences from cloud and local backups.
  - **Impact:** Potential transmission of unencrypted HTTP traffic if external network calls are added, and risk of exposing saved device state via unencrypted ADB backups.
  - **Fix:** Add `android:usesCleartextTraffic="false"` to the `<application>` tag in `AndroidManifest.xml`. Update `backup_rules.xml` and `data_extraction_rules.xml` to explicitly exclude shared preferences and diagnostic caches.
  - **Verification:** Inspect compiled manifest using `aapt2 dump badging` to confirm cleartext traffic is disabled. Test backup creation using `adb backup` and inspect extracted contents.

### Architecture & State Management

- [ ] **ARCH-0001 - Missing Jetpack ViewModel Architecture Across Feature Screens** `[High]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/ui/BentoDashboard.kt#L87-145`, `AppStopperScreen.kt#L100-140`, `CpuDashboard.kt#L90-110`, `MemoryDashboard.kt#L25-45`, `ProcessDashboard.kt#L45-75`, `NetworkDashboard.kt#L120-160`, `DeviceInfoDashboard.kt#L40-70`
  - **Problem:** Feature screens manage asynchronous state, background coroutines, and hardware queries directly inside composables using `remember { mutableStateOf(...) }` and `LaunchedEffect`.
  - **Impact:** Screen rotations, split-screen mode, or theme changes cause the entire screen state to be destroyed and recreated, triggering redundant hardware polling, resetting scroll positions, and causing visible UI flicker.
  - **Fix:** Implement Android Jetpack `ViewModel` classes (such as `BentoViewModel`, `AppStopperViewModel`, `ProcessViewModel`, `NetworkViewModel`) exposing `StateFlow<UiState>` and integrating `SavedStateHandle` for critical parameters.
  - **Verification:** Rotate the device 90 degrees or trigger dark mode toggle on each dashboard screen. Confirm that state is preserved without spinner reloading or duplicate procfs reads.

- [ ] **ARCH-0002 - Tight Coupling of Leaf Composables to Context Singletons** `[Medium]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/ui/AppStopperScreen.kt#L350-420`, `NetworkDashboard.kt#L400-470`
  - **Problem:** Deep leaf composables instantiate and access singleton instances (`AppStopperMonitor.getInstance(context)`, `NetworkMonitor.getInstance(context)`) directly within UI components rather than adhering to Unidirectional Data Flow (UDF).
  - **Impact:** Prevents UI components from being rendered in Compose `@Preview` tooling, blocks isolated unit testing of UI layouts, and tightly couples view rendering with platform services.
  - **Fix:** Refactor screens to follow UDF. Pass immutable UI view state models into composables and hoist user actions as lambda event callbacks to the host ViewModel.
  - **Verification:** Verify that all feature composables can be rendered in Android Studio `@Preview` with mock state and tested in isolated Compose unit tests without initializing Android singletons.

### Performance & Resource Usage

- [ ] **PERF-0001 - Static Memory Leak of Drawable App Icons in NetworkMonitor** `[High]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/monitor/NetworkMonitor.kt#L90-95`
  - **Problem:** `NetworkMonitor` maintains a static cache `ConcurrentHashMap<Int, CachedAppMeta>` where `CachedAppMeta` retains `Drawable?` icon instances indefinitely. `Drawable` objects hold references to package resources and system contexts.
  - **Impact:** Substantial and unbounded memory footprint growth as network traffic across multiple apps is tracked, creating a permanent memory leak and increasing OutOfMemory risk.
  - **Fix:** Remove `Drawable` storage from `CachedAppMeta` in the background monitor. Cache only lightweight primitives (`appName`, `packageName`). Delegate icon resolution to the UI layer using a bounded LRU memory cache or an asynchronous icon loader.
  - **Verification:** Run Android Studio Memory Profiler, generate network traffic across 50 applications, trigger garbage collection, and verify that no `ResourcesImpl` or `BitmapDrawable` objects are retained by `NetworkMonitor`.

- [ ] **PERF-0002 - Main Thread Blocking I/O During Initial Screen Composition** `[High]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/ui/BentoDashboard.kt#L89-91, L105-107`, `CpuDashboard.kt#L93`, `MemoryDashboard.kt#L28`
  - **Problem:** Methods such as `SystemMonitor.getCpuState()`, `getMemoryState()`, `getBatteryState()`, and `AppStopperMonitor.getManagedAppCounts()` are called synchronously inside `remember { mutableStateOf(...) }` on the UI main thread during initial composition.
  - **Impact:** Causes frame drops and jank during navigation transitions while the UI thread waits for file reads against `/proc/stat`, `/proc/meminfo`, and package manager iteration.
  - **Fix:** Initialize UI state with placeholder or cached values. Shift all procfs, sysfs, and package manager reads to background coroutines running on `Dispatchers.IO`.
  - **Verification:** Profile cold start and tab navigation using Android Studio System Trace. Verify zero blocking file reads or package queries on the Main thread.

- [ ] **PERF-0003 - Synchronous Bitmap Drawing During Lazy List Recomposition** `[Medium]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/ui/AppStopperScreen.kt#L408, L688, L975`, `NetworkDashboard.kt#L1216`
  - **Problem:** `Drawable.toSafeBitmap(width, height)` allocates a new software canvas and draws the drawable into a bitmap synchronously inside LazyColumn and LazyVerticalGrid item rendering.
  - **Impact:** Induces severe frame drops and jank while scrolling through lists containing dozens of installed applications.
  - **Fix:** Convert drawables to bitmaps asynchronously off the main thread or cache rendered `ImageBitmap` instances keyed by package name.
  - **Verification:** Rapidly scroll through the App Stopper and Network application lists with 100 or more applications installed while monitoring frame rendering with `Choreographer` metrics. Verify steady 60 or 120 FPS.

- [ ] **PERF-0004 - Unbounded Polling Loops Ignoring Configured Diagnostics Interval** `[Medium]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/ui/CpuDashboard.kt#L97`, `MemoryDashboard.kt#L31`, `BentoDashboard.kt#L138, L141`
  - **Problem:** Telemetry polling loops hardcode `delay(1000L)` instead of observing the user-configured `OsysterPreferences.diagnosticsInterval` preference.
  - **Impact:** Disables the user preference setting. Prevents battery-saving intervals (such as 2s or 5s) from taking effect and causes unnecessary CPU wakeups.
  - **Fix:** Connect polling delays directly to `OsysterPreferences.diagnosticsInterval` via state observation.
  - **Verification:** Change the diagnostic interval to 5 seconds in Settings. Verify that telemetry queries occur every 5000ms instead of every 1000ms.

### Reliability & System Telemetry

- [ ] **REL-0001 - Fabricated Fallback Telemetry When SELinux Restricts Procfs Access** `[High]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/monitor/SystemMonitor.kt#L177, L316`
  - **Problem:** When `/proc/stat` or thermal sysfs nodes are restricted by SELinux on unrooted Android 8+ devices, `getCpuState()` returns a static `8.5f` load and `getCpuTemperature()` returns a static `38.5f`.
  - **Impact:** Misleads users and developers by displaying fake hardware statistics without indicating that access is restricted by the Android platform sandbox.
  - **Fix:** Model telemetry results as sealed states representing available data versus restricted access. Update UI cards to display an explicit "Restricted by OS" indicator when procfs or sysfs access is denied.
  - **Verification:** Run the application on an unrooted production device running Android 11 or newer. Verify that the UI displays a restricted badge rather than constant 8.5% CPU load and 38.5 C temperature.

- [ ] **REL-0002 - Missing Process UID Resolution Resulting in Unknown User Labels** `[Medium]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/monitor/SystemMonitor.kt#L380-450`, `osyster-app/app/src/main/java/com/osyster/ui/ProcessDashboard.kt#L260`
  - **Problem:** `SystemMonitor.getActiveProcesses()` assigns `user = "unknown"` for all discovered processes because it does not parse UID status lines from `/proc/[pid]/status`.
  - **Impact:** Every process listed in the Process Dashboard displays "unknown" as its user, eliminating process ownership visibility.
  - **Fix:** Parse the `Uid:` field from `/proc/[pid]/status` and resolve the UID using `PackageManager.getNameForUid(uid)` or map common system UIDs (root, system, radio).
  - **Verification:** Open the Process Dashboard and verify that process owners display proper system or application usernames instead of "unknown".

- [ ] **REL-0003 - Temperature Unit Preference Ignored Across Dashboards** `[Low]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/ui/CpuDashboard.kt#L234`, `DeviceInfoDashboard.kt#L216`
  - **Problem:** Temperature values are hardcoded with Celsius formatting string `"°C"` without checking the user's `OsysterPreferences.temperatureUnit` preference.
  - **Impact:** Switching temperature units to Fahrenheit in application settings does not update the CPU Dashboard or Device Info displays.
  - **Fix:** Implement a shared temperature formatter that converts values to Fahrenheit when `TemperatureUnit.FAHRENHEIT` is active and updates the unit suffix accordingly.
  - **Verification:** Select Fahrenheit in Settings, navigate to the CPU Dashboard and Device Info screen, and confirm temperatures are converted and displayed with the "°F" unit.

- [ ] **REL-0004 - Show Kernel Threads Preference Has No Effect in SystemMonitor** `[Low]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/monitor/SystemMonitor.kt#L418`
  - **Problem:** `SystemMonitor.getActiveProcesses()` unconditionally skips bracketed kernel thread names (`[kcompactd0]`, `[kswapd0]`) regardless of the `OsysterPreferences.showKernelThreads` setting.
  - **Impact:** Users who enable "Show Kernel Threads" in Settings still cannot see kernel threads in the Process Dashboard.
  - **Fix:** Pass the `showKernelThreads` setting into `getActiveProcesses()` and include bracketed thread entries when the preference is true.
  - **Verification:** Toggle "Show Kernel Threads" on in Settings, open the Process Dashboard, and confirm kernel threads appear when running in an environment with sufficient procfs visibility.

### User Interface & Experience

- [ ] **UI-0001 - Hardcoded Bottom Spacers Causing Dock Overlap Across Varied Insets** `[Medium]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/ui/BentoDashboard.kt#L318`, `AppStopperScreen.kt#L329`, `CpuDashboard.kt#L309`, `MemoryDashboard.kt#L254`, `ProcessDashboard.kt#L305`, `NetworkDashboard.kt#L419`
  - **Problem:** Screens use fixed bottom spacers (`Spacer(modifier = Modifier.height(100.dp))` or `110.dp`) to prevent list items from hiding behind the floating `OsysterDock`.
  - **Impact:** On devices with 3-button navigation bars, split-screen mode, or landscape orientation, the fixed height is insufficient or excessive, causing list items to be obscured behind the dock or leaving unnecessary blank space.
  - **Fix:** Coordinate dock height dynamically using `WindowInsets.navigationBars` and provide the calculated bottom inset via a `CompositionLocal` or `Scaffold` padding values.
  - **Verification:** Test on devices with 3-button navigation and gesture navigation in both portrait and landscape modes. Verify the bottom-most list items are completely scrollable into view above the dock.

- [ ] **UI-0002 - Missing Pull-to-Refresh Gesture on Process and Network Dashboards** `[Low]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/ui/ProcessDashboard.kt#L90-120`, `NetworkDashboard.kt#L350-390`
  - **Problem:** Dashboards lack a standard pull-to-refresh interaction, forcing users to wait for periodic timer intervals to view updated process and bandwidth stats.
  - **Impact:** Reduces perceived application responsiveness when diagnosing immediate system changes.
  - **Fix:** Wrap scrollable dashboard containers in Material 3 `PullToRefreshBox` and wire the refresh gesture to trigger immediate monitor queries.
  - **Verification:** Perform a pull-down gesture on the Process and Network screens. Verify that the refresh indicator displays and data updates immediately.

- [ ] **UI-0003 - Dark Theme Contrast Deficiencies on Surface Containers** `[Low]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/ui/theme/Color.kt#L40-60`, `osyster-app/app/src/main/java/com/osyster/ui/theme/Theme.kt#L50-70`
  - **Problem:** Several card backgrounds and borders rely on static hex tokens with subtle contrast differences against the dark theme background.
  - **Impact:** Card boundaries and elevated surfaces lack clear visual hierarchy, particularly on low-brightness OLED displays.
  - **Fix:** Align color schemes with Material Design 3 surface container roles (`surfaceContainerLowest`, `surfaceContainerLow`, `surfaceContainerHigh`) to guarantee appropriate contrast ratios.
  - **Verification:** Inspect cards and containers across all dashboards on an OLED screen at minimum brightness in dark mode. Confirm card boundaries and content remain distinctly legible.

### Accessibility

- [ ] **A11Y-0001 - Interactive Touch Targets Below 48dp Minimum Threshold** `[High]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/ui/AppStopperScreen.kt#L226`, `DeviceInfoDashboard.kt#L123`, `NetworkDashboard.kt#L227, L492, L541, L1068`, `osyster-app/app/src/main/java/com/osyster/ui/navigation/OsysterDock.kt#L64, L156`
  - **Problem:** Several icon buttons and controls have explicit touch targets ranging from 28dp to 44dp without enclosing touch target expansion.
  - **Impact:** Violates WCAG 2.5.5 (Target Size) and Google Play accessibility guidelines, leading to mis-taps for users with motor impairments.
  - **Fix:** Apply `Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)` or `Modifier.minimumInteractiveComponentSize()` to all interactive icon buttons.
  - **Verification:** Run Google Accessibility Scanner on all screens. Confirm zero touch target size warnings.

- [ ] **A11Y-0002 - Inaccessible Network Usage Canvas Chart for Screen Readers** `[Medium]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/ui/NetworkDashboard.kt#L914-1011`
  - **Problem:** The daily and weekly network usage bar chart is drawn on a raw `Canvas` without `semantics` modifiers, content descriptions, or accessible child nodes.
  - **Impact:** TalkBack and Switch Access users receive no spoken feedback or context regarding network consumption trends or bar values.
  - **Fix:** Add a `Modifier.semantics` block containing an overall summary `contentDescription` and traverse individual bar data points with semantic values.
  - **Verification:** Enable TalkBack, navigate to the Network usage chart, and verify that TalkBack speaks a coherent summary including total usage and peak usage days.

- [ ] **A11Y-0003 - Dock Navigation Items Missing Tab Semantics and Selected State** `[Medium]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/ui/navigation/OsysterDock.kt#L138-185`
  - **Problem:** Floating dock navigation items use generic `clickable` modifiers without indicating `role = Role.Tab` or reporting their `selected` state to accessibility services.
  - **Impact:** Visually impaired users navigating with a screen reader cannot tell which navigation item is currently active or how many items are in the container.
  - **Fix:** Replace generic click modifiers with `Modifier.selectable(selected = isSelected, role = Role.Tab, onClick = ...)` and supply descriptive labels.
  - **Verification:** Navigate the dock using TalkBack. Verify that each item announces its label, "Tab", and whether it is currently selected.

### Internationalization & Localization

- [ ] **I18N-0001 - Extensive Hardcoded User-Facing Strings in Dashboard Screens** `[Medium]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/ui/AppStopperScreen.kt#L125-240`, `BentoDashboard.kt#L180-260`, `CpuDashboard.kt#L130-220`, `MemoryDashboard.kt#L70-150`, `ProcessDashboard.kt#L90-180`, `NetworkDashboard.kt#L150-300`, `onboarding/OnboardingScreen.kt#L80-160`
  - **Problem:** Hundreds of user-facing UI labels, dialog messages, and button texts are hardcoded as string literals in Kotlin code instead of using `stringResource(R.string...)`, even though corresponding string entries exist in `res/values/strings.xml`.
  - **Impact:** Prevents the application from being translated into other languages, creating a broken experience for non-English users.
  - **Fix:** Move all remaining hardcoded string literals into `res/values/strings.xml` and replace literals with `stringResource(...)`.
  - **Verification:** Change system language to a non-English language (such as German or Spanish). Verify that all dashboard titles, buttons, and status labels update correctly.

- [ ] **I18N-0002 - Hardcoded US Locale in Byte and Metric Formatters** `[Low]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/monitor/NetworkMonitor.kt#L657-671`
  - **Problem:** `formatBytes` uses `String.format(Locale.US, "%.1f %s", ...)` which forces dot decimal separators regardless of the user's device locale.
  - **Impact:** Violates regional conventions for locales that use commas for decimals (such as French, German, and Spanish).
  - **Fix:** Replace `Locale.US` with `Locale.getDefault()` or use Android's platform `Formatter.formatFileSize(context, bytes)`.
  - **Verification:** Set device language to French or German. Confirm that byte values format with commas (for example: "1,5 GB").

### Build Configuration & Maintenance

- [ ] **BUILD-0001 - Unused Dependency `material-kolor` Packaged in Production Builds** `[Low]`
  - **Location:** `osyster-app/app/build.gradle.kts#L176`, `osyster-app/gradle/libs.versions.toml#L15, L44`
  - **Problem:** `com.materialkolor:material-kolor:5.0.0` is declared as an implementation dependency but is never imported or utilized anywhere in the codebase.
  - **Impact:** Increases application download size, prolongs dependency resolution time, and introduces unnecessary transitive dependencies.
  - **Fix:** Remove `material-kolor` declarations from `app/build.gradle.kts` and `libs.versions.toml`.
  - **Verification:** Run `./gradlew app:dependencies` and `./gradlew assembleRelease`. Verify successful build with reduced dependency count.

- [ ] **BUILD-0002 - Dead Typography Tokens from Unrelated Project** `[Low]`
  - **Location:** `osyster-app/app/src/main/java/com/osyster/ui/theme/Type.kt#L143-159`
  - **Problem:** Typography extensions `filename`, `fileMetadata`, `pathBreadcrumb`, and `storageMetric` copied from a file manager project remain unused in `Type.kt`.
  - **Impact:** Dead code causes confusion for developers maintaining typography styles.
  - **Fix:** Remove unused file-manager typography extension properties.
  - **Verification:** Execute `./gradlew compileDebugKotlin` to verify no compilation errors occur after removal.

- [ ] **BUILD-0003 - APK Output Naming Conflicts Between Build Variants** `[Low]`
  - **Location:** `osyster-app/app/build.gradle.kts#L98-101`
  - **Problem:** Custom output file naming `outputFileName = "Osyster-${variant.versionName}.apk"` produces identical file names for debug and release builds.
  - **Impact:** Assembling both debug and release variants in continuous workflows causes one APK to overwrite the other in the build outputs directory.
  - **Fix:** Include the variant name in the output artifact filename (for example: `Osyster-${variant.versionName}-${variant.name}.apk`).
  - **Verification:** Run `./gradlew assembleDebug assembleRelease` and confirm that separate, uniquely named APK files exist in the output directory.

### Testing & Quality Assurance

- [ ] **TEST-0001 - Missing Unit Test Suite for SystemMonitor Telemetry Parsers** `[Medium]`
  - **Location:** `osyster-app/app/src/test/java/com/osyster/monitor/`
  - **Problem:** `SystemMonitor.kt` contains complex parsing logic for `/proc/stat`, `/proc/meminfo`, and `/proc/[pid]/status`, but lacks dedicated unit test coverage.
  - **Impact:** Changes or refactoring of procfs parsing can introduce undetected silent telemetry failures or parsing crashes.
  - **Fix:** Create `SystemMonitorTest.kt` with simulated procfs and sysfs string fixtures testing standard outputs, malformed lines, and permission denied cases.
  - **Verification:** Run `./gradlew testDebugUnitTest` and confirm all `SystemMonitorTest` test cases pass.

- [ ] **TEST-0002 - Unused Template Test Files in Test Suites** `[Low]`
  - **Location:** `osyster-app/app/src/test/java/com/osyster/ExampleUnitTest.kt#L1-17`, `osyster-app/app/src/androidTest/java/com/osyster/ExampleInstrumentedTest.kt#L1-24`
  - **Problem:** Default Android Studio template test classes containing trivial `2 + 2 = 4` assertions remain in the project.
  - **Impact:** Clutters test suites and produces misleading test count numbers.
  - **Fix:** Remove template test files and replace with actual domain tests.
  - **Verification:** Run `./gradlew testDebugUnitTest` and `./gradlew connectedCheck` to confirm clean test execution without template files.
