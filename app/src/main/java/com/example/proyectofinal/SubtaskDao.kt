package com.example.proyectofinal

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE noteId = :noteId")
    fun getSubtasksByNoteId(noteId: Int): Flow<List<Subtask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subtask: Subtask)

    @Update
    suspend fun update(subtask: Subtask)

    @Delete
    suspend fun delete(subtask: Subtask)
    
    @Query("DELETE FROM subtasks WHERE noteId = :noteId")
    suspend fun deleteSubtasksByNoteId(noteId: Int)
}
