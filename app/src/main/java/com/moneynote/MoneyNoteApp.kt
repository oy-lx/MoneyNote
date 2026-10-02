package com.moneynote

import android.app.Application
import com.moneynote.data.local.AppDatabase
import com.moneynote.data.repository.LedgerRepository
import com.moneynote.log.AppLog

/**
 * 手写轻量依赖注入：单例数据库 + 单例 Repository。
 * 项目规模不大时这样比引入 Hilt 更直观，也少了注解处理器的编译开销。
 */
class MoneyNoteApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }

    val repository: LedgerRepository by lazy { LedgerRepository(database) }

    override fun onCreate() {
        super.onCreate()
        // 日志要尽早初始化：越早越好，后面的启动流程才留得下痕迹
        AppLog.init(this)
        AppLog.installCrashHandler()
        AppLog.info(TAG, "应用启动：包名=$packageName，日志级别=${AppLog.currentMinLevel}")
    }

    private companion object {
        const val TAG = "MoneyNoteApp"
    }
}
