# LineageOS 23.2 for the Xiaomi Pad 8 Pro (piano)

Device tree for the Xiaomi Pad 8 Pro (`piano`, Snapdragon 8 Elite / SM8750),
LineageOS 23.2 (Android 16). Unofficial and in active bring-up: it boots and
most hardware works, but it is not a release yet.

Proprietary blobs, firmware and the prebuilt kernel come from the Global
HyperOS 3 firmware `OS3.0.304.0.WPYMIXM`. Flash that firmware before
installing a build.

## Status

Tested on hardware with build 24 (2026-10-02). Items marked "next build" are
in the tree but not yet tested on a build. The corrected incremental
`1790940907` also boots enforcing after a data-preserving OTA; its focused
checks are recorded in `docs/INSTALLED_BUILD_1790940907.md`.

| Area | Status |
| --- | --- |
| Boot, setup wizard, launcher | Works |
| Display (3200x2136, up to 144 Hz LCD), touch | Works; peak refresh rate is selectable in Settings |
| Color modes (native, sRGB, P3) | Works, stock panel calibration |
| Rotation | Works with auto-rotate (on by default) |
| Wi-Fi, Bluetooth | Works |
| Audio | Works; volume curve matches stock, with the HyperOS boost level on the top step |
| Dolby Atmos (DolbyManager) | Works |
| Sensors | Works (55 sensors) |
| Smart cover | Works: closing the cover puts the tablet to sleep |
| Cameras (Aperture, front and rear, 4 devices) | Works |
| MiuiCamera (optional) | Opens; full landscape screen is next build |
| USB webcams | Next build, untested with a device |
| Fingerprint (power button, Goodix) | Works, including lock screen unlock |
| Charging | Basic charging works; limit node selected for piano, limit and bypass behavior pending plugged-in validation |
| Thermal profiles (mi_thermald) | Works, stock profiles with a Quick Settings tile |
| Reading mode, True Tone, Sunlight mode | Work (Quick Settings tiles) |
| Desktop windowing | Enabled, untested |
| Default wallpaper by body colour | Works on installed incremental 1790940907; body-colour wallpaper confirmed |
| Stylus | Writes; battery popup like stock is next build, untested with a real pen |
| Keyboard cover | Settings, authentication, firmware update, DND key and guarded hall handling implemented; untested on hardware |
| SELinux | Installed build boots enforcing; full functional soak and suppressed-denial review remain open |

Piano-specific apps:

- `PianoParts`: Quick Settings tiles (thermal profile, touch game mode, reading
  mode, sunlight mode, true tone, bypass charging), edge palm rejection
  rotation, the stylus battery popup, the keyboard cover settings page, and
  MiuiCamera landscape support.
- `PianoVolumeBoost`: shows the stock warning when media volume reaches the
  boost level on the speakers.

Not ported, and why:

- Face unlock: LineageOS has no support for it.
- MEMC and virtual SIM: they need a display service and a modem that piano
  does not have.
- Keyboard NFC tap, touchpad haptics and the mute light: off on Global stock.
- Video enhancement (`videoservice`): it needs a `libgui` symbol Android 16 no
  longer exports.
- Xiaomi Magic Pointer and stylus gestures: see `docs/STOCK_FEATURE_AUDIT.md`.

Known issues:

- MiuiCamera (the stock Xiaomi camera app) comes from the optional
  `miuicamera-16` branch; without it, Aperture is the camera app.
- The camera HAL cannot write its zoom cache under enforcing SELinux, so it
  recomputes it when the camera opens.

## Branches

All trees live in this repository as separate branches:

| Path in the Lineage source | Branch |
| --- | --- |
| `device/xiaomi/piano` | `lineage-23.2` |
| `device/xiaomi/sm8750-common` | `device-common-16` |
| `vendor/xiaomi/piano` | `vendor-16` |
| `vendor/xiaomi/sm8750-common` | `vendor-common-16` |
| `vendor/xiaomi/piano-miuicamera` (optional) | `miuicamera-16` |
| `device/xiaomi/piano-kernel` | `kernel-16` |
| TWRP 16 recovery tree | `twrp-16` |
| OrangeFox R12 recovery tree | `ofox-16` |

The common trees started from the Xiaomi 15 Pro (`haotian`) LineageOS trees and
were adapted to piano's hardware. Piano is an LCD tablet with a side
fingerprint sensor, so display, fingerprint, sensor and camera configuration
come from the piano stock firmware, not from the donor.

## Building

1. Sync LineageOS 23.2 and check out the branches above at their paths.
   `hardware/xiaomi` (LineageOS) is also required.
2. Apply the source patches after every sync:

   ```sh
   bash device/xiaomi/piano/patches/apply-patches.sh
   ```

   They keep the stock display and audio init scripts and remove a conflicting
   generated-header dependency in boot control.
3. Build:

   ```sh
   source build/envsetup.sh
   breakfast piano userdebug
   m bacon
   ```

   Optional environment variable:
   - `PIANO_AVB_KEY_PATH`: a private AVB key kept outside every repository.

The proprietary file list (`proprietary-files.txt`) pins every blob by SHA1 to
the Global 304 firmware. Many stock HALs load libraries by path at runtime, so
a blob missing from the list fails only on the device, not at build time.

## Installing

1. Flash Global HyperOS 3 `OS3.0.304.0.WPYMIXM` firmware.
2. Boot a piano recovery (TWRP or OrangeFox from the `twrp-16` / `ofox-16`
   branches) and install the LineageOS zip.
3. Format data on first install.

Never relock the bootloader with an unofficial build, and do not flash
bootloader firmware (`xbl`, `abl`, `tz`, `hyp`) from other sources.

## Recovery

The TWRP and OrangeFox trees decrypt both HyperOS and custom ROM data using the
stock KeyMint, Gatekeeper and Weaver services. Recovery is still being worked
on; see the `twrp-16` and `ofox-16` branches for the current state.

## Credits

- The LineageOS project.
- The Xiaomi 15 Pro (`haotian`) LineageOS trees, the starting point for the
  common trees.
- Paranoid Android, for DolbyManager and the XiaomiParts features used as the
  model for PianoParts.
- Evolution-X (`hardware_xiaomi`), for the reverse-engineered Xiaomi
  TouchFeature and DisplayFeature interfaces.
