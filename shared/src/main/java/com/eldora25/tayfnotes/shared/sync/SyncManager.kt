package com.eldora25.tayfnotes.shared.sync

import com.eldora25.tayfnotes.shared.model.Note
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

class SyncManager {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    suspend fun syncNotes(notes: List<Note>): Result<Unit> {
        return try {
            // Simulated backup to "Tayfun Yamak" cloud
            println("Syncing ${notes.size} notes to cloud...")
            delay(2000) // Simulate network delay
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
