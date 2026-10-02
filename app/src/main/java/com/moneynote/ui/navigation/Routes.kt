package com.moneynote.ui.navigation

import androidx.annotation.DrawableRes
import com.moneynote.R

object Routes {

    const val ARG_TXN_ID = "txnId"

    /** 记账编辑页，txnId = -1 表示新增。 */
    const val EDIT = "edit?$ARG_TXN_ID={$ARG_TXN_ID}"

    const val CATEGORIES = "categories"

    const val FEEDBACK = "feedback"

    const val LOGS = "logs"

    fun edit(txnId: Long? = null): String = "edit?$ARG_TXN_ID=${txnId ?: -1L}"
}

/** 底部导航的四个一级页面。 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    @DrawableRes val icon: Int,
) {
    HOME("home", "明细", R.drawable.ic_nav_records),
    STATS("stats", "统计", R.drawable.ic_nav_stats),
    BUDGET("budget", "预算", R.drawable.ic_nav_budget),
    SETTINGS("settings", "我的", R.drawable.ic_nav_settings);

    companion object {
        val routeSet: Set<String> = TopLevelDestination.entries.map { it.route }.toSet()
    }
}
