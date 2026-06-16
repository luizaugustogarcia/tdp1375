#pragma once

#include <cuda_runtime.h>

class StreamGuard {
public:
    explicit StreamGuard(cudaStream_t stream = nullptr) : stream_(stream) {}

    ~StreamGuard() {
        if (stream_) cudaStreamDestroy(stream_);
    }

    cudaStream_t get() const noexcept { return stream_; }

    cudaStream_t release() noexcept {
        cudaStream_t tmp = stream_;
        stream_ = nullptr;
        return tmp;
    }

    void reset(cudaStream_t newStream = nullptr) noexcept {
        if (stream_ && stream_ != newStream) cudaStreamDestroy(stream_);
        stream_ = newStream;
    }

private:
    cudaStream_t stream_;
};
