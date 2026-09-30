/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include "V2_1/SubHal.h"

#include <android/binder_auto_utils.h>
#include <android/binder_ibinder.h>
#include <android/binder_manager.h>
#include <android/binder_parcel.h>
#include <android/binder_status.h>
#include <cutils/properties.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <linux/input.h>
#include <log/log.h>
#include <poll.h>
#include <unistd.h>
#include <utils/SystemClock.h>

#include <cmath>
#include <cstdio>
#include <cstring>
#include <mutex>
#include <string>
#include <thread>
#include <utility>
#include <vector>

namespace android {
namespace hardware {
namespace sensors {
namespace V2_1 {
namespace subhal {
namespace implementation {

using ::android::hardware::Return;
using ::android::hardware::Void;
using ::android::hardware::sensors::V1_0::OperationMode;
using ::android::hardware::sensors::V1_0::RateLevel;
using ::android::hardware::sensors::V1_0::Result;
using ::android::hardware::sensors::V1_0::SensorFlagBits;
using ::android::hardware::sensors::V1_0::SharedMemInfo;
using ::android::hardware::sensors::V2_1::Event;
using ::android::hardware::sensors::V2_1::SensorInfo;
using ::android::hardware::sensors::V2_1::SensorType;
using ::android::hardware::sensors::V2_1::implementation::IHalProxyCallback;
using ::android::hardware::sensors::V2_1::implementation::ISensorsSubHal;

namespace {

constexpr int32_t kSensorHandle = 1;
constexpr int32_t kPrimaryTouchId = 0;
constexpr int32_t kTouchModeFodEnable = 10;
constexpr int32_t kGetModeValueTransaction = FIRST_CALL_TRANSACTION;
constexpr int32_t kSetModeValueTransaction = FIRST_CALL_TRANSACTION + 8;
constexpr int32_t kTouchModeWatchdogMs = 2000;
constexpr int32_t kUdfpsBoundsMargin = 24;
constexpr float kSyntheticUdfpsMajorMinor = 20.0f;
constexpr char kInputDeviceName[] = "Xiaomi_Touch_Input";
constexpr char kInputDir[] = "/dev/input";
constexpr char kTouchFeatureService[] = "vendor.xiaomi.hw.touchfeature.ITouchFeature/default";
constexpr char kTouchFeatureDescriptor[] = "vendor.xiaomi.hw.touchfeature.ITouchFeature";

struct UdfpsBounds {
    int32_t x;
    int32_t y;
    int32_t radius;
};

bool loadUdfpsBounds(UdfpsBounds* bounds) {
    char location[PROPERTY_VALUE_MAX] = {};
    property_get("persist.vendor.fingerprint.sensor_location", location, "");

    int32_t x = 0;
    int32_t y = 0;
    int32_t radius = 0;
    if (std::sscanf(location, "%d|%d|%d", &x, &y, &radius) != 3 ||
        x <= 0 || y <= 0 || radius <= 0) {
        ALOGW("Invalid persist.vendor.fingerprint.sensor_location='%s'", location);
        return false;
    }

    *bounds = {x, y, radius};
    ALOGI("Using UDFPS bounds from persist.vendor.fingerprint.sensor_location: %s", location);
    return true;
}

binder_status_t touchFeatureOnTransact(AIBinder*, transaction_code_t, const AParcel*, AParcel*) {
    return STATUS_UNKNOWN_TRANSACTION;
}

AIBinder_Class* getTouchFeatureClass() {
    static AIBinder_Class* clazz = AIBinder_Class_define(
            kTouchFeatureDescriptor,
            [](void*) -> void* { return nullptr; },
            [](void*) {},
            touchFeatureOnTransact);
    return clazz;
}

}  // namespace

class UdfpsInputSubHal : public ISensorsSubHal {
  public:
    UdfpsInputSubHal();
    ~UdfpsInputSubHal() override;

    Return<void> getSensorsList_2_1(ISensors::getSensorsList_2_1_cb _hidl_cb) override;
    Return<Result> initialize(const sp<IHalProxyCallback>& halProxyCallback) override;
    Return<Result> setOperationMode(OperationMode mode) override;
    Return<Result> activate(int32_t sensorHandle, bool enabled) override;
    Return<Result> batch(int32_t sensorHandle, int64_t samplingPeriodNs,
                         int64_t maxReportLatencyNs) override;
    Return<Result> flush(int32_t sensorHandle) override;
    Return<Result> injectSensorData_2_1(const Event& event) override;
    Return<void> registerDirectChannel(const SharedMemInfo& mem,
                                       ISensors::registerDirectChannel_cb _hidl_cb) override;
    Return<Result> unregisterDirectChannel(int32_t channelHandle) override;
    Return<void> configDirectReport(int32_t sensorHandle, int32_t channelHandle, RateLevel rate,
                                    ISensors::configDirectReport_cb _hidl_cb) override;
    Return<void> debug(const hidl_handle& fd, const hidl_vec<hidl_string>& args) override;

    const std::string getName() override { return "XiaomiSm8750UdfpsInputSubHal"; }

  private:
    bool getTouchFeatureMode(int32_t mode, int32_t* value);
    bool setTouchFeatureMode(int32_t mode, int32_t value);
    bool setUdfpsTouchMode(bool enabled);
    bool ensureTouchFeatureBinderLocked();
    bool openInputDevice();
    std::string findTouchInputDevice();
    bool configureInputScale();
    bool isInUdfpsBounds(float x, float y) const;
    void loop();
    void interruptPoll();
    void retryTouchModeStateIfNeeded();
    void handleInputEvent(const input_event& inputEvent);
    void postUdfpsEvent(float x, float y);

    SensorInfo mSensorInfo = {};
    UdfpsBounds mBounds = {};
    bool mAvailable = false;
    bool mActive = false;
    bool mTriggered = false;
    bool mTouchModeEnabled = false;
    bool mTouchModeDisablePending = false;
    bool mTouchDown = false;
    bool mHaveX = false;
    bool mHaveY = false;
    int32_t mRawX = 0;
    int32_t mRawY = 0;
    float mScaleX = 1.0f;
    float mScaleY = 1.0f;
    int mInputFd = -1;
    int mWaitPipeFd[2] = {-1, -1};
    OperationMode mCurrentOperationMode = OperationMode::NORMAL;
    sp<IHalProxyCallback> mCallback;
    std::mutex mLock;
    std::thread mThread;
    ndk::SpAIBinder mTouchFeatureBinder;
};

UdfpsInputSubHal::UdfpsInputSubHal() {
    mSensorInfo.sensorHandle = kSensorHandle;
    mSensorInfo.name = "UDFPS Sensor";
    mSensorInfo.vendor = "Xiaomi";
    mSensorInfo.version = 1;
    // Xiaomi's FodSensorMonitor asks sensorservice for exactly
    // SensorType::DEVICE_PRIVATE_BASE.  Registering this bridge at a private
    // offset leaves it visible in dumpsys, but the stock fingerprint daemon
    // never activates it and touchfeature modes 10/16 remain disabled.
    mSensorInfo.type = SensorType::DEVICE_PRIVATE_BASE;
    // Keep the Lineage sensor string used by SystemUI's doze UDFPS trigger,
    // while the numeric type remains Xiaomi's DEVICE_PRIVATE_BASE for the
    // stock FodSensorMonitor. Both consumers must resolve the same bridge.
    mSensorInfo.typeAsString = "org.lineageos.sensor.udfps";
    mSensorInfo.maxRange = 2048.0f;
    mSensorInfo.resolution = 1.0f;
    mSensorInfo.power = 0.001f;
    mSensorInfo.minDelay = -1;
    mSensorInfo.maxDelay = 0;
    mSensorInfo.fifoReservedEventCount = 0;
    mSensorInfo.fifoMaxEventCount = 0;
    mSensorInfo.requiredPermission = "";
    mSensorInfo.flags = static_cast<uint32_t>(SensorFlagBits::ONE_SHOT_MODE) |
                        static_cast<uint32_t>(SensorFlagBits::WAKE_UP);

    if (pipe2(mWaitPipeFd, O_CLOEXEC | O_NONBLOCK) != 0) {
        ALOGE("Failed to create wait pipe: %s", strerror(errno));
        return;
    }

    mAvailable = loadUdfpsBounds(&mBounds) && openInputDevice();
    if (!mAvailable) {
        ALOGW("UDFPS input sensor is unavailable");
    }

    mThread = std::thread(&UdfpsInputSubHal::loop, this);
}

UdfpsInputSubHal::~UdfpsInputSubHal() {
    bool shouldDisableTouchMode = false;
    {
        std::lock_guard<std::mutex> lock(mLock);
        shouldDisableTouchMode = mTouchModeEnabled || mTouchModeDisablePending;
        mActive = false;
        mAvailable = false;
        mTouchModeEnabled = false;
        mTouchModeDisablePending = false;
    }
    if (shouldDisableTouchMode) {
        setUdfpsTouchMode(false);
    }
    interruptPoll();
    if (mThread.joinable()) {
        mThread.join();
    }
    if (mInputFd >= 0) {
        close(mInputFd);
    }
    if (mWaitPipeFd[0] >= 0) {
        close(mWaitPipeFd[0]);
    }
    if (mWaitPipeFd[1] >= 0) {
        close(mWaitPipeFd[1]);
    }
}

Return<void> UdfpsInputSubHal::getSensorsList_2_1(ISensors::getSensorsList_2_1_cb _hidl_cb) {
    std::vector<SensorInfo> sensors;
    if (mAvailable) {
        sensors.push_back(mSensorInfo);
    }
    _hidl_cb(sensors);
    return Void();
}

Return<Result> UdfpsInputSubHal::initialize(const sp<IHalProxyCallback>& halProxyCallback) {
    std::lock_guard<std::mutex> lock(mLock);
    mCallback = halProxyCallback;
    mCurrentOperationMode = OperationMode::NORMAL;
    mActive = false;
    mTriggered = false;
    mTouchModeEnabled = false;
    mTouchModeDisablePending = false;
    return Result::OK;
}

Return<Result> UdfpsInputSubHal::setOperationMode(OperationMode mode) {
    std::lock_guard<std::mutex> lock(mLock);
    mCurrentOperationMode = mode;
    interruptPoll();
    return Result::OK;
}

Return<Result> UdfpsInputSubHal::activate(int32_t sensorHandle, bool enabled) {
    if (sensorHandle != kSensorHandle || !mAvailable) {
        return Result::BAD_VALUE;
    }

    {
        std::lock_guard<std::mutex> lock(mLock);
        if (mActive == enabled) {
            return Result::OK;
        }
        mActive = enabled;
        mTriggered = false;
        mTouchDown = false;
        mHaveX = false;
        mHaveY = false;
    }

    bool touchModeEnabled = true;
    if (enabled) {
        touchModeEnabled = setUdfpsTouchMode(true);
    }

    {
        std::lock_guard<std::mutex> lock(mLock);
        if (enabled) {
            mTouchModeEnabled = touchModeEnabled;
        } else {
            // Mode 10 must remain enabled between operations. Xiaomi's touch
            // stack may clear it after an auth cycle, and a later pointer-down
            // then never reaches the ultrasonic fingerprint driver.
            mTouchModeDisablePending = false;
        }
    }
    interruptPoll();
    return Result::OK;
}

Return<Result> UdfpsInputSubHal::batch(int32_t sensorHandle, int64_t /* samplingPeriodNs */,
                                       int64_t /* maxReportLatencyNs */) {
    return sensorHandle == kSensorHandle && mAvailable ? Result::OK : Result::BAD_VALUE;
}

Return<Result> UdfpsInputSubHal::flush(int32_t /* sensorHandle */) {
    return Result::BAD_VALUE;
}

Return<Result> UdfpsInputSubHal::injectSensorData_2_1(const Event& /* event */) {
    return Result::INVALID_OPERATION;
}

Return<void> UdfpsInputSubHal::registerDirectChannel(
        const SharedMemInfo& /* mem */, ISensors::registerDirectChannel_cb _hidl_cb) {
    _hidl_cb(Result::INVALID_OPERATION, -1);
    return Void();
}

Return<Result> UdfpsInputSubHal::unregisterDirectChannel(int32_t /* channelHandle */) {
    return Result::INVALID_OPERATION;
}

Return<void> UdfpsInputSubHal::configDirectReport(
        int32_t /* sensorHandle */, int32_t /* channelHandle */, RateLevel /* rate */,
        ISensors::configDirectReport_cb _hidl_cb) {
    _hidl_cb(Result::INVALID_OPERATION, -1);
    return Void();
}

Return<void> UdfpsInputSubHal::debug(const hidl_handle& fd, const hidl_vec<hidl_string>& args) {
    if (fd.getNativeHandle() == nullptr || fd->numFds < 1) {
        return Void();
    }

    FILE* out = fdopen(dup(fd->data[0]), "w");
    if (out == nullptr) {
        return Void();
    }

    if (args.size() != 0) {
        fprintf(out, "Debug arguments are ignored.\n");
    }
    fprintf(out, "UDFPS input sub-HAL:\n");
    fprintf(out, "  available: %s\n", mAvailable ? "true" : "false");
    fprintf(out, "  active: %s\n", mActive ? "true" : "false");
    fprintf(out, "  bounds: x=%d y=%d r=%d margin=%d\n", mBounds.x, mBounds.y, mBounds.radius,
            kUdfpsBoundsMargin);
    fprintf(out, "  input fd: %d scale=(%.1f, %.1f)\n", mInputFd, mScaleX, mScaleY);
    fclose(out);
    return Void();
}

bool UdfpsInputSubHal::ensureTouchFeatureBinderLocked() {
    if (mTouchFeatureBinder.get() != nullptr) {
        return true;
    }

    AIBinder_Class* clazz = getTouchFeatureClass();
    if (clazz == nullptr) {
        ALOGE("Failed to create touchfeature binder class");
        return false;
    }

    AIBinder* binder = AServiceManager_checkService(kTouchFeatureService);
    if (binder == nullptr) {
        ALOGW("touchfeature service '%s' is not available", kTouchFeatureService);
        return false;
    }

    if (!AIBinder_associateClass(binder, clazz)) {
        ALOGE("Failed to associate touchfeature binder class");
        AIBinder_decStrong(binder);
        return false;
    }

    mTouchFeatureBinder = ndk::SpAIBinder(binder);
    return true;
}

bool UdfpsInputSubHal::setTouchFeatureMode(int32_t mode, int32_t value) {
    std::lock_guard<std::mutex> lock(mLock);
    if (!ensureTouchFeatureBinderLocked()) {
        return false;
    }

    AParcel* in = nullptr;
    binder_status_t status = AIBinder_prepareTransaction(mTouchFeatureBinder.get(), &in);
    if (status != STATUS_OK) {
        ALOGE("Failed to prepare touchfeature transaction: %d", status);
        mTouchFeatureBinder = nullptr;
        return false;
    }

    status = AParcel_writeInt32(in, kPrimaryTouchId);
    status = status == STATUS_OK ? AParcel_writeInt32(in, mode) : status;
    status = status == STATUS_OK ? AParcel_writeInt32(in, value) : status;
    if (status != STATUS_OK) {
        ALOGE("Failed to write touchfeature transaction: %d", status);
        AParcel_delete(in);
        return false;
    }

    AParcel* out = nullptr;
    status = AIBinder_transact(mTouchFeatureBinder.get(), kSetModeValueTransaction, &in, &out, 0);
    if (status != STATUS_OK) {
        ALOGE("touchfeature setModeValue(%d, %d) failed: %d", mode, value, status);
        mTouchFeatureBinder = nullptr;
        return false;
    }

    AStatus* aidlStatus = nullptr;
    status = AParcel_readStatusHeader(out, &aidlStatus);
    bool ok = status == STATUS_OK && aidlStatus != nullptr && AStatus_isOk(aidlStatus);
    if (ok) {
        int32_t ret = 0;
        status = AParcel_readInt32(out, &ret);
        if (status != STATUS_OK || ret != 0) {
            ALOGW("touchfeature setModeValue(%d, %d) ret=%d status=%d", mode, value, ret, status);
            ok = false;
        }
    } else {
        const binder_status_t aidlBinderStatus =
                aidlStatus != nullptr ? AStatus_getStatus(aidlStatus) : status;
        ALOGE("touchfeature setModeValue(%d, %d) returned status %d", mode, value,
              aidlBinderStatus);
    }

    if (aidlStatus != nullptr) {
        AStatus_delete(aidlStatus);
    }
    AParcel_delete(out);
    return ok;
}

bool UdfpsInputSubHal::getTouchFeatureMode(int32_t mode, int32_t* value) {
    std::lock_guard<std::mutex> lock(mLock);
    if (value == nullptr || !ensureTouchFeatureBinderLocked()) {
        return false;
    }

    AParcel* in = nullptr;
    binder_status_t status = AIBinder_prepareTransaction(mTouchFeatureBinder.get(), &in);
    if (status != STATUS_OK) {
        mTouchFeatureBinder = nullptr;
        return false;
    }

    status = AParcel_writeInt32(in, kPrimaryTouchId);
    status = status == STATUS_OK ? AParcel_writeInt32(in, mode) : status;
    if (status != STATUS_OK) {
        AParcel_delete(in);
        return false;
    }

    AParcel* out = nullptr;
    status = AIBinder_transact(mTouchFeatureBinder.get(), kGetModeValueTransaction, &in, &out, 0);
    if (status != STATUS_OK) {
        mTouchFeatureBinder = nullptr;
        return false;
    }

    AStatus* aidlStatus = nullptr;
    status = AParcel_readStatusHeader(out, &aidlStatus);
    bool ok = status == STATUS_OK && aidlStatus != nullptr && AStatus_isOk(aidlStatus);
    if (ok) {
        status = AParcel_readInt32(out, value);
        ok = status == STATUS_OK;
    }

    if (aidlStatus != nullptr) {
        AStatus_delete(aidlStatus);
    }
    AParcel_delete(out);
    return ok;
}

bool UdfpsInputSubHal::setUdfpsTouchMode(bool enabled) {
    const int32_t value = enabled ? 1 : 0;
    const bool fodEnabled = setTouchFeatureMode(kTouchModeFodEnable, value);
    if (!fodEnabled) {
        ALOGW("Failed to %s Xiaomi UDFPS touch modes", enabled ? "enable" : "disable");
    }
    return fodEnabled;
}

std::string UdfpsInputSubHal::findTouchInputDevice() {
    DIR* dir = opendir(kInputDir);
    if (dir == nullptr) {
        ALOGE("Failed to open %s: %s", kInputDir, strerror(errno));
        return "";
    }

    std::string path;
    dirent* entry = nullptr;
    while ((entry = readdir(dir)) != nullptr) {
        if (std::strncmp(entry->d_name, "event", 5) != 0) {
            continue;
        }

        const std::string candidate = std::string(kInputDir) + "/" + entry->d_name;
        int fd = open(candidate.c_str(), O_RDONLY | O_CLOEXEC);
        if (fd < 0) {
            continue;
        }

        char name[256] = {};
        if (ioctl(fd, EVIOCGNAME(sizeof(name)), name) >= 0 &&
            std::strncmp(name, kInputDeviceName, sizeof(kInputDeviceName) - 1) == 0) {
            path = candidate;
            close(fd);
            break;
        }
        close(fd);
    }
    closedir(dir);

    if (path.empty()) {
        ALOGE("Failed to find %s input device", kInputDeviceName);
    }
    return path;
}

bool UdfpsInputSubHal::openInputDevice() {
    const std::string path = findTouchInputDevice();
    if (path.empty()) {
        return false;
    }

    mInputFd = open(path.c_str(), O_RDONLY | O_CLOEXEC | O_NONBLOCK);
    if (mInputFd < 0) {
        ALOGE("Failed to open %s: %s", path.c_str(), strerror(errno));
        return false;
    }

    if (!configureInputScale()) {
        close(mInputFd);
        mInputFd = -1;
        return false;
    }

    ALOGI("Using UDFPS touch input %s with scale %.1f,%.1f", path.c_str(), mScaleX, mScaleY);
    return true;
}

bool UdfpsInputSubHal::configureInputScale() {
    input_absinfo absX = {};
    input_absinfo absY = {};
    if (ioctl(mInputFd, EVIOCGABS(ABS_MT_POSITION_X), &absX) < 0 ||
        ioctl(mInputFd, EVIOCGABS(ABS_MT_POSITION_Y), &absY) < 0) {
        ALOGE("Failed to read touch ABS ranges: %s", strerror(errno));
        return false;
    }

    mScaleX = absX.maximum > 10000 ? 100.0f : 1.0f;
    mScaleY = absY.maximum > 10000 ? 100.0f : 1.0f;
    return true;
}

bool UdfpsInputSubHal::isInUdfpsBounds(float x, float y) const {
    const float radius = static_cast<float>(mBounds.radius + kUdfpsBoundsMargin);
    const float dx = x - static_cast<float>(mBounds.x);
    const float dy = y - static_cast<float>(mBounds.y);
    return x > 0.0f && y > 0.0f && dx * dx + dy * dy <= radius * radius;
}

void UdfpsInputSubHal::loop() {
    while (true) {
        int pollTimeoutMs = kTouchModeWatchdogMs;
        {
            std::lock_guard<std::mutex> lock(mLock);
            if (!mAvailable) {
                return;
            }
        }

        pollfd fds[] = {
                {.fd = mWaitPipeFd[0], .events = POLLIN, .revents = 0},
                {.fd = mInputFd, .events = POLLIN, .revents = 0},
        };

        const int rc = poll(fds, sizeof(fds) / sizeof(fds[0]), pollTimeoutMs);
        if (rc < 0) {
            if (errno == EINTR) {
                continue;
            }
            ALOGE("Failed to poll UDFPS input: %s", strerror(errno));
            return;
        }
        if (rc == 0) {
            retryTouchModeStateIfNeeded();
            continue;
        }

        if (fds[0].revents & POLLIN) {
            char buffer[16];
            while (read(mWaitPipeFd[0], buffer, sizeof(buffer)) > 0) {
            }
            retryTouchModeStateIfNeeded();
        }

        if (fds[1].revents & POLLIN) {
            input_event inputEvent = {};
            while (read(mInputFd, &inputEvent, sizeof(inputEvent)) == sizeof(inputEvent)) {
                handleInputEvent(inputEvent);
            }
        }
    }
}

void UdfpsInputSubHal::interruptPoll() {
    if (mWaitPipeFd[1] < 0) {
        return;
    }

    char c = '1';
    write(mWaitPipeFd[1], &c, sizeof(c));
}

void UdfpsInputSubHal::retryTouchModeStateIfNeeded() {
    int32_t currentMode = 0;
    const bool modeReadable = getTouchFeatureMode(kTouchModeFodEnable, &currentMode);
    if (modeReadable && currentMode == 1) {
        std::lock_guard<std::mutex> lock(mLock);
        mTouchModeEnabled = true;
        mTouchModeDisablePending = false;
        return;
    }

    const bool enabled = setUdfpsTouchMode(true);
    {
        std::lock_guard<std::mutex> lock(mLock);
        mTouchModeEnabled = enabled;
        mTouchModeDisablePending = false;
        if (enabled) {
            ALOGI("Restored Xiaomi UDFPS touch mode 10");
        }
    }
}

void UdfpsInputSubHal::handleInputEvent(const input_event& inputEvent) {
    bool shouldPost = false;
    float x = 0.0f;
    float y = 0.0f;

    {
        std::lock_guard<std::mutex> lock(mLock);
        if (!mActive || !mTouchModeEnabled || mTriggered ||
            mCurrentOperationMode == OperationMode::DATA_INJECTION) {
            return;
        }

        if (inputEvent.type == EV_ABS) {
            if (inputEvent.code == ABS_MT_TRACKING_ID) {
                mTouchDown = inputEvent.value >= 0;
                if (!mTouchDown) {
                    mHaveX = false;
                    mHaveY = false;
                }
            } else if (inputEvent.code == ABS_MT_POSITION_X) {
                mRawX = inputEvent.value;
                mHaveX = true;
            } else if (inputEvent.code == ABS_MT_POSITION_Y) {
                mRawY = inputEvent.value;
                mHaveY = true;
            }
        } else if (inputEvent.type == EV_KEY && inputEvent.code == BTN_TOUCH) {
            mTouchDown = inputEvent.value != 0;
            if (!mTouchDown) {
                mHaveX = false;
                mHaveY = false;
            }
        } else if (inputEvent.type == EV_SYN && inputEvent.code == SYN_REPORT) {
            x = static_cast<float>(mRawX) / mScaleX;
            y = static_cast<float>(mRawY) / mScaleY;
            if (mTouchDown && mHaveX && mHaveY && isInUdfpsBounds(x, y)) {
                mTriggered = true;
                mActive = false;
                shouldPost = true;
            }
        }
    }

    if (shouldPost) {
        ALOGI("UDFPS input trigger at %.0f,%.0f", x, y);
        postUdfpsEvent(x, y);
    }
}

void UdfpsInputSubHal::postUdfpsEvent(float x, float y) {
    sp<IHalProxyCallback> callback;
    {
        std::lock_guard<std::mutex> lock(mLock);
        callback = mCallback;
    }

    if (callback == nullptr) {
        ALOGW("Dropping UDFPS input event before callback initialization");
        return;
    }

    Event event = {};
    event.sensorHandle = kSensorHandle;
    event.sensorType = mSensorInfo.type;
    event.timestamp = ::android::elapsedRealtimeNano();
    event.u.data[0] = x;
    event.u.data[1] = y;
    event.u.data[3] = kSyntheticUdfpsMajorMinor;
    event.u.data[4] = kSyntheticUdfpsMajorMinor;

    std::vector<Event> events{event};
    auto wakelock = callback->createScopedWakelock(true);
    callback->postEvents(events, std::move(wakelock));
}

}  // namespace implementation
}  // namespace subhal
}  // namespace V2_1
}  // namespace sensors
}  // namespace hardware
}  // namespace android

using ::android::hardware::sensors::V2_1::implementation::ISensorsSubHal;
using ::android::hardware::sensors::V2_1::subhal::implementation::UdfpsInputSubHal;

ISensorsSubHal* sensorsHalGetSubHal_2_1(uint32_t* version) {
    static UdfpsInputSubHal subHal;
    *version = SUB_HAL_2_1_VERSION;
    return &subHal;
}
