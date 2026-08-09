package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.shared.model.Folder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    folders: List<Folder>,
    onFolderClick: (Folder) -> Unit,
    onAddFolder: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Klasörler", style = MaterialTheme.typography.headlineMedium) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddFolder) {
                Icon(Icons.Default.Add, contentDescription = "Klasör Ekle")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            items(folders) { folder ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onFolderClick(folder) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FolderOpen, 
                                contentDescription = null,
                                tint = try { Color(android.graphics.Color.parseColor(folder.colorHex)) } catch(e: Exception) { Color.Gray }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(folder.name, style = MaterialTheme.typography.bodyLarge)
                        }
                        Badge {
                            Text("${folder.noteCount}")
                        }
                    }
                }
            }
        }
    }
}
