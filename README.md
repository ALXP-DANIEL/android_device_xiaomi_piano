# MiuiCamera for the Xiaomi Pad 8 Pro (piano)

Xiaomi's camera app from the piano Global HyperOS 3 firmware
(`OS3.0.303.0.WPYMIXM`, MiuiCamera 6.3.006770.1, tablet build), packaged for
LineageOS 23.2.

Place this repository at `vendor/xiaomi/piano-miuicamera`. The piano device
tree includes it automatically when it is present.

## Changes from stock

- `libmicampostproc_client.so`: removed the `libhidltransport.so` dependency,
  which Android 16 merged into `libhidlbase.so`.
- `libcamera_mianode_jni.xiaomi.so`: added `libandroid_runtime.so` as a
  dependency, which provides `android::lockImageFromBuffer`.
- `MiuiCamera.apk` is split into two parts to stay under GitHub's 100 MB file
  limit; the build joins them.

All files are proprietary and belong to Xiaomi.
