package com.moneynote.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun `本地时间戳往返转换保持同一天`() {
        val date = LocalDate.of(2025, 6, 15)
        assertEquals(date, date.toEpochMillis().toLocalDate())
    }

    @Test
    fun `本地零点时间戳落回当天`() {
        val date = LocalDate.of(2025, 1, 1)
        val millis = date.toEpochMillis()
        assertEquals(date, millis.toLocalDate())
    }

    @Test
    fun `DatePicker 的 UTC 时间戳往返转换保持同一天`() {
        val date = LocalDate.of(2025, 12, 31)
        assertEquals(date, date.toUtcMillis().utcMillisToLocalDate())
    }

    @Test
    fun `月末与年初边界都能正确转换`() {
        listOf(
            LocalDate.of(2024, 2, 29),
            LocalDate.of(2025, 2, 28),
            LocalDate.of(2025, 3, 1),
            LocalDate.of(2025, 12, 31),
        ).forEach { date ->
            assertEquals(date, date.toEpochMillis().toLocalDate())
        }
    }
}
