package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: Long, // e.g. 0xFF4F46E5
    val iconName: String = "book", // book, calculate, science, history, translate, computer, brush, music
    val teacherName: String = "",
    val classroom: String = ""
) {
    companion object {
        val DEFAULT_SUBJECTS = listOf(
            Subject(id = 1, name = "Mathematics", colorHex = 0xFF2563EB, iconName = "calculate"),
            Subject(id = 2, name = "Science", colorHex = 0xFF059669, iconName = "science"),
            Subject(id = 3, name = "English", colorHex = 0xFF7C3AED, iconName = "book"),
            Subject(id = 4, name = "History", colorHex = 0xFFD97706, iconName = "history"),
            Subject(id = 5, name = "Geography", colorHex = 0xFF0284C7, iconName = "public"),
            Subject(id = 6, name = "Computer Science", colorHex = 0xFF4F46E5, iconName = "computer"),
            Subject(id = 7, name = "Art & Design", colorHex = 0xFFDB2777, iconName = "brush"),
            Subject(id = 8, name = "Foreign Language", colorHex = 0xFFEA580C, iconName = "translate")
        )
    }
}
