/**
 * ============================================================================
 * NfcReceiver.kt - NFC 广播接收器
 * ============================================================================
 *
 * 功能简介：
 *   监听 NFC 标签发现事件、NDEF 消息、NFC 技术发现、适配器状态变化及
 *   HCE 交易事件，支持多种 NFC 技术类型的识别与分析。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

package com.service.framework.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * NFC 广播接收器
 *
 * 核心机制：
 * 1. 监听 NFC 标签发现事件
 * 2. 监听 NFC 适配器状态变化
 * 3. 这些广播可以静态注册（需要特定的 intent-filter 配置）
 *
 * 安全研究要点：
 * - NFC 标签发现可以静态注册（TAG_DISCOVERED 是最后的回退）
 * - NDEF_DISCOVERED 和 TECH_DISCOVERED 需要更具体的配置
 * - NFC 适配器状态变化也是可以静态注册的
 * - 很多支付、门禁、公交卡应用利用此机制
 *
 * 使用场景：
 * - 支付应用（Apple Pay, Google Pay 等）
 * - 门禁卡应用
 * - 公交卡充值应用
 * - NFC 标签读写工具
 */
class NfcReceiver : BroadcastReceiver() {

    companion object {
        // NFC 标签发现（最通用，作为最后的回退）
        const val ACTION_TAG_DISCOVERED = "android.nfc.action.TAG_DISCOVERED"

        // NDEF 消息发现（需要在 intent-filter 中指定 MIME 类型或 URI）
        const val ACTION_NDEF_DISCOVERED = "android.nfc.action.NDEF_DISCOVERED"

        // NFC 技术发现（需要在 meta-data 中指定技术列表）
        const val ACTION_TECH_DISCOVERED = "android.nfc.action.TECH_DISCOVERED"

        // NFC 适配器状态变化
        const val ACTION_ADAPTER_STATE_CHANGED = "android.nfc.action.ADAPTER_STATE_CHANGED"

        // NFC 交易事件（HCE 相关）
        const val ACTION_TRANSACTION_DETECTED = "android.nfc.action.TRANSACTION_DETECTED"

        // NFC 首选支付服务变化
        const val ACTION_PREFERRED_PAYMENT_CHANGED = "android.nfc.action.PREFERRED_PAYMENT_CHANGED"

        /**
         * 获取动态注册的 IntentFilter
         */
        fun getIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(ACTION_TAG_DISCOVERED)
                addAction(ACTION_NDEF_DISCOVERED)
                addAction(ACTION_TECH_DISCOVERED)
                addAction(ACTION_ADAPTER_STATE_CHANGED)
                addAction(ACTION_TRANSACTION_DETECTED)
                addAction(ACTION_PREFERRED_PAYMENT_CHANGED)
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return

        FwLog.d("NfcReceiver: 收到广播 - $action")

        when (action) {
            ACTION_TAG_DISCOVERED -> {
                handleTagDiscovered(context, intent)
            }
            ACTION_NDEF_DISCOVERED -> {
                handleNdefDiscovered(context, intent)
            }
            ACTION_TECH_DISCOVERED -> {
                handleTechDiscovered(context, intent)
            }
            ACTION_ADAPTER_STATE_CHANGED -> {
                handleAdapterStateChanged(context, intent)
            }
            ACTION_TRANSACTION_DETECTED -> {
                handleTransactionDetected(context, intent)
            }
            ACTION_PREFERRED_PAYMENT_CHANGED -> {
                FwLog.d("NfcReceiver: NFC 首选支付服务已变化")
            }
        }
    }

    /**
     * 处理 NFC 标签发现
     *
     * TAG_DISCOVERED 是最通用的 NFC Intent
     * 当没有其他应用处理 NDEF 或 TECH Intent 时，系统会发送此 Intent
     */
    private fun handleTagDiscovered(context: Context, intent: Intent) {
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        if (tag != null) {
            val tagId = tag.id?.joinToString(":") { String.format("%02X", it) } ?: "未知"
            val techList = tag.techList?.joinToString(", ") ?: "未知"

            FwLog.d("NfcReceiver: NFC 标签已发现")
            FwLog.d("  - 标签ID: $tagId")
            FwLog.d("  - 支持技术: $techList")

            // 分析标签类型
            analyzeNfcTech(tag.techList)
        } else {
            FwLog.d("NfcReceiver: NFC 标签已发现（无 Tag 对象）")
        }

        // 拉起服务
        ServiceStarter.startForegroundService(context, "NFC标签发现")
    }

    /**
     * 处理 NDEF 消息发现
     *
     * NDEF (NFC Data Exchange Format) 是 NFC 标签的标准数据格式
     * 包括：文本、URI、智能海报、MIME 数据等
     */
    private fun handleNdefDiscovered(context: Context, intent: Intent) {
        FwLog.d("NfcReceiver: NDEF 消息已发现")

        // 获取 NDEF 消息
        val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if (rawMsgs != null && rawMsgs.isNotEmpty()) {
            FwLog.d("  - NDEF 消息数量: ${rawMsgs.size}")
        }

        ServiceStarter.startForegroundService(context, "NDEF消息发现")
    }

    /**
     * 处理 NFC 技术发现
     *
     * 当标签支持特定的 NFC 技术时触发
     * 需要在 AndroidManifest 中配置支持的技术列表
     */
    private fun handleTechDiscovered(context: Context, intent: Intent) {
        FwLog.d("NfcReceiver: NFC 技术已发现")

        ServiceStarter.startForegroundService(context, "NFC技术发现")
    }

    /**
     * 处理 NFC 适配器状态变化
     */
    private fun handleAdapterStateChanged(context: Context, intent: Intent) {
        val state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF)
        val previousState = intent.getIntExtra("android.nfc.extra.ADAPTER_STATE", NfcAdapter.STATE_OFF)

        val stateName = when (state) {
            NfcAdapter.STATE_OFF -> "关闭"
            NfcAdapter.STATE_TURNING_ON -> "正在开启"
            NfcAdapter.STATE_ON -> "开启"
            NfcAdapter.STATE_TURNING_OFF -> "正在关闭"
            else -> "未知($state)"
        }

        FwLog.d("NfcReceiver: NFC 适配器状态变化 - $stateName")

        if (state == NfcAdapter.STATE_ON) {
            ServiceStarter.startForegroundService(context, "NFC已开启")
        }
    }

    /**
     * 处理 NFC 交易检测（HCE 相关）
     *
     * Host Card Emulation (HCE) 允许应用模拟 NFC 卡
     * 用于移动支付、门禁等场景
     */
    private fun handleTransactionDetected(context: Context, intent: Intent) {
        FwLog.d("NfcReceiver: NFC 交易已检测（HCE）")

        ServiceStarter.startForegroundService(context, "NFC交易检测")
    }

    /**
     * 分析 NFC 技术类型
     *
     * 常见的 NFC 技术：
     * - NfcA (ISO 14443-3A): MIFARE Classic, MIFARE Ultralight
     * - NfcB (ISO 14443-3B): 一些银行卡
     * - NfcF (JIS 6319-4): FeliCa (日本交通卡、支付卡)
     * - NfcV (ISO 15693): 邻近卡，距离更远
     * - IsoDep (ISO 14443-4): ISO-DEP 协议，银行卡
     * - Ndef: NDEF 格式数据
     * - NdefFormatable: 可格式化为 NDEF
     * - MifareClassic: MIFARE Classic 卡
     * - MifareUltralight: MIFARE Ultralight 卡
     */
    private fun analyzeNfcTech(techList: Array<String>?) {
        if (techList == null) return

        for (tech in techList) {
            val techName = tech.substringAfterLast('.')
            val description = when (techName) {
                "NfcA" -> "ISO 14443-3A (MIFARE)"
                "NfcB" -> "ISO 14443-3B (银行卡)"
                "NfcF" -> "JIS 6319-4 (FeliCa/日本交通卡)"
                "NfcV" -> "ISO 15693 (邻近卡)"
                "IsoDep" -> "ISO 14443-4 (银行卡/门禁卡)"
                "Ndef" -> "NDEF 数据格式"
                "NdefFormatable" -> "可格式化为 NDEF"
                "MifareClassic" -> "MIFARE Classic (公交卡)"
                "MifareUltralight" -> "MIFARE Ultralight (轻量级)"
                else -> "未知技术"
            }
            FwLog.d("  - $techName: $description")
        }
    }
}
