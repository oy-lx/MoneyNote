package com.moneynote.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

private val zone: ZoneId get() = ZoneId.systemDefault()

/** 取该日期本地时区当天 00:00 的时间戳，保证按天查询不会漏记录。 */
fun LocalDate.toEpochMillis(): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

/**
 * Material3 DatePicker 的 selectedDateMillis 以 UTC 零点为准，
 * 与上面按本地时区换算的 [toEpochMillis] 不是一回事，必须分开处理。
 */
fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun Long.utcMillisToLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

