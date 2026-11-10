# Osyster - Tasks

> **Project:** Osyster
>
> **Version:** 0.3.7
>
> **Last Updated:** 2026-11-10

Open work only. Finished items are in the [CHANGELOG](CHANGELOG.md).

---

## Testing

- [ ] **TEST-0001 - Full device pass**
  - **Location:** whole app
  - **Current:** The scanner, creator, music widgets, data widgets, and Bluetooth remote layouts were checked on a Moto G40 Fusion. Still unchecked on a device: the color picker capture, the glance stack widget, two panes on a tablet, Edit Home dragging, the power tools with Shizuku and root, the remote's gamepad and air mouse on a real host, and notification history over several days.
  - **Change:** Test every screen, tile, widget, shortcut, and power tool on a phone and a tablet, with and without Shizuku and root, and fix what the pass finds.

## Release assets

- [ ] **TEST-0002 - Screenshots**
  - **Location:** `docs/Screenshots`, README, website
  - **Current:** No screenshots yet; the website has no phone showcase and the README has no screenshot table.
  - **Change:** Capture real screens, convert them to lossless WebP, add the README table, and put the phone showcase back in the website hero.

- [ ] **TEST-0003 - Feature graphic**
  - **Location:** `docs/assets`
  - **Current:** The website's `og:image` is the app icon.
  - **Change:** Add a 1024 by 500 `Osyster-feature.png` and use it for `og:image`.
