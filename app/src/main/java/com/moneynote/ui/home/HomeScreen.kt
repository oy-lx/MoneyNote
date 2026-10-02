package com.moneynote.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneynote.data.local.entity.Category
import com.moneynote.data.local.entity.Txn
import com.moneynote.ui.appViewModelFactory
import com.moneynote.ui.components.CategoryAvatar
import com.moneynote.ui.components.EmptyPlaceholder
import com.moneynote.ui.components.MonthSelector
import com.moneynote.ui.theme.LedgerTheme
import com.moneynote.util.formatCents
import com.moneynote.util.formatMonthDay
import com.moneynote.util.formatRelativeDay
import com.moneynote.util.formatYearMonth

@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    onTxnClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = appViewModelFactory { HomeViewModel(it) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("记一笔") },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item(key = "summary") {
                MonthSummaryCard(
                    state = state,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                    onBackToToday = viewModel::goToCurrentMonth,
                )
            }

            if (state.groups.isEmpty()) {
                item(key = "empty") {
                    EmptyPlaceholder(
                        title = "本月还没有记录",
                        subtitle = "点右下角「记一笔」开始",
                    )
                }
            } else {
                state.groups.forEach { group ->
                    item(key = "day-${group.date}") { DayHeader(group) }
                    items(
                        items = group.txns,
                        key = { "txn-${it.id}" },
                    ) { txn ->
                        TxnRow(
                            txn = txn,
                            category = state.categories[txn.categoryId],
                            onClick = { onTxnClick(txn.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSummaryCard(
    state: HomeUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBackToToday: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            MonthSelector(
                title = formatYearMonth(state.month),
                onPrevious = onPrevious,
                onNext = onNext,
                onTitleClick = if (state.isCurrentMonth) null else onBackToToday,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "本月结余",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
            )
            Text(
                text = "¥" + formatCents(state.balanceCents),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryItem(
                    label = "收入",
                    value = "+" + formatCents(state.incomeCents),
                    modifier = Modifier.weight(1f),
                )
                SummaryItem(
                    label = "支出",
                    value = "-" + formatCents(state.expenseCents),
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.budgetCents > 0L) {
                Spacer(Modifier.height(14.dp))
                BudgetBar(state)
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun BudgetBar(state: HomeUiState) {
    val fraction = state.budgetProgress.coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "本月预算 ¥" + formatCents(state.budgetCents),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            )
            Text(
                text = if (state.budgetRemainingCents >= 0) {
                    "剩余 ¥" + formatCents(state.budgetRemainingCents)
                } else {
                    "超支 ¥" + formatCents(-state.budgetRemainingCents)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.28f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.onPrimary),
            )
        }
    }
}

@Composable
private fun DayHeader(group: DayGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatRelativeDay(group.date) + " · " + formatMonthDay(group.date),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        if (group.incomeCents > 0L) {
            Text(
                text = "收 " + formatCents(group.incomeCents),
                style = MaterialTheme.typography.labelMedium,
                color = LedgerTheme.colors.income,
            )
            Spacer(Modifier.width(10.dp))
        }
        if (group.expenseCents > 0L) {
            Text(
                text = "支 " + formatCents(group.expenseCents),
                style = MaterialTheme.typography.labelMedium,
                color = LedgerTheme.colors.expense,
            )
        }
    }
}

@Composable
private fun TxnRow(
    txn: Txn,
    category: Category?,
    onClick: () -> Unit,
) {
    val isExpense = txn.type.isExpense
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(
            emoji = category?.emoji ?: "❓",
            background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category?.name ?: "未分类",
                style = MaterialTheme.typography.titleMedium,
            )
            if (txn.note.isNotBlank()) {
                Text(
                    text = txn.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = (if (isExpense) "-" else "+") + formatCents(txn.amountCents),
            style = MaterialTheme.typography.titleMedium,
            color = if (isExpense) LedgerTheme.colors.expense else LedgerTheme.colors.income,
        )
    }
}
