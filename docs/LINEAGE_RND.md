# LineageOS 23.2 R&D — Xiaomi Pad 8 Pro (`piano`)

Started 2026-09-27. The first ROM build completed on 2026-09-29, but its
first boot failed; the tablet remains on stock Global 3.0.304 slot B.

## Safety and workspace

No ADB/fastboot/device access. OrangeFox Soong PID 11584 was active at initial
inspection, so no Lineage build or Soong analysis is allowed concurrently.
Recovery workspaces and private recovery keys are untouched. No Git push.
Work lives in `lineage-rnd/device/xiaomi/`; original builder files are preserved
in `lineage-rnd/evidence/builder-original-device/`. Stock inputs and generated
payload stay outside source repositories.

## Existing-tree audit

Builder `/home/alif/android/lineage-23.2` sync log ends `SYNC_EXIT=0`.
`build/make` HEAD: e5aaa62172df0f321e68133fa30f42316376bfe8.
Neither device directory is a Git checkout. Piano contains seven files:
BoardConfig, product/device makefiles, AndroidProducts, README, ignore file,
and fstab. Common contains BoardConfigCommon, common.mk, README and ignore file.
Common has only architecture/boot/A-B flags and dynamic-partition enablement.
Missing: kernel/DTB/module inputs, generated vendor trees, extraction lists,
AVB signing, Virtual A/B product inheritance, image filesystem configuration,
HAL/service packaging, VINTF, SELinux and tablet overlays. A synced platform is
not a buildable piano product.

Remote `ALXP-DANIEL/android_device_xiaomi_piano` checked with ls-remote:
only `ofox-16` (95b9530) and `twrp-16.0` (5df8c59); no lineage-23.2 branch.

Donor common 02029f7 and dada e97c8ed provide useful packaging patterns, but
common unconditionally includes phone services, UDFPS, overlays, unrelated
hardware permissions, donor security dates and bypasses for build checks.
Do not replace the small common skeleton wholesale. Keep donor source as a
reference and add only supported components. No donor binary is a piano input.

## Kernel decision

Use the complete China OS3.0.307.0.WPYCNXM stock tuple for initial assembly:
kernel, DTB, DTBO, vendor ramdisk modules, vendor_dlkm and system_dlkm,
including load/dependency/alias/blocklist metadata and both system module layouts.
Stock CN GKI: 6.6.77-android15-8-gf9a1d4bd8353-abogki440974771-4k.
Xiaomi OSS base is 6.6.57; external module coverage and reproducible KMI remain
incomplete. Build OSS as a later milestone after proving a matched source,
toolchain and proprietary-module contract. Never mix regional or H3/H4 modules.
See `analysis/kernel/kernel-strategy.md` for existing evidence.

## Verified stock facts and corrections

Fresh extraction from the requested China 307 logical images confirms:
- vendor first API 35; board first/current API 202404.
- ODM explicitly overrides product first API to 36. Retain PRODUCT_SHIPPING_API_LEVEL=36;
  older analysis reporting only vendor API 35 is incomplete.
- vendor security patch is 2026-02-01. Do not replace with the current platform date.
- Stock boot header v4 has a 36981248-byte kernel, no ramdisk and no OS patch header.
- Vendor boot contains DTB, LZ4 vendor ramdisk and bootconfig.

Keep physical DTBO capacity 25165824 bytes: saved 2026-09-09 device evidence
confirms it, although the stock image is only 18874368 bytes. No new device query.

## Partition and AVB plan

Stock super metadata and saved physical evidence agree: super 13958643712 bytes,
qti_dynamic_partitions maximum 13948157952 bytes (10 MiB outside the group).
Use seven logical members: odm, product, system, system_dlkm, system_ext, vendor,
vendor_dlkm. Omit mi_ext consistently from output, fstab and AVB. Do not use
shifted slot-B sizes in the old lpunpack-info JSON; they are a known parser defect.
Both slot groups share physical capacity under Virtual A/B; do not sum maxima.

Generate new vbmeta, never reuse stock vbmeta against modified partitions.
Keep system_dlkm directly under top-level vbmeta, matching the existing fstab;
vbmeta_system covers product/system/system_ext. Use a new private RSA4096 test
key outside all repositories. Preserve boot/recovery/system chain locations
3/1/2. Anti-rollback index 1 is not a substitute for inspecting each AVB index.
No bootloader relock or secure firmware packaging. Final image signing and
rollback values must be verified before any authorized device test.

## First-boot acceptance and remaining gates

First target: lockscreen, stable display/touch and authorized ADB. Camera,
fingerprint, wireless feature validation and OTA release are later milestones.
Preserve persist-before-qseecomd, modem firmware access for ssgtzd/Weaver,
ADSP/batterysecret charging and boot HAL access to all whole-disk UFS LUNs.
Do not reuse existing encrypted data across patch-level changes; no metadata
key mutation or formatting is authorized by this phase.

Before a full ROM build: finish selected service dependency closure, init,
VINTF/provider uniqueness, SELinux, overlays and kernel packaging; run extraction
and ELF checks, then image assembly/AVB checks. Before starting Soong inspect
`pgrep -a soong_ui`; use taskset -c 0-3, SOONG_GOMEMLIMIT=12GiB and -j2.
Log only to ~/android/builds/lineage-piano-build.log. No /tmp/piano-build.log.
Actual bootability remains unverified until separately authorized tablet testing.

## First installation and boot result (2026-09-29)

The user authorized installing the first Lineage ZIP and accepted a data wipe
if needed. The ZIP is `lineage-23.2-20260929-UNOFFICIAL-piano.zip`, SHA256
`fa93691e341c269306d8b1dcd1249c62eea2c3009186c61d75d7471c8fa2ff8a`.
The tablet actually runs Global OS3.0.304.0.WPYMIXM on slot B, while this ZIP
uses the China OS3.0.307.0.WPYCNXM boot, vendor and module set. Before install,
the active slot-B boot, init_boot, vendor_boot, recovery, vbmeta,
vbmeta_system and dtbo images were backed up and checked against the device;
the copies are under `RND/artifacts/lineage/rollback/OS3.0.304-global-slot-b/`
outside this Git repository.

OrangeFox reported a successful A/B OTA install to inactive slot A. Its OTA
partition list did not include protected bootloader/secure-firmware images.
OrangeFox self-reflashed its own recovery to slot A afterward, so the loose
Lineage recovery image was flashed to `recovery_a` and checked by readback.
Booting slot A showed Mi splash, blank screen, then Mi splash again; the
bootloader fell back to stock slot B and marked A unbootable. A separate
Lineage recovery boot attempt behaved the same way. No ADB appeared on A and
`/sys/fs/pstore` was empty. Stock Global304 on B then booted with
`sys.boot_completed=1`. No data wipe or metadata-key change was performed.

The stock Global304 boot kernel is `6.6.118-android15-8-ge56cf6b09cca`;
the China307-based Lineage boot kernel is `6.6.77-android15-8-gf9a1d4bd8353`.
The vendor_boot ramdisks and DTBs also differ. A controlled test signed the
stock Global304 boot kernel with the local Lineage test key and placed it on
slot A while retaining the China307 vendor_boot/DTB/modules. Recovery boot
still fell back to B. This mismatched test does not isolate the kernel as a
cause. The original Lineage boot_a was restored and verified afterward.
AVB metadata parses correctly and the compared rollback indices do not show
a lower-index explanation. Exact failure remains unknown; investigate the
whole boot/vendor_boot/DTB/module/firmware pairing and early boot chain before
another flash. A data wipe cannot explain the separate recovery boot failure.

At the end of this investigation, the tablet was rebooted from OrangeFox B to
stock Android B. `ro.boot.slot_suffix=_b`, build
`OS3.0.304.0.WPYMIXM`, and `sys.boot_completed=1` were verified. Slot A
remains a failed test installation, not a bootable Lineage release.

## Primary references

- https://source.android.com/docs/core/ota/dynamic_partitions/implement
- https://source.android.com/docs/core/ota/virtual_ab/implement
- https://source.android.com/docs/core/architecture/partitions/vendor-odm-dlkm-partition

Local stock artifacts take precedence for piano-specific facts. Target branch
build/extract-utils source takes precedence for supported build variables.

## Preparation delivered and exercised

Local source: `lineage-rnd/device/xiaomi/piano` and `sm8750-common`.
The prepared trees are also installed in the existing builder Lineage checkout.
Original builder files are backed up in
`~/android/builds/lineage-piano-rnd-20260927/builder-original-xiaomi`.
Neither original directory was Git-managed; no branch was created or pushed.
Do not copy the large `lineage-rnd` directory into a device Git repository.

| Item | Result |
| --- | --- |
| Firmware provenance | `stock-inputs.json`: SHA256 and byte size for seven exact China 307 images |
| Offline image preparation | `prepare-stock.py` successfully verified/unpacked vendor, ODM, both DLKM images, boot and vendor_boot; extracted stock DTBO |
| Kernel inputs | `device/xiaomi/piano-kernel` installed on builder; generated locally under `lineage-rnd/prepared-307/piano-kernel` |
| Module lists | 442 ramdisk, 403 vendor, 192 system .ko files (system includes 96 modules in two layouts); every normal/recovery load-list entry exists |
| Blob split | 242 common files + 162 piano files, no overlapping paths |
| Extraction | All 404 SHA1 pins passed branch-native extract-utils; generated both vendor trees successfully |
| Generated modules | 215 common + 31 piano named modules, no duplicate names within/between generated trees; not a global source collision check |
| Wrong input test | Invalid Weaver payload rejected before modifying generated vendor output |
| Super metadata | Fresh sparse-prefix read: geometry, header and tables SHA256 checks passed; A sizes match stock, every B entry is zero |
| Source checks | Python syntax, manifest path/pin checks, no private keys in source, no formattable fstab flags |
| AVB key | New RSA4096 key on each host outside repositories; builder key checked with OpenSSL, mode 0600 |
| Compilation/boot | Not run; OrangeFox was active during preparation (later PID 15095). Final process check found no Soong process. No Lineage image or lockscreen claim |

Platform source pins: build/make `e5aaa62172df0f321e68133fa30f42316376bfe8`,
extract-utils `44e3b07397a339c2721c637e40cb73159c78ad46`,
vendor/lineage `10b4c2b95466d3772448634f3ee2159d0da46df5`.

Added configuration: stock prebuilt kernel/DTB/DTBO input selection, stock module
copy layout, seven ext4 logical output targets, Virtual A/B with lz4 compression,
stock-aligned AVB chains, pinned boot/vendor SPL, generated vendor inheritance,
Soong device namespaces and vendor/first-stage fstab copy rules.
Existing data/metadata/secure-storage `formattable` flags were removed for this
research phase. Encryption algorithms/key paths were preserved. No new overlay
values were guessed; generic Lineage tablet policy remains inherited.

New AVB defaults use the inspected stock values: top-level rollback 0,
boot and vbmeta_system 1769904000 (2026-02-01), recovery 1; chain locations
3/2/1 respectively. This distinguishes image rollback values from the user's
bootloader anti-rollback index 1. Verification-disable flags were not added.
These source values still require inspection in assembled images.

The private keys are at each host's
`~/.local/share/xiaomi-pad-8-pro/keys/lineage-23.2-avb.pem`.
Mac and builder keys differ; builder-generated images must use the builder key.
No existing recovery signing material was read or reused.

Extraction warnings about failing to back up pinned blobs on the first run were
expected because generated vendor trees did not exist; all files subsequently
matched the source pins and extraction exited 0. Hash mismatch is now checked
explicitly before extract-utils is invoked, not inferred from its console colors.

## Concrete next implementation plan

1. **Init, modules and storage:** adapt the donor root-init ownership to piano;
   install module-loader scripts and order system before vendor loads; preserve
   ramdisk module dependency metadata. Provide init.qcom.rc/ueventd and ensure
   mount_all reaches the prepared fstab. Mount persist before enabling qseecomd;
   stock qseecomd.rc currently starts `on init` and must not ship unchanged.
   Retain modem mounts for ssgtzd/Weaver and ADSP/charging setup. Confirm explicit
   UFS whole-disk permissions and SELinux for the chosen boot HAL.
2. **HAL and ABI closure:** select one boot, USB, health, power and graphics
   provider per interface. Stock composer3/allocator/mapper are listed as the
   initial graphics candidate. `blob-audit.json` contains 1847 dependency edges
   requiring source/namespace checking (50 distinct library names), not 1847
   proven missing libraries. KeyMint V3, graphics-common V5 and sensors V2 frozen
   interfaces exist in the current source; that alone does not prove vendor
   variants or runtime symbol compatibility. No SONAME fixups were applied.
3. **Finish non-ELF dependencies:** trace dlopen clients, RC-imported scripts,
   configuration references and module-requested firmware. Four donor display
   files are absent from piano: libbarrage, libfcmintf, libframecapturemanager,
   and its AIDL library. Piano stock touch RC also references absent
   `/odm/etc/tp_kmsg_init.sh`; remove/adapt that service only after checking the
   intended logging behavior. Stock Novatek BOE/CSOT firmware is present in the
   piano list. Firmware retained for stock module loading is not camera support.
4. **VINTF and SELinux:** assemble a piano device manifest/matrix and import
   only required stock fragments. Reconcile FCM/sepolicy 202404 with the target
   framework; supply Qualcomm policy and minimal piano domains/labels. Do not
   ship stock policy binaries, make the whole device permissive, or disable ELF
   checks to force a pass. Avoid copying donor phone HALs/overlays.
5. **Tablet presentation:** choose panel density, orientation and brightness
   values from saved piano evidence/stock resources, then add only needed tablet
   resource overlays. Validate touch/keyboard declarations separately. No dada
   UDFPS geometry or phone density is an accepted default.
6. **Image gate when host is available:** perform product evaluation, focused
   image/target-files assembly, global module/provider checks, checkvintf, ELF
   checks, AVB chain/hash/size inspection and module-install path verification.
   Recheck active Soong immediately before launch. Full ROM compilation can
   follow only with the documented memory limits and dedicated Lineage log.
7. **First boot after separate device authorization:** establish a recoverable,
   region/slot-matched installation plan and address userdata/SPL compatibility
   without key mutation. Then test display, touch, ADB and lockscreen, collecting
   init/module/SELinux/SurfaceFlinger logs. No user data wipe is authorized now.

This completes an audited extraction and image-configuration starting point.
It does **not** complete the later bootable-tree milestone: init/HAL/SELinux/VINTF
integration and compilation are still required. Keep those gates explicit.

## Evidence locations

- `lineage-rnd/evidence/super-metadata.json` and `tools/check-super.py`
- `lineage-rnd/evidence/kernel-validation.json`
- `lineage-rnd/evidence/extraction.log`
- `lineage-rnd/tools/seed-blobs.py` (requires pyelftools 0.32)
- `lineage-rnd/device/xiaomi/piano/{stock-inputs.json,blob-audit.json}`
- Builder `~/android/builds/lineage-piano-rnd-20260927/extraction.log`

Follow piano's README for reproducible offline preparation. Large generated
payloads, firmware dumps and all private keys remain outside device source trees.

Final source comparison: all 25 prepared source files matched Mac and builder before this final status annotation. No Lineage build was started after OrangeFox became idle because the source integration gates remain open.

## Continuation — early init and provider wiring (2026-09-27)

This section supersedes the earlier statements that init/HAL selection is absent.
Implemented source wiring, still not an assembled or boot-tested ROM:

- Root hardware init now imports the selected Qualcomm USB RC and stock
  batterysecret RC, calls early/late mount_all, loads stock GKI modules before
  starting stock vendor-module loading, and creates secure/display data dirs.
  The stock module loaders retain their existing exit-status limitations;
  `vendor.all.modules.ready=1` is not proof every module loaded successfully.
- qseecomd RC is now source-owned, disabled for automatic class startup and
  started only after a small guard confirms an ext4 RW block-device mount at
  `/mnt/vendor/persist`. Removed the unused car-hibernation restart path so it
  cannot bypass the guard. No actual running service was stopped on the tablet.
- Added a dedicated SELinux domain/property for that guard and Qualcomm policy
  inheritance. This is initial policy wiring, not complete or compiled policy.
- ueventd now supplies firmware search paths, core graphics/security/DSP node
  permissions, and explicit root RW entries for each whole LUN sda through sdz.
  The added SELinux labels cover whole LUNs missing from Qualcomm defaults;
  existing sda GPT and sde boot labels are retained. Partition nodes are not
  matched by the new whole-LUN entries. Qualcomm boot HAL already has RW policy
  for the GPT and boot block types.
- Selected local source Qualcomm boot, USB/gadget, health and power providers,
  plus recovery boot/health and fastbootd. Their own RC/VINTF files remain owned
  by those source modules. No default AOSP duplicate HAL was added. Provider
  source presence does not establish piano runtime compatibility.
- Added the device base manifest at stock FCM/sepolicy 202404, with Gatekeeper V1
  from stock manifest_sun.xml. Added stock vendor properties selecting non-SPU
  Gatekeeper, Adreno EGL and snapalloc. No arbitrary display density was guessed.
- Added the stock peripheral manager service definitions, SSGTZD AID 2912 and
  pm-service NET_BIND_SERVICE file capability. ADSP behavior and charging still
  need runtime validation; recovery's RAM-disk firmware-copy helper was not
  transplanted into Android.
- The stock touch RC referenced missing `/odm/etc/tp_kmsg_init.sh`. Its logging
  service also failed host init validation due to an unspecified user. A
  source-owned copy now omits that single logging service. Touch HAL, input node
  setup and panel-info service remain; their policy/runtime dependencies remain
  part of the normal integration gates.
- Corrected lunch choices to this checkout's actual `bp4a` release config.

Blob lists remain at 404 pinned files: now 243 common + 161 piano. Two stock
module scripts were added; qseecomd and touch RCs moved to source ownership.
The reproducible seed script records the same ownership exclusions.

### Checks and limits

- Offline persist tests passed for RW ext4 mounted, absent mount, tmpfs, wrong
  mountpoint and read-only mount; only the first authorizes qseecomd. Checked that
  no other qsee start trigger remains. Test: `lineage-rnd/tools/test-persist-gate.py`.
- All 20 selected init RC files pass the available host_init_verifier parser.
  This tool is an existing host executable read from OrangeFox output; no
  OrangeFox sources or build artifacts were changed. This does not prove file
  labels, executable dependencies, service startup or whole-image init ordering.
- 15 VINTF inputs assemble successfully, including the five selected source HAL
  fragments and pinned stock fragments. Tool reports expected missing AIDL
  metadata for proprietary interfaces. This is manifest syntax/merge validation,
  **not** compatibility against the complete Lineage framework matrices.
- Offline extraction regenerated both vendor trees successfully after changes.
- A bounded dumpvars/product-evaluation attempt began when the builder had no
  Soong process. OrangeFox restarted (PID 20215) while Lineage's source finder
  was still running. Stopped only Lineage dumpvars PID 19938 and its descendants;
  OrangeFox was left untouched. No product-evaluation result was obtained.
  The log's `panic: write to panicWriter` follows our SIGTERM/shutdown timeout;
  do not misreport it as a new source error or an unexplained compiler crash.
  Log remains `~/android/builds/lineage-piano-build.log`. No full ROM build ran.
  The attempted release argument was corrected in the documented next command
  to bp4a after checking the actual release maps; it was not successfully evaluated.

### Remaining concrete blockers

Finish device policy for the selected Xiaomi services (including batterysecret,
touch and displayfeature), their labels/properties and framework matrix entries.
The focused persist policy and generic Qualcomm policy are not sufficient for
all retained stock services. Check proprietary ELF/source collisions and ABI
compatibility, dlopen/config dependencies, vendor mount directories and symlinks,
and module/firmware boot ordering. Prepare tablet overlays from stock resources.
These remain source/integration work; no SELinux or ELF bypass is authorized.

When the shared host is available, evaluate `lineage_piano-bp4a-userdebug` using
the external builder key, taskset 0-3 and SOONG_GOMEMLIMIT=12GiB before attempting
`-j2` image assembly. Recheck and monitor OrangeFox activity during that work.

New evidence: `lineage-rnd/evidence/init-validation.json`,
`manifest-assembled.xml`, and `vintf-assembly.log`. No tablet access, flash,
userdata operation, commit, or push occurred.


## 2026-09-27: product evaluation and policy build integration

Product dumpvars now passes on the builder: `lineage_piano`, shipping API 36,
and super size 13958643712. `lineage-rnd/tools/check-product-on-builder.py`
provides a five-minute bounded check, CPU 0-3, two jobs and 12 GiB Go limit.
It refuses active Soong and monitors for another Soong starting; only its own
process group is stopped. `--policy` requests selinux_policy, not a full ROM.

Added an initial batterysecret domain and stock-derived Xiaomi charging sysfs
labels. This candidate policy still needs compilation and runtime validation;
it does not import broad donor rootfs-write, sys_boot or generic sysfs-write.
Added nine proprietary AIDL HAL entries to the device framework matrix.
The existing host assemble_vintf tool accepts the fragment with a fixture of
policy version 30 and sepolicy 202404. A negative fixture removing touch also
passes, so this result is only assembly acceptance, not proof of required HAL
presence or complete framework/device compatibility.

The policy target exposed five corrected source/prebuilt partition collisions:
libavservices_minijail, libnetutils, libdmabufheap, libui and libion. These source modules provide vendor variants and
vendor_available. The seed generator now leaves them source-owned; extraction
regenerates 397 pinned files (236 common, 161 piano). Removing minijail's stock
closure also dropped its no-longer-needed private dependency files. ELF checks
remain enabled; using source versions still requires ABI verification.
The generated vendor namespace also incorrectly imported the nonexistent
hardware/qcom-caf/sm8750/display namespace. Both extract scripts now import
hardware/qcom-caf/sm8750, whose actual namespace imports the display trees.

The declaration audit now searches framework hardware interfaces, hardware
libraries and compiler prebuilts, and recognizes both AIDL frozen-version
syntaxes. Earlier seven unresolved names were audit limitations, not missing
libraries. Declaration matching is explicitly not visibility, variant, symbol
or linker-namespace proof. Latest evidence is recorded alongside the build log.
Policy compilation, touch/display policy and full VINTF/ELF integration remain
open. No full ROM, tablet access, flash, commit or push occurred.

### Current build gate result

The latest policy-target attempt stops during Soong analysis, before SELinux
compilation: `libflatbuffers-cpp` has the same source/prebuilt partition conflict.
A wider declaration scan also finds other platform-name overlaps (fmq, unwind,
keymaster support, XML/JSON and ML libraries). Review those together before the
next attempt rather than interpreting these as host memory crashes. Do not
blindly replace proprietary OpenCL or QTI display libraries merely because source
modules have matching names. No build is left running by this session.
Persist-gate regression checks still pass all five mount cases and the sole
QSEE start trigger. The source declaration audit is diagnostic only; its latest
report must be read against its accompanying blob-audit input.


## 2026-09-27: continued build integration

- The original namespace/partition conflicts are past Soong analysis. The
  curated list now has 370 stock-pinned files (209 common, 161 piano). Reviewed
  platform libraries and generated QTI HIDL interface libraries use source
  vendor variants. Symbol checks remain mandatory; this is not runtime proof.
- Common stock display DAL needs piano's ODM client2slpi notifier, so generated
  common vendor namespace explicitly imports vendor/xiaomi/piano.
- The sensor V2/V3 conflict was traced to source sensorservice V1 importing V3.
  Frozen sensors API 2 -> 3 differs only by SensorType.MOISTURE_INTRUSION=43 and
  its hash. Fifteen explicitly listed piano clients receive a DT_NEEDED V2 ->
  V3 fixup after stock SHA1 validation. Generated Android.bp and readelf confirm
  V3. Final ELF symbol checks are still outstanding.
- Added initial scoped touch policy; batterysecret and touch still await policy
  compilation and runtime validation. No permissive domains were introduced.
- Minimal framework overlay values come from China 307 product/overlay/
  AospFrameworkResOverlay.apk. Density 440 comes from product/etc/build.prop.
  Added navigation bar, non-voice tablet and stock floating brightness bounds
  and default. Auto-brightness and sensor calibration are not enabled here.
- The queued check ran after OrangeFox stopped and exposed a launcher omission:
  LINEAGE_BUILD=piano was missing, so build/make/core/config.mk skipped
  BoardConfigLineage.mk and Soong kernel variables were absent. Launcher now
  sets it. The correct Lineage version suffix and QTI namespaces appear.
- Build-check timeout increased to 30 minutes; monitoring for a foreign Soong
  and CPU/memory/job limits remain. Full ROM has not yet been produced.


### Guarded build retry and source integrity

The LINEAGE_BUILD correction reached Soong analysis, but another OrangeFox
Soong appeared and the guard terminated only Lineage's process group. This is
an interrupted check, not another source failure. A waiting job now retries
policy after the shared host is idle, then invokes `bacon` only if policy passes.
The full build is bounded at 24 hours and stops below 20 GiB free disk. Source
failures stop the launcher for diagnosis; it never blindly edits or disables
checks. The queue waits at most six hours for availability.

All fifteen modified sensor binaries now have both original and post-fixup SHA1
pins in proprietary-files.txt. Offline prevalidation checks the original hash;
extract-utils checks the patched hash. Re-extraction passed. The seed generator
preserves these pins from sensor-fixup-hashes.json. The touch HAL executable
label now matches init_daemon_domain's piano_touch_default_exec convention.
No policy success, final ROM or bootability is claimed by these updates.


### AVB fsgen path correction

Latest Soong analysis rejects absolute BOARD_AVB_KEY_PATH paths. The guarded
launcher now exposes the external key directory through `piano-local-keys`, a
builder-local symlink at the repo workspace root (outside any Git project).
Private key bytes remain under ~/.local/share/xiaomi-pad-8-pro/keys, outside
repositories. The launcher validates the existing symlink target and private-key
permissions and refuses to replace another path. No key is copied into a tree.
The updated check is queued behind OrangeFox; this correction is not yet proven
by a completed Soong run.


### Host memory/timeout diagnosis

The 30-minute timeout exposed a supervisor bug: Ninja creates another process
group inside the Lineage session, so killing only the original group left a
16 GiB soong_build alive. The next attempt waited while all 8 GiB swap was used.
Verified the stale command/parent/session before stopping only Lineage processes;
memory returned to about 1 GiB used. The supervisor now checks soong_build as
well as soong_ui and cleans all PIDs in its own session. Policy/ROM runs have a
24-hour ceiling, with the foreign-build and low-disk checks retained.

This checkout's hermetic env -i launch stripped both Go runtime limits from
soong_build. A builder-local patch in build/soong/ui/build/soong.go forwards
SOONG_GOMEMLIMIT as GOMEMLIMIT and forwards GOMAXPROCS via invocationEnv.
Patch saved as lineage-rnd/evidence/soong-host-memory-limits.patch. This is host
resource control, not a source/dependency/policy bypass. Restart is underway;
verify the actual child environment before claiming memory limits are effective.


### Make/Kati duplicate display interfaces

Soong analysis completed with the effective 12 GiB runtime limit. Kati then
reported duplicate vendor.qti.hardware.display.aiqe-V2-ndk from stock and QTI
source. Reviewed the seven matching display AIDL libraries together: aiqe V2,
composer3 V1, config V7/V12, color V1, demura V1, postproc V1. All exact frozen
versions exist in vendor/qcom/opensource/commonsys-intf/display with vendor
variants. Removed those seven stock interface DSOs from generated blob closure;
no ABI version replacements here. Extraction passed with 363 pins (202 common,
161 piano), retaining all fifteen patched sensor output hashes. Policy gate
restarted; symbol validation and final image build still pending.


### Display source namespace and composer import alignment

Added explicit commonsys-intf/display namespace imports to both extract modules;
Soong imports are not transitive. Source color V1 now resolves. The next error
was composer Android V3/V4 mixing: stock service uses Android composer3 V3,
but QTI source composer3 V1 imports Android V4 in this checkout. QTI composer3
V3 imports Android V3. Compared all six frozen QTI V1/V3 AIDL files: no semantic
text differences after removing comments/whitespace and normalizing integer
literal bases; no new files. Added one DT_NEEDED V1 -> V3 fixup to the common
stock composer service, with original/patched hash pins. ELF checking remains
on and symbol compatibility still must pass. Build restarted after extraction.


### Prebuilt kernel version for Lineage Make rules

After Soong and display interface resolution, Kati reached kernel.mk:140 and
failed its version comparison because TARGET_KERNEL_VERSION was unset. Source
Makefile discovery cannot infer a version for this prebuilt-only tree. Set it
explicitly to 6.6, matching the pinned stock kernel's 6.6.77 release, and restarted
the guarded policy/full-build sequence. No kernel binary or module was changed.


## 2026-09-28: single composer manifest owner

Kernel version comparison now passes. Kati reached final install rules but found
the same composer manifest destination from source and stock. Audited generated
install targets: this was the only distinct-provider duplicate destination.
Source manifest matches stock standard composer/color/postproc/config/aiqe HAL
versions. Removed the stock copy, explicitly selected the source manifest
module, and retained stock MiHwcExtension V1 in piano's base manifest. This
preserves the Xiaomi declaration rather than silently dropping it. Extraction
passed with 362 pins (201 common, 161 piano). Guarded policy build restarted.


### Mapper/allocator VINTF fragments

Composer conflict cleared. Kati next exposed mapper.qti.xml via an implicit
copy-vintf-manifest-checked macro; the earlier plain-rule audit did not cover
those macros. Expanded inspection found mapper and allocator overlapping stock
prebuilt modules. Both source declarations match stock (native mapper 5.0/qti,
AIDL allocator V2/default). Removed both stock fragment copies, retaining source
fragments associated with their HAL modules. Extraction passed with 360 pins
(199 common, 161 piano), and restarted the build. Final packaged manifest still
needs whole-image validation; no HAL declaration is intentionally dropped.

### Display fragment packaging follow-up

Read-only inspection of the generated Android-lineage_piano.mk found that the
selected stock mapper.qti and allocator-service prebuilt modules have install
pairs for their binaries but no explicit VINTF fragment attachment. The source
HAL modules declare those fragments in hal/gralloc/Android.bp, which explains
the duplicate global install rules seen before removal of the stock XML modules.
This does not yet prove that the fragments remain in the final selected product.
Before claiming the ROM complete, inspect the regenerated product install graph
and assembled vendor manifest for mapper native 5.0/qti and allocator AIDL 2
IAllocator/default. If absent, attach the existing source fragments to the
selected prebuilts; do not merely suppress compatibility checks.

### Stock display init RC ownership

The next guarded policy run passed Soong but Kati found a duplicate install
rule for `vendor.qti.hardware.display.allocator-service.rc`. An install-graph
audit found the same pending collision for the composer RC. China 307 RCs
include Xiaomi memory-pool and panel permissions absent from QTI source RCs,
so stock retains ownership. In the builder-only QTI display source, removed
the allocator binary's `init_rc` attachment and the composer source binary's
`required` reference to its RC. The stock generated vendor makefile still
installs both RCs. Reproducible patch:
`lineage-rnd/evidence/qti-display-stock-init-rc.patch`, applied relative to
`hardware/qcom-caf/sm8750/display`. No upstream repository was modified.
The next guarded build must verify single RC install rules and later verify
the packaged RC contents.

### Install-destination collision audit

Added `lineage-rnd/tools/audit-install-collisions.py` to compare explicit Soong
install targets against piano/common `PRODUCT_COPY_FILES` targets and detect
repeated targets on either side. The last generated install file, made before
the display source edits, reports exactly two copy-vs-Soong collisions: stock
allocator and composer RC versus their QTI source RCs. The source references
were removed while keeping the stock RC copies. This audit is limited to
explicit Soong rules and the listed makefiles; Kati macro-expanded ownership
and final package contents still need the live policy build to validate. Re-run
the scanner after Soong regenerates `installs-lineage_piano.mk`.

### Fresh collision audit after builder returned

New `installs-lineage_piano.mk` timestamp 2026-09-27 23:12:49 UTC passed
the stale-input check. The explicit destination audit found one remaining
collision: composer RC. Allocator RC was fixed. Soong emits an install rule
for a standalone `prebuilt_etc` composer RC module even after its service's
`required` reference is removed. Removed that unused source module from the
builder QTI display tree; the pinned China 307 RC remains in the generated
vendor makefile, preserving the Xiaomi-specific panel and memory-pool lines.
Updated and reverse dry-run validated
`lineage-rnd/evidence/qti-display-stock-init-rc.patch`. A new guarded policy
build is required to verify the regenerated install graph and Kati.

### First policy compilation on 23 GiB builder

The guarded policy target passed Soong and Kati, then reached 1791/2295
(78%) before `check_prop_prefix` rejected the piano property-context *type*
names `piano_panel_prop` and `piano_touch_prop`. Property names already use
allowed `vendor.` and `persist.vendor.` prefixes. Renamed the type declarations,
contexts, and set_prop references to `vendor_piano_panel_prop` and
`vendor_piano_touch_prop`; kept the property names unchanged. This is the
first source policy error found; restart the guarded policy gate and inspect
subsequent failures without disabling namespace enforcement.

### Concurrent OrangeFox/Lineage override

The user explicitly authorized running Lineage beside the active OrangeFox
build after the earlier guard stopped Lineage at 8968/155288 steps. The old
queue was stopped by exact PID; no OrangeFox process was touched. The guarded
Lineage runner now refuses only another *Lineage* Soong and only interrupts on
another Lineage Soong session. Lineage stays at `-j2` with CPUs 4-7; the
observed OrangeFox Soong is pinned to CPUs 0-3. Resource monitoring and the
20 GiB free-space gate remain. Launched the guarded `bacon` resume directly,
with logs in the dedicated Lineage build path. Concurrent compilation may
increase memory and I/O pressure, so inspect live process and available RAM
before drawing conclusions from slow progress.

### Build and Mac mirror follow-up (2026-09-29)

The user authorized a faster standalone Lineage build. The active guarded
`bacon` run uses `-j8` on CPUs 0-11 with the same 12 GiB Soong Go limit and
dedicated Lineage log. A builder-side watcher records status in
`~/android/builds/lineage-piano-watch.status` and retries only generated
archives proven invalid as ZIP files. Earlier interrupted runs left several
truncated JAR/APK intermediates; those were removed individually. No source
error has been attributed to these archive failures. The current run has
reached Ninja; a completed ROM and bootability are not yet claimed.

The Mac now mirrors builder `device/xiaomi/{piano,sm8750-common}` and
`vendor/xiaomi/{piano,sm8750-common}` under `lineage-rnd/`. The prepared
`piano-kernel` directory was compared with `rsync --checksum`; all 1,144
files matched, so no second copy was made. The local R&D document is newer
than the builder's and was preserved. A current builder install-graph audit
reported zero duplicate destinations across 72,026 Soong and 163 copy
destinations. The offline persist gate still passes all six checks. These
are source/build checks, not Android runtime validation.

### Kernel header generation (2026-09-29)

The first full `bacon` attempt stopped at `generated_kernel_includes` because
Lineage's default `TARGET_KERNEL_SOURCE` resolved to `kernel/xiaomi/piano`,
which was absent. The source tree now states that path explicitly. The
existing Xiaomi OSS `oss/kernel_piano` was copied to the builder there without
its Git history. An isolated `make ... ARCH=arm64 headers_install` completed
successfully and installed 1,958 UAPI header files, including `linux/bsg.h`.
The boot image and DLKM modules still use the pinned stock 307 prebuilts.
Xiaomi OSS is based on 6.6.57 while the stock kernel is 6.6.77; success of
header generation does not by itself establish runtime kernel ABI compatibility.

Subsequent build checks found a dangling `include/linux/mca` symlink in the
copied OSS tree. Its proprietary target is not available; the symlink was
removed only from the builder's header-generation copy. `headers_install`
still passed. Lineage's Make rules also require `TARGET_KERNEL_CONFIG` when
source exists, so the device tree specifies `gki_defconfig` and forces the
stock prebuilt kernel. An explicit clang version supplies the host compiler
for Soong's isolated `headers_install` command.

At 24% of Ninja, `libgptutils.qti` failed because its generated-kernel-header
dependency placed `linux/sched/types.h` ahead of bionic's copy, redefining
`sched_param`. The needed UFS BSG types already exist in bionic's UAPI headers.
The builder's `hardware/qcom-caf/bootctrl/gpt-utils/Android.bp` now drops that
dependency; the reproducible one-line patch is in
`patches/hardware_qcom-caf_bootctrl/0001-drop-generated-kernel-headers.patch`.
Run `patches/apply-patches.sh` from the Lineage root to restore source-project
changes after a repo sync. A new build is running;
this change and the ROM have not yet passed validation.

The following attempt reached 69% of Ninja, then Android's ELF copy check
rejected stock `.ko` files in `PRODUCT_COPY_FILES`. `kernel.mk` deliberately
copies the paired 307 vendor ramdisk and vendor DLKM modules alongside their
`modules.*` metadata; rebuilding or running depmod against the 6.6.57 OSS
source would break that pairing. `BoardConfigKernel.mk` now enables Android's
`BUILD_BROKEN_ELF_PREBUILT_PRODUCT_COPY_FILES` exception for this device.
This disables the ELF copy check for the device, so the final package must
still be audited for unexpected ELF copies. The build was restarted; no ROM
success or bootability is claimed yet.

The next failure was vendor VINTF assembly: the hand-written manifest fixed
`<sepolicy><version>` at `202404`, while this Lineage build exports
`BOARD_SEPOLICY_VERS=202504`. The explicit element was removed; the existing
`target-level="202404"` remains. Running `assemble_vintf` with the build's
environment now succeeds and emits policy version `202504`. Full VINTF and
runtime compatibility still require the completed build and device testing.

### First completed ROM build (2026-09-29)

The guarded `bacon` build exited 0. Its last 100% line was followed by a
false `FAILED` status from the watcher, which expected a success phrase absent
from this build's log. The wrapper launch log records `Product evaluation
exit: 0`; the watcher status has been corrected, and future wrappers will
write an explicit exit marker to the build log.

`lineage-23.2-20260929-UNOFFICIAL-piano.zip` is 844,829,226 bytes, SHA256
`fa93691e341c269306d8b1dcd1249c62eea2c3009186c61d75d7471c8fa2ff8a`.
Offline validation passed the ZIP CRC and piano A/B OTA metadata checks,
presence and partition-size checks for boot, init_boot, vendor_boot, recovery,
dtbo, vbmeta and vbmeta_system, and AVB algorithm, rollback indexes, flags and
chain locations (boot 3, recovery 1, vbmeta_system 2). `super_empty.img` is
present. This is a build result only; tablet boot and runtime behavior remain
untested.

### Boot bring-up on China 307 firmware (2026-09-29, Claude session)

- Tablet moved to China OS3.0.307 with the official fastboot package
  (`flash_all.sh`; data wiped). Stock 307 boots on slot A. OrangeFox R12.0_1
  is on both recovery slots. Lineage `fa93691e…` installed to slot B.
- The firmware region/version mismatch was not the cause: on matching 307
  firmware slot B still fell back to A within about 24 s
  (`ro.boot.bootreason=bootloader`).
- Cause of the fallback: the Lineage `vbmeta` listed 10 partitions; stock
  lists 13. `pvmfw` and `countrycode` were missing (`mi_ext` too, but Lineage
  has no `mi_ext`). Adding the stock 307 hash descriptors for `pvmfw` and
  `countrycode` (commit 2d46cf6, images in `piano-kernel/avb/`) makes the
  bootloader accept slot B and hand over to the kernel.
- New state: black screen, nothing on USB (no ADB, fastboot, or Qualcomm
  9008/900E) for over 10 minutes. With
  `androidboot.init_fatal_reboot_target=recovery` in bootconfig it still does
  not reach OrangeFox, so it is a hang, not a first-stage mount failure.
  ramoops is registered (0x400000@0xa3500000) but pstore is empty after a
  warm reset into OrangeFox.
- Compared with stock 307: kernel, DTB, dtbo, bootconfig and the whole vendor
  ramdisk are byte-identical, except the first-stage `fstab.qcom` (mi_ext
  overlays and `formattable` removed). The boot header gains os_version 16
  and patch level 2026-09. `init_boot` differs (Lineage first-stage init,
  1.69 MB vs stock 2.46 MB).
- Next test: stock 307 `init_boot` on `init_boot_b` with the rest of slot B
  Lineage, to split first-stage init from later stages.
