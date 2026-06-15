#pragma once

#include <cuda_runtime.h>
#include <algorithm>
#include <chrono>
#include <cstddef>
#include <cstdio>
#include <memory>
#include <string>
#include <thread>

#include "core/cuda_error.cuh"
#include "core/cuda_memory_pressure.cuh"
#include "core/cuda_memory_tracker.cuh"

struct CudaDeviceFreeDeleter {
    int device;
    size_t bytes = 0;
    const char *label = "unnamed";

    template<typename T>
    void operator()(T *ptr) const noexcept {
        if (ptr) {
            int originalDevice = -1;
            cudaGetDevice(&originalDevice);
            if (originalDevice != device) {
                cudaSetDevice(device);
            }
            cudaError_t err = cudaFree(ptr);
            if (err == cudaSuccess) {
                recordDeviceFree(static_cast<void *>(ptr), bytes, label);
            }
            if (originalDevice != -1 && originalDevice != device) {
                cudaSetDevice(originalDevice);
            }
        }
    }
};

struct CudaHostFreeDeleter {
    void operator()(void *ptr) const noexcept {
        if (ptr) cudaFreeHost(ptr);
    }
};

template<typename T>
using DeviceAsyncUniquePtr = std::unique_ptr<T, CudaDeviceFreeDeleter>;

template<typename T>
using HostPinnedUniquePtr = std::unique_ptr<T, CudaHostFreeDeleter>;

template<typename T>
HostPinnedUniquePtr<T> makePinnedHostBuffer(size_t elementCount) {
    if (elementCount == 0) return HostPinnedUniquePtr<T>(nullptr);
    T *raw = nullptr;
    CHECK_CUDA(cudaMallocHost(&raw, elementCount * sizeof(T)));
    return HostPinnedUniquePtr<T>(raw);
}

template<typename T>
DeviceAsyncUniquePtr<T> allocateDeviceBuffer(size_t elementCount, const char *label) {
    if (elementCount == 0) return DeviceAsyncUniquePtr<T>(nullptr);

    T *raw = nullptr;
    int device = 0;
    CHECK_CUDA(cudaGetDevice(&device));
    const size_t bytes = elementCount * sizeof(T);

    auto startTime = std::chrono::steady_clock::now();
    int backoffMs = 100;
    const int maxBackoffMs = 5000;
    const auto configuredTimeoutMs = gGpuMemoryRetryTimeoutMs.load(std::memory_order_relaxed);
    const int timeoutMs = static_cast<int>(std::max<int64_t>(configuredTimeoutMs, 0));

    while (true) {
        auto allocationStatus = cudaMalloc(&raw, bytes);
        if (allocationStatus == cudaSuccess) break;
        if (allocationStatus == cudaErrorMemoryAllocation) {
            auto now = std::chrono::steady_clock::now();
            auto elapsedMs = std::chrono::duration_cast<std::chrono::milliseconds>(now - startTime).count();
            if (elapsedMs >= timeoutMs) {
                throw GpuMemoryExhaustedException(
                    std::string("CUDA out of memory while allocating ")
                    + std::to_string(bytes) + " bytes for "
                    + (label ? std::string(label) : std::string("deviceBuffer")));
            }
            gGpuMemoryRetries.fetch_add(1, std::memory_order_relaxed);
            notifyGpuMemoryPressureForCurrentDevice();
            std::this_thread::sleep_for(std::chrono::milliseconds(backoffMs));
            backoffMs = std::min(backoffMs * 2, maxBackoffMs);
        } else {
            CHECK_CUDA(allocationStatus);
            break;
        }
    }

    recordDeviceAllocation(raw, bytes, label);
    return DeviceAsyncUniquePtr<T>(raw, CudaDeviceFreeDeleter{device, bytes, label});
}
