package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.ui.components.NoteGridItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    notes: List<Note>,
    onEditNote: (Note) -> Unit
) {
    val notesWithReminders = remember(notes) {
        notes.filter { it.reminderTimestamp != null }.sortedBy { it.reminderTimestamp }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Takvim", style = MaterialTheme.typography.headlineMedium) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary Header (Image 9 style)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = SimpleDateFormat("MMMM yyyy", Locale("tr")).format(Date()),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (notesWithReminders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Hatırlatıcısı olan not bulunamadı.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notesWithReminders) { note ->
                        val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale("tr"))
                            .format(Date(note.reminderTimestamp!!))
                        
                        Column {
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            NoteGridItem(note = note, onClick = { onEditNote(note) })
                        }
                    }
                }
            }
        }
    }
}
