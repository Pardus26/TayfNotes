package com.eldora25.tayfnotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.eldora25.tayfnotes.shared.model.Folder

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val colorHex: String
) {
    fun toDomain(noteCount: Int = 0): Folder = Folder(
        id = id,
        name = name,
        colorHex = colorHex,
        noteCount = noteCount
    )

    companion object {
        fun fromDomain(folder: Folder): FolderEntity = FolderEntity(
            id = folder.id,
            name = folder.name,
            colorHex = folder.colorHex
        )
    }
}
