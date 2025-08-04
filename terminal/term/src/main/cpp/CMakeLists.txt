cmake_minimum_required(VERSION 3.22.1)

project("jackpal-androidterm5")

add_library(${CMAKE_PROJECT_NAME} SHARED
        common.cpp
        termExec.cpp
)

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log)
