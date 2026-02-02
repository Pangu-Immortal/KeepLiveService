# ============================================================================
# CMakeLists.txt - MediaRoute 模块 CMake 配置
# ============================================================================
#
# 功能简介：
#   构建 fw_mediaroute native 库，用于 MediaRoute 保活模块
#
# @author Pangu-Immortal
# @github https://github.com/Pangu-Immortal/KeepLiveService
# @since 2.2.0
# ============================================================================

cmake_minimum_required(VERSION 3.22.1)

# 设置项目名称
project("fw_mediaroute")

# 添加 fw_mediaroute 库
add_library(
    fw_mediaroute  # 库名称
    SHARED         # 共享库
    fw_mediaroute_jni.cpp  # 源文件
)

# 链接 Android 日志库
target_link_libraries(
    fw_mediaroute
    log
)

# 设置 C++ 标准
set_target_properties(fw_mediaroute PROPERTIES
    CXX_STANDARD 17
    CXX_STANDARD_REQUIRED ON
)
