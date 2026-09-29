# OrangeFox for Xiaomi Pad 8 Pro (piano)

<img src="docs/piano.jpg" width="200" alt="Xiaomi Pad 8 Pro">

Unofficial OrangeFox R12.0 (`fox_16.0`) device tree for the Xiaomi Pad 8 Pro
(`piano`, SM8750). Other projects for this tablet are on the other branches;
see the [`main`](../../tree/main) branch.

## Status

Tested on Global OS3.0.303, China OS3.0.307, OTA to OS3.0.308 and OTA to
HyperOS 4 (OS4.0.0.42, Android 17). 26 feature checks pass on each.

**Working**
- Decryption (PIN), on HyperOS 3 and HyperOS 4
- ADB, MTP and ADB sideload
- Touch, landscape layout sized for the tablet
- Backup, restore, wipe, zip install
- OTA install with slot switch; OrangeFox copies itself to the new slot
- Magisk queued after an OTA goes to the new slot
- Battery percentage, clock and time zone
- Flashlight

**Not working**
- Charging while in recovery
- Vibration (the tablet has no vibration motor)

## Building

OrangeFox needs source changes on top of this tree. Setup, the local
manifests and the patch script are described in
[`patches/README.md`](patches/README.md). In short:

```
repo init -u https://github.com/OrangeFox16/platform_manifest_twrp_aosp -b twrp-16
cp device/xiaomi/piano/manifests/*.xml .repo/local_manifests/
repo sync
git clone -b fox_16.0 https://gitlab.com/OrangeFox/vendor/recovery.git vendor/recovery
device/xiaomi/piano/tools/apply-ofox-patches.sh .
source build/envsetup.sh
lunch twrp_piano-bp2a-eng
m recoveryimage
```

Put your own AVB key at `security/recovery_avb.pem` first. Keys are never
committed.

The GitHub Actions workflow (`Actions > OrangeFox R12 Builder`, file `orangefox.yml` on the `main` branch) runs the same
steps with a throwaway key. Its image proves the source builds; it is not the
tested release image.

## Installing

```
fastboot flash recovery_ab OrangeFox-R12.0-Unofficial-piano.img
```

After an OTA, unlock the tablet once in Android before decrypting in OrangeFox.

## Layout

```
├── BoardConfig.mk       # board config
├── device.mk            # product config
├── vendorsetup.sh       # OrangeFox build options
├── recovery.fstab       # partitions
├── recovery/root/       # recovery ramdisk
├── manifests/           # local manifests for the OrangeFox source
├── patches/             # OrangeFox source patches + README
├── kernel-headers/      # headers for the boot control HAL
├── prebuilt/            # stock secure-stack binaries
├── sepolicy/            # SELinux policy
├── tools/               # patch script and image audits
└── docs/                # notes
```
