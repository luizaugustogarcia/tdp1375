#ifndef SLOT_MANAGER_CUH
#define SLOT_MANAGER_CUH

#include <cuda_runtime.h>
#include <cstddef>

void initGpuSortSlots(int totalSlots, int devicesCount, size_t queueBytesBudget, int dedupTableSize);

void destroyGpuSortSlots();

void cancelGpuSort(int slot);

void clearGpuSortCancel(int slot);

bool isGpuSortCancelled(int slot);

int *getGpuSortCancelDevicePointer(int slot);

cudaStream_t getGpuSortSlotStream(int slot);

int getGpuSortSlotDevice(int slot);

#endif
