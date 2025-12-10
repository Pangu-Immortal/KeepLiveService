import java.text.SimpleDateFormat
import java.util.Date
import java.io.File

/**
 * ===============================================================================
 * Fw Android Keep-Alive Framework - App Module Build Configuration
 * ===============================================================================
 *
 * @author https://github.com/Pangu-Immortal/KeepAlivePerfect
 * @github https://github.com/Pangu-Immortal
 * @buildDate 2025-12-09
 *
 * @description
 * 保活框架示例应用模块构建配置
 * Build configuration for the keep-alive framework demo application
 *
 * @features
 * - 复杂混淆字典保护
 * - 详细日志输出
 * - Native C++ 支持
 * - Android 7.0 - 16 全版本适配
 *
 * @securityResearch
 * 本项目用于安全研究，研究商业应用的保活机制
 * This project is for security research to study keep-alive mechanisms
 *
 * ===============================================================================
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.google.services"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.google.services"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 构建配置字段 - 用于运行时获取构建信息
        buildConfigField("String", "BUILD_TIME", "\"2025-12-09T${System.currentTimeMillis()}\"")
        buildConfigField("String", "AUTHOR", "\"https://github.com/Pangu-Immortal/KeepAlivePerfect\"")
        buildConfigField("String", "AUTHOR_GITHUB", "\"https://github.com/Pangu-Immortal\"")
        buildConfigField("String", "PROJECT_DESC", "\"Android Keep-Alive Security Research Framework\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true

            // Debug 构建信息
            buildConfigField("boolean", "ENABLE_VERBOSE_LOG", "true")
            buildConfigField("String", "BUILD_TYPE_DESC", "\"Debug Build - Full Logging Enabled\"")
        }

        release {
            // 启用混淆
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // 使用 debug 签名以便于测试
            signingConfig = signingConfigs.getByName("debug")

            // Release 构建信息
            buildConfigField("boolean", "ENABLE_VERBOSE_LOG", "false")
            buildConfigField("String", "BUILD_TYPE_DESC", "\"Release Build - Obfuscated\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // 打包选项
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// 注意：proguard-dictionary.txt 已经手动配置好了，不需要自动生成
// 如果需要重新生成，可以手动运行 generateProguardDictionary task
val generateProguardDictionary by tasks.register("generateProguardDictionary") {
    group = "Build"
    description = "Generates a new random ProGuard dictionary (WARNING: overwrites existing dictionary)"
    doLast {
        val dictionaryFile = rootProject.file("proguard-dictionary-generated.txt")
        dictionaryFile.writer().use { writer ->
            val chars = ('a'..'z') + ('A'..'Z')
            writer.appendLine("# Auto-generated ProGuard dictionary on ${Date()}")
            repeat(1000) {
                val randomWord = (1..16).map { chars.random() }.joinToString("")
                writer.appendLine(randomWord)
            }
        }
        println("SUCCESS: Generated new random Proguard dictionary at ${dictionaryFile.path}")
        println("NOTE: To use this dictionary, manually replace proguard-dictionary.txt")
    }
}

// Register a new task with a UNIQUE name
tasks.register("buildTimestampedReleaseApk") {
    group = "Build"
    description = "Builds release APK with timestamp and copies to root/release directory."
    dependsOn("assembleRelease")

    doLast {
        val buildOutputDir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
        val mappingDir = layout.buildDirectory.dir("outputs/mapping/release").get().asFile
        val originalApk = File(buildOutputDir, "app-release.apk")
        val originalMapping = File(mappingDir, "mapping.txt")

        // 创建根目录下的 release 文件夹
        val releaseDir = rootProject.file("release")
        if (!releaseDir.exists()) {
            releaseDir.mkdirs()
        }

        if (originalApk.exists()) {
            // 时间戳格式：yyyyMMddHHmm
            val timestamp = SimpleDateFormat("yyyyMMddHHmm").format(Date())
            val newApk = File(releaseDir, "app-$timestamp.apk")
            val newMapping = File(releaseDir, "mapping-$timestamp.txt")

            // 删除已存在的同名文件
            if (newApk.exists()) newApk.delete()
            if (newMapping.exists()) newMapping.delete()

            // 复制 APK 到 release 目录
            originalApk.copyTo(newApk, overwrite = true)
            println("SUCCESS: APK copied to ${newApk.path}")

            // 复制 mapping 文件（如果存在）
            if (originalMapping.exists()) {
                originalMapping.copyTo(newMapping, overwrite = true)
                println("SUCCESS: Mapping copied to ${newMapping.path}")
            }
        } else {
            println("ERROR: 'app-release.apk' not found in $buildOutputDir. The build might have failed.")
        }
    }
}

dependencies {
    // Framework 模块
    implementation(project(":framework"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    implementation(libs.androidx.media)
    implementation(libs.androidx.lifecycle.service)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
