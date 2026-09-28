# China 307 stock tuple, produced by prepare-stock.py.
PIANO_KERNEL_PATH := device/xiaomi/piano-kernel
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
