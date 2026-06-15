#pragma once

#include <cuda_runtime.h>
#include <atomic>
#include <cstdint>
#include <memory>
#include <mutex>

inline std::atomic<uint64_t> gGpuMemoryRetries{0};
inline std::atomic<int64_t> gGpuMemoryRetryTimeoutMs{60000};
inline std::mutex gGpuMemoryPressureMutex;
inline std::unique_ptr<std::atomic<uint64_t>[]> gGpuMemoryPressureEpochs;
inline int gGpuMemoryPressureDeviceCount = 0;

inline void initializeGpuMemoryPressureEpochs(const int deviceCount) {
    std::lock_guard<std::mutex> lock(gGpuMemoryPressureMutex);
    if (deviceCount <= 0) {
        gGpuMemoryPressureEpochs.reset();
        gGpuMemoryPressureDeviceCount = 0;
        return;
    }
    gGpuMemoryPressureEpochs = std::make_unique<std::atomic<uint64_t>[]>(deviceCount);
    gGpuMemoryPressureDeviceCount = deviceCount;
    for (int device = 0; device < deviceCount; ++device) {
        gGpuMemoryPressureEpochs[device].store(0, std::memory_order_relaxed);
    }
}

inline void notifyGpuMemoryPressureForCurrentDevice() noexcept {
    int device = -1;
    const cudaError_t status = cudaGetDevice(&device);
    if (status != cudaSuccess) {
        return;
    }
    std::lock_guard<std::mutex> lock(gGpuMemoryPressureMutex);
    if (gGpuMemoryPressureEpochs && device < gGpuMemoryPressureDeviceCount) {
        gGpuMemoryPressureEpochs[device].fetch_add(1, std::memory_order_relaxed);
    }
}
