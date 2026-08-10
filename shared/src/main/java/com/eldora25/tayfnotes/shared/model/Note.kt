package com.eldora25.tayfnotes.shared.model

/**
 * TayfNotes Hybrid Note Model
 */
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val colorHex: String = "#FFFFFF",
    val type: NoteType = NoteType.TEXT,
    val tags: List<String> = emptyList(),
    val folderId: String? = null,
    val imageUris: List<String> = emptyList(), // New: Support for multiple images
    val audioPath: String? = null, // New: Support for one voice note per note
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val reminderTimestamp: Long? = null,
    val isLocked: Boolean = false
)

enum class NoteType {
    TEXT, CHECKLIST
}

data class Folder(
    val id: String,
    val name: String,
    val colorHex: String = "#757575",
    val noteCount: Int = 0
)
