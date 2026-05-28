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

# 引入上级 native 公共头文件。
target_include_directories(fw_mediaroute PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/..
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

# ==================== Native 防护配置 ====================
# Release/RelWithDebInfo 构建仅导出 JNI_OnLoad，其他业务符号全部本地化。
target_compile_options(fw_mediaroute PRIVATE
    -Wall
    -Wextra
    -fvisibility=hidden
    -fvisibility-inlines-hidden
    -ffunction-sections
    -fdata-sections
    $<$<NOT:$<CONFIG:Debug>>:-O2>
    $<$<NOT:$<CONFIG:Debug>>:-flto>
)

target_link_options(fw_mediaroute PRIVATE
    -Wl,-z,max-page-size=16384
    -Wl,--gc-sections
    -Wl,--exclude-libs,ALL
    -Wl,--version-script=${CMAKE_CURRENT_SOURCE_DIR}/fw_mediaroute.exports
    $<$<NOT:$<CONFIG:Debug>>:-flto>
    $<$<NOT:$<CONFIG:Debug>>:-Wl,--strip-all>
)
