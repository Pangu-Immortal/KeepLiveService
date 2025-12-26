/**
 * ============================================================================
 * ContentObserverManager.kt - 内容观察者管理器
 * ============================================================================
 *
 * 功能简介：
 *   监听各种 ContentProvider 的数据变化来拉活应用。
 *   支持监听相册、联系人、短信、系统设置等数据变化。
 *   使用节流机制避免频繁触发。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.observer

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony
import com.service.framework.Fw
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * 内容观察者管理器
 *
 * 监听各种 ContentProvider 的数据变化来拉活应用
 *
 * 安全研究要点：
 * - ContentObserver 可以监听系统数据库的变化
 * - 相册、联系人、短信等数据变化都可以触发
 * - 这种方式需要应用进程存活才能工作
 * - 强制停止后，ContentObserver 也会被取消
 * - 但如果应用被系统回收（而非强制停止），重新唤醒后可以重新注册
 */
object ContentObserverManager {

    private val observers = mutableListOf<ContentObserver>()
    private val handler = Handler(Looper.getMainLooper())

    /**
     * 注册所有观察者
     */
    fun registerAll(context: Context) {
        val config = Fw.config ?: return

        // 相册变化监听
        if (config.enableMediaContentObserver) {
            registerMediaObserver(context)
        }

        // 联系人变化监听
        if (config.enableContactsContentObserver) {
            registerContactsObserver(context)
        }

        // 短信变化监听
        if (config.enableSmsContentObserver) {
            registerSmsObserver(context)
        }

        // 设置变化监听
        if (config.enableSettingsContentObserver) {
            registerSettingsObserver(context)
        }
    }

    /**
     * 取消所有观察者
     */
    fun unregisterAll(context: Context) {
        val contentResolver = context.contentResolver
        observers.forEach { observer ->
            try {
                contentResolver.unregisterContentObserver(observer)
            } catch (e: Exception) {
                FwLog.e("取消观察者失败: ${e.message}", e)
            }
        }
        observers.clear()
        FwLog.d("所有 ContentObserver 已取消")
    }

    /**
     * 注册相册变化观察者
     *
     * 监听相册数据变化（拍照、截图、保存图片等）
     */
    private fun registerMediaObserver(context: Context) {
        try {
            val observer = object : ContentObserver(handler) {
                private var lastTriggerTime = 0L
                private val throttleInterval = 5000L // 5秒节流

                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    val now = System.currentTimeMillis()
                    if (now - lastTriggerTime < throttleInterval) {
                        return // 节流，避免频繁触发
                    }
                    lastTriggerTime = now

                    FwLog.d("相册变化: $uri")
                    ServiceStarter.startForegroundService(context, "相册变化")
                }
            }

            // 监听图片变化
            context.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )

            // 监听视频变化
            context.contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )

            // 监听音频变化
            context.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )

            observers.add(observer)
            FwLog.d("相册观察者注册成功")
        } catch (e: Exception) {
            FwLog.e("注册相册观察者失败: ${e.message}", e)
        }
    }

    /**
     * 注册联系人变化观察者
     */
    private fun registerContactsObserver(context: Context) {
        try {
            val observer = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    FwLog.d("联系人变化: $uri")
                    ServiceStarter.startForegroundService(context, "联系人变化")
                }
            }

            context.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                observer
            )

            observers.add(observer)
            FwLog.d("联系人观察者注册成功")
        } catch (e: Exception) {
            FwLog.e("注册联系人观察者失败: ${e.message}", e)
        }
    }

    /**
     * 注册短信变化观察者
     */
    private fun registerSmsObserver(context: Context) {
        try {
            val observer = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    FwLog.d("短信变化: $uri")
                    ServiceStarter.startForegroundService(context, "短信变化")
                }
            }

            context.contentResolver.registerContentObserver(
                Telephony.Sms.CONTENT_URI,
                true,
                observer
            )

            observers.add(observer)
            FwLog.d("短信观察者注册成功")
        } catch (e: Exception) {
            FwLog.e("注册短信观察者失败: ${e.message}", e)
        }
    }

    /**
     * 注册设置变化观察者
     *
     * 监听系统设置变化（亮度、音量、WiFi 等）
     */
    private fun registerSettingsObserver(context: Context) {
        try {
            val observer = object : ContentObserver(handler) {
                private var lastTriggerTime = 0L
                private val throttleInterval = 3000L // 3秒节流

                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    val now = System.currentTimeMillis()
                    if (now - lastTriggerTime < throttleInterval) {
                        return
                    }
                    lastTriggerTime = now

                    FwLog.d("系统设置变化: $uri")
                    ServiceStarter.startForegroundService(context, "设置变化")
                }
            }

            // 监听系统设置
            context.contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                observer
            )

            // 监听全局设置
            context.contentResolver.registerContentObserver(
                Settings.Global.CONTENT_URI,
                true,
                observer
            )

            // 监听安全设置
            context.contentResolver.registerContentObserver(
                Settings.Secure.CONTENT_URI,
                true,
                observer
            )

            observers.add(observer)
            FwLog.d("设置观察者注册成功")
        } catch (e: Exception) {
            FwLog.e("注册设置观察者失败: ${e.message}", e)
        }
    }
}
