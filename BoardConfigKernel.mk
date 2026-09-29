# China 307 stock tuple, produced by prepare-stock.py.
PIANO_KERNEL_PATH := device/xiaomi/piano-kernel
# Lineage's generated_kernel_includes still runs headers_install with a
# prebuilt boot kernel. Keep Xiaomi's piano OSS source available there.
TARGET_KERNEL_SOURCE := kernel/xiaomi/piano
# The source is used only for UAPI headers. Keep the stock kernel image;
# Lineage's kernel.mk requires a config when the source directory exists.
TARGET_KERNEL_CONFIG := gki_defconfig
TARGET_FORCE_PREBUILT_KERNEL := true
# The default LLVM_AOSP_PREBUILTS_VERSION is empty in this checkout;
# headers_install still compiles host tools and needs a real clang path.
TARGET_KERNEL_CLANG_VERSION := r574158
# Required by Lineage version comparisons even when using a prebuilt.
# Pinned stock release: 6.6.77-android15-8-gf9a1d4bd8353.
TARGET_KERNEL_VERSION := 6.6
TARGET_PREBUILT_KERNEL := $(PIANO_KERNEL_PATH)/kernel
BOARD_KERNEL_IMAGE_NAME := Image
BOARD_KERNEL_BASE := 0x00000000
BOARD_KERNEL_CMDLINE := video=vfb:640x400,bpp=32,memsize=3072000 erofs.reserved_pages=64
BOARD_MKBOOTIMG_ARGS += --dtb_offset 0x01f00000
BOARD_INCLUDE_DTB_IN_BOOTIMG := true
BOARD_PREBUILT_DTBIMAGE_DIR := $(PIANO_KERNEL_PATH)/dtb
BOARD_PREBUILT_DTBOIMAGE := $(PIANO_KERNEL_PATH)/dtbo.img
BOARD_BOOTCONFIG := androidboot.hardware=qcom androidboot.memcg=1 \
    androidboot.usbcontroller=a600000.dwc3 androidboot.load_modules_parallel=true \
    androidboot.hypervisor.protected_vm.supported=true androidboot.vendor.qspa=true

# Preserve the stock release directory, flattened layout and module metadata.
BOARD_SYSTEM_DLKM_SRC := $(PIANO_KERNEL_PATH)/system_dlkm

# Keep the stock .ko files and their modules.* metadata byte-for-byte.
# They are copied into vendor_ramdisk/vendor_dlkm rather than relinked.
BUILD_BROKEN_ELF_PREBUILT_PRODUCT_COPY_FILES := true
