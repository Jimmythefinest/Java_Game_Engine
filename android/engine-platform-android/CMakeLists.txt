cmake_minimum_required(VERSION 3.22.1)

project("njst_math")

add_library(njst_math SHARED
    src/main/cpp/njst_math.cpp
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(njst_math
    ${log-lib}
    ${android-lib}
)
