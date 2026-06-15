#pragma once

#include <cuda_runtime.h>
#include <cstddef>
#include <cstdio>
#include <map>
#include <mutex>
#include <string>
#include <vector>

#include "core/cuda_error.cuh"

struct DeviceAllocationSnapshot {
    std::string label;
    size_t bytes{0};
    void *pointer{nullptr};
};

class DeviceAllocationTracker {
public:
    static DeviceAllocationTracker &instance() {
        static DeviceAllocationTracker tracker;
        return tracker;
    }

    bool enabled() const { return enabled_; }

    void onAllocate(void *ptr, size_t bytes, const char *label) {
        if (!enabled_) return;
        std::lock_guard<std::mutex> lock(mutex_);
        currentLiveBytes_ += bytes;
        liveAllocations_[ptr] = DeviceAllocationSnapshot{label ? label : "unnamed", bytes, ptr};
    }

    void onFree(void *ptr, size_t bytes, const char *label) {
        if (!enabled_) return;
        std::lock_guard<std::mutex> lock(mutex_);
        const auto it = liveAllocations_.find(ptr);
        const size_t actualBytes = it != liveAllocations_.end() ? it->second.bytes : bytes;
        currentLiveBytes_ = currentLiveBytes_ >= actualBytes ? currentLiveBytes_ - actualBytes : 0;
        if (it != liveAllocations_.end()) liveAllocations_.erase(it);
    }

private:
    static bool shouldEnableFromEnvironment() {
        const char *value = std::getenv("TDP_CUDA_ALLOC_REPORT");
        return value != nullptr && value[0] != '\0' && value[0] != '0';
    }

    bool enabled_{shouldEnableFromEnvironment()};
    mutable std::mutex mutex_;
    size_t currentLiveBytes_{0};
    std::map<void *, DeviceAllocationSnapshot> liveAllocations_;
};

inline void recordDeviceAllocation(void *ptr, size_t bytes, const char *label) {
    DeviceAllocationTracker::instance().onAllocate(ptr, bytes, label);
}

inline void recordDeviceFree(void *ptr, size_t bytes, const char *label) {
    DeviceAllocationTracker::instance().onFree(ptr, bytes, label);
}
