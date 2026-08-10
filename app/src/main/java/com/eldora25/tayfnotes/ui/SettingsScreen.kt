package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.UploadFile
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
    onBiometricToggle: (Boolean) -> Unit,
    activeCloudProvider: String?,
    onCloudProviderSelected: (String?) -> Unit,
    onFullBackupClick: () -> Unit,
    onImportBackupClick: () -> Unit
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
            item { SettingCategory("Bulut Yedekleme ve Senkronizasyon") }
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Senkronizasyon Sağlayıcısı", style = MaterialTheme.typography.titleMedium)
                    Text("Gerçek hesap senkronizasyonu için bir servis seçin.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CloudChip(
                            selected = activeCloudProvider == null,
                            onClick = { onCloudProviderSelected(null) },
                            label = "Kapalı"
                        )
                        CloudChip(
                            selected = activeCloudProvider == "Google Drive",
                            onClick = { onCloudProviderSelected("Google Drive") },
                            label = "Google Drive"
                        )
                        CloudChip(
                            selected = activeCloudProvider == "Dropbox",
                            onClick = { onCloudProviderSelected("Dropbox") },
                            label = "Dropbox"
                        )
                    }
                }
            }
            
            item { 
                SettingItem(
                    title = if (activeCloudProvider == null) "Bulut Bağlantısı Kur" else "Şimdi Senkronize Et", 
                    subtitle = if (isSyncing) "Senkronize ediliyor..." else (activeCloudProvider ?: "Hiçbir bulut hesabı bağlı değil"),
                    onClick = onSyncClick
                ) 
            }

            item { SettingCategory("Veri Yönetimi (Migration)") }
            item {
                SettingItem(
                    title = "Tüm Veriyi Yedekle ve Paylaş",
                    subtitle = "Medya dosyaları ve veritabanını tek paket (ZIP) yap",
                    onClick = onFullBackupClick
                )
            }
            item {
                SettingItem(
                    title = "Yedekten Geri Yükle (İçe Aktar)",
                    subtitle = "Daha önce alınan .zip yedeğini uygulamaya yükle",
                    onClick = onImportBackupClick
                )
            }
            
            item { SettingCategory("Görünüm ve Tema") }
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Renk Paleti", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(TayfTheme.entries) { theme ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (theme == currentTheme) MaterialTheme.colorScheme.primary else Color.Gray.copy(0.3f))
                                    .border(2.dp, if (theme == currentTheme) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudChip(selected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun SettingCategory(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
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
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        if (subtitle.isNotEmpty()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
