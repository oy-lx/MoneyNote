package com.moneynote.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneynote.log.AppLog
import com.moneynote.log.LogExporter
import com.moneynote.log.LogLevel
import com.moneynote.util.DeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LogUiState(
    val lines: List<String> = emptyList(),
    val totalBytes: Long = 0L,
    val minLevel: LogLevel = LogLevel.INFO,
    val loaded: Boolean = false,
) {
    val sizeText: String get() = LogExporter.formatBytes(totalBytes)
}

class LogViewModel(private val deviceInfo: DeviceInfo) : ViewModel() {

    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val store = AppLog.currentStore
            val lines: List<String>
            val bytes: Long
            if (store == null) {
                lines = emptyList()
                bytes = 0L
            } else {
                // 读文件可能到百 KB 级，绝不能放在主线程
                val snapshot = withContext(Dispatchers.IO) {
                    store.readTail(TAIL_LINES) to store.totalBytes()
                }
                lines = snapshot.first.lines().filter { it.isNotEmpty() }
                bytes = snapshot.second
            }
            _uiState.update {
                it.copy(
                    lines = lines,
                    totalBytes = bytes,
                    minLevel = AppLog.currentMinLevel,
                    loaded = true,
                )
            }
        }
    }

    fun clear() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { AppLog.currentStore?.clear() }
            // 清空本身也留一条痕迹，避免用户以为没生效
            AppLog.warn(TAG, "用户清空了运行日志")
            refresh()
        }
    }

    /** 导出整份日志（不只是界面上展示的尾部若干行）。 */
    suspend fun buildExportText(): String = withContext(Dispatchers.IO) {
        LogExporter.buildDocument(
            deviceInfo = deviceInfo,
            exportedAtMillis = System.currentTimeMillis(),
            logText = AppLog.currentStore?.readAll().orEmpty(),
        )
    }

    companion object {
        /** 界面最多展示这么多行，够定位问题又不会拖慢列表渲染。 */
        const val TAIL_LINES: Int = 500

        private const val TAG = "LogViewModel"
    }
}
