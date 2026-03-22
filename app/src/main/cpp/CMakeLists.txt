cmake_minimum_required(VERSION 3.22.1)
project(proxmark_native C)

# libusb sources for Android (Linux usbfs backend, no udev)
set(LIBUSB_SOURCES
    libusb/core.c
    libusb/descriptor.c
    libusb/hotplug.c
    libusb/io.c
    libusb/strerror.c
    libusb/sync.c
    libusb/os/linux_usbfs.c
    libusb/os/linux_netlink.c
    libusb/os/events_posix.c
    libusb/os/threads_posix.c
)

# JNI wrapper
set(JNI_SOURCES
    usb_native.c
)

add_library(proxmark_native SHARED
    ${LIBUSB_SOURCES}
    ${JNI_SOURCES}
)

# config.h lives in libusb/ dir, libusb headers also there
target_include_directories(proxmark_native PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/libusb
    ${CMAKE_CURRENT_SOURCE_DIR}/libusb/os
)

target_compile_definitions(proxmark_native PRIVATE
    _GNU_SOURCE
    HAVE_CONFIG_H
)

# Link Android log and pthread
find_library(log-lib log)
target_link_libraries(proxmark_native ${log-lib})
