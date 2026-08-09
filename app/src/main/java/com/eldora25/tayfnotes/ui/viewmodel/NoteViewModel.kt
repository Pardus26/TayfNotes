package com.eldora25.tayfnotes.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.eldora25.tayfnotes.data.repository.FolderRepository
import com.eldora25.tayfnotes.data.repository.NoteRepository
import com.eldora25.tayfnotes.shared.model.Folder
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.sync.SyncManager
import com.eldora25.tayfnotes.ui.theme.TayfTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "settings")

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class NoteViewModel(
    application: Application,
    private val noteRepository: NoteRepository,
    private val folderRepository: FolderRepository
) : AndroidViewModel(application) {

    private val dataStore = application.dataStore
    private val syncManager = SyncManager()
    
    // Theme Keys
    private val THEME_KEY = stringPreferencesKey("app_theme")
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    private val BIOMETRIC_KEY = booleanPreferencesKey("biometric_lock")

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    val currentTheme: StateFlow<TayfTheme> = dataStore.data
        .map { pref -> 
            val themeName = pref[THEME_KEY] ?: TayfTheme.MIDNIGHT.name
            TayfTheme.valueOf(themeName)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TayfTheme.MIDNIGHT)

    val isDarkMode: StateFlow<Boolean?> = dataStore.data
        .map { pref -> pref[DARK_MODE_KEY] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isBiometricEnabled: StateFlow<Boolean> = dataStore.data
        .map { pref -> pref[BIOMETRIC_KEY] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val allFolders: StateFlow<List<Folder>> = folderRepository.allFolders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val notes: StateFlow<List<Note>> = combine(_searchQuery, _selectedFolderId) { query, folderId ->
        Pair(query, folderId)
    }
    .debounce(300)
    .flatMapLatest { (query, folderId) ->
        when {
            query.isNotEmpty() -> noteRepository.search(query)
            folderId != null -> noteRepository.getNotesByFolder(folderId)
            else -> noteRepository.allNotes
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFolderSelected(folderId: String?) {
        _selectedFolderId.value = folderId
    }

    fun setTheme(theme: TayfTheme) {
        viewModelScope.launch {
            dataStore.edit { it[THEME_KEY] = theme.name }
        }
    }

    fun setDarkMode(enabled: Boolean?) {
        viewModelScope.launch {
            dataStore.edit { 
                if (enabled == null) it.remove(DARK_MODE_KEY) else it[DARK_MODE_KEY] = enabled
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[BIOMETRIC_KEY] = enabled }
        }
    }

    fun saveNote(note: Note) {
        viewModelScope.launch {
            noteRepository.insert(note.copy(lastModified = System.currentTimeMillis()))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteRepository.delete(note)
        }
    }

    fun addFolder(name: String, colorHex: String) {
        viewModelScope.launch {
            folderRepository.insert(Folder(id = System.currentTimeMillis().toString(), name = name, colorHex = colorHex))
        }
    }
    
    fun updateFolder(folder: Folder) {
        viewModelScope.launch {
            folderRepository.insert(folder)
        }
    }

    fun syncData() {
        viewModelScope.launch {
            _isSyncing.value = true
            val currentNotes = notes.value
            syncManager.syncNotes(currentNotes)
            _isSyncing.value = false
        }
    }
}

class NoteViewModelFactory(
    private val application: Application,
    private val noteRepository: NoteRepository,
    private val folderRepository: FolderRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(application, noteRepository, folderRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
