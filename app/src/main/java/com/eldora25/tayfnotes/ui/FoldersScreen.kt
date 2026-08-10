package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    onAddFolder: (String) -> Unit,
    onUpdateFolder: (Folder) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var folderToEdit by remember { mutableStateOf<Folder?>(null) }
    var folderNameInput by remember { mutableStateOf("") }
    
    var sortType by remember { mutableStateOf(SortType.ALPHABETICAL) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sortedFolders = remember(folders, sortType) {
        when (sortType) {
            SortType.ALPHABETICAL -> folders.sortedBy { it.name.lowercase() }
            SortType.COLOR -> folders.sortedBy { it.colorHex }
            else -> folders
        }
    }

    if (showAddDialog || folderToEdit != null) {
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                folderToEdit = null
                folderNameInput = ""
            },
            title = { Text(if (showAddDialog) "Yeni Klasör" else "Klasörü Düzenle") },
            text = {
                TextField(
                    value = folderNameInput,
                    onValueChange = { folderNameInput = it },
                    placeholder = { Text("Klasör Adı") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (folderNameInput.isNotEmpty()) {
                        if (showAddDialog) {
                            onAddFolder(folderNameInput)
                        } else {
                            folderToEdit?.let {
                                onUpdateFolder(it.copy(name = folderNameInput))
                            }
                        }
                    }
                    showAddDialog = false
                    folderToEdit = null
                    folderNameInput = ""
                }) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    folderToEdit = null
                    folderNameInput = ""
                }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Klasörler", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sırala")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(text = { Text("Alfabetik") }, onClick = { sortType = SortType.ALPHABETICAL; showSortMenu = false })
                            DropdownMenuItem(text = { Text("Renge Göre") }, onClick = { sortType = SortType.COLOR; showSortMenu = false })
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Klasör Ekle")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sortedFolders, key = { it.id }) { folder ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.FolderOpen, 
                                contentDescription = null,
                                tint = try { Color(android.graphics.Color.parseColor(folder.colorHex)) } catch(e: Exception) { Color.Gray }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(folder.name, style = MaterialTheme.typography.bodyLarge)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { 
                                folderToEdit = folder
                                folderNameInput = folder.name
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Düzenle", modifier = Modifier.size(20.dp))
                            }
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text("${folder.noteCount}", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
