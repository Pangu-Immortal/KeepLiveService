/**
 * ===============================================================================
 * Fw Android Keep-Alive Framework - Library Module Build Configuration
 * ===============================================================================
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal
 * @buildDate 2025-12-09
 *
 * @description
 * 保活框架核心库模块构建配置
 * Build configuration for the keep-alive framework core library module
 *
 * @features
 * - 20+ 种保活策略
 * - Native C++ 守护进程
 * - 复杂混淆字典保护
 * - Android 7.0 - 16 全版本适配
 *
 * @architectures
 * - armeabi-v7a (32-bit ARM)
 * - arm64-v8a (64-bit ARM)
 * - x86 (32-bit Intel)
 * - x86_64 (64-bit Intel)
 *
 * @securityResearch
 * 本项目用于安全研究，研究商业应用的保活机制
 * This project is for security research to study keep-alive mechanisms
 *
 * ===============================================================================
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.service.framework"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        // 构建配置字段 - 用于运行时获取构建信息
        buildConfigField("String", "FW_VERSION", "\"1.0.0\"")
        buildConfigField("String", "FW_BUILD_TIME", "\"2025-12-09T${System.currentTimeMillis()}\"")
        buildConfigField("String", "FW_AUTHOR", "\"Pangu-Immortal\"")
        buildConfigField("String", "FW_GITHUB", "\"https://github.com/Pangu-Immortal\"")
        buildConfigField("int", "FW_STRATEGY_COUNT", "20")

        // NDK 配置 - 支持所有主流架构
        ndk {
            // 支持的 ABI 架构
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        // CMake 配置 - Native C++ 编译选项
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_TOOLCHAIN=clang"
                )
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false

            // Debug 配置
            buildConfigField("boolean", "FW_DEBUG", "true")
            buildConfigField("boolean", "FW_VERBOSE_LOG", "true")
        }

        release {
            // 库模块不启用混淆，由应用模块统一混淆
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Release 配置
            buildConfigField("boolean", "FW_DEBUG", "false")
            buildConfigField("boolean", "FW_VERBOSE_LOG", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = true
    }

    // 外部 Native 构建配置
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // 打包选项
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.media)
    implementation(libs.androidx.mediarouter)  // MediaRoute 保活策略依赖
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
