package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.Homework
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeworkDao {

    @Query("SELECT * FROM homeworks ORDER BY isDone ASC, alarmTimestamp ASC, dueTimestamp ASC")
    fun getAllHomeworkFlow(): Flow<List<Homework>>

    @Query("SELECT * FROM homeworks WHERE isDone = 0 ORDER BY alarmTimestamp ASC, dueTimestamp ASC")
    fun getPendingHomeworkFlow(): Flow<List<Homework>>

    @Query("SELECT * FROM homeworks WHERE isDone = 1 ORDER BY completedTimestamp DESC")
    fun getCompletedHomeworkFlow(): Flow<List<Homework>>

    @Query("SELECT * FROM homeworks WHERE id = :id")
    suspend fun getHomeworkById(id: Long): Homework?

    @Query("SELECT * FROM homeworks WHERE id = :id")
    fun getHomeworkByIdFlow(id: Long): Flow<Homework?>

    @Query("SELECT * FROM homeworks WHERE isDone = 0")
    suspend fun getAllPendingHomework(): List<Homework>

    @Query("SELECT * FROM homeworks WHERE isDone = 1")
    suspend fun getAllCompletedHomework(): List<Homework>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(homework: Homework): Long

    @Update
    suspend fun update(homework: Homework)

    @Delete
    suspend fun delete(homework: Homework)

    @Query("DELETE FROM homeworks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE homeworks SET isDone = :isDone, completedTimestamp = :completedTimestamp, isPackedInBag = :isPacked WHERE id = :id")
    suspend fun updateDoneStatus(id: Long, isDone: Boolean, completedTimestamp: Long?, isPacked: Boolean)

    @Query("UPDATE homeworks SET isPackedInBag = :isPacked, bagReminderScheduled = :bagReminder WHERE id = :id")
    suspend fun updateBagStatus(id: Long, isPacked: Boolean, bagReminder: Boolean)

    @Query("UPDATE homeworks SET alarmTimestamp = :newAlarmTime WHERE id = :id")
    suspend fun updateAlarmTime(id: Long, newAlarmTime: Long)
}
