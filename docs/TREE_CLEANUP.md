# Tree cleanup — 2026-10-02

Removed four unselected bring-up copies from piano rootdir/etc:

- init.qcom.rc: installed module is sm8750-common/rootdir/etc/init.qcom.rc.
- qseecomd.rc: extracted common vendor RC is selected by sm8750-common-vendor.mk; its pinned fixup starts QSEE after persist mounts.
- ueventd.piano.rc: installed module is sm8750-common/rootdir/etc/ueventd.qcom.rc.
- vendor.xiaomi.hw.touchfeature-service.rc: common vendor COPY_FILES installs its pinned, fixed stock RC.

None is referenced by a makefile, Blueprint, extraction script, or installed init import in the current five trees. The removed copies remain recoverable in Git history. No vendor blob or selected module was removed.

CAF integration currently uses hardware/qcom-caf/common, CAF boot control, power, health, USB, and related source modules. Primary audio and display still select stock prebuilts with source patches to avoid duplicate RC/VINTF ownership. A full source HAL migration requires compiling and testing those providers; the existing mix does not prove that migration complete.
