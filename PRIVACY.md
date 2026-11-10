# Privacy Policy for Osyster

**Last Updated:** 2026-09-28

Osyster reads information about your phone and changes settings you ask it to change. All of that happens on your phone. This policy explains what it reads, what it stores, and every permission it can use.

## 1. No Data Collection

Osyster **does not send** personal information, usage statistics, device identifiers, or diagnostics to the developer or anyone else. There is no account, no analytics, and no crash reporting.

It never reads permanent identifiers: no serial number, IMEI or MEID, phone number, SIM serial, MAC address, or DRM device ID.

## 2. Offline by Design

The app does not declare `android.permission.INTERNET`, so Android blocks it from the network entirely. The "internet check" on the Network page only asks Android whether the current connection is validated; it does not connect anywhere.

Links to GitHub, Discord, the Shizuku guide, or the project website open your browser or another app, which follows its own privacy policy. The project website loads public GitHub statistics from the GitHub API; the app's offline promise does not cover the website.

## 3. No Advertisements or Trackers

The app contains **no advertisements** and **no third-party analytics, tracking, or crash SDKs**.

## 4. What Osyster Stores on Your Phone

- Theme, Home layout (pins, order, hidden pages), and tool choices.
- Package names you add to App stopper, Shut-Up!, and Freeze lists.
- The debugging switches as they were before a Shut-Up! launch, until you restore them.
- Whether you finished the welcome.
- Battery history, unless you turn it off: the battery level, charging state and charger type, temperature, voltage, and current, read every half hour and kept 30 days.
- Which apps the music widgets follow, and apps that have played media (to offer them in that list).
- Notification history, only if you turn it on: the text of each notification (title, full text, chat messages, replies, button labels), the app it came from, when it arrived and left, and its picture if it had one. It is cleared after the time you choose (one week to one year), and you can delete one entry, one app, or all of it.
- Your temperature unit, refresh speed, and whether screenshots are blocked.

Android may include these preferences in its own device backup. Scan history, notification history, and saved notification pictures are always left out of it. Settings export writes them to a file only where you choose; nothing is uploaded.

## 5. Permissions

Osyster asks for each permission only when a tool needs it, and every tool works without the ones it does not need.

### Normal permissions (granted at install)

- **Network state and Wi-Fi state:** connection type, signal, and link speed on the Network page and the live speed meter.
- **Foreground service, special use, and wake lock:** Stay awake keeps the screen on with a visible notification and Stop button.
- **Query all packages:** App stopper, Activity launcher, Link actions, Shut-Up!, Freeze, and Standby apps list your apps by name and icon.
- **Shizuku API permission:** lets an installed Shizuku recognise Osyster. It does nothing unless you allow Osyster in Shizuku.

### Runtime permissions (asked when you choose)

- **Notifications (Android 13 and newer):** the Stay awake countdown and the Shut-Up! Restore notification.
- **Location:** Android requires it to show the Wi-Fi network name. Asked only when you tap to show it on the Network page. Osyster never reads your position.
- **Phone:** carrier, network type, and roaming on the Network page. Asked only when you tap to show them. Your phone number and IMEI are never read.

### Special access (granted in system Settings)

- **Usage access:** Data usage, Screen usage, and the Data today tile read Android's own counters.
- **Notification access:** music widgets read the playing song from media sessions; Flashlight pulse sees that a notification arrived and which app sent it. Notification text is read and stored only when you turn on notification history, and then only on this phone.
- **Screen capture (Color picker tile):** Android asks every time. Osyster captures one frame, keeps it only in its private cache while you pick a color, and deletes it when you close the picker. A short foreground notification shows while the capture runs.

### Power tools (optional, granted by you outside the app)

- **Write secure settings:** granted once with an ADB command, or by Shizuku or root from Power setup. Used to change settings such as battery saver, animation scales, status bar icons, debugging switches, and Private DNS. Remove it by clearing Osyster's storage or reinstalling.
- **Shizuku or root:** used only while a power tool runs a command (freezing apps, standby buckets, display size, force stop). Each command is a normal Android shell command with quoted arguments. Osyster keeps no root shell open.

Osyster **never** uses an AccessibilityService and never calls hidden system interfaces.

## 6. Children

Osyster collects no data from anyone, including children.

## 7. Changes

Changes to this policy are recorded in this file and in the changelog.

## 8. Contact

Questions or concerns: open an issue on [GitHub](https://github.com/qtremors/osyster/issues) or ask in the [Discord community](https://discord.gg/QgUjuNj9U8).
