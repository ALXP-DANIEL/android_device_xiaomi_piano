# Stock feature audit — 2026-10-02

Not all stock features are ported. Most hardware plumbing is present, and
PianoParts now implements several HyperOS controls. Source presence does not
prove installed behavior or full HyperOS parity.

## Baselines and evidence

- Vendor/ODM baseline: Global `OS3.0.304.0.WPYMIXM`, from
  `RND/lineage-rnd/prepared-global-304-full/dump` in the Mac workspace.
- Framework/app reference: Global `OS3.0.303.0.WPYMIXM` product and system_ext
  EROFS images. Global 304 product/system_ext are not in that dump. Differences
  between those versions remain unverified.
- Reviewed device HEAD `116b5f8`, common HEAD `345e6ca`, and the current vendor
  and MiuiCamera trees. The installed tablet still reports
  `23.2-20261001-UNOFFICIAL-piano`; its build number cannot establish which of
  the newest source changes it contains.
- [Inventory](stock-audit/vendor-odm-inventory.tsv) covers all 4,726 regular
  files/resolvable file symlinks in the supplied stock vendor/ODM dump.
  3,046 are selected and physically present at their extraction destinations.
  160 other paths have build-file name references requiring replacement review;
  1,520 have no such reference. Those counts are file counts, not feature counts.
- Generated installed-files JSON reports from the builder's most recent output
  contain 3,681 of the stock paths or declared extraction destinations. These
  reports are an older build snapshot, not the latest local source. A missing
  path can also be a relocation, source replacement or symlink omitted from
  the JSON; it must not be interpreted as a missing feature without inspection.
- The local evidence directory `RND/evidence/stock-feature-audit-20261002`
  contains product app lists, framework lists, and descriptors extracted from
  the Global 303 pencil/pointer framework JARs. Descriptors prove code presence,
  not complete functionality.

## Feature matrix

"Implemented" below means code/configuration exists in the current tree. It
does not mean the newest installed build passed a hardware test.

| Stock area | Current implementation/evidence | Remaining work |
| --- | --- | --- |
| Display and refresh rate | Stock display stack, panel calibration and framework overlays | Verify every advertised refresh rate, color mode and suspend/resume on the current build |
| Automatic brightness | Stock lux/backlight curve in piano framework overlay | Validate transitions outdoors and in darkness; compare full stock display policy |
| Sunlight mode | `parts/.../SunlightModeController.java`, stock panel thresholds, QS control | Physical high-lux test and enforcing access test |
| Reading mode | PianoParts DisplayFeature feature 31 | Full stock schedule, texture and intensity controls are not implemented by this tile |
| True Tone | PianoParts DisplayFeature feature 32 | Validate sensor-driven color changes; complete stock color-temperature controls remain open |
| Touch palm rejection | PianoParts sends rotation via TouchFeature mode 8 | Verify all rotations with fingers and pen |
| Game touch mode | PianoParts mode 0 QS control; stock touch/stylus configs selected | A binary toggle is not all Game Turbo touch tuning |
| Thermal management | Stock thermal profiles and mi_thermald; default/game profile QS control | Full per-app profile selection is not demonstrated; verify policy selection and temperatures |
| Charging limit | Common Lineage health uses piano `smart_night` path | Current-build charge limit test, including plugged-in reboot |
| Bypass charging | PianoParts writes stock `smart_chg` command `0x400` | Check battery current/charger behavior and automatic stop conditions on hardware |
| Keyboard cover | PadKeyboardManager/Transport/Protocol/Angle/Updater; settings and stock firmware | Wake, keepalive, backlight, caps LED, fold inhibition, touchpad, versions and update protocol need keyboard hardware validation |
| Stylus battery | PenBatteryNotifier consumes charger uevents/Bluetooth battery data; popup/notification | Real pen attachment, low battery, disconnect and enforcing tests |
| Stylus gestures/buttons | Stock `xiaomi-pencilengine-pad.jar` contains gesture/handwriting classes; no equivalent integration located in current piano source | Trace stock framework input handling and port compatible pieces; battery reporting does not cover this |
| Mouse/touchpad pointer behavior | Stock `miui-services-pointer-pad.jar` and `miui-framework-pointer-pad.jar` declare Magic Pointer classes; current tree uses Android input plus keyboard controls | Xiaomi Magic Pointer behavior is not ported; assess framework hooks rather than copying JARs into system_server |
| Camera HAL | Stock algorithms/configs, provider and tinyxml2 ABI fixups present | Photo/video modes, slow motion, HDR, front/rear combinations and enforcing tests |
| Xiaomi Camera UI | Optional Global 303 tablet app | Portrait/landscape rotation still broken; temporary orientation override is insufficient |
| Concurrent cameras | Feature XML provided by common.mk | Declaration alone does not prove simultaneous-camera support |
| USB cameras | Source external camera provider and stock configuration | Test an actual UVC device |
| Video enhancement | Stock videoservice omitted; tree README records missing Android 16 libgui ABI symbol | Investigate a compatible shim/service or port; not imported |
| Audio/Dolby/volume boost | Stock audio stack, DolbyManager, PianoVolumeBoost and 15-step media curve | Screen-off playback, routes, microphone, boost warning and enforcing regression tests |
| Wi-Fi/SoftAP | Common WifiOverlayCommon plus source Wi-Fi services | Vendor target overlays match current explicit values or AOSP defaults; remaining product-overlay and vendor-extension review described below |
| Bluetooth | Source service and selected stock dependencies | Keyboard/stylus/audio reconnection and profiles require current-build tests |
| Fingerprint | Goodix stack and Xiaomi-compatible source HAL | Current-build lockscreen/enforcing tests |
| Face unlock | Stock `miface` declaration/service is absent from the selected tree | Stock startup requires `vendor.mitee_vm.boot_completed=1`; separate MiTEE/framework integration is needed before claiming this can work |
| Secure stack/DRM | KeyMint/Gatekeeper/Weaver and DRM stack selected | Enforcing boot, lockscreen keys and actual protected playback levels remain unproven |
| Smart cover | Android lid-sleep overlay and stock hall/input support | Verify sleep/wake with each cover/keyboard posture |
| Body-color wallpaper | Stock images and body-color init selection | The three wallpaper inputs are Global 303, now explicitly recorded and pinned |
| Desktop windowing | Framework enables internal-display desktops | Android desktop support is not proof of all HyperOS workstation UX or app adaptations |
| Xiaomi inter-device features | Global 303 includes MirrorOS3Global, MiLinkOS3Global, MiPCExpend, MIShareGlobal and related framework code | No complete equivalent integration established; inspect APIs/authentication/framework dependencies before proposing ports |

## Wi-Fi overlay comparison

The four Global 304 vendor Wi-Fi target APKs were dumped with the builder's
`aapt2 dump resources`. They contain seven unique boolean resources:

| Resource | Stock | Current tree/effective source default |
| --- | --- | --- |
| config_wifiBridgedSoftApSupported | false | AOSP Wi-Fi default false |
| config_wifiMultiStaLocalOnlyConcurrencyEnabled | false | AOSP default false |
| config_wifiMultiStaMultiInternetConcurrencyEnabled | false | Common overlay false |
| config_wifiMultiStaNetworkSwitchingMakeBeforeBreakEnabled | false | AOSP default false |
| config_wifiMultiStaRestrictedConcurrencyEnabled | false | AOSP default false |
| config_wifiSoftapIeee80211beSupported | false | AOSP default false |
| config_wifiUseHalApiToDisableFwRoaming | true | Common overlay true |

These particular APKs do not need copying to reproduce their values. The
product Wi-Fi overlays and Xiaomi `p2p_165chan` framework extension are separate
items; this comparison does not establish their complete coverage.

## Why unselected files are not automatically missing features

Stock boot, health, sensors, thermal, USB, Wi-Fi, supplicant, hostapd, memtrack
and power services have source implementations listed in common.mk. Stock
feature permission XMLs can also be replaced by frameworks/native XMLs.
Installing both implementations can create duplicate modules, services or
VINTF declarations.

Other stock files are factory/CIT diagnostics, phone/modem scaffolding,
alternate SKUs, telemetry, stock framework libraries or incompatible Xiaomi
services. Their existence in a shared stock image is not evidence that the
tablet exposes the corresponding hardware. Review the complete inventory
against service startup, consumers, dependencies and runtime behavior before
importing anything.

The old `blob-audit.json` is explicitly a China 307, 360-blob bring-up audit.
It is not current Global 304 closure evidence and must not be used to claim
that the current 3,046 selected stock vendor/ODM files are fully covered.

The generated output also exposes candidates for follow-up: stock
`dynamiccameraserver`, `misensor_camera`, `qvrservice`, and the external camera
provider executable are absent at their stock paths. The local source adds
the external provider, while the built output does not yet contain it. Some
camera VINTF/init files exist without the corresponding stock executable at
that path. Resolve whether those services moved, were folded into another
provider, or are genuinely missing before adding another daemon.

Two of those are concrete incomplete service imports, rather than an XML-only
feature guess:

- `misensor_camera.rc` starts `/odm/bin/hw/misensor_camera` when
  `vendor.native_camera.control=1`, and declares `vendor.xiaomi.sensor.camera.IMiCam`.
  The RC and VINTF XML are selected, but the executable is not. Stock ELF
  dependencies include `libcamera2ndk_vendor`, `libmediandk`, `libui` and
  `vendor.xiaomi.sensor.camera-V1-ndk`. Determine the stock framework caller
  and required sensor/privacy behavior before importing it.
- `vendor.xiaomi.hardware.dynamiccameraserver.xml` declares
  `vendor.xiaomi.hardware.camera.synthetic.IVirtualCameraRegistrar/default`.
  The XML is selected, while its stock executable is not. That executable
  depends on Xiaomi companion/injection/synthetic interfaces and implementations.
  Virtual-camera injection/registration is therefore a port candidate; the
  ordinary Camera HAL working does not prove this extension works.

Stock `qvrservice` additionally has its own QVR daemon, sockets and library
dependencies. The common tree includes some QVR client/test libraries, but
that is not evidence that the daemon's use cases are implemented. Establish
an actual piano consumer before enabling it.

## Reproducibility fixes made during this audit

The nine keyboard/touchpad firmware entries were unpinned. Their repository
bytes match the Global 304 dump byte-for-byte; SHA1 pins now record those
verified inputs. Three unpinned body-color wallpaper images match the Global
303 product image exactly; pins and a provenance comment now record that.
The extraction error now says "pinned stock input", avoiding an incorrect
Global 304 claim for those wallpaper files.

All inputs in the two current proprietary file lists now have hash pins.
Re-extraction still requires a staged dump containing every declared partition
and source path; vendor/ODM alone cannot satisfy the product wallpaper inputs.

## Next audit gates

1. Classify the remaining unselected paths by actual source replacement,
   non-device SKU/diagnostic component, incompatible stock dependency or useful
   feature candidate. Filename references alone are insufficient.
2. Compare Global 303 product/system_ext overlays and framework consumers;
   obtain Global 304 equivalents before claiming exact 304 framework parity.
3. Trace pencil buttons/gestures, Magic Pointer, face/MiTEE, video enhancement
   and cross-device services to determine which can be ported with concrete
   dependencies and tests.
4. Validate implemented controls on a build containing the current commits,
   including enforcing mode. No "all stock features imported" claim is valid
   until those gaps are resolved or explicitly explained.

Regenerate the file inventory from the workspace root:

```sh
rtk proxy python3 device/xiaomi/piano/tools/audit-stock-features.py \
  --workspace . \
  --stock RND/lineage-rnd/prepared-global-304-full/dump \
  --built-files RND/evidence/stock-feature-audit-20261002/build-installed \
  --output device/xiaomi/piano/docs/stock-audit
```
