package com.eldora25.tayfnotes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.eldora25.tayfnotes.data.repository.FolderRepository
import com.eldora25.tayfnotes.data.repository.NoteRepository
import com.eldora25.tayfnotes.shared.model.Folder
import com.eldora25.tayfnotes.shared.model.Note
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class NoteViewModel(
    private val noteRepository: NoteRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

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
}

class NoteViewModelFactory(
    private val noteRepository: NoteRepository,
    private val folderRepository: FolderRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(noteRepository, folderRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
