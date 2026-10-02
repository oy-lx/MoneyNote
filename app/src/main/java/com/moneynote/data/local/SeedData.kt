package com.moneynote.data.local

import com.moneynote.data.model.TxnType

/** 首次启动时写入的默认分类。 */
object SeedData {

    data class Seed(
        val name: String,
        val emoji: String,
        val type: TxnType,
        val sortOrder: Int,
    )

    @Suppress("LongMethod")
    val DEFAULT_CATEGORIES: List<Seed> = listOf(
        // ---------- 支出 ----------
        Seed("餐饮", "🍜", TxnType.EXPENSE, 0),
        Seed("交通", "🚌", TxnType.EXPENSE, 1),
        Seed("购物", "🛍️", TxnType.EXPENSE, 2),
        Seed("居住", "🏠", TxnType.EXPENSE, 3),
        Seed("水电", "💡", TxnType.EXPENSE, 4),
        Seed("通讯", "📱", TxnType.EXPENSE, 5),
        Seed("娱乐", "🎮", TxnType.EXPENSE, 6),
        Seed("医疗", "💊", TxnType.EXPENSE, 7),
        Seed("学习", "📚", TxnType.EXPENSE, 8),
        Seed("服饰", "👕", TxnType.EXPENSE, 9),
        Seed("人情", "🧧", TxnType.EXPENSE, 10),
        Seed("旅行", "✈️", TxnType.EXPENSE, 11),
        Seed("运动", "🏃", TxnType.EXPENSE, 12),
        Seed("宠物", "🐾", TxnType.EXPENSE, 13),
        Seed("其他支出", "📦", TxnType.EXPENSE, 14),
        // ---------- 收入 ----------
        Seed("工资", "💰", TxnType.INCOME, 0),
        Seed("奖金", "🎁", TxnType.INCOME, 1),
        Seed("兼职", "🧰", TxnType.INCOME, 2),
        Seed("理财", "📈", TxnType.INCOME, 3),
        Seed("报销", "🧾", TxnType.INCOME, 4),
        Seed("红包", "🧧", TxnType.INCOME, 5),
        Seed("其他收入", "🪙", TxnType.INCOME, 6),
    )
}
