cmake_minimum_required(VERSION 3.22.1)

project("starter")

add_executable(starter
        starter/starter.c)

set_target_properties(starter PROPERTIES PREFIX "lib")
set_target_properties(starter PROPERTIES SUFFIX ".so")
