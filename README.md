# Xiaomi Pad 8 Pro (piano)

<img src="docs/piano.jpg" width="200" alt="Xiaomi Pad 8 Pro">

Unofficial TWRP 16 device tree for Xiaomi Pad 8 Pro (piano, SM8750). OrangeFox and ROM trees are on the other branches; see [`main`](../../tree/main).

## Status

**Working**
- Decryption (PIN)
- ADB
- Touch
- Mount / unmount
- Backup
- OTA install
- Slot switch
- Battery percentage
- USB detection

**Not verified**
- Charging in recovery (OrangeFox, which uses the same charger setup, charges at standard speed)

## Repository layout

```
├── BoardConfig.mk       # board config
├── device.mk            # product config
├── recovery.fstab       # partitions
├── recovery/root/       # recovery ramdisk
├── prebuilt/            # stock secure-stack binaries
├── sepolicy/            # SELinux policy
├── patches/             # source patches
├── tools/               # build helper scripts
└── docs/                # notes
```
