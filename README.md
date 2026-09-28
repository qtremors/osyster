<p align="center">
  <a href="https://qtremors.github.io/osyster/">
    <img src="docs/assets/Osyster.png" alt="Osyster logo" width="120"/>
  </a>
</p>

<h1 align="center"><a href="https://qtremors.github.io/osyster/">Osyster</a></h1>

<p align="center">
  Everything about your Android phone, and the tools to tune it, in one private app.
</p>

<p align="center">
  <a href="https://github.com/qtremors/osyster/releases/latest">
    <img src="https://img.shields.io/github/v/release/qtremors/osyster?label=Download%20APK&color=00E6FF&logo=android&logoColor=black" alt="Download APK" height="32">
  </a>
</p>

<p align="center">
  <a href="https://github.com/qtremors/osyster/releases"><img src="https://img.shields.io/github/downloads/qtremors/osyster/total?label=Total%20Downloads&color=00E6FF" alt="Total Downloads"></a>
  <a href="https://github.com/qtremors/osyster/releases"><img src="https://img.shields.io/github/downloads/qtremors/osyster/latest/total?label=Latest%20Downloads&color=00E6FF" alt="Latest Downloads"></a>
</p>
<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0%2B-34A853?logo=android" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/License-GPLv3-blue" alt="License: GPLv3">
</p>

> [!NOTE]
> **Private by design:** Osyster does not request `android.permission.INTERNET`, so Android itself keeps it offline. It has no ads, analytics, accounts, or crash reports, and it never reads serial numbers, IMEI, phone numbers, or MAC addresses.

## Why Osyster

Most device info apps stop at a list of specs, and most tweak apps want root or a network connection. Osyster puts both in one calm Material 3 Expressive app: read what your phone is made of, see how you use it, and change the settings Android tucks away, without sending anything anywhere. Power tools are optional and visible, never hidden behind tricks: no accessibility service and no hidden system interfaces.

## Screenshots

Screenshots are added during the v0.3.0 device testing pass.

## Download

Download the latest APK from [GitHub Releases](https://github.com/qtremors/osyster/releases) and install it on Android 8.0 (API 26) or newer. No root is needed for anything outside the power tools.

## Features

### Home and device pages

- **Home you arrange:** a live card with CPU, RAM, and battery trend lines, pinned pages and tools, and grouped device pages that each show a live fact. Edit Home to drag them into order or switch pages off; long-press a row to pin it or read what it shows.
- **Twelve device pages:** System, Device, Processor and GPU, Memory and storage, Battery, Display, Cameras, Sensors, Network, Bluetooth, Media and DRM, and Certificates.
- **Honest values:** anything Android hides says "Not available" or which permission would show it, never a fake zero. Copy any value with a long-press.
- **Screen tester:** full-screen colors, gray ramps, and a multi-touch test from the Display page.
- **Live sensors:** tap a sensor to watch its readings; they stop when you close the sheet.
- **Share device report:** a plain-text summary without network addresses or the Wi-Fi name.
- **Search:** find any page, tool, setting, or device value from Home.

### Tools

- **Data usage:** mobile and Wi-Fi data today, this week, or this month, per app, with a daily chart, live speed, earlier periods, and app search.
- **Screen usage:** seven days of screen time, hourly charts, and per-app time and opens.
- **App stopper:** a list of apps you often stop, with counts. Stop opens App info, or stops directly with Shizuku or root, plus Stop all; long-press to open an app or find it in the store.
- **Stay awake:** keep the screen on for 15 minutes to "until I stop it" with a countdown notification, or while charging through Android's own setting.
- **Flashlight:** brightness levels on Android 13 and newer, and a pulse that blinks the torch for new notifications while the screen is off.
- **Activity launcher:** open and pin any app's exported screens.
- **Link actions:** choose which app opens each link, copy or share links.
- **Widgets:** a glance stack to swipe between battery, memory, storage, data, and screen time, 3x3 disc and 5x1 capsule music players, and data usage widgets, all added from one page.
- **Quick Settings tiles:** Stay awake, Awake while charging, Flashlight, RAM, Data today, Color picker, Private DNS, USB debugging, Developer options, Window blur, Tap to wake, Freeze, and Restart SystemUI.
- **Launcher shortcuts:** long-press the icon for Data usage, Stay awake, Flashlight, or Screen time.

### Power tools (optional)

Allow them once with an ADB command, or through [Shizuku](https://shizuku.rikka.app/) or root. With Shizuku or root, Osyster grants itself settings access, so settings tools keep working when neither is running.

- **Power saving:** battery saver, its automatic level, and each limit it applies.
- **Scale and animations:** all animation speeds at once, extra font sizes, and display size (Shizuku or root).
- **Status bar icons:** hide icons you never look at and show the battery percentage.
- **System tweaks:** debugging switches, window blur, tap to wake, pop-up notifications, touch indicators, and Private DNS with your own provider.
- **Shut-Up!:** open banking apps that refuse developer options; debugging turns off first and Restore puts it back.
- **Standby apps:** see and change each app's standby bucket (Shizuku or root).
- **Freeze:** disable rarely used apps without uninstalling them (Shizuku or root).

### Look and feel

- Wallpaper colors by default, Osyster cyan and five more presets, System, Light, Dark, and Pure black.
- Connected rows, a floating toolbar that turns into a back button, springy motion with predictive back, haptics, and Reduce motion.
- Temperature in °C, °F, or K, a live refresh speed setting, and Block screenshots.
- Two panes on tablets and unfolded foldables.
- A short welcome on first launch, a "What's new" sheet after updates, and settings export and import.

## Community and support

- Join the [Discord community](https://discord.gg/QgUjuNj9U8).
- Report bugs or request features through [GitHub Issues](https://github.com/qtremors/osyster/issues).
- Review changes in the [changelog](CHANGELOG.md).
- Read the [privacy policy](PRIVACY.md).

## Credits

Osyster is built by [Tremors](https://github.com/qtremors). Thanks to the maintainers of:

- [AndroidX](https://developer.android.com/jetpack/androidx), [Jetpack Compose](https://developer.android.com/compose), and [Material 3](https://m3.material.io/)
- [Kotlin](https://kotlinlang.org/), [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines), and [Kotlin Serialization](https://github.com/Kotlin/kotlinx.serialization)
- [Hilt](https://dagger.dev/hilt/), [MaterialKolor](https://github.com/jordond/MaterialKolor), and the [Shizuku API](https://github.com/RikkaApps/Shizuku-API)
- [Roboto Flex](https://fonts.google.com/specimen/Roboto+Flex) for the app's text
- [Essentials](https://github.com/sameerasw/essentials) by sameerasw (MIT), which inspired the interface design
- [Lucide](https://lucide.dev/) and [Simple Icons](https://simpleicons.org/) for the website icons

The app's **Settings, About, Open source licenses** screen lists every license in full. Each project remains the property of its authors and is used under its own license.

## For developers

Architecture, modules, build commands, testing, and release steps live in [DEVELOPMENT.md](DEVELOPMENT.md). Planned work is in [TASKS.md](TASKS.md).

## License

Osyster is free and open source under the **GNU General Public License v3.0**. You can use, study, change, and share it. If you distribute a modified version, you must release its full source under the same license. Read [LICENSE.md](LICENSE.md).

---

<p align="center">
  Made by <a href="https://github.com/qtremors">Tremors</a>
</p>
