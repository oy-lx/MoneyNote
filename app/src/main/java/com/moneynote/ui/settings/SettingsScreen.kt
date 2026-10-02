package com.moneynote.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneynote.R
import com.moneynote.ui.appViewModelFactory
import com.moneynote.ui.components.ConfirmDialog
import com.moneynote.ui.components.SectionCard
import com.moneynote.util.FeedbackComposer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    onOpenCategories: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenLogs: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = appViewModelFactory { SettingsViewModel(it) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showClearConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val csv = viewModel.buildCsv()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            output.write(csv.toByteArray(Charsets.UTF_8))
                        }
                    }
                }.onSuccess {
                    Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "导出失败：" + it.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "overview") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "本地存储 · 无需账号 · 数据不出手机",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        HeaderStat("流水", state.txnCount.toString(), Modifier.weight(1f))
                        HeaderStat("分类", state.categoryCount.toString(), Modifier.weight(1f))
                    }
                }
            }
        }

        item(key = "manage") {
            SectionCard {
                SettingRow(
                    title = "分类管理",
                    subtitle = "新增、改名、删除支出与收入分类",
                    onClick = onOpenCategories,
                )
                Spacer(Modifier.height(4.dp))
                SettingRow(
                    title = "导出 CSV",
                    subtitle = "导出全部流水，可用 Excel 或 WPS 打开",
                    enabled = state.txnCount > 0,
                    onClick = {
                        exportLauncher.launch("MoneyNote-" + System.currentTimeMillis() + ".csv")
                    },
                )
                Spacer(Modifier.height(4.dp))
                SettingRow(
                    title = "意见反馈",
                    subtitle = "发送到 " + FeedbackComposer.FEEDBACK_EMAIL,
                    onClick = onOpenFeedback,
                )
                Spacer(Modifier.height(4.dp))
                SettingRow(
                    title = "运行日志",
                    subtitle = "查看和导出运行日志，排查问题时很有用",
                    onClick = onOpenLogs,
                )
            }
        }

        item(key = "danger") {
            SectionCard {
                SettingRow(
                    title = "清空所有数据",
                    subtitle = "删除全部流水与预算设置，分类保持不变",
                    enabled = state.txnCount > 0,
                    danger = true,
                    onClick = { showClearConfirm = true },
                )
            }
        }

        item(key = "about") {
            SectionCard {
                Text(text = "关于", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                AboutRow("版本", "1.0.0")
                AboutRow("存储", "Room / SQLite（本地）")
                AboutRow("架构", "Kotlin · Jetpack Compose · MVVM")
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "所有数据仅保存在本机数据库中，卸载应用会一并删除。" +
                        "建议定期用「导出 CSV」做备份。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "bottomSpace") { Spacer(Modifier.height(12.dp)) }
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = "清空所有数据？",
            message = "将删除全部流水记录和预算设置，此操作不可恢复。",
            confirmText = "清空",
            onConfirm = {
                showClearConfirm = false
                viewModel.clearAllData()
                Toast.makeText(context, "已清空", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showClearConfirm = false },
        )
    }
}

@Composable
private fun HeaderStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    val titleColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        danger -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
