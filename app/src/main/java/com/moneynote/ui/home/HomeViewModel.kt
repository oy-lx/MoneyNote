package com.moneynote.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneynote.data.local.entity.Budget
import com.moneynote.data.local.entity.Category
import com.moneynote.data.local.entity.Txn
import com.moneynote.data.repository.LedgerRepository
import com.moneynote.util.toLocalDate
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

data class DayGroup(
    val date: LocalDate,
    val txns: List<Txn>,
    val expenseCents: Long,
    val incomeCents: Long,
)

data class HomeUiState(
    val month: YearMonth = YearMonth.now(),
    val groups: List<DayGroup> = emptyList(),
    val expenseCents: Long = 0L,
    val incomeCents: Long = 0L,
    val budgetCents: Long = 0L,
    val categories: Map<Long, Category> = emptyMap(),
    val isCurrentMonth: Boolean = true,
) {
    val balanceCents: Long get() = incomeCents - expenseCents

    val budgetProgress: Float
        get() = if (budgetCents > 0L) expenseCents.toFloat() / budgetCents.toFloat() else 0f

    val budgetRemainingCents: Long get() = budgetCents - expenseCents
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repo: LedgerRepository) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<HomeUiState> = selectedMonth
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
            initialValue = HomeUiState(),
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

    fun delete(txn: Txn) {
        viewModelScope.launch { repo.deleteTxn(txn) }
    }

    private fun reduce(
        month: YearMonth,
        txns: List<Txn>,
        categories: List<Category>,
        budgets: List<Budget>,
    ): HomeUiState {
        val groups = txns
            .groupBy { it.dateMillis.toLocalDate() }
            .entries
            .sortedByDescending { it.key }
            .map { (date, list) ->
                DayGroup(
                    date = date,
                    txns = list.sortedByDescending { it.createdAt },
                    expenseCents = list.filter { it.type.isExpense }.sumOf { it.amountCents },
                    incomeCents = list.filterNot { it.type.isExpense }.sumOf { it.amountCents },
                )
            }

        return HomeUiState(
            month = month,
            groups = groups,
            expenseCents = txns.filter { it.type.isExpense }.sumOf { it.amountCents },
            incomeCents = txns.filterNot { it.type.isExpense }.sumOf { it.amountCents },
            budgetCents = budgets.firstOrNull { it.isTotal }?.amountCents ?: 0L,
            categories = categories.associateBy { it.id },
            isCurrentMonth = month == YearMonth.now(),
        )
    }
}
