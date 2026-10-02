package com.moneynote.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneynote.data.local.entity.Budget
import com.moneynote.data.local.entity.Category
import com.moneynote.data.local.entity.Txn
import com.moneynote.data.model.TxnType
import com.moneynote.data.repository.LedgerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CategoryBudgetRow(
    val category: Category,
    val budgetCents: Long,
    val spentCents: Long,
) {
    val progress: Float
        get() = if (budgetCents > 0L) spentCents.toFloat() / budgetCents.toFloat() else 0f

    val remainingCents: Long get() = budgetCents - spentCents

    val isOver: Boolean get() = budgetCents > 0L && spentCents > budgetCents
}

data class BudgetUiState(
    val month: YearMonth = YearMonth.now(),
    val totalBudgetCents: Long = 0L,
    val totalSpentCents: Long = 0L,
    val rows: List<CategoryBudgetRow> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val unbudgetedSpentCents: Long = 0L,
    val daysInMonth: Int = 30,
    val daysLeft: Int = 0,
    val isCurrentMonth: Boolean = true,
) {
    val totalProgress: Float
        get() = if (totalBudgetCents > 0L) {
            totalSpentCents.toFloat() / totalBudgetCents.toFloat()
        } else {
            0f
        }

    val totalRemainingCents: Long get() = totalBudgetCents - totalSpentCents

    val isTotalOver: Boolean get() = totalBudgetCents > 0L && totalSpentCents > totalBudgetCents

    val dailyAvailableCents: Long
        get() = if (daysLeft > 0) totalRemainingCents.coerceAtLeast(0L) / daysLeft else 0L

    val unbudgetedCategories: List<Category>
        get() = expenseCategories.filter { category -> rows.none { it.category.id == category.id } }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(private val repo: LedgerRepository) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<BudgetUiState> = selectedMonth
        .flatMapLatest { month ->
            combine(
                repo.observeTxnsOfMonth(month),
                repo.observeCategories(),
                repo.observeBudgets(month),
            ) { txns, categories, budgets ->
                reduce(month, txns, categories, budgets)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetUiState(),
        )

    fun previousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }

    fun goToCurrentMonth() {
        selectedMonth.value = YearMonth.now()
    }

    fun setTotalBudget(cents: Long) {
        viewModelScope.launch {
            repo.setBudget(selectedMonth.value, Budget.TOTAL_CATEGORY_ID, cents)
        }
    }

    fun setCategoryBudget(categoryId: Long, cents: Long) {
        viewModelScope.launch {
            repo.setBudget(selectedMonth.value, categoryId, cents)
        }
    }

    private fun reduce(
        month: YearMonth,
        txns: List<Txn>,
        categories: List<Category>,
        budgets: List<Budget>,
    ): BudgetUiState {
        val expenseCategories = categories.filter { it.type == TxnType.EXPENSE }
        val expenseTxns = txns.filter { it.type.isExpense }
        val totalSpent = expenseTxns.sumOf { it.amountCents }

        val spentByCategory = expenseTxns
            .groupBy { it.categoryId }
            .map { entry -> entry.key to entry.value.sumOf { it.amountCents } }
            .toMap()

        val budgetByCategory = budgets
            .filterNot { it.isTotal }
            .associate { it.categoryId to it.amountCents }

        val rows = expenseCategories
            .mapNotNull { category ->
                budgetByCategory[category.id]?.let { budget ->
                    CategoryBudgetRow(
                        category = category,
                        budgetCents = budget,
                        spentCents = spentByCategory[category.id] ?: 0L,
                    )
                }
            }
            .sortedByDescending { it.progress }

        val budgetedSpent = rows.sumOf { it.spentCents }

        val today = LocalDate.now()
        val daysInMonth = month.lengthOfMonth()
        val daysLeft = when {
            month == YearMonth.from(today) -> daysInMonth - today.dayOfMonth + 1
            month.isAfter(YearMonth.from(today)) -> daysInMonth
            else -> 0
        }

        return BudgetUiState(
            month = month,
            totalBudgetCents = budgets.firstOrNull { it.isTotal }?.amountCents ?: 0L,
            totalSpentCents = totalSpent,
            rows = rows,
            expenseCategories = expenseCategories,
            unbudgetedSpentCents = (totalSpent - budgetedSpent).coerceAtLeast(0L),
            daysInMonth = daysInMonth,
            daysLeft = daysLeft,
            isCurrentMonth = month == YearMonth.now(),
        )
    }
}
