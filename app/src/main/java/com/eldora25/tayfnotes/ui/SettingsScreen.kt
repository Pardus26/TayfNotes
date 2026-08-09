package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    isSyncing: Boolean,
    onSyncClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item { SettingCategory("Çevrimiçi Senkronizasyon") }
            item { 
                SettingItem(
                    title = "Çevrimiçi yedekleme", 
                    subtitle = if (isSyncing) "Senkronize ediliyor..." else "Tayfun Yamak",
                    onClick = onSyncClick
                ) 
            }
            
            item { SettingCategory("Genel") }
            item { SettingItem("Varsayılan Ekran", "Notlar") }
            item { SettingItem("Varsayılan renk", "") }
            item { SettingItem("Varsayılan yazı büyüklüğü", "Büyük") }
            
            item { SettingCategory("Sıralama") }
            item { SettingItem("Varsayılan not sıralaması", "Son kullanılan sıralama") }
            
            item { SettingCategory("Hakkında") }
            item { SettingItem("Sürüm", "v01.${BuildConfig.BUILD_NO}") }
            item { SettingItem("Yazar", "Tayfun YAMAK©") }
        }
    }
}

@Composable
fun SettingCategory(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingItem(title: String, subtitle: String, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (subtitle.isNotEmpty()) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
