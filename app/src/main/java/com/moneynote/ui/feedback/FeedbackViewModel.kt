package com.moneynote.ui.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneynote.data.repository.LedgerRepository
import com.moneynote.util.DeviceInfo
import com.moneynote.util.FeedbackCategory
import com.moneynote.util.FeedbackComposer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class FeedbackUiState(
    val category: FeedbackCategory = FeedbackCategory.SUGGESTION,
    val message: String = "",
    val contact: String = "",
    val includeDeviceInfo: Boolean = true,
    val txnCount: Int = 0,
) {
    val canSend: Boolean get() = message.isNotBlank()
}

class FeedbackViewModel(
    repo: LedgerRepository,
    private val deviceInfo: DeviceInfo,
) : ViewModel() {

    private val form = MutableStateFlow(FeedbackUiState())

    val uiState: StateFlow<FeedbackUiState> = combine(
        form,
        repo.observeAllTxns().map { it.size },
    ) { state, txnCount ->
        state.copy(txnCount = txnCount)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedbackUiState(),
    )

    fun selectCategory(category: FeedbackCategory) {
        form.update { it.copy(category = category) }
    }

    fun onMessageChange(text: String) {
        form.update { it.copy(message = text.take(MAX_MESSAGE_LENGTH)) }
    }

    fun onContactChange(text: String) {
        form.update { it.copy(contact = text.take(MAX_CONTACT_LENGTH)) }
    }

    fun setIncludeDeviceInfo(include: Boolean) {
        form.update { it.copy(includeDeviceInfo = include) }
    }

    fun composeSubject(): String = FeedbackComposer.subject(uiState.value.category)

    fun composeBody(): String {
        val state = uiState.value
        return FeedbackComposer.body(
            category = state.category,
            message = state.message,
            contact = state.contact,
            deviceInfo = deviceInfo,
            txnCount = state.txnCount,
            includeDeviceInfo = state.includeDeviceInfo,
        )
    }

    companion object {
        const val MAX_MESSAGE_LENGTH = 1000
        const val MAX_CONTACT_LENGTH = 60
    }
}
