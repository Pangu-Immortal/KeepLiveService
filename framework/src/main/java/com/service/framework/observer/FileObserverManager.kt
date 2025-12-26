/**
 * ============================================================================
 * FileObserverManager.kt - 文件系统观察者管理器
 * ============================================================================
 *
 * 功能简介：
 *   基于 Linux inotify 的文件监听机制，监听文件系统变化。
 *   支持监听下载目录、相册目录、截图目录、文档目录等。
 *   文件创建/修改/删除时触发回调，可作为辅助保活手段。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.observer

import android.os.Build
import android.os.Environment
import android.os.FileObserver
import com.service.framework.Fw
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter
import java.io.File

/**
 * 文件系统观察者管理器
 *
 * 核心机制：
 * 1. 使用 FileObserver 监听文件系统变化
 * 2. 监听关键目录（下载、相册、文档等）
 * 3. 文件创建/修改/删除时触发回调
 *
 * 安全研究要点：
 * - FileObserver 是基于 Linux inotify 的文件监听机制
 * - 可以监听文件的创建、删除、修改、移动等操作
 * - 用户下载文件、拍照、截图都会触发
 * - 需要应用进程活着才能工作
 * - 但可以作为辅助保活手段
 *
 * 使用场景：
 * - 文件同步应用（Dropbox、Google Drive）
 * - 相册自动备份
 * - 下载管理器
 * - 文件管理器
 *
 * 注意：
 * - Android 10+ 的 Scoped Storage 限制了对外部存储的访问
 * - 需要 READ_EXTERNAL_STORAGE 或 MANAGE_EXTERNAL_STORAGE 权限
 */
object FileObserverManager {

    private val observers = mutableListOf<FileObserver>()
    private var isRegistered = false

    // 节流控制：避免频繁触发
    private var lastTriggerTime = 0L
    private const val THROTTLE_INTERVAL = 5000L // 5秒节流

    /**
     * 注册所有文件观察者
     */
    fun registerAll(context: android.content.Context) {
        if (isRegistered) {
            FwLog.d("FileObserverManager: 已注册，跳过")
            return
        }

        val config = Fw.config ?: return

        if (config.enableFileObserver) {
            FwLog.d("FileObserverManager: 开始注册文件观察者...")

            // 监听下载目录
            registerDownloadObserver(context)

            // 监听相册目录
            registerDcimObserver(context)

            // 监听截图目录
            registerScreenshotObserver(context)

            // 监听文档目录
            registerDocumentObserver(context)

            // 监听应用私有目录（用于跨进程通信）
            registerPrivateObserver(context)

            isRegistered = true
            FwLog.d("FileObserverManager: 文件观察者注册完成，共 ${observers.size} 个")
        }
    }

    /**
     * 注销所有文件观察者
     */
    fun unregisterAll() {
        FwLog.d("FileObserverManager: 注销所有文件观察者...")

        observers.forEach { observer ->
            try {
                observer.stopWatching()
            } catch (e: Exception) {
                FwLog.e("FileObserverManager: 停止观察者失败 - ${e.message}", e)
            }
        }
        observers.clear()
        isRegistered = false

        FwLog.d("FileObserverManager: 文件观察者已全部注销")
    }

    /**
     * 监听下载目录
     *
     * 用户下载文件时触发
     */
    private fun registerDownloadObserver(context: android.content.Context) {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        registerObserver(context, downloadDir, "下载目录")
    }

    /**
     * 监听相册目录
     *
     * 用户拍照、保存图片时触发
     */
    private fun registerDcimObserver(context: android.content.Context) {
        val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        registerObserver(context, dcimDir, "相册目录")

        // 还有 Pictures 目录
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        registerObserver(context, picturesDir, "图片目录")
    }

    /**
     * 监听截图目录
     *
     * 用户截图时触发（不同厂商路径可能不同）
     */
    private fun registerScreenshotObserver(context: android.content.Context) {
        // 标准截图目录
        val screenshotDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Screenshots"
        )
        registerObserver(context, screenshotDir, "截图目录")

        // 小米截图目录
        val miuiScreenshotDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "Screenshots"
        )
        registerObserver(context, miuiScreenshotDir, "MIUI截图目录")
    }

    /**
     * 监听文档目录
     *
     * 用户保存文档时触发
     */
    private fun registerDocumentObserver(context: android.content.Context) {
        val documentDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        registerObserver(context, documentDir, "文档目录")
    }

    /**
     * 监听应用私有目录
     *
     * 用于守护进程之间的通信
     * 主进程和守护进程可以通过写入/读取特定文件来感知对方存活
     */
    private fun registerPrivateObserver(context: android.content.Context) {
        val privateDir = context.getExternalFilesDir(null)
        if (privateDir != null) {
            registerObserver(context, privateDir, "私有目录")
        }

        // 内部存储私有目录（不需要权限）
        val internalDir = context.filesDir
        registerObserver(context, internalDir, "内部私有目录")
    }

    /**
     * 注册单个目录的观察者
     */
    private fun registerObserver(context: android.content.Context, dir: File, dirName: String) {
        if (!dir.exists()) {
            FwLog.d("FileObserverManager: $dirName 不存在，跳过 - ${dir.absolutePath}")
            return
        }

        try {
            val observer = createFileObserver(context, dir.absolutePath, dirName)
            observer.startWatching()
            observers.add(observer)
            FwLog.d("FileObserverManager: 已监听 $dirName - ${dir.absolutePath}")
        } catch (e: Exception) {
            FwLog.e("FileObserverManager: 监听 $dirName 失败 - ${e.message}", e)
        }
    }

    /**
     * 创建 FileObserver
     *
     * 监听事件类型：
     * - CREATE: 文件创建
     * - DELETE: 文件删除
     * - MODIFY: 文件修改
     * - MOVED_FROM: 文件移出
     * - MOVED_TO: 文件移入
     * - CLOSE_WRITE: 文件写入后关闭
     */
    @Suppress("DEPRECATION")
    private fun createFileObserver(
        context: android.content.Context,
        path: String,
        dirName: String
    ): FileObserver {
        val eventMask = FileObserver.CREATE or
                FileObserver.DELETE or
                FileObserver.MODIFY or
                FileObserver.MOVED_FROM or
                FileObserver.MOVED_TO or
                FileObserver.CLOSE_WRITE

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(File(path), eventMask) {
                override fun onEvent(event: Int, path: String?) {
                    handleFileEvent(context, dirName, event, path)
                }
            }
        } else {
            object : FileObserver(path, eventMask) {
                override fun onEvent(event: Int, path: String?) {
                    handleFileEvent(context, dirName, event, path)
                }
            }
        }
    }

    /**
     * 处理文件事件
     */
    private fun handleFileEvent(
        context: android.content.Context,
        dirName: String,
        event: Int,
        path: String?
    ) {
        val eventName = when (event and FileObserver.ALL_EVENTS) {
            FileObserver.CREATE -> "创建"
            FileObserver.DELETE -> "删除"
            FileObserver.MODIFY -> "修改"
            FileObserver.MOVED_FROM -> "移出"
            FileObserver.MOVED_TO -> "移入"
            FileObserver.CLOSE_WRITE -> "写入完成"
            else -> return // 忽略其他事件
        }

        // 忽略临时文件
        if (path != null && (path.startsWith(".") || path.endsWith(".tmp"))) {
            return
        }

        // 节流检查
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < THROTTLE_INTERVAL) {
            return
        }
        lastTriggerTime = now

        FwLog.d("FileObserverManager: 文件变化 - $dirName - $eventName - $path")

        // 拉起服务
        ServiceStarter.startForegroundService(context, "文件变化:$dirName:$eventName")
    }
}
