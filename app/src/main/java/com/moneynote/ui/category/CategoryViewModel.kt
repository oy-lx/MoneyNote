package com.moneynote.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneynote.data.local.entity.Category
import com.moneynote.data.model.TxnType
import com.moneynote.data.repository.LedgerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryUiState(
    val type: TxnType = TxnType.EXPENSE,
    val categories: List<Category> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModel(private val repo: LedgerRepository) : ViewModel() {

    private val selectedType = MutableStateFlow(TxnType.EXPENSE)

    val uiState: StateFlow<CategoryUiState> = selectedType
        .flatMapLatest { type ->
            repo.observeCategories(type).map { categories ->
                CategoryUiState(type = type, categories = categories)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoryUiState(),
        )

    fun selectType(type: TxnType) {
        selectedType.value = type
    }

    fun create(name: String, emoji: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val type = selectedType.value
        viewModelScope.launch {
            val order = repo.nextCategorySortOrder(type)
            repo.saveCategory(
                Category(
                    name = trimmed,
                    emoji = emoji.ifBlank { DEFAULT_EMOJI },
                    type = type,
                    sortOrder = order,
                )
            )
        }
    }

    fun update(category: Category, name: String, emoji: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repo.saveCategory(
                category.copy(
                    name = trimmed,
                    emoji = emoji.ifBlank { category.emoji },
                )
            )
        }
    }

    /**
     * 已被流水引用时改为归档，避免历史记录失去分类归属；否则物理删除。
     */
    fun delete(category: Category) {
        viewModelScope.launch {
            val referenced = repo.countTxnsInCategory(category.id) > 0
            repo.deleteOrArchiveCategory(category, referenced)
        }
    }

    companion object {
        const val DEFAULT_EMOJI = "📦"
    }
}
