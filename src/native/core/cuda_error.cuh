#pragma once

#include <cuda_runtime.h>
#include <cstdio>
#include <cstdlib>
#include <sstream>
#include <stdexcept>
#include <string>

inline thread_local bool gCudaRuntimePoisoned = false;
inline thread_local bool gCudaPoisonedCleanupWarningEmitted = false;

inline bool isCudaPoisoningError(cudaError_t code) {
    switch (code) {
        case cudaErrorIllegalAddress:
        case cudaErrorAssert:
        case cudaErrorLaunchFailure:
        case cudaErrorLaunchTimeout:
        case cudaErrorIllegalInstruction:
        case cudaErrorMisalignedAddress:
        case cudaErrorInvalidAddressSpace:
        case cudaErrorInvalidPc:
        case cudaErrorHardwareStackError:
        case cudaErrorUnknown:
            return true;
        default:
            return false;
    }
}

inline void clearCudaRuntimePoisoned() noexcept {
    gCudaRuntimePoisoned = false;
    gCudaPoisonedCleanupWarningEmitted = false;
}

inline void markCudaRuntimePoisoned() noexcept {
    gCudaRuntimePoisoned = true;
}

inline bool isCudaRuntimePoisoned() noexcept {
    return gCudaRuntimePoisoned;
}

class CudaException : public std::runtime_error {
public:
    CudaException(cudaError_t code, const char *file, int line)
        : std::runtime_error(buildMessage(code, file, line)), code_(code) {
        if (isCudaPoisoningError(code)) {
            markCudaRuntimePoisoned();
        }
    }

    cudaError_t code() const noexcept { return code_; }

private:
    static std::string buildMessage(cudaError_t code, const char *file, int line) {
        std::ostringstream oss;
        oss << file << ':' << line << ": CUDA error "
                << cudaGetErrorName(code) << " (" << cudaGetErrorString(code) << ')';
        return oss.str();
    }

    cudaError_t code_;
};

class GpuMemoryExhaustedException : public std::runtime_error {
public:
    explicit GpuMemoryExhaustedException(const std::string &message)
        : std::runtime_error(message) {}
};

class GpuSortCancelledException : public std::runtime_error {
public:
    explicit GpuSortCancelledException(const std::string &message)
        : std::runtime_error(message) {}
};

#ifndef CHECK_CUDA
#define CHECK_CUDA(x)                                                                                              \
    do {                                                                                                           \
        cudaError_t e = (x);                                                                                       \
        if (e != cudaSuccess) {                                                                                    \
            throw CudaException(e, __FILE__, __LINE__);                                                             \
        }                                                                                                          \
    } while (0)
#endif
