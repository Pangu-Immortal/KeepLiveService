/**
 * ============================================================================
 * FwAuthenticator.kt - 账户认证器
 * ============================================================================
 *
 * 功能简介：
 *   实现 AbstractAccountAuthenticator 的账户认证器，用于账户同步机制。
 *   通过注册虚假账户，利用系统同步机制唤醒应用，同步间隔可设置为 60 秒。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.account

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.os.Bundle
import com.service.framework.Fw
import com.service.framework.util.FwLog

/**
 * 账户认证器
 *
 * 安全研究要点：
 * - 账户认证器用于账户同步机制
 * - 注册一个虚假账户，利用系统同步机制唤醒应用
 * - 同步间隔可以非常短（60秒）
 * - 强制停止后同步会被暂停，但账户仍然存在
 */
class FwAuthenticator(
    private val context: Context
) : AbstractAccountAuthenticator(context) {

    override fun editProperties(
        response: AccountAuthenticatorResponse?,
        accountType: String?
    ): Bundle? = null

    override fun addAccount(
        response: AccountAuthenticatorResponse?,
        accountType: String?,
        authTokenType: String?,
        requiredFeatures: Array<out String>?,
        options: Bundle?
    ): Bundle? {
        FwLog.d("AccountAuthenticator addAccount")
        return null
    }

    override fun confirmCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        options: Bundle?
    ): Bundle? = null

    override fun getAuthToken(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle? = null

    override fun getAuthTokenLabel(authTokenType: String?): String? = null

    override fun updateCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle? = null

    override fun hasFeatures(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        features: Array<out String>?
    ): Bundle? = null

    companion object {
        /**
         * 添加账户
         */
        fun addAccount(context: Context): Boolean {
            val config = Fw.config ?: return false

            try {
                val accountManager = AccountManager.get(context)
                val account = Account("Fw", config.accountType)

                // 检查账户是否已存在
                val existingAccounts = accountManager.getAccountsByType(config.accountType)
                if (existingAccounts.isNotEmpty()) {
                    FwLog.d("账户已存在")
                    return true
                }

                // 添加账户
                val result = accountManager.addAccountExplicitly(account, null, null)
                FwLog.d("添加账户结果: $result")
                return result
            } catch (e: Exception) {
                FwLog.e("添加账户失败: ${e.message}", e)
                return false
            }
        }

        /**
         * 移除账户
         */
        fun removeAccount(context: Context) {
            val config = Fw.config ?: return

            try {
                val accountManager = AccountManager.get(context)
                val accounts = accountManager.getAccountsByType(config.accountType)

                for (account in accounts) {
                    accountManager.removeAccountExplicitly(account)
                }
                FwLog.d("账户已移除")
            } catch (e: Exception) {
                FwLog.e("移除账户失败: ${e.message}", e)
            }
        }
    }
}
