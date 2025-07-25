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

add_library(rish SHARED
        rish/main.cpp
        rish/pts.cpp
        rish/rikka_rish_RishTerminal.cpp
        rish/rikka_rish_RishHost.cpp)

add_executable(starter
        starter.c)

target_link_libraries(rish log cxx::cxx)

add_library(processutils SHARED
        processutils.c)

set_target_properties(starter PROPERTIES PREFIX "lib")
set_target_properties(starter PROPERTIES SUFFIX ".so")

set_target_properties(processutils PROPERTIES PREFIX "lib")
set_target_properties(processutils PROPERTIES SUFFIX ".so")

if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    add_custom_command(TARGET rish POST_BUILD
            COMMAND ${CMAKE_STRIP} --remove-section=.comment "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/librish.so")
endif ()