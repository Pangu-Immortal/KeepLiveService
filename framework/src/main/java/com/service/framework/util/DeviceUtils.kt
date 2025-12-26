/**
 * ============================================================================
 * DeviceUtils.kt - 设备信息工具类
 * ============================================================================
 *
 * 功能简介：
 *   提供设备厂商识别、机型判断等工具方法。
 *   将设备相关的判断逻辑统一抽离，供全局使用。
 *
 * 主要功能：
 *   - getManufacturer(): 获取设备厂商名称
 *   - isSpecialVendor(): 判断是否为特殊机型（需要额外权限配置）
 *   - isXiaomiVendor(): 判断是否为小米/红米机型
 *   - isHuaweiVendor(): 判断是否为华为/荣耀机型
 *   - isOppoVendor(): 判断是否为 OPPO 系机型
 *   - isVivoVendor(): 判断是否为 vivo 机型
 *
 * 设计原则：
 *   - 工具类使用 object 单例模式
 *   - 方法无副作用，仅返回判断结果
 *   - 与业务代码解耦，可独立测试
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.util

import android.os.Build

/**
 * 设备信息工具类
 *
 * 提供设备厂商识别和机型判断功能。
 * 用于根据不同厂商定制保活策略和权限引导。
 */
object DeviceUtils {

    // ==================== 厂商名称常量 ====================

    /** 小米厂商标识 */
    private const val VENDOR_XIAOMI = "xiaomi"

    /** 红米厂商标识 */
    private const val VENDOR_REDMI = "redmi"

    /** 华为厂商标识 */
    private const val VENDOR_HUAWEI = "huawei"

    /** 荣耀厂商标识 */
    private const val VENDOR_HONOR = "honor"

    /** OPPO 厂商标识 */
    private const val VENDOR_OPPO = "oppo"

    /** Realme 厂商标识 */
    private const val VENDOR_REALME = "realme"

    /** 一加厂商标识 */
    private const val VENDOR_ONEPLUS = "oneplus"

    /** vivo 厂商标识 */
    private const val VENDOR_VIVO = "vivo"

    /** 三星厂商标识 */
    private const val VENDOR_SAMSUNG = "samsung"

    /** 魅族厂商标识 */
    private const val VENDOR_MEIZU = "meizu"

    // ==================== 基础方法 ====================

    /**
     * 获取当前设备厂商名称（小写）
     *
     * @return 厂商名称，如 "xiaomi"、"huawei" 等
     */
    fun getManufacturer(): String = Build.MANUFACTURER.lowercase()

    /**
     * 获取设备型号
     *
     * @return 设备型号，如 "Redmi K60"
     */
    fun getModel(): String = Build.MODEL

    /**
     * 获取设备品牌（小写）
     *
     * @return 品牌名称，如 "redmi"、"honor"
     */
    fun getBrand(): String = Build.BRAND.lowercase()

    // ==================== 厂商判断方法 ====================

    /**
     * 判断是否为需要额外权限配置的特殊机型
     *
     * 小米、红米、vivo、OPPO、Realme、一加等机型需要：
     * - 电池优化白名单
     * - 自启动权限
     * - 悬浮窗权限
     *
     * 这些机型的后台管控较严格，需要额外配置才能保活。
     * 普通机型（如三星、Google Pixel）通常不需要这些额外配置。
     *
     * @return true 为特殊机型，false 为普通机型
     */
    fun isSpecialVendor(): Boolean {
        val manufacturer = getManufacturer()
        return manufacturer.contains(VENDOR_XIAOMI) ||
                manufacturer.contains(VENDOR_REDMI) ||
                manufacturer.contains(VENDOR_VIVO) ||
                manufacturer.contains(VENDOR_OPPO) ||
                manufacturer.contains(VENDOR_REALME) ||
                manufacturer.contains(VENDOR_ONEPLUS)
    }

    /**
     * 判断是否为小米/红米机型
     *
     * 小米/红米机型特点：
     * - 安全中心管理自启动
     * - MIUI 系统有严格的后台限制
     * - 需要在安全中心开启自启动权限
     *
     * @return true 为小米/红米机型
     */
    fun isXiaomiVendor(): Boolean {
        val manufacturer = getManufacturer()
        return manufacturer.contains(VENDOR_XIAOMI) ||
                manufacturer.contains(VENDOR_REDMI)
    }

    /**
     * 判断是否为华为/荣耀机型
     *
     * 华为/荣耀机型特点：
     * - 手机管家管理启动应用
     * - 有独特的电池优化策略
     * - 需要在手机管家中设置
     *
     * @return true 为华为/荣耀机型
     */
    fun isHuaweiVendor(): Boolean {
        val manufacturer = getManufacturer()
        return manufacturer.contains(VENDOR_HUAWEI) ||
                manufacturer.contains(VENDOR_HONOR)
    }

    /**
     * 判断是否为 OPPO 系机型（OPPO、Realme、OnePlus）
     *
     * OPPO 系机型特点：
     * - ColorOS/Realme UI/OxygenOS 系统
     * - 统一的后台管理策略
     * - 需要在电池设置中允许后台运行
     *
     * @return true 为 OPPO 系机型
     */
    fun isOppoVendor(): Boolean {
        val manufacturer = getManufacturer()
        return manufacturer.contains(VENDOR_OPPO) ||
                manufacturer.contains(VENDOR_REALME) ||
                manufacturer.contains(VENDOR_ONEPLUS)
    }

    /**
     * 判断是否为 vivo 机型
     *
     * vivo 机型特点：
     * - OriginOS/Funtouch OS 系统
     * - 需要在后台高耗电设置中配置
     *
     * @return true 为 vivo 机型
     */
    fun isVivoVendor(): Boolean {
        val manufacturer = getManufacturer()
        return manufacturer.contains(VENDOR_VIVO)
    }

    /**
     * 判断是否为三星机型
     *
     * @return true 为三星机型
     */
    fun isSamsungVendor(): Boolean {
        val manufacturer = getManufacturer()
        return manufacturer.contains(VENDOR_SAMSUNG)
    }

    /**
     * 判断是否为魅族机型
     *
     * @return true 为魅族机型
     */
    fun isMeizuVendor(): Boolean {
        val manufacturer = getManufacturer()
        return manufacturer.contains(VENDOR_MEIZU)
    }

    // ==================== 设备信息摘要 ====================

    /**
     * 获取设备信息摘要
     *
     * @return 设备信息字符串，如 "Xiaomi Redmi K60 (Android 14)"
     */
    fun getDeviceSummary(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
    }

    /**
     * 获取厂商引导文案
     *
     * 根据不同厂商返回对应的权限设置引导文案
     *
     * @return 引导文案
     */
    fun getVendorGuideText(): String {
        return when {
            isXiaomiVendor() -> "请在「自启动管理」中开启本应用的自启动权限"
            isHuaweiVendor() -> "请在「应用启动管理」中开启本应用的自启动权限，并关闭电池优化"
            isOppoVendor() -> "请在「自启动管理」中开启本应用的自启动权限"
            isVivoVendor() -> "请在「后台高耗电」中允许本应用后台运行"
            isSamsungVendor() -> "请在「电池」设置中将本应用设为「不受监视的应用」"
            else -> "请在系统设置中允许本应用自启动和后台运行"
        }
    }
}
