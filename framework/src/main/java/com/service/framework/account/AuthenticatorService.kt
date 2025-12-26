/**
 * ============================================================================
 * AuthenticatorService.kt - 账户认证服务
 * ============================================================================
 *
 * 功能简介：
 *   提供系统账户认证服务，系统通过此服务与账户认证器通信。
 *   作为 Android 账户同步机制的入口服务。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.account

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * 账户认证服务
 *
 * 系统通过此服务与账户认证器通信
 */
class AuthenticatorService : Service() {

    private lateinit var authenticator: FwAuthenticator

    override fun onCreate() {
        super.onCreate()
        authenticator = FwAuthenticator(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return authenticator.iBinder
    }
}
