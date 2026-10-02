package com.moneynote.data.model

/** 记账方向。 */
enum class TxnType {
    /** 支出 */
    EXPENSE,

    /** 收入 */
    INCOME;

    val isExpense: Boolean get() = this == EXPENSE
}
