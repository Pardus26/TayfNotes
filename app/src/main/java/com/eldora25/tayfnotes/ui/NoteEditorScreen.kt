package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.ui.components.ColorSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: Note? = null,
    onBack: () -> Unit,
    onSave: (Note) -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var colorHex by remember { mutableStateOf(note?.colorHex ?: "#FFFFFF") }
    
    val backgroundColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.surface
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (note == null) "Yeni Not" else "Notu Düzenle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val finalNote = (note ?: Note(
                            id = System.currentTimeMillis().toString(),
                            title = "",
                            content = ""
                        )).copy(
                            title = title,
                            content = content,
                            colorHex = colorHex
                        )
                        onSave(finalNote)
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Kaydet")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor.copy(alpha = 0.8f)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor.copy(alpha = 0.1f))
                .padding(16.dp)
        ) {
            ColorSelector(
                selectedColorHex = colorHex,
                onColorSelected = { colorHex = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Başlık", style = MaterialTheme.typography.headlineSmall) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("Notunuzu yazın...", style = MaterialTheme.typography.bodyLarge) },
                modifier = Modifier.fillMaxSize().weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                textStyle = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
