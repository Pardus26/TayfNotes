package com.eldora25.tayfnotes.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.shared.model.NoteType

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onTypeSelected: (NoteType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ekle") },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("Metin") },
                    leadingContent = { Icon(Icons.Default.List, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).padding(4.dp).padding(8.dp).padding(4.dp) // Dummy spacing
                )
                // Using clickable on column/row for simplicity
                TextButton(
                    onClick = { onTypeSelected(NoteType.TEXT) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.List, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Metin")
                    }
                }
                TextButton(
                    onClick = { onTypeSelected(NoteType.CHECKLIST) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Kontrol Listesi")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}
