#
# SPDX-License-Identifier: Apache-2.0
#

PRODUCT_SOONG_NAMESPACES += \
    vendor/xiaomi/piano-miuicamera

PRODUCT_PACKAGES += \
    MiuiCamera

PRODUCT_SYSTEM_PROPERTIES += \
    ro.miui.notch=0 \
    ro.product.mod_device=piano_global

# Let the camera HAL expose its private and logical cameras to MiuiCamera.
PRODUCT_VENDOR_PROPERTIES += \
    persist.vendor.camera.privapp.list=com.android.camera,org.lineageos.aperture \
    vendor.camera.aux.packagelist=com.android.camera,org.lineageos.aperture
