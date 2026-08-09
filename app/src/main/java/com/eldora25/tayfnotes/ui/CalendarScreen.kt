package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Takvim", style = MaterialTheme.typography.headlineMedium) })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text("Takvim Görünümü Yakında...", style = MaterialTheme.typography.headlineSmall)
        }
    }
}
