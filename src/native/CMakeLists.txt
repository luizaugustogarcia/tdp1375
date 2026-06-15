cmake_minimum_required(VERSION 3.24)
project(tdp1375_native LANGUAGES CXX CUDA)

find_package(CUDAToolkit REQUIRED)

set(TDP_DEFAULT_CUDA_ARCHITECTURES 60-real 70-real 75-real 80-real 86-real)
set(TDP_FORWARD_CUDA_ARCHITECTURE 86-virtual)
if(CMAKE_CUDA_COMPILER_VERSION VERSION_GREATER_EQUAL 11.8)
    list(APPEND TDP_DEFAULT_CUDA_ARCHITECTURES 89-real)
    set(TDP_FORWARD_CUDA_ARCHITECTURE 89-virtual)
endif()
if(CMAKE_CUDA_COMPILER_VERSION VERSION_GREATER_EQUAL 12.0)
    list(APPEND TDP_DEFAULT_CUDA_ARCHITECTURES 90-real)
    set(TDP_FORWARD_CUDA_ARCHITECTURE 90-virtual)
endif()
list(APPEND TDP_DEFAULT_CUDA_ARCHITECTURES ${TDP_FORWARD_CUDA_ARCHITECTURE})
set(TDP_CUDA_ARCHITECTURES "${TDP_DEFAULT_CUDA_ARCHITECTURES}"
    CACHE STRING "CUDA architectures supported by the native libraries")
set(CMAKE_CUDA_ARCHITECTURES ${TDP_CUDA_ARCHITECTURES})
message(STATUS "CUDA architectures: ${CMAKE_CUDA_ARCHITECTURES}")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CUDA_STANDARD 17)
set(CMAKE_CUDA_STANDARD_REQUIRED ON)
set(CMAKE_CUDA_RUNTIME_LIBRARY Shared)

# ── tdp1375_jni (JNI shared library) ────────────────────────────────────────

find_package(JNI)

if(JNI_FOUND)
    add_library(tdp1375_jni SHARED
        jni/jni_sorter.cu
        backtracking/slot_manager.cu
        backtracking/sorting_search.cu
        backtracking/pi_filter.cu
    )

    target_include_directories(tdp1375_jni PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${JNI_INCLUDE_DIRS}
    )
    target_link_libraries(tdp1375_jni PRIVATE CUDA::cudart ${JNI_LIBRARIES})

    set_target_properties(tdp1375_jni PROPERTIES
        POSITION_INDEPENDENT_CODE ON
        CUDA_SEPARABLE_COMPILATION ON
        CUDA_RESOLVE_DEVICE_SYMBOLS ON
    )

    target_compile_definitions(tdp1375_jni PRIVATE MAX_N=63 MAX_DEPTH=15)

    target_compile_options(tdp1375_jni PRIVATE
        $<$<COMPILE_LANGUAGE:CUDA>:-Xptxas=-v>
        $<$<CONFIG:Debug>:-G>
        $<$<CONFIG:RelWithDebInfo>:-lineinfo>
    )

    install(TARGETS tdp1375_jni DESTINATION lib)
endif()

# ── tdp1375_main (standalone test executable) ────────────────────────────────

add_executable(tdp1375_main
    test/main.cu
    backtracking/slot_manager.cu
    backtracking/sorting_search.cu
    backtracking/pi_filter.cu
)

target_include_directories(tdp1375_main PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

target_link_libraries(tdp1375_main PRIVATE CUDA::cudart)

set_target_properties(tdp1375_main PROPERTIES
    CUDA_SEPARABLE_COMPILATION ON
    CUDA_RESOLVE_DEVICE_SYMBOLS ON
)

target_compile_definitions(tdp1375_main PRIVATE MAX_N=63 MAX_DEPTH=15)

target_compile_options(tdp1375_main PRIVATE
    $<$<COMPILE_LANGUAGE:CUDA>:-Xptxas=-v>
    $<$<CONFIG:RelWithDebInfo>:-lineinfo>
)
