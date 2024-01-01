# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.
cmake_minimum_required(VERSION 3.21.1)

project(MyTV)

# Specifies a path to native header files.
include_directories(src/main/cpp/include)

if (IS_SO_BUILD)
    # Specifies a library name, specifies whether the library is STATIC or
    # SHARED, and provides relative paths to the source code. You can
    # define multiple libraries by adding multiple add_library() commands,
    # and CMake builds them for you. When you build your app, Gradle
    # automatically packages shared libraries with your APK.
    add_library( # Specifies the name of the library.
            native

            # Sets the library as a shared library.
            SHARED

            # Provides a relative path to your source file(s).
            src/main/cpp/native.c)

    # 设置编译输出路径
    set_target_properties(
            native
            PROPERTIES
            LIBRARY_OUTPUT_DIRECTORY
            ${CMAKE_SOURCE_DIR}/src/main/cpp/${ANDROID_ABI}
    )
else ()
    add_library(
            nothing
            SHARED
            src/main/cpp/nothing.c)

    add_library(native
            SHARED
            IMPORTED)

    set_target_properties( # Specifies the target library.
            native

            # Specifies the parameter you want to define.
            PROPERTIES IMPORTED_LOCATION

            # Provides the path to the library you want to import.
            ${CMAKE_SOURCE_DIR}/src/main/cpp/${ANDROID_ABI}/libnative.so)
endif ()

#add_library(libssl
#        STATIC
#        IMPORTED)
#
#set_target_properties( # Specifies the target library.
#        libssl
#
#        # Specifies the parameter you want to define.
#        PROPERTIES IMPORTED_LOCATION
#
#        # Provides the path to the library you want to import.
#        ${CMAKE_SOURCE_DIR}/src/main/cpp/${ANDROID_ABI}/libssl.so)
#
#add_library(libcrypto
#        SHARED
#        IMPORTED)
#
#set_target_properties( # Specifies the target library.
#        libcrypto
#
#        # Specifies the parameter you want to define.
#        PROPERTIES IMPORTED_LOCATION
#
#        # Provides the path to the library you want to import.
#        ${CMAKE_SOURCE_DIR}/src/main/cpp/${ANDROID_ABI}/libcrypto.so)

add_library(libssl
        STATIC
        IMPORTED)

set_target_properties( # Specifies the target library.
        libssl

        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION

        # Provides the path to the library you want to import.
        ${CMAKE_SOURCE_DIR}/src/main/cpp/${ANDROID_ABI}/libssl.a)

add_library(libcrypto
        STATIC
        IMPORTED)

set_target_properties( # Specifies the target library.
        libcrypto

        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION

        # Provides the path to the library you want to import.
        ${CMAKE_SOURCE_DIR}/src/main/cpp/${ANDROID_ABI}/libcrypto.a)

find_library( # Defines the name of the path variable that stores the
        # location of the NDK library.
        log-lib

        # Specifies the name of the NDK library that
        # CMake needs to locate.
        log)

if (IS_SO_BUILD)
    # Links your native library against one or more other native libraries.
    target_link_libraries( # Specifies the target library.
            native
            libssl
            libcrypto
            ${log-lib})
else ()
    target_link_libraries( # Specifies the target library.
            nothing
            libssl
            libcrypto
            native
            ${log-lib})
endif ()