LOCAL_PATH := device/xiaomi/sm8750-common


PRODUCT_USE_DYNAMIC_PARTITIONS := true

# Android 13+ init_boot layout: snapuserd is in the generic ramdisk.
$(call inherit-product, $(SRC_TARGET_DIR)/product/virtual_ab_ota/vabc_features.mk)
PRODUCT_VIRTUAL_AB_COMPRESSION_METHOD := lz4

$(call inherit-product, vendor/xiaomi/sm8750-common/sm8750-common-vendor.mk)

PRODUCT_PACKAGES += \
    android.hardware.boot-service.qti \
    android.hardware.boot-service.qti.recovery \
    android.hardware.health-service.qti \
    android.hardware.health-service.qti_recovery \
    android.hardware.usb-service.qti \
    android.hardware.usb.gadget-service.qti \
    android.hardware.power-service-qti \
    android.hardware.fastboot-service.example_recovery \
    fastbootd \
    init.qcom.usb.rc \
    init.qcom.usb.sh \
    usb_compositions.conf \
    sh_vendor \
    toybox_vendor \
    toolbox_vendor
PRODUCT_SOONG_NAMESPACES += \
    hardware/qcom-caf/bootctrl \
    vendor/qcom/opensource/usb/etc
