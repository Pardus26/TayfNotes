package com.eldora25.tayfnotes.data.dao

import androidx.room.*
import com.eldora25.tayfnotes.data.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY lastModified DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY lastModified DESC")
    fun getNotesByFolder(folderId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE title LIKE :query OR content LIKE :query ORDER BY lastModified DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?
}
