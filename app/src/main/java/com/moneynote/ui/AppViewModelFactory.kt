package com.moneynote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.moneynote.MoneyNoteApp
import com.moneynote.data.repository.LedgerRepository

/**
 * 从 Application 中取出 [LedgerRepository] 来构造 ViewModel 的通用工厂。
 *
 * 用法：`viewModel(factory = appViewModelFactory { HomeViewModel(it) })`
 *
 * 这里刻意手写 [ViewModelProvider.Factory] 而不是用 `viewModelFactory { initializer { ... } }`：
 * 后者的 `initializer` 是 reified 内联函数，无法接受外层未具体化的类型参数 `VM`。
 */
fun <VM : ViewModel> appViewModelFactory(
    build: (LedgerRepository) -> VM,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MoneyNoteApp
        return build(app.repository) as T
    }
}
