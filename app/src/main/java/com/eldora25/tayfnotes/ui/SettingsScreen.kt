package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
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
            item { SettingItem("Çevrimiçi yedekleme", "Tayfun Yamak") }
            
            item { SettingCategory("Genel") }
            item { SettingItem("Varsayılan Ekran", "Notlar") }
            item { SettingItem("Varsayılan renk", "") } // Icon/Box would be here
            item { SettingItem("Varsayılan yazı büyüklüğü", "Büyük") }
            
            item { SettingCategory("Sıralama") }
            item { SettingItem("Varsayılan not sıralaması", "Son kullanılan sıralama") }
            
            item { SettingCategory("Hatırlatıcı") }
            item { SettingItem("Widget'ta sayacı göster", "Açık") }
            
            item { SettingCategory("Güvenlik") }
            item { SettingItem("Ana Parola", "Ana parolayı sıfırlayın veya kaldırın") }
            
            item { SettingCategory("Yedekleme") }
            item { SettingItem("Otomatik Yedekleme", "Açık") }
            
            item { SettingCategory("Hakkında") }
            item { SettingItem("Sürüm", "v01.x") }
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
fun SettingItem(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (subtitle.isNotEmpty()) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
