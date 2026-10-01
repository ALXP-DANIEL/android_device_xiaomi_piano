# LineageOS 23.2 for the Xiaomi Pad 8 Pro (piano)

Device tree for the Xiaomi Pad 8 Pro (`piano`, Snapdragon 8 Elite / SM8750),
LineageOS 23.2 (Android 16). Unofficial and in active bring-up: it boots and
most hardware works, but it is not a release yet.

Proprietary blobs, firmware and the prebuilt kernel come from the Global
HyperOS 3 firmware `OS3.0.304.0.WPYMIXM`. Flash that firmware before
installing a build.

## Status

Tested on hardware with build 17 (2026-10-02). Changes made since then are
marked "next build" until a build containing them has been tested.

| Area | Status |
| --- | --- |
| Boot, setup wizard, launcher | Works |
| Display (3200x2136, up to 144 Hz LCD), touch | Works; peak refresh rate is selectable in Settings |
| Color modes (natural, boosted, saturated, automatic) | Next build: stock panel calibration added |
| Rotation | Works with auto-rotate (on by default); natural orientation is portrait for now |
| Wi-Fi, Bluetooth | Works |
| Audio | Works; volume curve matches stock, with the HyperOS boost level on the top step |
| Dolby Atmos (DolbyManager) | Works |
| Sensors | Works (55 sensors) |
| Smart cover | Works: closing the cover puts the tablet to sleep |
| Camera (Aperture, front and rear) | Works |
| MiuiCamera (optional) | Opens; the camera HAL crash may be fixed in the next build |
| Fingerprint (power button, Goodix) | Works, including lock screen unlock |
| Charging | Works; charging limit next build |
| Thermal profiles (mi_thermald) | Next build: stock profiles added |
| Keyboard cover | Types; arrow key fix next build, battery level and wake handshake not done |
| Stylus | Writes; battery level and buttons not done |
| SELinux | Permissive; enforcing is in progress |

Piano-specific apps:

- `PianoParts` (next build): sends the screen rotation to the touch panel for
  edge palm rejection, and adds a Quick Settings tile for the game thermal
  profile.
- `PianoVolumeBoost`: shows the stock warning when media volume reaches the
  boost level on the speakers.

Known issues:

- MiuiCamera (the stock Xiaomi camera app) comes from the optional
  `miuicamera-16` branch; without it, Aperture is the camera app. Its UI is
  laid out for MIUI and looks compressed.
- The video enhancement service (`videoservice`) is dropped: it needs a `libgui`
  symbol Android 16 no longer exports.

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

   Optional environment variables:
   - `PIANO_AVB_KEY_PATH`: a private AVB key kept outside every repository.
   - `PIANO_ADB_KEYS`: a public adb key, so a userdebug build can be debugged
     before setup finishes. Leave it unset for releases.

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
