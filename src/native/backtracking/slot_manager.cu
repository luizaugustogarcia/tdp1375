#include <cuda_runtime.h>

#include <atomic>
#include <cstdint>
#include <memory>
#include <mutex>
#include <vector>

#include "core/cuda_error.cuh"
#include "backtracking/slot_manager.cuh"
#include "backtracking/sorting_search.cuh"

static std::unique_ptr<std::atomic<bool>[]> gCancelFlags;
static int gCancelFlagsSize = 0;
static std::vector<cudaStream_t> gSlotStreams;
static std::vector<cudaStream_t> gCancelSignalStreams;
static std::vector<int> gSlotDevices;
static std::vector<int *> gCancelDeviceFlags;
static std::vector<int *> gCancelSignalValues;
static std::mutex gSlotStateMutex;

void destroyGpuSortSlots() {
    std::lock_guard<std::mutex> lock(gSlotStateMutex);
    int originalDevice = -1;
    cudaGetDevice(&originalDevice);
    for (int slot = 0; slot < static_cast<int>(gSlotStreams.size()); ++slot) {
        if (gSlotStreams[slot] == nullptr) continue;
        if (slot < static_cast<int>(gSlotDevices.size()) && gSlotDevices[slot] >= 0) {
            cudaSetDevice(gSlotDevices[slot]);
        }
        cudaStreamDestroy(gSlotStreams[slot]);
        gSlotStreams[slot] = nullptr;
    }
    for (int slot = 0; slot < static_cast<int>(gCancelSignalStreams.size()); ++slot) {
        const int device = (slot < static_cast<int>(gSlotDevices.size())) ? gSlotDevices[slot] : -1;
        if (device >= 0) cudaSetDevice(device);
        if (gCancelSignalStreams[slot] != nullptr) {
            cudaStreamSynchronize(gCancelSignalStreams[slot]);
            cudaStreamDestroy(gCancelSignalStreams[slot]);
            gCancelSignalStreams[slot] = nullptr;
        }
        if (slot < static_cast<int>(gCancelDeviceFlags.size()) && gCancelDeviceFlags[slot] != nullptr) {
            cudaFree(gCancelDeviceFlags[slot]);
            gCancelDeviceFlags[slot] = nullptr;
        }
        if (slot < static_cast<int>(gCancelSignalValues.size()) && gCancelSignalValues[slot] != nullptr) {
            cudaFreeHost(gCancelSignalValues[slot]);
            gCancelSignalValues[slot] = nullptr;
        }
    }
    gSlotStreams.clear();
    gCancelSignalStreams.clear();
    gSlotDevices.clear();
    gCancelDeviceFlags.clear();
    gCancelSignalValues.clear();
    destroyGpuSortingSearchSlots();
    gCancelFlags.reset();
    gCancelFlagsSize = 0;
    if (originalDevice != -1) cudaSetDevice(originalDevice);
}

void initGpuSortSlots(const int totalSlots, const int devicesCount, const size_t queueBytesBudget, const int dedupTableSize) {
    destroyGpuSortSlots();
    std::lock_guard<std::mutex> lock(gSlotStateMutex);
    int originalDevice = -1;
    cudaGetDevice(&originalDevice);

    gCancelFlags = std::make_unique<std::atomic<bool>[]>(totalSlots);
    gCancelFlagsSize = totalSlots;
    gSlotStreams.assign(totalSlots, nullptr);
    gCancelSignalStreams.assign(totalSlots, nullptr);
    gSlotDevices.assign(totalSlots, -1);
    gCancelDeviceFlags.assign(totalSlots, nullptr);
    gCancelSignalValues.assign(totalSlots, nullptr);
    for (int i = 0; i < totalSlots; ++i) {
        const int device = i % devicesCount;
        CHECK_CUDA(cudaSetDevice(device));
        cudaStream_t stream = nullptr;
        cudaStream_t cancelSignalStream = nullptr;
        int *deviceCancelFlag = nullptr;
        int *hostCancelValue = nullptr;
        CHECK_CUDA(cudaStreamCreateWithFlags(&stream, cudaStreamNonBlocking));
        CHECK_CUDA(cudaStreamCreateWithFlags(&cancelSignalStream, cudaStreamNonBlocking));
        CHECK_CUDA(cudaMalloc(&deviceCancelFlag, sizeof(int)));
        CHECK_CUDA(cudaMemset(deviceCancelFlag, 0, sizeof(int)));
        CHECK_CUDA(cudaHostAlloc(&hostCancelValue, sizeof(int), cudaHostAllocPortable));
        *hostCancelValue = 1;
        gSlotStreams[i] = stream;
        gCancelSignalStreams[i] = cancelSignalStream;
        gSlotDevices[i] = device;
        gCancelDeviceFlags[i] = deviceCancelFlag;
        gCancelSignalValues[i] = hostCancelValue;
        gCancelFlags[i].store(false, std::memory_order_relaxed);
        allocateGpuSortingSearchSlot(i, device, queueBytesBudget, dedupTableSize);
    }

    if (originalDevice != -1) cudaSetDevice(originalDevice);
}

void cancelGpuSort(const int slot) {
    std::lock_guard<std::mutex> lock(gSlotStateMutex);
    if (slot >= 0 && slot < gCancelFlagsSize) {
        gCancelFlags[slot].store(true, std::memory_order_release);
        if (slot < static_cast<int>(gCancelDeviceFlags.size())
            && slot < static_cast<int>(gCancelSignalStreams.size())
            && slot < static_cast<int>(gCancelSignalValues.size())
            && slot < static_cast<int>(gSlotDevices.size())
            && gCancelDeviceFlags[slot] != nullptr
            && gCancelSignalStreams[slot] != nullptr
            && gCancelSignalValues[slot] != nullptr
            && gSlotDevices[slot] >= 0) {
            int originalDevice = -1;
            cudaGetDevice(&originalDevice);
            cudaSetDevice(gSlotDevices[slot]);
            cudaMemcpyAsync(gCancelDeviceFlags[slot],
                            gCancelSignalValues[slot],
                            sizeof(int),
                            cudaMemcpyHostToDevice,
                            gCancelSignalStreams[slot]);
            if (originalDevice != -1 && originalDevice != gSlotDevices[slot]) {
                cudaSetDevice(originalDevice);
            }
        }
    }
}

void clearGpuSortCancel(const int slot) {
    std::lock_guard<std::mutex> lock(gSlotStateMutex);
    if (slot >= 0 && slot < gCancelFlagsSize) {
        gCancelFlags[slot].store(false, std::memory_order_release);
        if (slot < static_cast<int>(gCancelSignalStreams.size())
            && slot < static_cast<int>(gSlotDevices.size())
            && gCancelSignalStreams[slot] != nullptr
            && gSlotDevices[slot] >= 0) {
            int originalDevice = -1;
            cudaGetDevice(&originalDevice);
            cudaSetDevice(gSlotDevices[slot]);
            cudaStreamSynchronize(gCancelSignalStreams[slot]);
            if (originalDevice != -1 && originalDevice != gSlotDevices[slot]) {
                cudaSetDevice(originalDevice);
            }
        }
    }
}

bool isGpuSortCancelled(const int slot) {
    std::lock_guard<std::mutex> lock(gSlotStateMutex);
    if (slot < 0 || slot >= gCancelFlagsSize) return false;
    return gCancelFlags[slot].load(std::memory_order_acquire);
}

int *getGpuSortCancelDevicePointer(const int slot) {
    std::lock_guard<std::mutex> lock(gSlotStateMutex);
    if (slot < 0 || slot >= static_cast<int>(gCancelDeviceFlags.size())) return nullptr;
    return gCancelDeviceFlags[slot];
}

cudaStream_t getGpuSortSlotStream(const int slot) {
    std::lock_guard<std::mutex> lock(gSlotStateMutex);
    if (slot < 0 || slot >= gCancelFlagsSize || slot >= static_cast<int>(gSlotStreams.size())) return nullptr;
    const int device = (slot < static_cast<int>(gSlotDevices.size())) ? gSlotDevices[slot] : -1;
    if (device < 0) return nullptr;
    CHECK_CUDA(cudaSetDevice(device));
    cudaStream_t &stream = gSlotStreams[slot];
    if (stream == nullptr) {
        CHECK_CUDA(cudaStreamCreateWithFlags(&stream, cudaStreamNonBlocking));
    }
    return stream;
}

int getGpuSortSlotDevice(const int slot) {
    std::lock_guard<std::mutex> lock(gSlotStateMutex);
    if (slot < 0 || slot >= static_cast<int>(gSlotDevices.size())) return -1;
    return gSlotDevices[slot];
}
