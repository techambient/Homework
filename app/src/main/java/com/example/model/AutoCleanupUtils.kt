package com.example.model

import java.util.Calendar
import java.util.Date
import java.util.Locale

object AutoCleanupUtils {

    /**
     * Calculates the exact auto-delete timestamp based on when the homework was completed.
     * Rule:
     * - If completed on Monday, Tuesday, Wednesday, or Thursday: auto-deletes 24 hours after completion.
     * - If completed on Friday: auto-deletes after Sunday (i.e., next Monday 00:00:00).
     * - If completed on Saturday or Sunday: also auto-deletes after Sunday (i.e., next Monday 00:00:00).
     */
    fun calculateAutoDeleteTime(completedTimestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = completedTimestamp
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        return when (dayOfWeek) {
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY -> {
                // 24 hours after completion timestamp
                completedTimestamp + (24L * 60L * 60L * 1000L)
            }
            Calendar.FRIDAY -> {
                // Auto delete after Sunday: Calculate upcoming Monday at 00:00:00
                val target = Calendar.getInstance().apply {
                    timeInMillis = completedTimestamp
                    // Move forward to Monday (3 days after Friday)
                    add(Calendar.DAY_OF_YEAR, 3)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                target.timeInMillis
            }
            Calendar.SATURDAY -> {
                // Move forward to Monday (2 days after Saturday)
                val target = Calendar.getInstance().apply {
                    timeInMillis = completedTimestamp
                    add(Calendar.DAY_OF_YEAR, 2)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                target.timeInMillis
            }
            Calendar.SUNDAY -> {
                // Move forward to Monday (1 day after Sunday)
                val target = Calendar.getInstance().apply {
                    timeInMillis = completedTimestamp
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                target.timeInMillis
            }
            else -> completedTimestamp + (24L * 60L * 60L * 1000L)
        }
    }

    /**
     * Checks if the completed homework has expired and should be deleted.
     */
    fun isExpired(completedTimestamp: Long?, currentTimestamp: Long = System.currentTimeMillis()): Boolean {
        if (completedTimestamp == null || completedTimestamp <= 0) return false
        val expireTime = calculateAutoDeleteTime(completedTimestamp)
        return currentTimestamp >= expireTime
    }

    /**
     * Returns human-readable remaining time until auto-deletion.
     */
    fun getRemainingTimeFormatted(completedTimestamp: Long, currentTimestamp: Long = System.currentTimeMillis()): String {
        val expireTime = calculateAutoDeleteTime(completedTimestamp)
        val diffMs = expireTime - currentTimestamp
        if (diffMs <= 0) return "Expiring soon"

        val hours = diffMs / (1000 * 60 * 60)
        val minutes = (diffMs % (1000 * 60 * 60)) / (1000 * 60)

        val cal = Calendar.getInstance().apply { timeInMillis = completedTimestamp }
        val isWeekendRule = cal.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)

        return if (isWeekendRule && hours > 24) {
            val days = hours / 24
            "Auto-deletes after Sunday (~${days}d ${hours % 24}h)"
        } else if (hours > 0) {
            "Auto-deletes in ${hours}h ${minutes}m"
        } else {
            "Auto-deletes in ${minutes}m"
        }
    }

    /**
     * Returns explanation for the cleanup rule applied.
     */
    fun getCleanupRuleDescription(completedTimestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = completedTimestamp }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.FRIDAY -> "Completed on Friday (Kept through weekend, deletes after Sunday midnight)"
            Calendar.SATURDAY, Calendar.SUNDAY -> "Completed on Weekend (Deletes after Sunday midnight)"
            else -> "Completed on ${cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())} (Auto-deletes after 24h)"
        }
    }
}
