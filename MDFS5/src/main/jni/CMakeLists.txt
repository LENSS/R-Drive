
cmake_minimum_required(VERSION 3.4.1)

# # configure import libs
# set(distribution_DIR ${CMAKE_SOURCE_DIR}/../../../../distribution)

# add_library(lib_gmath STATIC IMPORTED)
# set_target_properties(lib_gmath PROPERTIES IMPORTED_LOCATION
    # ${distribution_DIR}/gmath/lib/${ANDROID_ABI}/libgmath.a)

# # shared lib will also be tucked into APK and sent to target
# # refer to app/build.gradle, jniLibs section for that purpose.
# # ${ANDROID_ABI} is handy for our purpose here. Probably this ${ANDROID_ABI} is
# # the most valuable thing of this sample, the rest are pretty much normal cmake
# add_library(lib_gperf SHARED IMPORTED)
# set_target_properties(lib_gperf PROPERTIES IMPORTED_LOCATION
    # ${distribution_DIR}/gperf/lib/${ANDROID_ABI}/libgperf.so)

# build application's shared lib
# set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

add_library(ReedSolomon SHARED
            ReedSolomon.c galois.c jerasure.c reed_sol.c)

target_include_directories(ReedSolomon PRIVATE)

target_link_libraries(ReedSolomon
                      android
                      log)
