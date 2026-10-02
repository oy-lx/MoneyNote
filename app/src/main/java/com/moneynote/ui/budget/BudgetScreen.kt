package com.moneynote.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneynote.data.local.entity.Category
import com.moneynote.ui.appViewModelFactory
import com.moneynote.ui.components.CategoryAvatar
import com.moneynote.ui.components.MonthSelector
import com.moneynote.ui.components.ProgressRing
import com.moneynote.ui.components.SectionCard
import com.moneynote.ui.theme.LedgerTheme
import com.moneynote.util.formatCents
import com.moneynote.util.formatCentsPlain
import com.moneynote.util.formatYearMonth
import com.moneynote.util.parseYuanToCents

@Composable
fun BudgetScreen(
    modifier: Modifier = Modifier,
    viewModel: BudgetViewModel = viewModel(factory = appViewModelFactory { BudgetViewModel(it) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showTotalDialog by remember { mutableStateOf(false) }
    var editingRow by remember { mutableStateOf<CategoryBudgetRow?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "month") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    MonthSelector(
                        title = formatYearMonth(state.month),
                        onPrevious = viewModel::previousMonth,
                        onNext = viewModel::nextMonth,
                        onTitleClick = if (state.isCurrentMonth) null else viewModel::goToCurrentMonth,
                    )
                }
            }
        }

        item(key = "total") {
            TotalBudgetCard(state = state, onEdit = { showTotalDialog = true })
        }

        if (state.unbudgetedSpentCents > 0L && state.rows.isNotEmpty()) {
            item(key = "unbudgeted") {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "未设置预算的分类支出",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "¥" + formatCents(state.unbudgetedSpentCents),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }

        item(key = "catTitle") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "分类预算",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (state.unbudgetedCategories.isNotEmpty()) {
                    TextButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("添加")
                    }
                }
            }
        }

        if (state.rows.isEmpty()) {
            item(key = "catEmpty") {
                SectionCard {
                    Text(
                        text = "还没有设置分类预算。给「餐饮」「购物」这类容易超支的分类单独设个上限，" +
                            "能更早发现花超了。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(items = state.rows, key = { "row-" + it.category.id }) { row ->
                CategoryBudgetItem(row = row, onClick = { editingRow = row })
            }
        }

        item(key = "bottomSpace") { Spacer(Modifier.height(12.dp)) }
    }

    if (showTotalDialog) {
        BudgetEditorDialog(
            title = "本月总预算",
            initialAmountCents = state.totalBudgetCents,
            onConfirm = { _, cents ->
                viewModel.setTotalBudget(cents)
                showTotalDialog = false
            },
            onDismiss = { showTotalDialog = false },
        )
    }

    editingRow?.let { row ->
        BudgetEditorDialog(
            title = row.category.name + " 预算",
            initialAmountCents = row.budgetCents,
            onConfirm = { _, cents ->
                viewModel.setCategoryBudget(row.category.id, cents)
                editingRow = null
            },
            onDismiss = { editingRow = null },
        )
    }

    if (showAddDialog) {
        BudgetEditorDialog(
            title = "添加分类预算",
            initialAmountCents = 0L,
            categories = state.unbudgetedCategories,
            onConfirm = { categoryId, cents ->
                if (categoryId != null) {
                    viewModel.setCategoryBudget(categoryId, cents)
                }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun TotalBudgetCard(
    state: BudgetUiState,
    onEdit: () -> Unit,
) {
    SectionCard {
        if (state.totalBudgetCents <= 0L) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "还没有设置本月总预算",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "设置一个上限，首页就会实时显示预算进度和剩余额度。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Text("设置总预算")
                }
            }
        } else {
            val ringColor = if (state.isTotalOver) {
                LedgerTheme.colors.expense
            } else {
                MaterialTheme.colorScheme.primary
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProgressRing(
                    progress = state.totalProgress,
                    modifier = Modifier.size(170.dp),
                    strokeWidth = 14.dp,
                    color = ringColor,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (state.isTotalOver) "已超支" else "剩余",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "¥" + formatCents(
                                if (state.isTotalOver) {
                                    -state.totalRemainingCents
                                } else {
                                    state.totalRemainingCents
                                }
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = ringColor,
                        )
                        Text(
                            text = (state.totalProgress * 100).toInt().toString() + "%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    MiniStat(
                        label = "本月预算",
                        value = "¥" + formatCents(state.totalBudgetCents),
                        modifier = Modifier.weight(1f),
                    )
                    MiniStat(
                        label = "已支出",
                        value = "¥" + formatCents(state.totalSpentCents),
                        modifier = Modifier.weight(1f),
                    )
                    MiniStat(
                        label = if (state.daysLeft > 0) "日均可用" else "剩余",
                        value = if (state.daysLeft > 0) {
                            "¥" + formatCents(state.dailyAvailableCents)
                        } else {
                            "¥" + formatCents(state.totalRemainingCents.coerceAtLeast(0L))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(16.dp))

                OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Text("调整总预算")
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun CategoryBudgetItem(
    row: CategoryBudgetRow,
    onClick: () -> Unit,
) {
    val barColor = if (row.isOver) LedgerTheme.colors.expense else MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                emoji = row.category.emoji,
                background = barColor.copy(alpha = 0.16f),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = row.category.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatCents(row.spentCents) + " / " + formatCents(row.budgetCents),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(8.dp))
                BudgetBar(progress = row.progress, color = barColor)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (row.isOver) {
                        "已超支 ¥" + formatCents(-row.remainingCents)
                    } else {
                        "剩余 ¥" + formatCents(row.remainingCents)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (row.isOver) barColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BudgetBar(progress: Float, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.18f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BudgetEditorDialog(
    title: String,
    initialAmountCents: Long,
    onConfirm: (categoryId: Long?, cents: Long) -> Unit,
    onDismiss: () -> Unit,
    categories: List<Category>? = null,
    initialCategoryId: Long? = null,
) {
    var amountText by remember(initialAmountCents) {
        mutableStateOf(if (initialAmountCents > 0L) formatCentsPlain(initialAmountCents) else "")
    }
    var selectedCategoryId by remember(categories, initialCategoryId) {
        mutableStateOf(initialCategoryId ?: categories?.firstOrNull()?.id)
    }

    val parsed = parseYuanToCents(amountText)
    val categoryMissing = categories != null && selectedCategoryId == null
    val confirmEnabled = !categoryMissing && (amountText.isEmpty() || parsed != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (categories != null) {
                    if (categories.isEmpty()) {
                        Text(
                            text = "所有支出分类都已设置预算。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            categories.forEach { category ->
                                val selected = category.id == selectedCategoryId
                                val background = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                }
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(background)
                                        .clickable { selectedCategoryId = category.id }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(category.emoji)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) {
                            val dot = filtered.indexOf('.')
                            if (dot < 0 || filtered.length - dot - 1 <= 2) {
                                amountText = filtered
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("金额") },
                    prefix = { Text("¥") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "留空或填 0 表示清除该预算。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = confirmEnabled,
                onClick = { onConfirm(selectedCategoryId, parseYuanToCents(amountText) ?: 0L) },
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
