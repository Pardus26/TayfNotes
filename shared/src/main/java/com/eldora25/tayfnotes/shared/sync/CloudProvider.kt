package com.eldora25.tayfnotes.shared.sync

interface CloudProvider {
    val name: String
    val authUrl: String
    suspend fun uploadFile(filePath: String, destinationName: String): Result<Unit>
    suspend fun downloadFile(remoteName: String, localPath: String): Result<Unit>
    fun isAuthorized(): Boolean
}

class GoogleDriveProvider : CloudProvider {
    override val name: String = "Google Drive"
    override val authUrl: String = "https://accounts.google.com/o/oauth2/v2/auth?client_id=YOUR_CLIENT_ID&..."
    override suspend fun uploadFile(filePath: String, destinationName: String): Result<Unit> {
        return Result.success(Unit)
    }
    override suspend fun downloadFile(remoteName: String, localPath: String): Result<Unit> {
        return Result.success(Unit)
    }
    override fun isAuthorized(): Boolean = false // Real check needed
}

class DropboxProvider : CloudProvider {
    override val name: String = "Dropbox"
    override val authUrl: String = "https://www.dropbox.com/oauth2/authorize?client_id=YOUR_CLIENT_ID&..."
    override suspend fun uploadFile(filePath: String, destinationName: String): Result<Unit> {
        return Result.success(Unit)
    }
    override suspend fun downloadFile(remoteName: String, localPath: String): Result<Unit> {
        return Result.success(Unit)
    }
    override fun isAuthorized(): Boolean = false
}
