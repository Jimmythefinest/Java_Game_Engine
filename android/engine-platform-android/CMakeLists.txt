cmake_minimum_required(VERSION 3.22.1)

project("njst_native")

add_library(njst_math SHARED
    src/main/cpp/njst_math.cpp
)

add_library(njst_audio SHARED
    src/main/cpp/njst_audio.cpp
)

find_library(log-lib log)
find_library(android-lib android)
find_library(opensles-lib OpenSLES)

target_link_libraries(njst_math
    ${log-lib}
    ${android-lib}
)

target_link_libraries(njst_audio
    ${log-lib}
    ${android-lib}
    ${opensles-lib}
)
