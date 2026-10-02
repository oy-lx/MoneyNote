package com.moneynote.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneynote.data.local.entity.Category
import com.moneynote.data.local.entity.Txn
import com.moneynote.data.model.TxnType
import com.moneynote.data.repository.LedgerRepository
import com.moneynote.util.toLocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

data class CategoryStat(
    val categoryId: Long?,
    val name: String,
    val emoji: String,
    val totalCents: Long,
    val percent: Float,
    val rank: Int,
)

data class DailyPoint(
    val day: Int,
    val amountCents: Long,
)

data class StatsUiState(
    val month: YearMonth = YearMonth.now(),
    val type: TxnType = TxnType.EXPENSE,
    val expenseCents: Long = 0L,
    val incomeCents: Long = 0L,
    val scopedTotalCents: Long = 0L,
    val categoryStats: List<CategoryStat> = emptyList(),
    val daily: List<DailyPoint> = emptyList(),
    val maxDailyCents: Long = 0L,
    val avgDailyCents: Long = 0L,
    val activeDays: Int = 0,
    val peakDay: Int = 0,
    val isCurrentMonth: Boolean = true,
) {
    val balanceCents: Long get() = incomeCents - expenseCents
    val hasData: Boolean get() = scopedTotalCents > 0L
}

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(private val repo: LedgerRepository) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val selectedType = MutableStateFlow(TxnType.EXPENSE)

    val uiState: StateFlow<StatsUiState> = combine(selectedMonth, selectedType) { month, type ->
        month to type
    }
        .flatMapLatest { (month, type) ->
            combine(
                repo.observeTxnsOfMonth(month),
                repo.observeCategories(),
            ) { txns, categories ->
                reduce(month, type, txns, categories)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatsUiState(),
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

    fun selectType(type: TxnType) {
        selectedType.value = type
    }

    private fun reduce(
        month: YearMonth,
        type: TxnType,
        txns: List<Txn>,
        categories: List<Category>,
    ): StatsUiState {
        val expenseList = txns.filter { it.type.isExpense }
        val incomeList = txns.filterNot { it.type.isExpense }
        val expenseTotal = expenseList.sumOf { it.amountCents }
        val incomeTotal = incomeList.sumOf { it.amountCents }

        val scoped = if (type.isExpense) expenseList else incomeList
        val scopedTotal = if (type.isExpense) expenseTotal else incomeTotal

        val categoryStats = scoped
            .groupBy { it.categoryId }
            .map { entry -> entry.key to entry.value.sumOf { it.amountCents } }
            .sortedByDescending { it.second }
            .mapIndexed { index, entry ->
                val category = categories.firstOrNull { it.id == entry.first }
                CategoryStat(
                    categoryId = entry.first,
                    name = category?.name ?: "未分类",
                    emoji = category?.emoji ?: "❓",
                    totalCents = entry.second,
                    percent = if (scopedTotal > 0L) {
                        entry.second.toFloat() / scopedTotal.toFloat()
                    } else {
                        0f
                    },
                    rank = index,
                )
            }

        val byDay = txns.groupBy { it.dateMillis.toLocalDate().dayOfMonth }
        val daysInMonth = month.lengthOfMonth()
        val daily = (1..daysInMonth).map { day ->
            val list = byDay[day].orEmpty()
            val amount = if (type.isExpense) {
                list.filter { it.type.isExpense }.sumOf { it.amountCents }
            } else {
                list.filterNot { it.type.isExpense }.sumOf { it.amountCents }
            }
            DailyPoint(day = day, amountCents = amount)
        }

        val activeDays = daily.count { it.amountCents > 0L }
        val peak = daily.maxByOrNull { it.amountCents }

        return StatsUiState(
            month = month,
            type = type,
            expenseCents = expenseTotal,
            incomeCents = incomeTotal,
            scopedTotalCents = scopedTotal,
            categoryStats = categoryStats,
            daily = daily,
            maxDailyCents = peak?.amountCents ?: 0L,
            avgDailyCents = if (activeDays > 0) scopedTotal / activeDays else 0L,
            activeDays = activeDays,
            peakDay = if ((peak?.amountCents ?: 0L) > 0L) peak?.day ?: 0 else 0,
            isCurrentMonth = month == YearMonth.now(),
        )
    }
}
