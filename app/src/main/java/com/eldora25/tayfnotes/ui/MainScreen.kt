package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.BuildConfig
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.ui.components.NoteGridItem

enum class SortType { DATE_MODIFIED, DATE_CREATED, ALPHABETICAL, COLOR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    notes: List<Note>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onAddNote: () -> Unit,
    onAddChecklist: () -> Unit,
    onAddSketch: () -> Unit,
    onEditNote: (Note) -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var sortType by remember { mutableStateOf(SortType.DATE_MODIFIED) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    val columns = when {
        screenWidth > 900.dp -> StaggeredGridCells.Fixed(4)
        screenWidth > 600.dp -> StaggeredGridCells.Fixed(3)
        else -> StaggeredGridCells.Fixed(2)
    }

    val sortedNotes = remember(notes, sortType) {
        when (sortType) {
            SortType.DATE_MODIFIED -> notes.sortedByDescending { it.lastModified }
            SortType.DATE_CREATED -> notes.sortedByDescending { it.createdAt }
            SortType.ALPHABETICAL -> notes.sortedBy { it.title.lowercase() }
            SortType.COLOR -> notes.sortedBy { it.colorHex }
        }
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChanged,
                            placeholder = { Text("Notlarda ara...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            isSearchActive = false
                            onSearchQueryChanged("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat")
                        }
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { 
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TayfNotes", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "buildv01.${BuildConfig.BUILD_NO} Tayfun YAMAK©", 
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Ara")
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sırala")
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                DropdownMenuItem(text = { Text("Düzenlenme Zamanı") }, onClick = { sortType = SortType.DATE_MODIFIED; showSortMenu = false })
                                DropdownMenuItem(text = { Text("Oluşturulma Zamanı") }, onClick = { sortType = SortType.DATE_CREATED; showSortMenu = false })
                                DropdownMenuItem(text = { Text("Alfabetik") }, onClick = { sortType = SortType.ALPHABETICAL; showSortMenu = false })
                                DropdownMenuItem(text = { Text("Renge Göre") }, onClick = { sortType = SortType.COLOR; showSortMenu = false })
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = onAddSketch,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp).size(48.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Gesture, contentDescription = "Sketch")
                }
                FloatingActionButton(
                    onClick = onAddChecklist,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 8.dp).size(48.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Checklist, contentDescription = "Liste")
                }
                FloatingActionButton(
                    onClick = onAddNote,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Note")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp)
        ) {
            if (sortedNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isEmpty()) "Henüz not yok." else "Sonuç bulunamadı.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = columns,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp
                ) {
                    items(sortedNotes, key = { it.id }) { note ->
                        NoteGridItem(
                            note = note,
                            onClick = { onEditNote(note) }
                        )
                    }
                }
            }
        }
    }
}
