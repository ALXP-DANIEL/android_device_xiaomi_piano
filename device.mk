LOCAL_PATH := device/xiaomi/piano

$(call inherit-product, device/xiaomi/sm8750-common/common.mk)

PRODUCT_SHIPPING_API_LEVEL := 36

# The stock kernel uses 4 KB pages (6.6.118-...-4k); prebuilt compat libraries
# such as libtinyxml2-v34 are 4 KB aligned, so skip the 16 KB ELF check.
PRODUCT_CHECK_PREBUILT_MAX_PAGE_SIZE := false

PRODUCT_CHARACTERISTICS := tablet

# Offline extraction must generate both vendor trees before product evaluation.
$(call inherit-product, vendor/xiaomi/piano/piano-vendor.mk)
$(call inherit-product, device/xiaomi/piano/kernel.mk)
PRODUCT_SOONG_NAMESPACES += device/xiaomi/piano device/xiaomi/sm8750-common

PRODUCT_COPY_FILES += \
    device/xiaomi/piano/rootdir/etc/fstab.qcom:$(TARGET_COPY_OUT_VENDOR_RAMDISK)/first_stage_ramdisk/fstab.qcom

PRODUCT_COPY_FILES += \
    device/xiaomi/piano/rootdir/bin/piano-persist-check.sh:$(TARGET_COPY_OUT_VENDOR)/bin/piano-persist-check.sh

# Bring-up boot log catcher (userdebug only).
ifneq ($(TARGET_BUILD_VARIANT),user)
PRODUCT_COPY_FILES += \
    device/xiaomi/piano/rootdir/debug/piano-bootlog.sh:$(TARGET_COPY_OUT_SYSTEM_EXT)/bin/piano-bootlog.sh \
    device/xiaomi/piano/rootdir/debug/piano-bootlog.rc:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/init/piano-bootlog.rc
endif

# Bring-up: authorize a developer adb key, so a boot that hangs before the
# setup wizard can still be debugged. PIANO_ADB_KEYS is a source-relative path
# to a public key kept outside every repository; leave it unset for releases.
ifneq ($(TARGET_BUILD_VARIANT),user)
ifneq ($(strip $(PIANO_ADB_KEYS)),)
PRODUCT_ADB_KEYS := $(PIANO_ADB_KEYS)
endif
endif

# Global 304 vendor properties required by the selected providers.
PRODUCT_VENDOR_PROPERTIES += \
    ro.hardware.egl=adreno \
    vendor.gralloc.enable_snapalloc=1 \
    vendor.gatekeeper.disable_spu=true \
    vendor.gatekeeper.is_security_level_spu=0

# Global 304 vendor/build.prop: the panel is mounted rotated; the NVT touch
# grid is portrait.
PRODUCT_VENDOR_PROPERTIES += \
    debug.sf.ignore_hwc_physical_display_orientation=true \
    ro.surface_flinger.primary_display_orientation=ORIENTATION_270

# Stock touchfeature init is installed by the adapted common vendor list.

# Minimal tablet values traced to the pinned stock framework overlay.
DEVICE_PACKAGE_OVERLAYS += $(LOCAL_PATH)/overlay

# Global 304 product/etc/build.prop.
PRODUCT_PRODUCT_PROPERTIES += ro.sf.lcd_density=440

# Composer VINTF (including the Xiaomi IMiHwcExtension) is installed by the
# common vendor list; the device manifest must not declare it again.

# Shims (hardware/lineage/compat)
PRODUCT_PACKAGES += \
    libprocessgroup_shim

# Core features (app widgets, device admin, home screen, input methods, ...)
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/tablet_core_hardware.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/tablet_core_hardware.xml
