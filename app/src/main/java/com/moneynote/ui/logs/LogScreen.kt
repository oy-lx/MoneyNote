package com.moneynote.ui.logs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneynote.ui.appViewModelFactory
import com.moneynote.ui.components.ConfirmDialog
import com.moneynote.ui.components.EmptyPlaceholder
import com.moneynote.ui.components.SectionCard
import com.moneynote.ui.theme.WarnAmber
import com.moneynote.util.collectDeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** 时间戳固定 23 个字符，所以这一位就是级别标记（D/I/W/E）。 */
private const val LEVEL_MARKER_INDEX = 23

private val fileNameFormat = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val deviceInfo = remember(context) { collectDeviceInfo(context) }
    val viewModel: LogViewModel = viewModel(
        factory = appViewModelFactory { LogViewModel(deviceInfo) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showClearConfirm by remember { mutableStateOf(false) }

    fun hint(text: String) {
        scope.launch { snackbarHostState.showSnackbar(text) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val text = viewModel.buildExportText()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            output.write(text.toByteArray(Charsets.UTF_8))
                        }
                    }
                }.onSuccess {
                    hint("日志已导出")
                }.onFailure {
                    hint("导出失败：" + it.message)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("运行日志") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "summary") {
                SectionCard {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SummaryStat("当前级别", state.minLevel.name, Modifier.weight(1f))
                        SummaryStat("日志大小", state.sizeText, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Release 包只记录 WARN 与 ERROR，Debug 包记录全部级别。" +
                            "出于隐私考虑，日志不会写入金额和备注内容。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item(key = "actions") {
                SectionCard {
                    Button(
                        onClick = {
                            exportLauncher.launch(
                                "MoneyNote-log-" + LocalDateTime.now().format(fileNameFormat) + ".txt"
                            )
                        },
                        enabled = state.lines.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("导出为文件")
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val text = viewModel.buildExportText()
                                    clipboard.setText(AnnotatedString(text))
                                    hint("日志已复制到剪贴板")
                                }
                            },
                            enabled = state.lines.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("复制全部")
                        }
                        OutlinedButton(
                            onClick = { showClearConfirm = true },
                            enabled = state.lines.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("清空日志")
                        }
                    }
                }
            }

            if (!state.loaded) {
                item(key = "loading") {
                    EmptyPlaceholder(title = "正在读取…")
                }
            } else if (state.lines.isEmpty()) {
                item(key = "empty") {
                    EmptyPlaceholder(
                        title = "暂无日志",
                        subtitle = "在应用里做一些操作后再回来看看",
                    )
                }
            } else {
                item(key = "logTitle") {
                    Text(
                        text = "最近 ${state.lines.size} 行",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                items(state.lines) { line ->
                    LogLine(line)
                }
            }

            item(key = "bottomSpace") { Spacer(Modifier.height(12.dp)) }
        }
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = "清空运行日志？",
            message = "将删除本机保存的全部日志文件，此操作不影响你的记账数据。",
            confirmText = "清空",
            onConfirm = {
                showClearConfirm = false
                viewModel.clear()
                hint("日志已清空")
            },
            onDismiss = { showClearConfirm = false },
        )
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun LogLine(line: String) {
    val color = when {
        line.getOrNull(LEVEL_MARKER_INDEX) == 'E' -> MaterialTheme.colorScheme.error
        line.getOrNull(LEVEL_MARKER_INDEX) == 'W' -> WarnAmber
        line.startsWith("    ") -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    Text(
        text = line,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    )
}
