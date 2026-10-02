package com.moneynote.ui.feedback

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneynote.ui.appViewModelFactory
import com.moneynote.ui.components.ConfirmDialog
import com.moneynote.ui.components.SectionCard
import com.moneynote.util.FeedbackCategory
import com.moneynote.util.FeedbackComposer
import com.moneynote.util.collectDeviceInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 环境信息只在本机采集、且只写进邮件正文，不会自动外发
    val deviceInfo = remember(context) { collectDeviceInfo(context) }
    val viewModel: FeedbackViewModel = viewModel(
        factory = appViewModelFactory { FeedbackViewModel(it, deviceInfo) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showNoMailAppDialog by remember { mutableStateOf(false) }

    fun hint(text: String) {
        scope.launch { snackbarHostState.showSnackbar(text) }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("意见反馈") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp),
        ) {
            SectionCard {
                Text(
                    text = "你的建议会被认真看完",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "填好后点「发送邮件」，会唤起你手机上的邮件应用，" +
                        "内容已经帮你填好，确认无误后点发送即可。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "收件邮箱：" + FeedbackComposer.FEEDBACK_EMAIL,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(text = "反馈类型", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FeedbackCategory.entries.forEach { category ->
                    CategoryChip(
                        label = category.label,
                        selected = state.category == category,
                        onClick = { viewModel.selectCategory(category) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = state.message,
                onValueChange = viewModel::onMessageChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("反馈内容") },
                placeholder = { Text("遇到了什么问题，或者希望增加什么功能？") },
                minLines = 5,
                maxLines = 10,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = state.message.length.toString() + " / " +
                    FeedbackViewModel.MAX_MESSAGE_LENGTH,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.contact,
                onValueChange = viewModel::onContactChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("联系方式（可选）") },
                placeholder = { Text("想收到回复的话，留个邮箱或微信") },
                singleLine = true,
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "附上运行环境信息",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "应用版本、系统版本、设备型号、记录条数",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = state.includeDeviceInfo,
                    onCheckedChange = viewModel::setIncludeDeviceInfo,
                )
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    val intent = buildFeedbackIntent(
                        subject = viewModel.composeSubject(),
                        body = viewModel.composeBody(),
                    )
                    val launched = runCatching { context.startActivity(intent) }.isSuccess
                    if (launched) {
                        hint("已打开邮件应用，请在里面点发送")
                    } else {
                        showNoMailAppDialog = true
                    }
                },
                enabled = state.canSend,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(text = "发送邮件", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    clipboard.setText(
                        AnnotatedString(
                            viewModel.composeSubject() + "\n\n" + viewModel.composeBody()
                        )
                    )
                    hint("反馈内容已复制，可粘贴到任意渠道发送")
                },
                enabled = state.canSend,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("复制反馈内容")
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showNoMailAppDialog) {
        ConfirmDialog(
            title = "没有找到邮件应用",
            message = "这台设备上似乎没有安装邮件客户端。可以复制下面的地址，" +
                "用任意方式发送到 " + FeedbackComposer.FEEDBACK_EMAIL + "。",
            confirmText = "复制邮箱地址",
            dismissText = "知道了",
            onConfirm = {
                clipboard.setText(AnnotatedString(FeedbackComposer.FEEDBACK_EMAIL))
                showNoMailAppDialog = false
                hint("邮箱地址已复制")
            },
            onDismiss = { showNoMailAppDialog = false },
        )
    }
}

/**
 * 用 mailto: 承载收件人、主题与正文。
 *
 * 相比 `ACTION_SENDTO` + `EXTRA_EMAIL/EXTRA_SUBJECT/EXTRA_TEXT` 的写法，
 * 把内容放进 URI 里在各家邮件客户端上的兼容性更一致，也不会出现正文重复。
 */
private fun buildFeedbackIntent(subject: String, body: String): Intent {
    val uri = Uri.parse(
        "mailto:" + FeedbackComposer.FEEDBACK_EMAIL +
            "?subject=" + Uri.encode(subject) +
            "&body=" + Uri.encode(body)
    )
    return Intent(Intent.ACTION_SENDTO, uri)
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val foreground = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = foreground,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}
