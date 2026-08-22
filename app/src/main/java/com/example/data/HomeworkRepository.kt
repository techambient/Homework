package com.example.data

import android.content.Context
import android.util.Log
import com.example.alarm.HomeworkAlarmScheduler
import com.example.model.AutoCleanupUtils
import com.example.model.Homework
import com.example.model.Subject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class HomeworkRepository(
    private val context: Context,
    private val homeworkDao: HomeworkDao,
    private val subjectDao: SubjectDao
) {
    private val TAG = "HomeworkRepository"

    val allHomeworkFlow: Flow<List<Homework>> = homeworkDao.getAllHomeworkFlow()
    val pendingHomeworkFlow: Flow<List<Homework>> = homeworkDao.getPendingHomeworkFlow()
    val completedHomeworkFlow: Flow<List<Homework>> = homeworkDao.getCompletedHomeworkFlow()
    val allSubjectsFlow: Flow<List<Subject>> = subjectDao.getAllSubjectsFlow()

    fun getHomeworkByIdFlow(id: Long): Flow<Homework?> = homeworkDao.getHomeworkByIdFlow(id)

    suspend fun getHomeworkById(id: Long): Homework? = withContext(Dispatchers.IO) {
        homeworkDao.getHomeworkById(id)
    }

    /**
     * Executes auto-cleanup of completed homeworks according to rule:
     * - Mon-Thu completed: deleted after 24h
     * - Fri/Weekend completed: deleted after Sunday midnight
     */
    suspend fun cleanupExpiredHomeworks(): Int = withContext(Dispatchers.IO) {
        val completedList = homeworkDao.getAllCompletedHomework()
        val now = System.currentTimeMillis()
        var deletedCount = 0

        for (hw in completedList) {
            if (AutoCleanupUtils.isExpired(hw.completedTimestamp, now)) {
                Log.d(TAG, "Auto-cleaning expired homework: '${hw.title}' completed at ${hw.completedTimestamp}")
                homeworkDao.delete(hw)
                deletedCount++
            }
        }
        deletedCount
    }

    suspend fun insertHomework(homework: Homework): Long = withContext(Dispatchers.IO) {
        val insertedId = homeworkDao.insert(homework)
        val savedHomework = homework.copy(id = insertedId)
        HomeworkAlarmScheduler.scheduleHomeworkAlarms(context, savedHomework)
        insertedId
    }

    suspend fun updateHomework(homework: Homework) = withContext(Dispatchers.IO) {
        homeworkDao.update(homework)
        HomeworkAlarmScheduler.cancelAlarms(context, homework.id)
        if (!homework.isDone) {
            HomeworkAlarmScheduler.scheduleHomeworkAlarms(context, homework)
        }
    }

    suspend fun deleteHomework(homework: Homework) = withContext(Dispatchers.IO) {
        HomeworkAlarmScheduler.cancelAlarms(context, homework.id)
        homeworkDao.delete(homework)
    }

    suspend fun deleteHomeworkById(id: Long) = withContext(Dispatchers.IO) {
        HomeworkAlarmScheduler.cancelAlarms(context, id)
        homeworkDao.deleteById(id)
    }

    suspend fun markHomeworkDone(id: Long, isDone: Boolean) = withContext(Dispatchers.IO) {
        if (isDone) {
            val now = System.currentTimeMillis()
            homeworkDao.updateDoneStatus(id, true, now, false)
            HomeworkAlarmScheduler.cancelAlarms(context, id)
        } else {
            homeworkDao.updateDoneStatus(id, false, null, false)
            val hw = homeworkDao.getHomeworkById(id)
            if (hw != null) {
                HomeworkAlarmScheduler.scheduleHomeworkAlarms(context, hw)
            }
        }
    }

    suspend fun markPackedInBag(id: Long) = withContext(Dispatchers.IO) {
        homeworkDao.updateBagStatus(id, isPacked = true, bagReminder = false)
        // Cancel pending bag reminder if any
        val hw = homeworkDao.getHomeworkById(id)
        if (hw != null) {
            HomeworkAlarmScheduler.cancelAlarms(context, id)
        }
    }

    suspend fun scheduleBagReminder(id: Long, delayMinutes: Int = 5) = withContext(Dispatchers.IO) {
        homeworkDao.updateBagStatus(id, isPacked = false, bagReminder = true)
        val hw = homeworkDao.getHomeworkById(id)
        if (hw != null) {
            HomeworkAlarmScheduler.scheduleBagReminder(context, hw.id, hw.title, hw.subjectName, delayMinutes)
        }
    }

    suspend fun updateAlarmTime(id: Long, newAlarmTime: Long) = withContext(Dispatchers.IO) {
        homeworkDao.updateAlarmTime(id, newAlarmTime)
        val hw = homeworkDao.getHomeworkById(id)
        if (hw != null && !hw.isDone) {
            HomeworkAlarmScheduler.cancelAlarms(context, id)
            HomeworkAlarmScheduler.scheduleHomeworkAlarms(context, hw.copy(alarmTimestamp = newAlarmTime))
        }
    }

    suspend fun insertSubject(subject: Subject): Long = withContext(Dispatchers.IO) {
        subjectDao.insert(subject)
    }

    suspend fun deleteSubject(subject: Subject) = withContext(Dispatchers.IO) {
        subjectDao.delete(subject)
    }
}
