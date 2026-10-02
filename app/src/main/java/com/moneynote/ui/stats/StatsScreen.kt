package com.moneynote.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.moneynote.data.model.TxnType
import com.moneynote.ui.appViewModelFactory
import com.moneynote.ui.components.BarChart
import com.moneynote.ui.components.CategoryAvatar
import com.moneynote.ui.components.DonutChart
import com.moneynote.ui.components.ChartSlice
import com.moneynote.ui.components.EmptyPlaceholder
import com.moneynote.ui.components.MonthSelector
import com.moneynote.ui.components.SectionCard
import com.moneynote.ui.theme.LedgerTheme
import com.moneynote.ui.theme.chartColorAt
import com.moneynote.util.formatCents
import com.moneynote.util.formatYearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = viewModel(factory = appViewModelFactory { StatsViewModel(it) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "head") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    MonthSelector(
                        title = formatYearMonth(state.month),
                        onPrevious = viewModel::previousMonth,
                        onNext = viewModel::nextMonth,
                        onTitleClick = if (state.isCurrentMonth) null else viewModel::goToCurrentMonth,
                    )
                    Spacer(Modifier.height(14.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        TxnType.entries.forEachIndexed { index, type ->
                            SegmentedButton(
                                selected = state.type == type,
                                onClick = { viewModel.selectType(type) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = TxnType.entries.size,
                                ),
                            ) {
                                Text(if (type.isExpense) "支出" else "收入")
                            }
                        }
                    }
                }
            }
        }

        item(key = "overview") {
            SectionCard {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCell(
                        label = "收入",
                        value = formatCents(state.incomeCents),
                        valueColor = LedgerTheme.colors.income,
                        modifier = Modifier.weight(1f),
                    )
                    StatCell(
                        label = "支出",
                        value = formatCents(state.expenseCents),
                        valueColor = LedgerTheme.colors.expense,
                        modifier = Modifier.weight(1f),
                    )
                    StatCell(
                        label = "结余",
                        value = formatCents(state.balanceCents),
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item(key = "donut") {
            SectionCard {
                Text(
                    text = if (state.type.isExpense) "支出构成" else "收入构成",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(16.dp))

                if (!state.hasData) {
                    EmptyPlaceholder(title = "本月暂无数据")
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        DonutChart(
                            slices = state.categoryStats.map { stat ->
                                ChartSlice(
                                    label = stat.name,
                                    value = stat.totalCents,
                                    color = chartColorAt(stat.rank),
                                )
                            },
                            modifier = Modifier.size(200.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (state.type.isExpense) "总支出" else "总收入",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "¥" + formatCents(state.scopedTotalCents),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "daily") {
            SectionCard {
                Text(text = "每日趋势", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "日均 ¥" + formatCents(state.avgDailyCents) +
                        " · 记账 " + state.activeDays + " 天",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                BarChart(
                    values = state.daily.map { it.amountCents.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    color = if (state.type.isExpense) {
                        LedgerTheme.colors.expense
                    } else {
                        LedgerTheme.colors.income
                    },
                    highlightIndex = state.peakDay - 1,
                    highlightColor = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "1日",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.peakDay > 0) {
                        Text(
                            text = "最高 " + state.peakDay + " 日 ¥" + formatCents(state.maxDailyCents),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = state.daily.size.toString() + "日",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (state.hasData) {
            item(key = "breakdownTitle") {
                Text(
                    text = "分类排行",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
            items(
                count = state.categoryStats.size,
                key = { index -> "stat-" + state.categoryStats[index].rank },
            ) { index ->
                CategoryStatRow(stat = state.categoryStats[index])
            }
        }

        item(key = "bottomSpace") { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
        )
    }
}

@Composable
private fun CategoryStatRow(stat: CategoryStat) {
    val color = chartColorAt(stat.rank)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryAvatar(
                emoji = stat.emoji,
                background = color.copy(alpha = 0.16f),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stat.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "¥" + formatCents(stat.totalCents),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color.copy(alpha = 0.18f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(stat.percent.coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "占比 " + formatPercent(stat.percent),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatPercent(percent: Float): String =
    String.format(java.util.Locale.US, "%.1f%%", percent * 100f)
