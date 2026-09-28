LOCAL_PATH := device/xiaomi/piano

$(call inherit-product, device/xiaomi/sm8750-common/common.mk)

PRODUCT_SHIPPING_API_LEVEL := 36

PRODUCT_CHARACTERISTICS := tablet

# Offline extraction must generate both vendor trees before product evaluation.
$(call inherit-product, vendor/xiaomi/piano/piano-vendor.mk)
$(call inherit-product, device/xiaomi/piano/kernel.mk)
PRODUCT_SOONG_NAMESPACES += device/xiaomi/piano device/xiaomi/sm8750-common

PRODUCT_COPY_FILES += \
    device/xiaomi/piano/rootdir/etc/fstab.qcom:$(TARGET_COPY_OUT_VENDOR)/etc/fstab.qcom \
    device/xiaomi/piano/rootdir/etc/fstab.qcom:$(TARGET_COPY_OUT_VENDOR_RAMDISK)/first_stage_ramdisk/fstab.qcom

PRODUCT_COPY_FILES += \
    device/xiaomi/piano/rootdir/etc/init.qcom.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/hw/init.qcom.rc \
    device/xiaomi/piano/rootdir/etc/qseecomd.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/qseecomd.rc \
    device/xiaomi/piano/rootdir/etc/ueventd.piano.rc:$(TARGET_COPY_OUT_VENDOR)/etc/ueventd.rc \
    device/xiaomi/piano/rootdir/bin/piano-persist-check.sh:$(TARGET_COPY_OUT_VENDOR)/bin/piano-persist-check.sh

# China 307 vendor properties required by the selected providers.
PRODUCT_VENDOR_PROPERTIES += \
    ro.hardware.egl=adreno \
    vendor.gralloc.enable_snapalloc=1 \
    vendor.gatekeeper.disable_spu=true \
    vendor.gatekeeper.is_security_level_spu=0

PRODUCT_COPY_FILES += \
    device/xiaomi/piano/rootdir/etc/vendor.xiaomi.hw.touchfeature-service.rc:$(TARGET_COPY_OUT_ODM)/etc/init/vendor.xiaomi.hw.touchfeature-service.rc

# Minimal tablet values traced to the pinned stock framework overlay.
DEVICE_PACKAGE_OVERLAYS += $(LOCAL_PATH)/overlay

# China 307 product/etc/build.prop.
PRODUCT_PRODUCT_PROPERTIES += ro.sf.lcd_density=440

# One owner for the composer manifest; Xiaomi extension is in the base manifest.
PRODUCT_PACKAGES += vendor.qti.hardware.display.composer-service.xml
