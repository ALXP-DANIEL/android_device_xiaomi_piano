# Installed build 1790940907: focused verification

Checked on piano on 2026-10-02. This is partial verification, not release approval.

- Data-preserving OrangeFox sideload reported successful post-install and kSuccess.
- Boot completed on slot B with SELinux enforcing. Snapshot update state returned to none.
- QSEE, KeyMint, Weaver, DisplayFeature, mi_thermald and PianoParts processes run.
- Wi-Fi connected using 802.11ax/WPA3; Bluetooth was ON with no recorded crashes in this boot.
- Sensor service enumerates 55 sensors. Xiaomi Camera connected to rear camera 0.
- Body-colour property BU selects the existing wallpaper_BU.jpg. Wallpaper behavior confirmed.
- Wallpaper accents use the wallpaper-derived blue seed with TONAL_SPOT; softer accent saturation is palette behavior.
- PianoParts has TABLET_MODE, WRITE_SECURE_SETTINGS and MANAGE_NOTIFICATIONS grants.
- Charging limit node exists and reads 1. A 100% battery does not prove charging-limit or bypass thresholds.
- No visible AVCs during collection; broad dontaudit rules prevent treating this as complete SELinux proof.

Still required: front/rear capture and video, audio, rotation, keyboard/pen behavior,
charging thresholds, recovery PIN decryption, and suppressed-denial review.
Historical tombstones are not current-boot failures. Aurora Store's duplicate
Compose list-key exception is an app issue and does not justify a ROM change.

## Extraction verification

Stock vendor/odm inventory: 4,726 files, 3,046 selected and present, none selected
and missing. All extraction inputs are pinned. Fifty-five common blobs previously
lacked their patched output hash; every one reproduced byte-for-byte from its
pinned stock input using the existing extraction fixups. Those output hashes are
now recorded. Blob bytes and runtime behavior were unchanged by this metadata fix.

## Recovery diagnosis limits

A temporary normal-Android snapshotctl required gsiservice, and starting a
matching temporary gsid released its cancel operation. Recovery update_engine
uses libsnapshot_nobinder and the ImageManager passthrough path, so that observation
alone does not justify shipping gsid in recovery. Busy mounted stock logical
partitions remain a separate installation concern. Do not cancel a healthy OTA
or format data merely to reproduce this diagnostic.
