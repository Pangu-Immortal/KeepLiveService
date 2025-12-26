/**
 * ============================================================================
 * StubContentProvider.kt - 同步用 ContentProvider（空实现）
 * ============================================================================
 *
 * 功能简介：
 *   账户同步机制需要一个 ContentProvider 作为数据源。
 *   这里只是一个空壳实现，用于满足同步框架的要求。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.service.framework.account

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * 同步用的 ContentProvider（空实现）
 *
 * 账户同步机制需要一个 ContentProvider 作为数据源
 * 这里只是一个空壳，用于满足同步框架的要求
 */
class StubContentProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
