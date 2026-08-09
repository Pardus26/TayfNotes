package com.eldora25.tayfnotes.data.repository

import com.eldora25.tayfnotes.data.dao.FolderDao
import com.eldora25.tayfnotes.data.entity.FolderEntity
import com.eldora25.tayfnotes.shared.model.Folder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FolderRepository(private val folderDao: FolderDao) {
    val allFolders: Flow<List<Folder>> = folderDao.getAllFolders().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun insert(folder: Folder) {
        folderDao.insertFolder(FolderEntity.fromDomain(folder))
    }

    suspend fun delete(folder: Folder) {
        folderDao.deleteFolder(FolderEntity.fromDomain(folder))
    }
}
