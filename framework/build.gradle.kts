import org.gradle.api.GradleException
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec
import java.util.Properties

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
    `maven-publish`
    signing
}

data class FwRustAndroidTarget(
    val abi: String,
    val rustTriple: String,
    val linkerPrefix: String
)

val fwRustAndroidTargets = listOf(
    FwRustAndroidTarget("armeabi-v7a", "armv7-linux-androideabi", "armv7a-linux-androideabi"),
    FwRustAndroidTarget("arm64-v8a", "aarch64-linux-android", "aarch64-linux-android"),
    FwRustAndroidTarget("x86", "i686-linux-android", "i686-linux-android"),
    FwRustAndroidTarget("x86_64", "x86_64-linux-android", "x86_64-linux-android")
)
val fwAndroidNdkVersion = "27.2.12479018"
val fwBuildRust = providers.gradleProperty("fwBuildRust")
    .map { value -> value.equals("true", ignoreCase = true) }
    .orElse(false)
val fwRustManifest = layout.projectDirectory.file("src/main/rust/fw_rust/Cargo.toml")
val fwRustTargetDir = layout.buildDirectory.dir("rust/target")
val fwRustJniDir = layout.buildDirectory.dir("generated/rustJniLibs")
val fwRustMinSdk = 24

fun String.toFwTaskSuffix(): String =
    split("-", "_").joinToString("") { part ->
        part.replaceFirstChar { char -> char.uppercase() }
    }

fun rustLinkerEnvName(rustTriple: String): String =
    "CARGO_TARGET_${rustTriple.uppercase().replace("-", "_")}_LINKER"

android {
    namespace = "com.service.framework"
    compileSdk = 36
    ndkVersion = fwAndroidNdkVersion

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        // 构建配置字段 - 用于运行时获取构建信息
        buildConfigField("String", "FW_VERSION", "\"2.0.1\"")
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

androidComponents {
    onVariants(selector().all()) { variant ->
        // 只有显式开启 Rust 构建时才把生成目录加入 jniLibs，避免默认构建误打包旧 so。
        if (fwBuildRust.get()) {
            variant.sources.jniLibs?.addStaticSourceDirectory(
                fwRustJniDir.get().asFile.absolutePath
            )
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

// ==================== Rust Native 构建配置 ====================

fun resolveAndroidSdkDir(): File {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        val properties = Properties()
        localPropertiesFile.inputStream().use { input -> properties.load(input) }
        val sdkDir = properties.getProperty("sdk.dir")
        if (!sdkDir.isNullOrBlank()) {
            return File(sdkDir)
        }
    }
    val envSdkDir = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
    if (!envSdkDir.isNullOrBlank()) {
        return File(envSdkDir)
    }
    throw GradleException("未找到 Android SDK 目录，请配置 local.properties 的 sdk.dir 或 ANDROID_HOME")
}

fun resolveExecutable(command: String): File? {
    val commandFile = File(command)
    if (commandFile.isAbsolute || command.contains(File.separator)) {
        return commandFile.takeIf { file -> file.canExecute() }
    }
    return System.getenv("PATH")
        .orEmpty()
        .split(File.pathSeparator)
        .asSequence()
        .filter { path -> path.isNotBlank() }
        .map { path -> File(path, command) }
        .firstOrNull { file -> file.canExecute() }
}

fun resolveNdkToolchainBin(): File {
    val ndkDir = File(resolveAndroidSdkDir(), "ndk/$fwAndroidNdkVersion")
    val prebuiltDir = File(ndkDir, "toolchains/llvm/prebuilt")
    val hostCandidates = listOf("darwin-x86_64", "darwin-aarch64", "linux-x86_64", "windows-x86_64")
    val hostDir = hostCandidates
        .map { hostName -> File(prebuiltDir, hostName) }
        .firstOrNull { candidate -> candidate.isDirectory }
        ?: throw GradleException("未找到 Android NDK LLVM 工具链目录：${prebuiltDir.absolutePath}")
    return File(hostDir, "bin")
}

tasks.register<Delete>("cleanFwRustAndroid") {
    group = "rust"
    description = "清理 Fw Rust Native 产物"
    delete(fwRustTargetDir, fwRustJniDir)
}

tasks.register("checkFwRustToolchain") {
    group = "rust"
    description = "检查 Rust、Cargo 与 Android NDK 链接器是否可用"
    doLast {
        val cargoExecutable = findProperty("fwCargoPath") as String? ?: "cargo"
        val cargoFile = resolveExecutable(cargoExecutable)
            ?: throw GradleException("未找到 Cargo 可执行文件，请安装 Rust 或通过 -PfwCargoPath 指定 cargo 路径")
        val rustcExecutable = findProperty("fwRustcPath") as String? ?: File(cargoFile.parentFile, "rustc").absolutePath
        resolveExecutable(rustcExecutable)
            ?: throw GradleException("未找到 rustc 可执行文件，请安装 Rust 或通过 -PfwRustcPath 指定 rustc 路径")
        val toolchainBin = resolveNdkToolchainBin()
        fwRustAndroidTargets.forEach { target ->
            val linker = File(toolchainBin, "${target.linkerPrefix}${fwRustMinSdk}-clang")
            if (!linker.canExecute()) {
                throw GradleException("Rust Android 链接器不可执行：${linker.absolutePath}")
            }
        }
    }
}

val copyFwRustTasks = fwRustAndroidTargets.map { target ->
    val suffix = target.abi.toFwTaskSuffix()
    val buildTask = tasks.register<Exec>("buildFwRust$suffix") {
        group = "rust"
        description = "构建 ${target.abi} 对应的 libfw_rust.so"
        onlyIf { fwBuildRust.get() }
        workingDir = fwRustManifest.asFile.parentFile
        executable = findProperty("fwCargoPath") as String? ?: "cargo"
        args(
            "build",
            "--manifest-path",
            fwRustManifest.asFile.absolutePath,
            "--target",
            target.rustTriple,
            "--release"
        )
        doFirst {
            if (!fwRustManifest.asFile.isFile) {
                throw GradleException("Rust manifest 不存在：${fwRustManifest.asFile.absolutePath}")
            }
            val configuredCargo = findProperty("fwCargoPath") as String? ?: "cargo"
            val cargoFile = resolveExecutable(configuredCargo)
                ?: throw GradleException("未找到 Cargo 可执行文件，请安装 Rust 或通过 -PfwCargoPath 指定 cargo 路径")
            val rustcExecutable = findProperty("fwRustcPath") as String? ?: File(cargoFile.parentFile, "rustc").absolutePath
            val rustcFile = resolveExecutable(rustcExecutable)
                ?: throw GradleException("未找到 rustc 可执行文件，请安装 Rust 或通过 -PfwRustcPath 指定 rustc 路径")
            val toolchainBin = resolveNdkToolchainBin()
            val linker = File(toolchainBin, "${target.linkerPrefix}${fwRustMinSdk}-clang")
            if (!linker.canExecute()) {
                throw GradleException("Rust Android 链接器不可执行：${linker.absolutePath}")
            }
            val baseRustFlags = System.getenv("RUSTFLAGS").orEmpty()
            val androidRustFlags = "-C link-arg=-Wl,-z,max-page-size=16384 -C link-arg=-Wl,--gc-sections"
            val rustPath = listOf(cargoFile.parentFile.absolutePath, System.getenv("PATH").orEmpty())
                .filter { value -> value.isNotBlank() }
                .joinToString(File.pathSeparator)
            environment("CARGO_TARGET_DIR", fwRustTargetDir.get().asFile.absolutePath)
            environment(rustLinkerEnvName(target.rustTriple), linker.absolutePath)
            environment("PATH", rustPath)
            environment("RUSTC", rustcFile.absolutePath)
            environment(
                "RUSTFLAGS",
                listOf(baseRustFlags, androidRustFlags).filter { value -> value.isNotBlank() }.joinToString(" ")
            )
            logger.lifecycle("Fw Rust: 构建 ${target.abi}，target=${target.rustTriple}")
        }
    }

    tasks.register<Copy>("copyFwRust$suffix") {
        group = "rust"
        description = "复制 ${target.abi} 的 libfw_rust.so 到 Android jniLibs 产物目录"
        onlyIf { fwBuildRust.get() }
        dependsOn(buildTask)
        from(fwRustTargetDir.map { targetDir ->
            targetDir.file("${target.rustTriple}/release/libfw_rust.so")
        })
        into(fwRustJniDir.map { jniDir -> jniDir.dir(target.abi) })
        rename { "libfw_rust.so" }
    }
}

val buildFwRustAndroid = tasks.register("buildFwRustAndroid") {
    group = "rust"
    description = "构建并打包 4 个 Android ABI 的 Rust Native 骨架库"
    onlyIf { fwBuildRust.get() }
    dependsOn(copyFwRustTasks)
    doFirst {
        logger.lifecycle("Fw Rust: 已启用 -PfwBuildRust=true，将把 libfw_rust.so 纳入 AAR")
    }
}

tasks.matching { task ->
    task.name.startsWith("merge") && task.name.endsWith("JniLibFolders")
}.configureEach {
    if (fwBuildRust.get()) {
        dependsOn(buildFwRustAndroid)
    }
}

// ==================== Maven Central 发布配置 ====================

// 从 gradle.properties 或 ~/.gradle/gradle.properties 读取发布参数
val libGroupId: String = findProperty("LIB_GROUP_ID") as String? ?: "io.github.pangu-immortal"
val libArtifactId: String = findProperty("LIB_ARTIFACT_ID") as String? ?: "keeplive-framework"
val libVersion: String = findProperty("LIB_VERSION") as String? ?: "2.0.1"

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
                    description.set("Android 保活技术百科全书 — 35+ 种保活策略，Native C++ 守护进程，统一体外 Activity / startActivity 策略，适配 Android 7.0-16 全版本，覆盖 10+ 厂商 ROM")
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
            // Maven Central 发布地址：使用 Central Portal 兼容 OSSRH Staging API 的新端点。
            maven {
                name = "sonatype"
                val releasesUrl = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
                val snapshotsUrl = uri("https://central.sonatype.com/repository/maven-snapshots/")
                url = if (libVersion.endsWith("SNAPSHOT")) snapshotsUrl else releasesUrl
                credentials {
                    username = findProperty("ossrhUsername") as String? ?: ""
                    password = findProperty("ossrhPassword") as String? ?: ""
                }
            }
        }
    }

    // GPG 签名：发布任务必须签名，普通本地构建不强制检查签名环境。
    signing {
        isRequired = gradle.startParameter.taskNames.any { taskName ->
            taskName.contains("publish", ignoreCase = true) ||
                taskName.contains("checkSigningConfiguration", ignoreCase = true)
        }
        useGpgCmd()
        sign(publishing.publications)
    }
}
