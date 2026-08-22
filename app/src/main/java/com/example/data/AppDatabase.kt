package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.Homework
import com.example.model.Subject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Homework::class, Subject::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun homeworkDao(): HomeworkDao
    abstract fun subjectDao(): SubjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "homework_database"
                )
                .addCallback(DatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.subjectDao(), database.homeworkDao())
                    }
                }
            }
        }

        private suspend fun populateInitialData(subjectDao: SubjectDao, homeworkDao: HomeworkDao) {
            subjectDao.insertAll(Subject.DEFAULT_SUBJECTS)

            // Seed a couple sample homework tasks to get students started nicely
            val now = System.currentTimeMillis()
            val oneHourLater = now + (60 * 60 * 1000L)
            val tomorrow = now + (24 * 60 * 60 * 1000L)

            val sampleHomework1 = Homework(
                title = "Calculus Chapter 4 Exercises 1-15",
                subjectName = "Mathematics",
                subjectColor = 0xFF2563EB,
                description = "Complete odd-numbered problems and show full step-by-step working.",
                dueTimestamp = tomorrow + (8 * 60 * 60 * 1000L),
                alarmTimestamp = oneHourLater,
                hasPreReminder = true,
                estimatedMinutes = 45,
                priority = "HIGH"
            )

            val sampleHomework2 = Homework(
                title = "Cell Division Lab Report Summary",
                subjectName = "Science",
                subjectColor = 0xFF059669,
                description = "Write a 1-page hypothesis and conclusion on mitosis phases.",
                dueTimestamp = tomorrow + (14 * 60 * 60 * 1000L),
                alarmTimestamp = now + (3 * 60 * 60 * 1000L),
                hasPreReminder = true,
                estimatedMinutes = 30,
                priority = "MEDIUM"
            )

            val sampleHomework3 = Homework(
                title = "Read Shakespeare Act 2 Scene 3",
                subjectName = "English",
                subjectColor = 0xFF7C3AED,
                description = "Highlight key metaphors and character motivations.",
                dueTimestamp = tomorrow + (10 * 60 * 60 * 1000L),
                alarmTimestamp = now + (5 * 60 * 60 * 1000L),
                hasPreReminder = true,
                estimatedMinutes = 25,
                priority = "LOW"
            )

            homeworkDao.insert(sampleHomework1)
            homeworkDao.insert(sampleHomework2)
            homeworkDao.insert(sampleHomework3)
        }
    }
}
