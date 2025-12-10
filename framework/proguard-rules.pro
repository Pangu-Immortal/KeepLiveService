# ===============================================================================
# Fw Android Keep-Alive Framework - Library Module ProGuard Rules
# ===============================================================================
#
# Author: Pangu-Immortal
# GitHub: https://github.com/Pangu-Immortal
# Build Date: 2025-12-09
#
# Description:
# Framework 库模块的混淆配置
# ProGuard rules for the Framework library module
#
# Security Research Purpose:
# 研究 Android 保活机制的安全框架
# Security research framework for studying Android keep-alive mechanisms
#
# ===============================================================================

# ==================== 基础配置 ====================

# 混淆字典配置
-obfuscationdictionary ../proguard-dictionary.txt
-classobfuscationdictionary ../proguard-dictionary.txt
-packageobfuscationdictionary ../proguard-dictionary.txt

# 优化配置
-optimizationpasses 10
-allowaccessmodification
-repackageclasses 'fw'
-flattenpackagehierarchy 'core'

# 保留源文件名和行号（调试用）
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute FwLib

# ==================== 保留公开 API ====================

# 保留框架入口点
-keep public class com.service.framework.Fw {
    public *;
}

# 保留配置类
-keep public class com.service.framework.core.FwConfig {
    public *;
}
-keep public class com.service.framework.core.FwConfig$Builder {
    public *;
}

# ==================== 保留所有 Service ====================

-keep public class * extends android.app.Service {
    public <init>();
    public void onCreate();
    public int onStartCommand(android.content.Intent, int, int);
    public android.os.IBinder onBind(android.content.Intent);
}

# ==================== 保留所有 BroadcastReceiver ====================

-keep public class * extends android.content.BroadcastReceiver {
    public <init>();
    public void onReceive(android.content.Context, android.content.Intent);
}

# ==================== 保留所有 Activity ====================

-keep public class * extends android.app.Activity {
    public <init>();
    protected void onCreate(android.os.Bundle);
}

# ==================== 保留所有 ContentProvider ====================

-keep public class * extends android.content.ContentProvider {
    public <init>();
}

# ==================== 保留账户相关类 ====================

-keep public class * extends android.accounts.AbstractAccountAuthenticator {
    public <init>(android.content.Context);
}

-keep public class * extends android.content.AbstractThreadedSyncAdapter {
    public <init>(android.content.Context, boolean);
    public <init>(android.content.Context, boolean, boolean);
}

# ==================== 保留系统服务类 ====================

-keep public class * extends android.accessibilityservice.AccessibilityService {
    public <init>();
}

-keep public class * extends android.service.notification.NotificationListenerService {
    public <init>();
}

# ==================== 保留 Native 方法 ====================

-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class com.service.framework.native.FwNative {
    *;
}

# ==================== 保留公开策略管理器 ====================

-keep public class com.service.framework.strategy.BatteryOptimizationManager {
    public *;
}

-keep public class com.service.framework.strategy.AutoStartPermissionManager {
    public *;
}

-keep public class com.service.framework.strategy.FloatWindowManager {
    public *;
}

-keep public class com.service.framework.strategy.LockScreenActivity {
    public static *;
}

-keep public class com.service.framework.strategy.OnePixelActivity {
    public static *;
}

-keep public class com.service.framework.strategy.VendorIntegrationAnalyzer {
    public *;
}

-keep public class com.service.framework.strategy.ProcessPriorityManager {
    public *;
}

# ==================== WorkManager ====================

-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ==================== Kotlin 支持 ====================

-keep class kotlin.Metadata { *; }

-keepclassmembers class ** {
    public static ** Companion;
}

-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ==================== 保留注解 ====================

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ==================== 保留枚举 ====================

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ==================== 保留 Parcelable ====================

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ==================== 保留 Serializable ====================

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ==================== 警告抑制 ====================

-dontwarn **
-ignorewarnings
