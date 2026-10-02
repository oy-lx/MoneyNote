package com.moneynote.data.local

import androidx.room.TypeConverter
import com.moneynote.data.model.TxnType

class Converters {

    @TypeConverter
    fun fromTxnType(value: TxnType): String = value.name

    @TypeConverter
    fun toTxnType(value: String): TxnType = TxnType.valueOf(value)
}
