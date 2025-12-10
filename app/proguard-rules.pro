# ===============================================================================
# Fw Android Keep-Alive Framework - App Module ProGuard Rules
# ===============================================================================
#
# Author: Pangu-Immortal
# GitHub: https://github.com/Pangu-Immortal
# Build Date: 2025-12-09
#
# Description:
# 高强度混淆配置，用于保护保活框架的实现细节
# High-strength obfuscation configuration to protect keep-alive framework
#
# Security Research Purpose:
# 研究商业应用如何通过混淆保护其保活机制
# Study how commercial apps protect their keep-alive mechanisms through obfuscation
#
# ===============================================================================

# ==================== 基础优化配置 ====================

# 启用优化
-optimizationpasses 10
-allowaccessmodification
-repackageclasses 'o'
-flattenpackagehierarchy 'p'

# 混淆字典配置 - 使用复杂的混淆名称
-obfuscationdictionary ../proguard-dictionary.txt
-classobfuscationdictionary ../proguard-dictionary.txt
-packageobfuscationdictionary ../proguard-dictionary.txt

# 保留行号信息用于调试
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute FwSecure

# 移除日志（Release 模式）
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# ==================== 保留必要的类 ====================

# 保留 Application 入口
-keep public class * extends android.app.Application {
    public <init>();
    public void onCreate();
}

# 保留 Activity
-keep public class * extends android.app.Activity {
    public <init>();
}

# 保留 Service
-keep public class * extends android.app.Service {
    public <init>();
}

# 保留 BroadcastReceiver
-keep public class * extends android.content.BroadcastReceiver {
    public <init>();
}

# 保留 ContentProvider
-keep public class * extends android.content.ContentProvider {
    public <init>();
}

# 保留 Native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留 Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留 Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保留枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留注解
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ==================== Kotlin 相关配置 ====================

# 保留 Kotlin Metadata
-keep class kotlin.Metadata { *; }

# 保留 Kotlin 协程
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# 保留 Kotlin 伴生对象
-keepclassmembers class ** {
    public static ** Companion;
}

# ==================== AndroidX 配置 ====================

# WorkManager
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Lifecycle
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# ==================== 框架特定保留 ====================

# 保留 Fw 框架入口（需要从外部调用）
-keep class com.service.framework.Fw {
    public *;
}

# 保留配置类（Builder 模式需要）
-keep class com.service.framework.core.FwConfig {
    public *;
}
-keep class com.service.framework.core.FwConfig$Builder {
    public *;
}

# 保留公开的管理器类
-keep class com.service.framework.strategy.BatteryOptimizationManager {
    public *;
}
-keep class com.service.framework.strategy.AutoStartPermissionManager {
    public *;
}
-keep class com.service.framework.strategy.FloatWindowManager {
    public *;
}
-keep class com.service.framework.strategy.LockScreenActivity {
    public *;
}
-keep class com.service.framework.strategy.VendorIntegrationAnalyzer {
    public *;
}
-keep class com.service.framework.strategy.ProcessPriorityManager {
    public *;
}

# 保留 Native JNI 接口
-keep class com.service.framework.native.FwNative {
    public *;
    native <methods>;
}

# ==================== 删除调试信息 ====================

# 删除 debug 和 verbose 日志
-assumenosideeffects class com.service.framework.util.FwLog {
    public static void v(...);
    public static void d(...);
}

# ==================== 优化选项 ====================

# 删除未使用的代码
-dontwarn **
-ignorewarnings

# 启用激进优化
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
