package com.moneynote.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.moneynote.data.model.TxnType

/**
 * 收支分类。
 *
 * [emoji] 直接作为分类图标使用，避免引入体积庞大的图标库，
 * 同时排版上比矢量图更直观。
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val emoji: String,
    val type: TxnType,
    val sortOrder: Int = 0,
    /** 系统内置分类，仅用于展示提示，允许修改与删除。 */
    val isBuiltIn: Boolean = false,
    /** 软删除标记：已被历史流水引用时改为归档，避免外键置空后丢失统计口径。 */
    val archived: Boolean = false,
)
