# Xiaomi Pad 8 Pro (piano)

<img src="docs/piano.jpg" width="200" alt="Xiaomi Pad 8 Pro">

Unofficial recovery and ROM trees for the Xiaomi Pad 8 Pro (codename `piano`,
Snapdragon 8 Elite / SM8750).

Each project lives on its own branch. This branch only holds this page and the
GitHub Actions workflows.

## Branches

### Recoveries

| Branch | Project | Status | Checkout path |
| --- | --- | --- | --- |
| [`twrp-16`](../../tree/twrp-16) | TWRP 3.7.1 | Released | `device/xiaomi/piano` |
| [`ofox-16`](../../tree/ofox-16) | OrangeFox R12.0 | Tested, release coming | `device/xiaomi/piano` |

### ROMs

| Branch | Contents | Status | Checkout path |
| --- | --- | --- | --- |
| [`lineage-23.2`](../../tree/lineage-23.2) | LineageOS 23.2 device tree | Work in progress, does not boot yet | `device/xiaomi/piano` |
| [`common-16`](../../tree/common-16) | Shared SM8750 device tree | Work in progress | `device/xiaomi/sm8750-common` |
| [`kernel-16`](../../tree/kernel-16) | Prebuilt kernel, DTB, DTBO and modules | From stock HyperOS | `device/xiaomi/piano-kernel` |
| [`vendor-16`](../../tree/vendor-16) | Proprietary files for piano | From stock HyperOS | `vendor/xiaomi/piano` |
| [`vendor-common-16`](../../tree/vendor-common-16) | Proprietary files shared by SM8750 | From stock HyperOS | `vendor/xiaomi/sm8750-common` |

`16` is the Android version the branch is built for. The recoveries built from
Android 16 source also work with Android 17 ROMs such as HyperOS 4. A ROM
branch uses the ROM's own version (`lineage-23.2`).

`common-16` holds build rules and config shared by SM8750 devices;
`vendor-common-16` holds the matching stock binaries. Device trees and
proprietary files are kept on separate branches, the usual Android layout.

## Tested firmware

- Global OS3.0.303 (Android 16)
- China OS3.0.307 and OS3.0.308 (Android 16)
- China OS4.0.0.42 / HyperOS 4 (Android 17)

## Building LineageOS

Add this as `.repo/local_manifests/piano.xml` in a LineageOS 23.2 tree, then
run `repo sync`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<manifest>
  <remote name="piano" fetch="https://github.com/ALXP-DANIEL" />
  <project name="android_device_xiaomi_piano" path="device/xiaomi/piano" remote="piano" revision="lineage-23.2" />
  <project name="android_device_xiaomi_piano" path="device/xiaomi/sm8750-common" remote="piano" revision="common-16" />
  <project name="android_device_xiaomi_piano" path="device/xiaomi/piano-kernel" remote="piano" revision="kernel-16" clone-depth="1" />
  <project name="android_device_xiaomi_piano" path="vendor/xiaomi/piano" remote="piano" revision="vendor-16" clone-depth="1" />
  <project name="android_device_xiaomi_piano" path="vendor/xiaomi/sm8750-common" remote="piano" revision="vendor-common-16" clone-depth="1" />
</manifest>
```

```
source build/envsetup.sh
breakfast piano
m bacon
```

## Building a recovery

- TWRP: check out `twrp-16` into `device/xiaomi/piano` of a TWRP
  `twrp-16.0` source tree.
- OrangeFox: check out `ofox-16` into `device/xiaomi/piano` of an OrangeFox
  `fox_16.0` source tree, then follow that branch's `patches/README.md`.

Both also build in GitHub Actions: `Actions > TWRP 16 Builder` and
`Actions > OrangeFox R12 Builder`.

## Releases

Downloads are on the [Releases](../../releases) page.
