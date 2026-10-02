package com.moneynote.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneynote.data.local.entity.Category
import com.moneynote.data.local.entity.Txn
import com.moneynote.data.model.TxnType
import com.moneynote.data.repository.LedgerRepository
import com.moneynote.util.formatCentsPlain
import com.moneynote.util.parseYuanToCents
import com.moneynote.util.toEpochMillis
import com.moneynote.util.toLocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EditTxnUiState(
    val txnId: Long = -1L,
    val isEditing: Boolean = false,
    val type: TxnType = TxnType.EXPENSE,
    val amountText: String = "",
    val categoryId: Long? = null,
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    val createdAt: Long = System.currentTimeMillis(),
    val categories: List<Category> = emptyList(),
    val loaded: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
) {
    val amountCents: Long? get() = parseYuanToCents(amountText)

    val canSave: Boolean get() = (amountCents ?: 0L) > 0L && categoryId != null

    val categoriesOfType: List<Category> get() = categories.filter { it.type == type }
}

class EditTxnViewModel(
    private val repo: LedgerRepository,
    private val txnId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(
        EditTxnUiState(txnId = txnId, isEditing = txnId > 0L)
    )
    val state: StateFlow<EditTxnUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeCategories().collect { categories ->
                _state.update { current ->
                    // 保留用户已选分类；若失效（切换了收支方向 / 分类被删）则回退到该方向第一个
                    val kept = current.categoryId?.takeIf { id -> categories.any { it.id == id } }
                    val fallback = categories.firstOrNull { it.type == current.type }?.id
                    current.copy(categories = categories, categoryId = kept ?: fallback)
                }
            }
        }

        viewModelScope.launch {
            if (txnId > 0L) {
                val txn = repo.findTxn(txnId)
                if (txn != null) {
                    _state.update {
                        it.copy(
                            type = txn.type,
                            amountText = formatCentsPlain(txn.amountCents),
                            categoryId = txn.categoryId,
                            note = txn.note,
                            date = txn.dateMillis.toLocalDate(),
                            createdAt = txn.createdAt,
                            loaded = true,
                        )
                    }
                } else {
                    _state.update { it.copy(loaded = true, deleted = true) }
                }
            } else {
                _state.update { it.copy(loaded = true) }
            }
        }
    }

    fun selectType(type: TxnType) {
        _state.update { current ->
            if (current.type == type) {
                current
            } else {
                current.copy(
                    type = type,
                    categoryId = current.categories.firstOrNull { it.type == type }?.id,
                )
            }
        }
    }

    fun onAmountChange(raw: String) {
        val filtered = raw.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } > 1) return
        val dot = filtered.indexOf('.')
        if (dot >= 0 && filtered.length - dot - 1 > 2) return
        if (filtered.substringBefore('.').length > 9) return
        _state.update { it.copy(amountText = filtered) }
    }

    fun selectCategory(id: Long) {
        _state.update { it.copy(categoryId = id) }
    }

    fun onNoteChange(note: String) {
        _state.update { it.copy(note = note.take(60)) }
    }

    fun setDate(date: LocalDate) {
        _state.update { it.copy(date = date) }
    }

    fun save() {
        val current = _state.value
        val cents = current.amountCents ?: return
        val categoryId = current.categoryId ?: return
        if (cents <= 0L || current.saved) return

        viewModelScope.launch {
            repo.saveTxn(
                Txn(
                    id = if (current.isEditing) current.txnId else 0L,
                    amountCents = cents,
                    type = current.type,
                    categoryId = categoryId,
                    note = current.note.trim(),
                    dateMillis = current.date.toEpochMillis(),
                    createdAt = current.createdAt,
                )
            )
            _state.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        val current = _state.value
        if (!current.isEditing || current.deleted) return
        viewModelScope.launch {
            repo.deleteTxn(current.txnId)
            _state.update { it.copy(deleted = true) }
        }
    }
}
