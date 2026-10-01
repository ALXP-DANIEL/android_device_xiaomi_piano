# LineageOS 23.2 for the Xiaomi Pad 8 Pro (piano)

Device tree for the Xiaomi Pad 8 Pro (`piano`, Snapdragon 8 Elite / SM8750),
LineageOS 23.2 (Android 16). Unofficial and in active bring-up: it boots and
most hardware works, but it is not a release yet.

Proprietary blobs, firmware and the prebuilt kernel come from the Global
HyperOS 3 firmware `OS3.0.304.0.WPYMIXM`. Flash that firmware before
installing a build.

## Status

Tested on hardware with build 12 (2026-10-01):

| Area | Status |
| --- | --- |
| Boot, setup wizard, launcher | Works |
| Display (3200x2136, 120 Hz LCD), touch | Works |
| Rotation | Works with auto-rotate (on by default); natural orientation is portrait for now |
| Wi-Fi, Bluetooth | Works |
| Audio | Works (speakers and mic not fully tested) |
| Sensors | Works (55 sensors) |
| Camera (Aperture, front and rear) | Works |
| Fingerprint (power button, Goodix) | Works, including lock screen unlock |
| Charging | Works |
| Stylus, keyboard cover | Untested |
| SELinux | Permissive; enforcing is in progress |

Known issues:

- MiuiCamera (the stock Xiaomi camera app) comes from the optional
  `miuicamera-16` branch; without it, Aperture is the camera app.
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
