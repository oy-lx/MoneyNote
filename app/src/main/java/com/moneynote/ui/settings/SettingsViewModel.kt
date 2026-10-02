package com.moneynote.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneynote.data.repository.LedgerRepository
import com.moneynote.util.formatCentsPlain
import com.moneynote.util.toLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val txnCount: Int = 0,
    val categoryCount: Int = 0,
)

class SettingsViewModel(private val repo: LedgerRepository) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        repo.observeAllTxns().map { it.size },
        repo.observeCategories().map { it.size },
    ) { txnCount, categoryCount ->
        SettingsUiState(txnCount = txnCount, categoryCount = categoryCount)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun clearAllData() {
        viewModelScope.launch { repo.clearAllData() }
    }

    /**
     * 生成 CSV 文本。开头写入 UTF-8 BOM，否则 Excel 打开中文会乱码。
     */
    suspend fun buildCsv(): String = withContext(Dispatchers.IO) {
        val txns = repo.observeAllTxns().first()
        val categories = repo.observeCategories().first().associateBy { it.id }

        buildString {
            append('\uFEFF')
            append("日期,类型,分类,金额(元),备注\r\n")
            txns.sortedBy { it.dateMillis }.forEach { txn ->
                val categoryName = categories[txn.categoryId]?.name ?: "未分类"
                val note = txn.note
                    .replace("\"", "\"\"")
                    .replace(",", "，")
                    .replace("\r", " ")
                    .replace("\n", " ")
                append(txn.dateMillis.toLocalDate().toString()).append(',')
                append(if (txn.type.isExpense) "支出" else "收入").append(',')
                append('"').append(categoryName).append('"').append(',')
                append(formatCentsPlain(txn.amountCents)).append(',')
                append('"').append(note).append('"')
                append("\r\n")
            }
        }
    }
}
