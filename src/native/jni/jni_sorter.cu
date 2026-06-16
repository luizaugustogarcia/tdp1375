#include <cuda_runtime.h>
#include <jni.h>

#include <cstdio>
#include <cstddef>
#include <cstdint>
#include <exception>
#include <stdexcept>
#include <vector>

#include "core/cuda_error.cuh"
#include "backtracking/slot_manager.cuh"
#include "backtracking/sorting_search.cuh"
#include "backtracking/pi_filter.cuh"

static void throwRuntimeException(JNIEnv *env, const char *message) {
    if (env->ExceptionCheck()) env->ExceptionClear();
    jclass cls = env->FindClass("java/lang/RuntimeException");
    if (cls) {
        env->ThrowNew(cls, message);
        env->DeleteLocalRef(cls);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_br_unb_cic_tdp_base_GPUSortingSearch_initSlots(JNIEnv *env, jclass, jint totalSlots, jint devicesCount,
                                                     jlong queueBytesBudget, jint dedupTableSize) {
    try {
        initGpuSortSlots(static_cast<int>(totalSlots), static_cast<int>(devicesCount),
                         static_cast<size_t>(queueBytesBudget), static_cast<int>(dedupTableSize));
    } catch (const std::exception &e) {
        throwRuntimeException(env, e.what());
    }
}

extern "C" JNIEXPORT void JNICALL
Java_br_unb_cic_tdp_base_GPUSortingSearch_destroySlots(JNIEnv *env, jclass) {
    try {
        destroyGpuSortSlots();
    } catch (const std::exception &e) {
        throwRuntimeException(env, e.what());
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_br_unb_cic_tdp_base_GPUSortingSearch_searchForSortingSeq(JNIEnv *env, jclass,
                                                               jbyteArray javaPi,
                                                               jbyteArray javaSpi,
                                                               jint initialEvenCycles,
                                                               jfloat minRate,
                                                               jint maxMoves,
                                                               jboolean fullSorting,
                                                               jint slot) {
    try {
        clearCudaRuntimePoisoned();

        const jsize n = env->GetArrayLength(javaPi);
        std::vector<short> pi(n);
        std::vector<short> spi(n);

        jbyte *piPtr = static_cast<jbyte *>(env->GetPrimitiveArrayCritical(javaPi, nullptr));
        if (piPtr) {
            for (jsize i = 0; i < n; ++i) pi[i] = static_cast<short>(piPtr[i] & 0xFF);
            env->ReleasePrimitiveArrayCritical(javaPi, piPtr, JNI_ABORT);
        }
        jbyte *spiPtr = static_cast<jbyte *>(env->GetPrimitiveArrayCritical(javaSpi, nullptr));
        if (spiPtr) {
            for (jsize i = 0; i < n; ++i) spi[i] = static_cast<short>(spiPtr[i] & 0xFF);
            env->ReleasePrimitiveArrayCritical(javaSpi, spiPtr, JNI_ABORT);
        }

        const int slotIndex = static_cast<int>(slot);
        const int slotDevice = getGpuSortSlotDevice(slotIndex);
        CHECK_CUDA(cudaSetDevice(slotDevice));
        cudaStream_t rawStream = getGpuSortSlotStream(slotIndex);

        const auto moves = performGpuSortingSearch(
                std::move(pi), std::move(spi),
                static_cast<int>(initialEvenCycles),
                static_cast<double>(minRate),
                static_cast<int>(maxMoves),
                static_cast<bool>(fullSorting),
                rawStream, slotIndex);

        jclass intArrayClass = env->FindClass("[I");
        jobjectArray outer = env->NewObjectArray(static_cast<jsize>(moves.size()), intArrayClass, nullptr);
        for (jsize idx = 0; idx < static_cast<jsize>(moves.size()); ++idx) {
            jint move[3] = {moves[idx].firstIndex, moves[idx].secondIndex, moves[idx].thirdIndex};
            jintArray inner = env->NewIntArray(3);
            env->SetIntArrayRegion(inner, 0, 3, move);
            env->SetObjectArrayElement(outer, idx, inner);
            env->DeleteLocalRef(inner);
        }
        env->DeleteLocalRef(intArrayClass);
        return outer;
    } catch (const GpuSortCancelledException &e) {
        throwRuntimeException(env, e.what());
    } catch (const GpuMemoryExhaustedException &e) {
        throwRuntimeException(env, e.what());
    } catch (const std::exception &e) {
        throwRuntimeException(env, e.what());
    }
    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_br_unb_cic_tdp_base_GPUSortingSearch_cancelSort(JNIEnv *, jclass, jint slot) {
    cancelGpuSort(static_cast<int>(slot));
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_br_unb_cic_tdp_base_GPUSortingSearch_filterPiPermutations(JNIEnv *env, jclass,
                                                                jbyteArray javaCycle,
                                                                jint cycleLen,
                                                                jbyteArray javaTriple,
                                                                jint tripleLen,
                                                                jint n,
                                                                jboolean skip2MoveCheck) {
    try {
        std::vector<uint8_t> cycle(cycleLen);
        std::vector<uint8_t> triple(tripleLen);

        jbyte *cyclePtr = env->GetByteArrayElements(javaCycle, nullptr);
        for (int i = 0; i < cycleLen; ++i) cycle[i] = static_cast<uint8_t>(cyclePtr[i] & 0xFF);
        env->ReleaseByteArrayElements(javaCycle, cyclePtr, JNI_ABORT);

        jbyte *triplePtr = env->GetByteArrayElements(javaTriple, nullptr);
        for (int i = 0; i < tripleLen; ++i) triple[i] = static_cast<uint8_t>(triplePtr[i] & 0xFF);
        env->ReleaseByteArrayElements(javaTriple, triplePtr, JNI_ABORT);

        auto result = performGpuPiFilter(cycle.data(), cycleLen, triple.data(), tripleLen, n,
                                         static_cast<bool>(skip2MoveCheck));

        jclass byteArrayClass = env->FindClass("[B");
        jobjectArray outer = env->NewObjectArray(static_cast<jsize>(result.permutations.size()), byteArrayClass, nullptr);
        for (jsize idx = 0; idx < static_cast<jsize>(result.permutations.size()); ++idx) {
            const auto &perm = result.permutations[idx];
            jbyteArray inner = env->NewByteArray(static_cast<jsize>(perm.size()));
            env->SetByteArrayRegion(inner, 0, static_cast<jsize>(perm.size()),
                                    reinterpret_cast<const jbyte *>(perm.data()));
            env->SetObjectArrayElement(outer, idx, inner);
            env->DeleteLocalRef(inner);
        }
        env->DeleteLocalRef(byteArrayClass);
        return outer;
    } catch (const std::exception &e) {
        throwRuntimeException(env, e.what());
    }
    return nullptr;
}
