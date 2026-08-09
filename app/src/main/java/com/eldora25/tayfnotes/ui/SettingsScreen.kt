package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.BuildConfig
import com.eldora25.tayfnotes.ui.theme.TayfTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    currentTheme: TayfTheme,
    onThemeSelected: (TayfTheme) -> Unit,
    isDarkMode: Boolean?,
    onDarkModeChanged: (Boolean?) -> Unit,
    isBiometricEnabled: Boolean,
    onBiometricToggle: (Boolean) -> Unit
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
            
            item { SettingCategory("Görünüm") }
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Renk Paleti", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TayfTheme.entries) { theme ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (theme == currentTheme) MaterialTheme.colorScheme.primary else Color.Gray)
                                    .clickable { onThemeSelected(theme) }
                            )
                        }
                    }
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Karanlık Mod", style = MaterialTheme.typography.titleMedium)
                    Switch(checked = isDarkMode == true, onCheckedChange = { onDarkModeChanged(it) })
                }
            }
            
            item { SettingCategory("Güvenlik") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Biyometrik Kilit", style = MaterialTheme.typography.titleMedium)
                    Switch(checked = isBiometricEnabled, onCheckedChange = onBiometricToggle)
                }
            }
            
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
