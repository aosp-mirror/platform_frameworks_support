cmake_minimum_required(VERSION 3.4.1)

add_library(jni-lib SHARED jni-lib.c)

target_link_libraries(jni-lib android log)