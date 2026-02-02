# ============================================================================
# CMakeLists.txt - Native 库构建配置
# ============================================================================
#
# 功能简介：
#   配置 fw_native 共享库的 CMake 构建脚本，包括：
#   - 基础功能：守护进程、进程管理、Socket 通信、JNI 接口
#   - 无法强制停止策略：Binder 直接调用、Parcel 数据容器
#
# @author Pangu-Immortal
# @github https://github.com/Pangu-Immortal/KeepLiveService
# @since 2.1.0
# ============================================================================

cmake_minimum_required(VERSION 3.22.1)

project("fw_native")

# 设置 C++ 标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 添加编译选项
add_compile_options(-Wall -Wextra -fvisibility=hidden)

# ==================== Android 16K Page Size Support ====================
# Add linker flags for 16KB page size alignment (Android 15+)
# This ensures the native library works on devices with 16KB page sizes
# https://developer.android.com/guide/practices/page-sizes
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

# ==================== 头文件包含路径 ====================
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/binder
    ${CMAKE_CURRENT_SOURCE_DIR}/utils
)

# ==================== 源文件列表 ====================

# 基础功能源文件
set(FW_BASIC_SOURCES
    fw_daemon.cpp
    fw_process.cpp
    fw_socket.cpp
    fw_jni.cpp
)

# 无法强制停止策略相关源文件（教学用途）
# 复现 Android 5.0-16.0 时代的保活技术
set(FW_FORCE_STOP_SOURCES
    fw_force_stop.cpp
    binder/cParcel.cpp
    binder/data_transact.cpp
    utils/SharedBuffer.cpp
    utils/String16.cpp
    utils/Unicode.cpp
)

# 创建共享库
add_library(fw_native SHARED
    ${FW_BASIC_SOURCES}
    ${FW_FORCE_STOP_SOURCES}
)

# 查找系统库
find_library(log-lib log)
find_library(android-lib android)

# 链接库
target_link_libraries(fw_native
    ${log-lib}
    ${android-lib}
)

# ==================== MediaRoute 子模块 ====================
# 添加 MediaRoute 保活模块（与主模块隔离）
add_subdirectory(mediaroute)
