package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class HomeworkPriority(val label: String, val colorHex: Long) {
    HIGH("High Priority", 0xFFEF4444),
    MEDIUM("Medium", 0xFFF59E0B),
    LOW("Low", 0xFF10B981);

    companion object {
        fun fromString(value: String): HomeworkPriority {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}

@Entity(tableName = "homeworks")
data class Homework(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subjectName: String,
    val subjectColor: Long = 0xFF4F46E5,
    val description: String = "",
    val dueTimestamp: Long, // Date and time due
    val alarmTimestamp: Long, // Desired "Do Homework" alarm time
    val hasPreReminder: Boolean = true, // 2-min before reminder
    val estimatedMinutes: Int = 30,
    val priority: String = HomeworkPriority.MEDIUM.name,
    val isDone: Boolean = false,
    val completedTimestamp: Long? = null,
    val isPackedInBag: Boolean = false,
    val bagReminderScheduled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val priorityEnum: HomeworkPriority
        get() = HomeworkPriority.fromString(priority)

    val isAlarmActive: Boolean
        get() = !isDone && alarmTimestamp > System.currentTimeMillis()

    val preReminderTimestamp: Long
        get() = alarmTimestamp - (2 * 60 * 1000L) // 2 minutes before
}
