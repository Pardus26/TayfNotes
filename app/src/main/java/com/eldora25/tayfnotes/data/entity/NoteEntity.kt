package com.eldora25.tayfnotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.model.NoteType

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val colorHex: String,
    val type: String, // Store as String for simplicity in Room
    val tags: String, // Store as comma-separated string
    val createdAt: Long,
    val isLocked: Boolean
) {
    fun toDomain(): Note = Note(
        id = id,
        title = title,
        content = content,
        colorHex = colorHex,
        type = NoteType.valueOf(type),
        tags = if (tags.isEmpty()) emptyList() else tags.split(","),
        createdAt = createdAt,
        isLocked = isLocked
    )

    companion object {
        fun fromDomain(note: Note): NoteEntity = NoteEntity(
            id = note.id,
            title = note.title,
            content = note.content,
            colorHex = note.colorHex,
            type = note.type.name,
            tags = note.tags.joinToString(","),
            createdAt = note.createdAt,
            isLocked = note.isLocked
        )
    }
}
