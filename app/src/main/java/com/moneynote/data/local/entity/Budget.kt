package com.moneynote.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 月度预算。
 *
 * [categoryId] 等于 [TOTAL_CATEGORY_ID] 时代表当月总预算，否则是对应分类的预算。
 * 这里刻意不使用 null 作为“总预算”的标记：SQLite 的唯一索引不会把多个 NULL 视为重复，
 * 用 0 作为哨兵值才能让 (yearMonth, categoryId) 的唯一约束真正生效。
 */
@Entity(
    tableName = "budgets",
    indices = [Index(value = ["yearMonth", "categoryId"], unique = true)],
)
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** 形如 2025-06 */
    val yearMonth: String,
    val categoryId: Long = TOTAL_CATEGORY_ID,
    val amountCents: Long,
) {
    val isTotal: Boolean get() = categoryId == TOTAL_CATEGORY_ID

    companion object {
        const val TOTAL_CATEGORY_ID = 0L
    }
}
