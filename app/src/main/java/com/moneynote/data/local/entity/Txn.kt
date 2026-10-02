package com.moneynote.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.moneynote.data.model.TxnType

/**
 * 一条流水记录。
 *
 * 金额统一以 **分** 为单位存 [Long]，彻底规避浮点累加误差。
 */
@Entity(
    tableName = "txns",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId"), Index("dateMillis")],
)
data class Txn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** 金额（分），恒为正数，收支方向由 [type] 决定。 */
    val amountCents: Long,
    val type: TxnType,
    val categoryId: Long? = null,
    val note: String = "",
    /** 记账日期，取当天 00:00 的本地时间戳。 */
    val dateMillis: Long,
    val createdAt: Long = System.currentTimeMillis(),
)
