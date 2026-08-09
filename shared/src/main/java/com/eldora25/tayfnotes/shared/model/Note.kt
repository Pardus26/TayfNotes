package com.eldora25.tayfnotes.shared.model

/**
 * TayfNotes Hybrid Note Model
 * Combines ColorNote's simplicity (colors) with Evernote's richness (tags, types).
 */
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val colorHex: String = "#FFFFFF", // ColorNote style
    val type: NoteType = NoteType.TEXT,
    val tags: List<String> = emptyList(), // Evernote style
    val createdAt: Long = System.currentTimeMillis(),
    val isLocked: Boolean = false
)

enum class NoteType {
    TEXT, CHECKLIST
}
