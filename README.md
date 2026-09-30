# Prebuilt kernel and modules for Xiaomi Pad 8 Pro (piano)

Goes at `device/xiaomi/piano-kernel` in any Android 16 ROM tree (LineageOS
23.2 and ROMs based on it). Taken from stock HyperOS firmware, so it does not
depend on the ROM. Other parts of the piano build are on the other branches;
see the [`main`](../../tree/main) branch.

## Kernel headers

`prebuilt_kernel_headers.tar.gz` and `kernel-headers/Makefile` come from
[xiaomi-haotian-devs/android_device_xiaomi_haotian-kernel](https://github.com/xiaomi-haotian-devs/android_device_xiaomi_haotian-kernel)
(branch `bp4a`), an SM8750 Xiaomi tree. They are UAPI headers used only to build
userspace HALs (for example `linux/msm_ipa.h`, which Xiaomi's piano OSS kernel
does not include); the boot kernel and modules above stay stock. The wrapper
Makefile unpacks the tarball through Android's normal `headers_install` step.
