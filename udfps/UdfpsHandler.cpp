/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

#define LOG_TAG "UdfpsHandler.xiaomi_sm8750"

#include <aidl/android/hardware/biometrics/fingerprint/BnFingerprint.h>
#include <android-base/logging.h>
#include <android-base/unique_fd.h>

#include <dlfcn.h>

#include <fstream>
#include <sstream>

#include "UdfpsHandler.h"

#define COMMAND_FOD_PRESS_STATUS 1
#define COMMAND_FOD_PRESS_X 2
#define COMMAND_FOD_PRESS_Y 3
#define PARAM_FOD_PRESSED 1
#define PARAM_FOD_RELEASED 0
namespace {

template <typename T>
static void set(const std::string& path, const T& value) {
    std::ofstream file(path);
    file << value;
}

static void traceResolver(const std::string& message) {
    std::ofstream trace("/data/local/tmp/udfps-resolver.log", std::ios::app);
    trace << message << '\n';
}

}  // anonymous namespace

class SM8750UdfpsHandler : public UdfpsHandler {
  public:
    void init(fingerprint_device_t* device) {
        mDevice = device;
        initQfpNativePointerApi();
        LOG(INFO) << __func__;
    }

    void onFingerDown(uint32_t x, uint32_t y, float minor, float major) {
        LOG(DEBUG) << __func__ << " x: " << x << ", y: " << y << ", minor: " << minor
                   << ", major: " << major;
        if (!mDevice) {
            LOG(ERROR) << __func__ << ": fingerprint HAL device is unavailable";
            return;
        }

        // QFP's native pointer API consumes the touch axes and derives a contact
        // area from them. The Xiaomi extCmd compatibility path only forwards X/Y
        // and calls QFP with minor=major=0, which makes ultrasonic enrollment
        // captures fail with vendor acquired 28 (QFP enroll result -100).
        refreshQfpServiceWrapper();
        if (mQfpServiceWrapper && mQfpOnPointerDown) {
            mQfpOnPointerDown(mQfpServiceWrapper, 0, x, y, minor, major);
            return;
        }

        if (mDevice->onPointerDown) {
            mDevice->onPointerDown(mDevice, 0, x, y, minor, major);
            return;
        }

        if (!mDevice->extCmd) {
            LOG(ERROR) << __func__ << ": fingerprint HAL pointer APIs are unavailable";
            return;
        }
        mDevice->extCmd(mDevice, COMMAND_FOD_PRESS_X, x);
        mDevice->extCmd(mDevice, COMMAND_FOD_PRESS_Y, y);
        mDevice->extCmd(mDevice, COMMAND_FOD_PRESS_STATUS, PARAM_FOD_PRESSED);
    }

    void onFingerUp() {
        LOG(DEBUG) << __func__;
        if (!mDevice) {
            LOG(ERROR) << __func__ << ": fingerprint HAL device is unavailable";
            return;
        }

        refreshQfpServiceWrapper();
        if (mQfpServiceWrapper && mQfpOnPointerUp) {
            mQfpOnPointerUp(mQfpServiceWrapper, 0);
            return;
        }

        if (mDevice->onPointerUp) {
            mDevice->onPointerUp(mDevice, 0);
            return;
        }

        if (!mDevice->extCmd) {
            LOG(ERROR) << __func__ << ": fingerprint HAL pointer APIs are unavailable";
            return;
        }
        mDevice->extCmd(mDevice, COMMAND_FOD_PRESS_X, 0);
        mDevice->extCmd(mDevice, COMMAND_FOD_PRESS_Y, 0);
        mDevice->extCmd(mDevice, COMMAND_FOD_PRESS_STATUS, PARAM_FOD_RELEASED);
    }

    void onAcquired(int32_t result, int32_t vendorCode) {
        LOG(DEBUG) << __func__ << " result: " << result << " vendorCode: " << vendorCode;
        // QFP sends vendor acquired messages (notably 28, 201 and 202) while its
        // enroll/authenticate operation is still active. Calling extCmd(FINGER_UP)
        // synchronously from this callback re-enters the lower HAL and makes QFP
        // cancel an otherwise successful enrollment capture. Only forward a real
        // pointer-up from the framework, or release the sensor while cancelling.
    }

    void cancel() {
        LOG(INFO) << __func__;
        onFingerUp();
    }

  private:
    using QfpOnPointerDown = int (*)(void*, int32_t, int32_t, int32_t, float, float);
    using QfpOnPointerUp = int (*)(void*, int32_t);

    void initQfpNativePointerApi() {
        traceResolver("initQfpNativePointerApi: enter");
        if (!mDevice || !mDevice->extCmd) {
            traceResolver("initQfpNativePointerApi: missing device/extCmd");
            return;
        }

        Dl_info extCmdInfo = {};
        if (!dladdr(reinterpret_cast<void*>(mDevice->extCmd), &extCmdInfo) ||
            !extCmdInfo.dli_fbase || !extCmdInfo.dli_fname) {
            LOG(WARNING) << __func__ << ": cannot identify the loaded fingerprint HAL";
            traceResolver("initQfpNativePointerApi: dladdr failed");
            return;
        }

        traceResolver(std::string("initQfpNativePointerApi: HAL=") + extCmdInfo.dli_fname);

        void* handle = dlopen(extCmdInfo.dli_fname, RTLD_NOW | RTLD_NOLOAD);
        if (!handle) {
            LOG(WARNING) << __func__ << ": cannot open " << extCmdInfo.dli_fname;
            traceResolver(std::string("initQfpNativePointerApi: dlopen failed: ") + dlerror());
            return;
        }

        auto onPointerDown = reinterpret_cast<QfpOnPointerDown>(
                dlsym(handle, "_ZN17QfpServiceWrapper13onPointerDownEiiiff"));
        auto onPointerUp = reinterpret_cast<QfpOnPointerUp>(
                dlsym(handle, "_ZN17QfpServiceWrapper11onPointerUpEi"));

        const auto base = reinterpret_cast<uintptr_t>(extCmdInfo.dli_fbase);
        const auto extCmdOffset = reinterpret_cast<uintptr_t>(mDevice->extCmd) - base;
        const auto downOffset = reinterpret_cast<uintptr_t>(onPointerDown) - base;
        const auto upOffset = reinterpret_cast<uintptr_t>(onPointerUp) - base;

        std::ostringstream offsets;
        offsets << "initQfpNativePointerApi: offsets ext=0x" << std::hex << extCmdOffset
                << " down=0x" << downOffset << " up=0x" << upOffset;
        traceResolver(offsets.str());

        // fingerprint.qcom_us.default from haotian OS3.0.302.0 keeps the
        // QfpServiceWrapper shared_ptr in a file-local global. Its legacy HMI
        // deliberately leaves the native pointer callbacks empty and extCmd
        // consequently supplies zero touch axes. Only use the private slot after
        // validating all exported entry points against this exact blob layout.
        constexpr uintptr_t kExtCmdOffset = 0xdd74;
        constexpr uintptr_t kOnPointerDownOffset = 0x123c0;
        constexpr uintptr_t kOnPointerUpOffset = 0x124e8;
        constexpr uintptr_t kQfpServiceWrapperSlotOffset = 0x1c2e0;
        if (extCmdOffset != kExtCmdOffset || downOffset != kOnPointerDownOffset ||
            upOffset != kOnPointerUpOffset) {
            LOG(WARNING) << __func__ << ": unsupported fingerprint HAL layout; using extCmd"
                         << " extCmd=0x" << std::hex << extCmdOffset << " down=0x" << downOffset
                         << " up=0x" << upOffset;
            dlclose(handle);
            traceResolver("initQfpNativePointerApi: layout rejected");
            return;
        }

        mQfpServiceWrapperSlot = reinterpret_cast<void**>(base + kQfpServiceWrapperSlotOffset);
        mQfpOnPointerDown = onPointerDown;
        mQfpOnPointerUp = onPointerUp;
        mQfpHalHandle = handle;
        refreshQfpServiceWrapper();
        if (!mQfpServiceWrapper) {
            // The lower HAL constructs QfpServiceWrapper in set_active_group(),
            // after this handler's init() has already run. Keep the validated
            // entry points and resolve the object lazily on the first touch.
            LOG(INFO) << __func__ << ": QfpServiceWrapper resolution deferred";
            traceResolver("initQfpNativePointerApi: wrapper resolution deferred");
            return;
        }
        LOG(INFO) << __func__ << ": enabled QFP native pointer API";
        traceResolver("initQfpNativePointerApi: enabled");
    }

    void refreshQfpServiceWrapper() {
        if (!mQfpServiceWrapper && mQfpServiceWrapperSlot && *mQfpServiceWrapperSlot) {
            mQfpServiceWrapper = *mQfpServiceWrapperSlot;
            traceResolver("refreshQfpServiceWrapper: enabled");
        }
    }

    fingerprint_device_t* mDevice = nullptr;
    void* mQfpHalHandle = nullptr;
    void** mQfpServiceWrapperSlot = nullptr;
    void* mQfpServiceWrapper = nullptr;
    QfpOnPointerDown mQfpOnPointerDown = nullptr;
    QfpOnPointerUp mQfpOnPointerUp = nullptr;
};

static UdfpsHandler* create() {
    return new SM8750UdfpsHandler();
}

static void destroy(UdfpsHandler* handler) {
    delete handler;
}

extern "C" UdfpsHandlerFactory UDFPS_HANDLER_FACTORY = {
        .create = create,
        .destroy = destroy,
};
