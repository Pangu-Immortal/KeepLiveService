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
    `maven-publish`
    signing
}

android {
    namespace = "com.service.framework"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        // 构建配置字段 - 用于运行时获取构建信息
        buildConfigField("String", "FW_VERSION", "\"2.0.0\"")
        buildConfigField("String", "FW_BUILD_TIME", "\"2026-02-20T${System.currentTimeMillis()}\"")
        buildConfigField("String", "FW_AUTHOR", "\"Pangu-Immortal\"")
        buildConfigField("String", "FW_GITHUB", "\"https://github.com/Pangu-Immortal\"")
        buildConfigField("int", "FW_STRATEGY_COUNT", "35")

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

    // Maven Central 发布配置：仅发布 release 变体，附带源码和文档
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
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

// ==================== Maven Central 发布配置 ====================

// 从 gradle.properties 或 ~/.gradle/gradle.properties 读取发布参数
val libGroupId: String = findProperty("LIB_GROUP_ID") as String? ?: "io.github.pangu-immortal"
val libArtifactId: String = findProperty("LIB_ARTIFACT_ID") as String? ?: "keeplive-framework"
val libVersion: String = findProperty("LIB_VERSION") as String? ?: "2.0.0"

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = libGroupId
                artifactId = libArtifactId
                version = libVersion

                pom {
                    name.set("KeepLive Framework")
                    description.set("Android 保活技术百科全书 — 35+ 种保活策略，Native C++ 守护进程，适配 Android 7.0-16 全版本，覆盖 10+ 厂商 ROM")
                    url.set("https://github.com/Pangu-Immortal/KeepLiveService")
                    inceptionYear.set("2024")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("Pangu-Immortal")
                            name.set("Pangu-Immortal")
                            url.set("https://github.com/Pangu-Immortal")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/Pangu-Immortal/KeepLiveService.git")
                        developerConnection.set("scm:git:ssh://github.com:Pangu-Immortal/KeepLiveService.git")
                        url.set("https://github.com/Pangu-Immortal/KeepLiveService")
                    }
                }
            }
        }

        repositories {
            // Maven Central（Sonatype OSSRH）
            maven {
                name = "sonatype"
                val releasesUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                val snapshotsUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                url = if (libVersion.endsWith("SNAPSHOT")) snapshotsUrl else releasesUrl
                credentials {
                    username = findProperty("ossrhUsername") as String? ?: ""
                    password = findProperty("ossrhPassword") as String? ?: ""
                }
            }
        }
    }

    // GPG 签名（Maven Central 强制要求，本地测试时可跳过）
    val hasSigningKey = findProperty("signing.gnupg.keyName") != null
    if (hasSigningKey) {
        signing {
            useGpgCmd()
            sign(publishing.publications["release"])
        }
    }
}
