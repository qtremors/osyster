# Osyster Privacy Policy

**Last updated:** 2026-11-10

Osyster reads information about your phone and changes settings when you ask it to. All of it happens on your phone. This page lists what Osyster reads, what it keeps, and why it asks for each permission.

## The short version

- Osyster sends nothing to anyone. Not to the developer, not to anyone else.
- It has no internet permission, so Android blocks it from going online at all.
- No ads, no accounts, no analytics, no crash reports.
- It never reads your serial number, IMEI, phone number, SIM number, or MAC address.

## Offline

Because Osyster has no internet permission, it cannot connect anywhere. The "internet check" on the Network page only asks Android whether your connection works; Osyster itself connects to nothing.

Links to GitHub, Discord, the Shizuku guide, or the website open in your browser or another app, which has its own privacy policy. The website shows public numbers from GitHub (stars and downloads). The website is not part of the app, so the app's offline promise doesn't cover it.

## What Osyster keeps on your phone

- Your theme, your Home layout, and your choices in each tool.
- The apps you add to App stopper, Shut-Up!, and Freeze.
- Your debugging settings from before a Shut-Up! launch, until you restore them.
- Whether you finished the welcome screens.
- Your temperature unit, refresh speed, and whether screenshots are blocked.
- **Battery history** (on unless you turn it off): battery level, charging state and charger type, temperature, voltage, and current, once every half hour, kept for 30 days.
- **Music widgets:** which apps they follow, and which apps have played music (so you can pick from them).
- **Scan history:** codes you scanned, until you delete them.
- **Notification history** (only if you turn it on): each notification's text, chat messages, replies, button labels, picture, the app it came from, and when it arrived and left. It's deleted after the time you choose (one week to one year), and you can delete one entry, one app, or all of it at any time.

Android may copy your settings into its own phone backup. Scan history, notification history, and notification pictures are always left out. Settings export saves a file only where you choose, and uploads nothing.

## Permissions

Osyster asks for a permission only when a tool needs it. Every tool works without the permissions it doesn't need.

### Granted when you install

- **Network and Wi-Fi state:** connection type, signal, and speed on the Network page.
- **Foreground service and wake lock:** lets Stay awake keep the screen on, with a notification and a Stop button.
- **See installed apps:** lets App stopper, App manager, Activity launcher, Link actions, Shut-Up!, Freeze, Standby, and the music app list show your apps. App manager also shows what Android tells any app about the others: versions, permissions, and signing certificates. It changes nothing.
- **Ask to uninstall apps:** App manager's Uninstall button opens Android's own uninstall prompt. Android asks you every time.
- **Shizuku:** lets Shizuku recognise Osyster. It does nothing unless you allow Osyster inside Shizuku.

### Asked when you use a tool

- **Notifications (Android 13 and newer):** the Stay awake countdown and the Shut-Up! restore notification.
- **Location:** Android needs it to show your Wi-Fi network's name. Asked only when you tap to show it. Osyster never reads where you are.
- **Camera:** the barcode scanner reads codes from the camera. Nothing is saved.
- **Microphone:** the sound meter turns what it hears into a loudness number while the page is open. Nothing is recorded.
- **Nearby devices (Bluetooth):** the Bluetooth remote connects to your TV or computer. Device addresses are used only to connect.
- **Phone:** carrier, network type, and roaming on the Network page. Asked only when you tap to show them. Your phone number and IMEI are never read.

### Turned on in Android's settings

- **Usage access:** data usage, screen time, the Data today tile, and app sizes in App manager. Osyster reads Android's own counters.
- **Notification access:** music widgets see what's playing, and the flashlight blink sees that a notification arrived. Notification text is saved only if you turn on notification history.
- **Screen capture (Color picker tile):** Android asks every time. Osyster takes one picture of the screen, keeps it only while you pick a color, and deletes it when you close the picker.

### Extra tools (optional)

- **Change system settings:** allowed once with a command from a computer, or through Shizuku or root. Used to change battery saver, animation speed, status bar icons, debugging switches, and Private DNS. To take it back, clear Osyster's storage or reinstall it.
- **Shizuku or root:** used only while an extra tool runs a command, such as freezing an app or changing display size. Osyster never keeps root open in the background.

Osyster never uses accessibility services and never uses hidden parts of Android.

## Children

Osyster collects nothing from anyone, children included.

## Changes

Changes to this policy are written here and in the [changelog](CHANGELOG.md).

## Contact

Questions? Open an issue on [GitHub](https://github.com/qtremors/osyster/issues) or ask in the [Discord](https://discord.gg/QgUjuNj9U8).
