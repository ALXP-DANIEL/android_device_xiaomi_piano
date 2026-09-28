# Xiaomi Pad 8 Pro (piano)

<img src="docs/piano.jpg" width="200" alt="Xiaomi Pad 8 Pro">

Unofficial recovery and ROM trees for the Xiaomi Pad 8 Pro (codename `piano`,
Snapdragon 8 Elite / SM8750).

Each project lives on its own branch. This branch only holds this page.

## Branches

| Branch | Project | Status | Checkout path |
| --- | --- | --- | --- |
| [`twrp-16.0`](../../tree/twrp-16.0) | TWRP 3.7.1 | Released | `device/xiaomi/piano` |
| [`ofox-16`](../../tree/ofox-16) | OrangeFox R12.0 | Tested, not released yet | `device/xiaomi/piano` |
| [`lineage-23.2`](../../tree/lineage-23.2) | LineageOS 23.2 | Work in progress, does not boot yet | `device/xiaomi/piano` |
| [`common-16`](../../tree/common-16) | Shared tree for Android 16 ROMs | Work in progress | `device/xiaomi/sm8750-common` |

The number in a recovery branch (`16`) is the Android source the recovery is
built from. Those recoveries also work with Android 17 ROMs such as HyperOS 4.
The number in a ROM branch is the ROM's own version.

## Tested firmware

- Global OS3.0.303 (Android 16)
- China OS3.0.307 and OS3.0.308 (Android 16)
- China OS4.0.0.42 / HyperOS 4 (Android 17)

## Building a ROM

A ROM needs its ROM branch plus `common-16`:

```xml
<project name="ALXP-DANIEL/android_device_xiaomi_piano" path="device/xiaomi/piano" remote="github" revision="lineage-23.2" />
<project name="ALXP-DANIEL/android_device_xiaomi_piano" path="device/xiaomi/sm8750-common" remote="github" revision="common-16" />
```

Proprietary vendor files are not in this repository. Generate them from a
stock ROM with `extract-files.py` on the ROM branch.

## Building OrangeFox

Check out `ofox-16` into `device/xiaomi/piano` of an OrangeFox `fox_16.0`
source tree, then follow the steps in that branch's `patches/README.md`.

## Releases

Downloads are on the [Releases](../../releases) page.
