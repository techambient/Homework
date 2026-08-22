package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.Subject
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjectsFlow(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    suspend fun getAllSubjects(): List<Subject>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Long): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subject: Subject): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(subjects: List<Subject>)

    @Update
    suspend fun update(subject: Subject)

    @Delete
    suspend fun delete(subject: Subject)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteById(id: Long)
}
