package com.eldora25.tayfnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.eldora25.tayfnotes.data.database.AppDatabase
import com.eldora25.tayfnotes.data.repository.FolderRepository
import com.eldora25.tayfnotes.data.repository.NoteRepository
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.ui.*
import com.eldora25.tayfnotes.ui.components.AddNoteDialog
import com.eldora25.tayfnotes.ui.components.BottomNavigationBar
import com.eldora25.tayfnotes.ui.theme.TayfNotesTheme
import com.eldora25.tayfnotes.ui.viewmodel.NoteViewModel
import com.eldora25.tayfnotes.ui.viewmodel.NoteViewModelFactory

sealed class Screen {
    object Main : Screen()
    object Folders : Screen()
    object Calendar : Screen()
    object More : Screen()
    object Settings : Screen()
    data class EditNote(val note: Note? = null) : Screen()
}

class MainActivity : ComponentActivity() {
    
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val noteRepository by lazy { NoteRepository(database.noteDao()) }
    private val folderRepository by lazy { FolderRepository(database.folderDao()) }
    
    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(noteRepository, folderRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TayfNotesTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
                val notes by noteViewModel.notes.collectAsState()
                val searchQuery by noteViewModel.searchQuery.collectAsState()
                val folders by noteViewModel.allFolders.collectAsState()
                val isSyncing by noteViewModel.isSyncing.collectAsState()
                
                var showAddNoteDialog by remember { mutableStateOf(false) }

                if (showAddNoteDialog) {
                    AddNoteDialog(
                        onDismiss = { showAddNoteDialog = false },
                        onTypeSelected = { type ->
                            showAddNoteDialog = false
                            currentScreen = Screen.EditNote(Note(
                                id = System.currentTimeMillis().toString(),
                                title = "",
                                content = "",
                                type = type
                            ))
                        }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (currentScreen is Screen.EditNote) {
                        val screen = currentScreen as Screen.EditNote
                        BackHandler { currentScreen = Screen.Main }
                        NoteEditorScreen(
                            note = screen.note,
                            folders = folders,
                            onBack = { currentScreen = Screen.Main },
                            onSave = { noteViewModel.saveNote(it) },
                            onDelete = { 
                                noteViewModel.deleteNote(it)
                                currentScreen = Screen.Main
                            }
                        )
                    } else {
                        Scaffold(
                            bottomBar = {
                                BottomNavigationBar(
                                    currentScreen = currentScreen,
                                    onScreenChange = { currentScreen = it }
                                )
                            }
                        ) { innerPadding ->
                            Surface(modifier = Modifier.padding(innerPadding)) {
                                when (currentScreen) {
                                    is Screen.Main -> MainScreen(
                                        notes = notes,
                                        searchQuery = searchQuery,
                                        onSearchQueryChanged = { noteViewModel.onSearchQueryChanged(it) },
                                        onAddNote = { showAddNoteDialog = true },
                                        onEditNote = { note -> currentScreen = Screen.EditNote(note) }
                                    )
                                    is Screen.Folders -> FoldersScreen(
                                        folders = folders,
                                        onFolderClick = { folder ->
                                            noteViewModel.onFolderSelected(folder.id)
                                            currentScreen = Screen.Main
                                        },
                                        onAddFolder = { name -> noteViewModel.addFolder(name, "#D4AF37") },
                                        onUpdateFolder = { folder -> noteViewModel.updateFolder(folder) }
                                    )
                                    is Screen.Calendar -> CalendarScreen(
                                        notes = notes,
                                        onEditNote = { note -> currentScreen = Screen.EditNote(note) }
                                    )
                                    is Screen.More -> MoreScreen(onScreenChange = { currentScreen = it })
                                    is Screen.Settings -> SettingsScreen(
                                        onBack = { currentScreen = Screen.More },
                                        isSyncing = isSyncing,
                                        onSyncClick = { noteViewModel.syncData() }
                                    )
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
