package com.eldora25.tayfnotes.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.eldora25.tayfnotes.shared.model.Folder
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.ui.components.ColorSelector
import com.eldora25.tayfnotes.util.AudioRecorder
import com.eldora25.tayfnotes.util.FileExportHelper
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: Note? = null,
    folders: List<Folder> = emptyList(),
    onBack: () -> Unit,
    onSave: (Note) -> Unit,
    onDelete: (Note) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var colorHex by remember { mutableStateOf(note?.colorHex ?: "#FFFFFF") }
    var reminderTimestamp by remember { mutableStateOf(note?.reminderTimestamp) }
    var folderId by remember { mutableStateOf(note?.folderId) }
    var imageUris by remember { mutableStateOf(note?.imageUris ?: emptyList<String>()) }
    var audioPath by remember { mutableStateOf(note?.audioPath) }
    
    var isPreviewMode by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }
    
    val recorder = remember { AudioRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    val backgroundColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.surface
    }

    // Permission Launchers
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            // Temporary URI handling logic would go here
            Toast.makeText(context, "Fotoğraf eklendi", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUris = imageUris + it.toString() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        // Handle results
    }

    // Auto-save logic
    LaunchedEffect(title, content, colorHex, reminderTimestamp, folderId, imageUris, audioPath) {
        if (title.isNotEmpty() || content.isNotEmpty() || imageUris.isNotEmpty() || audioPath != null) {
            delay(1000)
            
            var finalTitle = title
            if (finalTitle.isEmpty() && content.isNotEmpty()) {
                finalTitle = content.trim().split("\\s+".toRegex()).take(5).joinToString(" ")
            }

            val finalNote = (note ?: Note(
                id = note?.id ?: System.currentTimeMillis().toString(),
                title = finalTitle,
                content = content
            )).copy(
                title = finalTitle,
                content = content,
                colorHex = colorHex,
                reminderTimestamp = reminderTimestamp,
                folderId = folderId,
                imageUris = imageUris,
                audioPath = audioPath,
                lastModified = System.currentTimeMillis()
            )
            onSave(finalNote)
        }
    }

    fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        reminderTimestamp?.let { calendar.timeInMillis = it }
        
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        reminderTimestamp = calendar.timeInMillis
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    if (showDeleteDialog && note != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Notu Sil") },
            text = { Text("Bu notu silmek istediğinize emin misiniz?") },
            confirmButton = {
                TextButton(onClick = { 
                    onDelete(note)
                    showDeleteDialog = false
                }) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (note == null) "Yeni Not" else "Notu Düzenle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Image, contentDescription = "Resim Ekle")
                    }
                    IconButton(onClick = {
                        if (!isRecording) {
                            val file = File(context.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
                            audioPath = file.absolutePath
                            recorder.startRecording(file)
                            isRecording = true
                        } else {
                            recorder.stopRecording()
                            isRecording = false
                        }
                    }) {
                        Icon(
                            if (isRecording) Icons.Default.StopCircle else Icons.Default.Mic,
                            contentDescription = "Ses Kaydet",
                            tint = if (isRecording) Color.Red else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { FileExportHelper.exportNoteToTxt(context, note ?: Note("temp", title, content, colorHex)) }) {
                        Icon(Icons.Default.Share, contentDescription = "Dışa Aktar")
                    }
                    IconButton(onClick = { showDateTimePicker() }) {
                        Icon(
                            imageVector = if (reminderTimestamp == null) Icons.Default.NotificationsNone else Icons.Default.NotificationsActive,
                            contentDescription = "Hatırlatıcı",
                            tint = if (reminderTimestamp != null) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                        Icon(
                            if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = if (isPreviewMode) "Düzenle" else "Önizle"
                        )
                    }
                    if (note != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil")
                        }
                    }
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Check, contentDescription = "Tamam")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor.copy(alpha = 0.8f)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor.copy(alpha = 0.1f))
                .padding(16.dp)
        ) {
            if (!isPreviewMode) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box {
                        AssistChip(
                            onClick = { showFolderMenu = true },
                            label = { Text(folders.find { it.id == folderId }?.name ?: "Klasör Seç") },
                            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        DropdownMenu(expanded = showFolderMenu, onDismissRequest = { showFolderMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Klasör Yok") },
                                onClick = { folderId = null; showFolderMenu = false }
                            )
                            folders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder.name) },
                                    onClick = { folderId = folder.id; showFolderMenu = false }
                                )
                            }
                        }
                    }
                    
                    ColorSelector(
                        selectedColorHex = colorHex,
                        onColorSelected = { colorHex = it }
                    )
                }

                if (reminderTimestamp != null) {
                    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr")).format(Date(reminderTimestamp!!))
                    AssistChip(
                        onClick = { reminderTimestamp = null },
                        label = { Text("Hatırlatıcı: $dateStr") },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Kaldır", modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Multimedia List
                if (imageUris.isNotEmpty()) {
                    LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(imageUris) { uri ->
                            Box {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { imageUris = imageUris - uri },
                                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(0.5f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                if (audioPath != null) {
                    AssistChip(
                        onClick = { 
                            if (!isPlaying) {
                                isPlaying = true
                                recorder.startPlaying(File(audioPath!!)) { isPlaying = false }
                            } else {
                                recorder.stopPlaying()
                                isPlaying = false
                            }
                        },
                        label = { Text(if (isPlaying) "Oynatılıyor..." else "Sesli Notu Dinle") },
                        leadingIcon = { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { 
                                audioPath = null 
                                recorder.stopPlaying()
                                isPlaying = false
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Sil", modifier = Modifier.size(16.dp))
                            }
                        }
                    )
                }

                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Başlık (Opsiyonel)", style = MaterialTheme.typography.headlineSmall) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Notunuzu yazın...", style = MaterialTheme.typography.bodyLarge) },
                    modifier = Modifier.fillMaxSize().weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            } else {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    val displayTitle = if (title.isEmpty() && content.isNotEmpty()) {
                        content.trim().split("\\s+".toRegex()).take(5).joinToString(" ")
                    } else if (title.isEmpty()) {
                        "Başlıksız Not"
                    } else {
                        title
                    }
                    
                    Text(displayTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (reminderTimestamp != null) {
                        Text(
                            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr")).format(Date(reminderTimestamp!!)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (imageUris.isNotEmpty()) {
                        LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(imageUris) { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.size(200.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(content, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
