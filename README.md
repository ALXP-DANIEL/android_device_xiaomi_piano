# LineageOS 23.2 piano — research tree

LineageOS 23.2 device tree for the Xiaomi Pad 8 Pro (`piano`). Work in progress: it does not boot yet. Use it together with the [`device-common-16`](../../tree/device-common-16) branch at `device/xiaomi/sm8750-common`, plus `kernel-16`, `vendor-16` and `vendor-common-16` (see the manifest in `main`). Other projects: [`main`](../../tree/main).

China HyperOS 3 `OS3.0.307.0.WPYCNXM` only. A Lineage `bacon` build is in
progress on the builder; no completed or boot-tested ROM is claimed. See
`docs/LINEAGE_RND.md` for findings and remaining gates.

The tree inherits the small adapted sm8750-common scaffold, not the complete
phone donor. `proprietary-files.txt` adds piano assets to the common first-boot
list. Both lists are pinned to the same stock input; ELF checking stays enabled.
No source HAL substitutions, SONAME rewrites or donor check bypasses are implied.

## Offline preparation

On the Mac, from the project root (requires Python 3, fsck.erofs, lz4 and cpio):

```sh
rtk proxy python3 lineage-rnd/device/xiaomi/piano/prepare-stock.py \
  stock/hyperos3/china/OS3.0.307.0.WPYCNXM/super-unpacked \
  --output lineage-rnd/new-prepared-307 \
  --unpack-bootimg tools/mkbootimg/unpack_bootimg.py
```

Output must be new. Seven image SHA256 hashes are checked before extraction.
Copy output `piano-kernel` into `device/xiaomi/piano-kernel` in Lineage. Place
this tree and the prepared common tree in their matching device paths. Pass the
output `dump` directory to `device/xiaomi/piano/extract-files.py` using Python 3
from the Lineage checkout. This invokes the branch-native extract-utils and
generates `vendor/xiaomi/piano` and `vendor/xiaomi/sm8750-common`.
Lineage also runs `generated_kernel_includes` even with the prebuilt boot
kernel. Place Xiaomi's OSS `oss/kernel_piano` source at
`kernel/xiaomi/piano` for its `headers_install` target. That header step does
not replace the pinned stock kernel image or modules.
The extraction entry point requires an offline directory and rejects any missing
or mismatched blob before touching generated vendor files. It cannot default to ADB.

`stock-inputs.json` pins image SHA256/size; proprietary lists use extract-utils
SHA1 syntax. `blob-audit.json` records missing donor seeds and unresolved ABI edges.
Do not interpret successful extraction as successful ELF linking or bootability.

## Kernel and signing

Preserve all stock ramdisk/vendor/system module files and metadata. System DLKM
keeps both release-tree and flattened module layouts; stock etc/build.prop and
fs_config are excluded so the build can generate its own metadata.

Run `generate-test-key.py` once per host. It exclusively creates an external
RSA4096 key under `~/.local/share/xiaomi-pad-8-pro/keys/lineage-23.2-avb.pem`.
Existing keys are never overwritten. The current builder wrapper sets
`PIANO_AVB_KEY_PATH=piano-local-keys/lineage-23.2-avb.pem`, where
`piano-local-keys` is a source-root symlink to the external key directory.
No private key belongs in device/vendor/kernel trees.
The Mac and builder keys are different; builder images use the builder key.

## Before building

Finish validation of init/ueventd, VINTF, SELinux, framework ABI closure and
tablet overlays.
Root init/module loading and source USB/boot/health/power providers are now wired.
qseecomd starts only after an offline-tested persist mount guard. Twenty RCs pass
parsing and 15 VINTF fragments assemble. Full policy, matrix/ABI compatibility and
runtime behavior remain unverified. See the continuation section in the R&D log.
The current user-authorized build uses `taskset -c 0-11`,
`SOONG_GOMEMLIMIT=12GiB`, `-j8`, and
`~/android/builds/lineage-piano-build.log`. The watcher status is
`~/android/builds/lineage-piano-watch.status`. Check memory and other Soong
processes before restarting it; do not start a duplicate Lineage build.

No Lineage image has been flashed. Userdata formatting, secure firmware
packaging, bootloader relocking, commits and remote pushes remain outside this
preparation phase.
