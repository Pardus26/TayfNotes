package com.eldora25.tayfnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.ui.MainScreen
import com.eldora25.tayfnotes.ui.NoteEditorScreen
import com.eldora25.tayfnotes.ui.theme.TayfNotesTheme

sealed class Screen {
    object Main : Screen()
    data class EditNote(val note: Note? = null) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TayfNotesTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val screen = currentScreen) {
                        is Screen.Main -> {
                            MainScreen(
                                onAddNote = { currentScreen = Screen.EditNote() },
                                onEditNote = { note -> currentScreen = Screen.EditNote(note) }
                            )
                        }
                        is Screen.EditNote -> {
                            BackHandler { currentScreen = Screen.Main }
                            NoteEditorScreen(
                                note = screen.note,
                                onBack = { currentScreen = Screen.Main },
                                onSave = { /* TODO: Persistence */ currentScreen = Screen.Main }
                            )
                        }
                    }
                }
            }
        }
    }
}
