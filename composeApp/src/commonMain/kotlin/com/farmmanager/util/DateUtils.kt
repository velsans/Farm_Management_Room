package com.farmmanager.util

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun today(): String {
    val date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${date.year}-${date.monthNumber.toString().padStart(2, '0')}-${date.dayOfMonth.toString().padStart(2, '0')}"
}

fun exportTimestamp(): String {
    val dateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return buildString {
        append(dateTime.date.year)
        append(dateTime.date.monthNumber.toString().padStart(2, '0'))
        append(dateTime.date.dayOfMonth.toString().padStart(2, '0'))
        append('_')
        append(dateTime.hour.toString().padStart(2, '0'))
        append(dateTime.minute.toString().padStart(2, '0'))
    }
}

fun currentMonthKey(): String = today().take(7)

fun currentMonthNumber(): Int = today().substring(5, 7).toIntOrNull() ?: 1

fun currentYearKey(): String = today().take(4)
