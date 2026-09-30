package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    private val shortMonthYearFormat = SimpleDateFormat("MMM yyyy", Locale.US)
    private val yearMonthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    private val monthShortFormat = SimpleDateFormat("MMM", Locale.US)
    private val dayMonthFormat = SimpleDateFormat("MMM dd", Locale.US)
    private val fullDateFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US)
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
    private val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.US)

    fun getCurrentYearMonth(): String {
        return yearMonthKeyFormat.format(Date())
    }

    fun formatYearMonth(yearMonth: String): String {
        return try {
            val date = yearMonthKeyFormat.parse(yearMonth) ?: Date()
            monthYearFormat.format(date)
        } catch (e: Exception) {
            yearMonth
        }
    }

    fun formatShortMonth(yearMonth: String): String {
        return try {
            val date = yearMonthKeyFormat.parse(yearMonth) ?: Date()
            monthShortFormat.format(date)
        } catch (e: Exception) {
            yearMonth
        }
    }

    fun getPreviousMonth(yearMonth: String): String {
        return try {
            val cal = Calendar.getInstance()
            val date = yearMonthKeyFormat.parse(yearMonth) ?: Date()
            cal.time = date
            cal.add(Calendar.MONTH, -1)
            yearMonthKeyFormat.format(cal.time)
        } catch (e: Exception) {
            yearMonth
        }
    }

    fun getNextMonth(yearMonth: String): String {
        return try {
            val cal = Calendar.getInstance()
            val date = yearMonthKeyFormat.parse(yearMonth) ?: Date()
            cal.time = date
            cal.add(Calendar.MONTH, 1)
            yearMonthKeyFormat.format(cal.time)
        } catch (e: Exception) {
            yearMonth
        }
    }

    fun getMonthRangeMillis(yearMonth: String): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val date = try {
            yearMonthKeyFormat.parse(yearMonth) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        cal.time = date
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    fun getDaysInMonth(yearMonth: String): Int {
        val cal = Calendar.getInstance()
        val date = try {
            yearMonthKeyFormat.parse(yearMonth) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        cal.time = date
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun getDayOfMonth(timestamp: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return cal.get(Calendar.DAY_OF_MONTH)
    }

    fun getDayOfWeek(timestamp: Long): String {
        return dayOfWeekFormat.format(Date(timestamp))
    }

    fun getDayOfWeekIndex(timestamp: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        // Calendar.SUNDAY = 1, MONDAY = 2...
        return cal.get(Calendar.DAY_OF_WEEK)
    }

    fun formatDate(timestamp: Long): String {
        val now = Calendar.getInstance()
        val itemCal = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR) -> "Today"
            now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) - itemCal.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"
            else -> dayMonthFormat.format(Date(timestamp))
        }
    }

    fun formatFullDate(timestamp: Long): String {
        return fullDateFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun getLastNMonths(count: Int, referenceYearMonth: String = getCurrentYearMonth()): List<String> {
        val list = mutableListOf<String>()
        var current = referenceYearMonth
        for (i in 0 until count) {
            list.add(0, current)
            current = getPreviousMonth(current)
        }
        return list
    }
}
