cmake_minimum_required(VERSION 3.31)

project("runner")

set(CMAKE_CXX_STANDARD 17)

add_compile_options(-Werror=format -fdata-sections -ffunction-sections -fno-exceptions -fno-rtti -fno-threadsafe-statics)

if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    message("Builing Release...")

    add_compile_options(-Os -flto -fvisibility=hidden -fvisibility-inlines-hidden)
    add_link_options(-flto -Wl,--exclude-libs,ALL -Wl,--gc-sections -Wl,--strip-all)
else ()
    message("Builing Debug...")

    add_definitions(-DDEBUG)
endif ()

find_package(cxx REQUIRED CONFIG)

# libprocessutils
add_library(processutils SHARED
        processutils.c)

set_target_properties(processutils PROPERTIES PREFIX "lib")
set_target_properties(processutils PROPERTIES SUFFIX ".so")

# libexecutils
add_library(executils SHARED
        executils.c)

set_target_properties(executils PROPERTIES PREFIX "lib")
set_target_properties(executils PROPERTIES SUFFIX ".so")
target_link_libraries(executils log)
