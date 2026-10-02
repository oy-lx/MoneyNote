package com.moneynote.data

import com.moneynote.data.local.SeedData
import com.moneynote.data.local.entity.Budget
import com.moneynote.data.model.TxnType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedAndModelTest {

    @Test
    fun `内置分类数量符合预期`() {
        val expense = SeedData.DEFAULT_CATEGORIES.count { it.type == TxnType.EXPENSE }
        val income = SeedData.DEFAULT_CATEGORIES.count { it.type == TxnType.INCOME }
        assertEquals(15, expense)
        assertEquals(7, income)
    }

    @Test
    fun `同一收支方向内分类名不重复`() {
        TxnType.entries.forEach { type ->
            val names = SeedData.DEFAULT_CATEGORIES
                .filter { it.type == type }
                .map { it.name }
            assertEquals(
                "分类名在 $type 下出现重复",
                names.size,
                names.toSet().size,
            )
        }
    }

    @Test
    fun `同一收支方向内排序值唯一且连续`() {
        TxnType.entries.forEach { type ->
            val orders = SeedData.DEFAULT_CATEGORIES
                .filter { it.type == type }
                .map { it.sortOrder }
                .sorted()
            assertEquals(orders.indices.toList(), orders)
        }
    }

    @Test
    fun `每个内置分类都有名称和图标`() {
        SeedData.DEFAULT_CATEGORIES.forEach { seed ->
            assertTrue("分类名为空", seed.name.isNotBlank())
            assertTrue("分类图标为空: " + seed.name, seed.emoji.isNotBlank())
        }
    }

    @Test
    fun `预算的 total 哨兵值语义正确`() {
        val total = Budget(yearMonth = "2025-06", amountCents = 100_000L)
        assertTrue(total.isTotal)
        assertEquals(Budget.TOTAL_CATEGORY_ID, total.categoryId)

        val categoryBudget = Budget(
            yearMonth = "2025-06",
            categoryId = 7L,
            amountCents = 20_000L,
        )
        assertFalse(categoryBudget.isTotal)
    }

    @Test
    fun `收支方向语义正确`() {
        assertTrue(TxnType.EXPENSE.isExpense)
        assertFalse(TxnType.INCOME.isExpense)
    }
}
