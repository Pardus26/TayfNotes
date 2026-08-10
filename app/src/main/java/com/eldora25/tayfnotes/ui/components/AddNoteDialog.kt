package com.eldora25.tayfnotes.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.shared.model.NoteType

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onTypeSelected: (NoteType, Boolean) -> Unit // (Type, isSketch)
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Ekle") },
        text = {
            Column {
                AddOptionItem(
                    icon = Icons.Default.Description,
                    title = "Metin Notu",
                    subtitle = "Yazılı notlar ve resimler",
                    onClick = { onTypeSelected(NoteType.TEXT, false) }
                )
                AddOptionItem(
                    icon = Icons.Default.Checklist,
                    title = "Kontrol Listesi",
                    subtitle = "Görevler ve alt adımlar",
                    onClick = { onTypeSelected(NoteType.CHECKLIST, false) }
                )
                AddOptionItem(
                    icon = Icons.Default.Gesture,
                    title = "Sketch (Çizim)",
                    subtitle = "El yazısı ve çizim notu",
                    onClick = { onTypeSelected(NoteType.TEXT, true) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}

@Composable
fun AddOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
