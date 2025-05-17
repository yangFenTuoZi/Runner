cmake_minimum_required(VERSION 3.22.1)

project("runner")

add_executable(starter
        starter.c)

add_library(processutils SHARED
        processutils.c)

set_target_properties(starter PROPERTIES PREFIX "lib")
set_target_properties(starter PROPERTIES SUFFIX ".so")

set_target_properties(processutils PROPERTIES PREFIX "lib")
set_target_properties(processutils PROPERTIES SUFFIX ".so")
