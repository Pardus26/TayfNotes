package com.eldora25.tayfnotes.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import coil.compose.AsyncImage
import com.eldora25.tayfnotes.shared.model.ChecklistItem
import com.eldora25.tayfnotes.shared.model.Folder
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.model.NoteType
import com.eldora25.tayfnotes.ui.components.ChecklistEditor
import com.eldora25.tayfnotes.ui.components.ColorSelector
import com.eldora25.tayfnotes.ui.components.DrawingCanvas
import com.eldora25.tayfnotes.ui.theme.NeonIcon
import com.eldora25.tayfnotes.util.AudioRecorder
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: Note? = null,
    folders: List<Folder> = emptyList(),
    initialSketch: Boolean = false,
    onBack: () -> Unit,
    onSave: (Note) -> Unit,
    onDelete: (Note) -> Unit
) {
    val context = LocalContext.current

    val noteId = remember {
        note?.id ?: System.currentTimeMillis().toString()
    }

    var title by remember {
        mutableStateOf(note?.title ?: "")
    }

    var content by remember {
        mutableStateOf(note?.content ?: "")
    }

    var colorHex by remember {
        mutableStateOf(note?.colorHex ?: "#FFFFFF")
    }

    var reminderTimestamp by remember {
        mutableStateOf(note?.reminderTimestamp)
    }

    var folderId by remember {
        mutableStateOf(note?.folderId)
    }

    /*
     * Images attached to the normal note.
     *
     * These are intentionally separate from Sketch images.
     */
    var imageUris by remember {
        mutableStateOf(
            note?.imageUris ?: emptyList()
        )
    }

    /*
     * Temporary URI returned from the Sketch image picker.
     *
     * DrawingCanvas consumes this URI and stores it inside
     * SketchDocument.
     */
    var pendingSketchImageUri by remember {
        mutableStateOf<String?>(null)
    }

    var audioPath by remember {
        mutableStateOf(note?.audioPath)
    }

    var sketchData by remember {
        mutableStateOf(note?.sketchData)
    }

    val initialItems = remember(note) {

        if (
            note?.type == NoteType.CHECKLIST &&
            note.content.isNotEmpty()
        ) {

            try {
                Json.decodeFromString<List<ChecklistItem>>(
                    note.content
                )
            } catch (_: Exception) {
                emptyList()
            }

        } else {
            emptyList()
        }
    }

    var checklistItems by remember {
        mutableStateOf(initialItems)
    }

    var isPreviewMode by remember {
        mutableStateOf(false)
    }

    var isSketchMode by remember {
        mutableStateOf(
            initialSketch ||
                sketchData != null
        )
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var showFolderMenu by remember {
        mutableStateOf(false)
    }

    val recorder = remember {
        AudioRecorder(context)
    }

    var isRecording by remember {
        mutableStateOf(false)
    }

    val backgroundColor =
        try {
            Color(
                android.graphics.Color.parseColor(
                    colorHex
                )
            )
        } catch (_: Exception) {
            MaterialTheme.colorScheme.surface
        }

    /*
     * ------------------------------------------------------------
     * NORMAL NOTE IMAGE PICKER
     * ------------------------------------------------------------
     */
    val galleryLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            uri?.let {
                imageUris =
                    imageUris + it.toString()
            }
        }

    /*
     * ------------------------------------------------------------
     * SKETCH IMAGE PICKER
     * ------------------------------------------------------------
     */
    val sketchGalleryLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            uri?.let {
                pendingSketchImageUri =
                    it.toString()
            }
        }

    /*
     * ------------------------------------------------------------
     * AUTO SAVE
     * ------------------------------------------------------------
     */
    LaunchedEffect(
        title,
        content,
        colorHex,
        reminderTimestamp,
        folderId,
        imageUris,
        audioPath,
        checklistItems,
        sketchData
    ) {

        if (
            title.isNotEmpty() ||
            content.isNotEmpty() ||
            imageUris.isNotEmpty() ||
            audioPath != null ||
            checklistItems.isNotEmpty() ||
            sketchData != null
        ) {

            delay(1000)

            var finalContent = content

            if (checklistItems.isNotEmpty()) {
                finalContent =
                    Json.encodeToString(
                        checklistItems
                    )
            }

            var finalTitle = title

            if (finalTitle.isEmpty()) {

                val textForTitle =
                    if (checklistItems.isNotEmpty()) {

                        checklistItems
                            .firstOrNull()
                            ?.text
                            ?: ""

                    } else {
                        content
                    }

                if (textForTitle.isNotEmpty()) {

                    finalTitle =
                        textForTitle
                            .trim()
                            .split("\\s+".toRegex())
                            .take(5)
                            .joinToString(" ")
                }
            }

            val finalNote =
                Note(
                    id = noteId,
                    title = finalTitle,
                    content = finalContent,
                    colorHex = colorHex,
                    type =
                        if (checklistItems.isNotEmpty()) {
                            NoteType.CHECKLIST
                        } else {
                            NoteType.TEXT
                        },
                    reminderTimestamp =
                        reminderTimestamp,
                    folderId = folderId,
                    imageUris = imageUris,
                    audioPath = audioPath,
                    sketchData = sketchData,
                    lastModified =
                        System.currentTimeMillis()
                )

            onSave(finalNote)
        }
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        if (note == null) {
                            "Yeni Ekle"
                        } else {
                            "Düzenle"
                        },
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight =
                            FontWeight.Bold
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        NeonIcon(
                            backgroundColor =
                                backgroundColor
                        ) {

                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Geri"
                            )
                        }
                    }
                },

                actions = {

                    if (!isSketchMode) {

                        /*
                         * Enter Sketch mode.
                         */
                        IconButton(
                            onClick = {
                                isSketchMode = true
                            }
                        ) {

                            NeonIcon(
                                backgroundColor =
                                    backgroundColor
                            ) {

                                Icon(
                                    Icons.Default.Gesture,
                                    contentDescription =
                                        "Sketch"
                                )
                            }
                        }

                        /*
                         * Normal note image.
                         */
                        IconButton(
                            onClick = {
                                galleryLauncher.launch(
                                    "image/*"
                                )
                            }
                        ) {

                            NeonIcon(
                                backgroundColor =
                                    backgroundColor
                            ) {

                                Icon(
                                    Icons.Default.Image,
                                    contentDescription =
                                        "Resim"
                                )
                            }
                        }

                        /*
                         * Audio.
                         */
                        IconButton(
                            onClick = {

                                if (!isRecording) {

                                    val file =
                                        File(
                                            context.cacheDir,
                                            "audio_${System.currentTimeMillis()}.m4a"
                                        )

                                    audioPath =
                                        file.absolutePath

                                    try {

                                        recorder.startRecording(
                                            file
                                        )

                                        isRecording =
                                            true

                                    } catch (_: Exception) {

                                        Toast.makeText(
                                            context,
                                            "Ses kaydı hatası",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                } else {

                                    recorder.stopRecording()
                                    isRecording = false
                                }
                            }
                        ) {

                            NeonIcon(
                                backgroundColor =
                                    backgroundColor
                            ) {

                                Icon(
                                    if (isRecording) {
                                        Icons.Default.StopCircle
                                    } else {
                                        Icons.Default.Mic
                                    },
                                    contentDescription =
                                        "Ses",
                                    tint =
                                        if (isRecording) {
                                            Color.Red
                                        } else {
                                            LocalContentColor.current
                                        }
                                )
                            }
                        }

                        /*
                         * Preview.
                         */
                        IconButton(
                            onClick = {
                                isPreviewMode =
                                    !isPreviewMode
                            }
                        ) {

                            NeonIcon(
                                backgroundColor =
                                    backgroundColor
                            ) {

                                Icon(
                                    if (isPreviewMode) {
                                        Icons.Default.Edit
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription =
                                        "Önizle"
                                )
                            }
                        }

                        /*
                         * Delete existing note.
                         */
                        if (note != null) {

                            IconButton(
                                onClick = {
                                    showDeleteDialog = true
                                }
                            ) {

                                NeonIcon(
                                    backgroundColor =
                                        backgroundColor
                                ) {

                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription =
                                            "Sil"
                                    )
                                }
                            }
                        }

                    } else {

                        /*
                         * Exit Sketch mode.
                         */
                        IconButton(
                            onClick = {
                                isSketchMode = false
                            }
                        ) {

                            NeonIcon(
                                backgroundColor =
                                    backgroundColor
                            ) {

                                Icon(
                                    Icons.Default.TextFields,
                                    contentDescription =
                                        "Metin Modu"
                                )
                            }
                        }
                    }

                    /*
                     * Finish / save.
                     */
                    IconButton(
                        onClick = onBack
                    ) {

                        NeonIcon(
                            backgroundColor =
                                backgroundColor
                        ) {

                            Icon(
                                Icons.Default.Check,
                                contentDescription =
                                    "Bitti"
                            )
                        }
                    }
                },

                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            backgroundColor.copy(
                                alpha = 0.9f
                            )
                    )
            )
        }

    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    backgroundColor.copy(
                        alpha = 0.1f
                    )
                )
        ) {

            if (!isPreviewMode) {

                /*
                 * ------------------------------------------------
                 * COMMON NOTE HEADER
                 * ------------------------------------------------
                 */
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box {

                        AssistChip(
                            onClick = {
                                showFolderMenu = true
                            },

                            label = {

                                Text(
                                    folders
                                        .find {
                                            it.id == folderId
                                        }
                                        ?.name
                                        ?: "Klasör Seç"
                                )
                            },

                            leadingIcon = {

                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier =
                                        Modifier.size(18.dp)
                                )
                            }
                        )

                        DropdownMenu(
                            expanded =
                                showFolderMenu,
                            onDismissRequest = {
                                showFolderMenu = false
                            }
                        ) {

                            DropdownMenuItem(
                                text = {
                                    Text("Klasör Yok")
                                },
                                onClick = {
                                    folderId = null
                                    showFolderMenu = false
                                }
                            )

                            folders.forEach { folder ->

                                DropdownMenuItem(
                                    text = {
                                        Text(folder.name)
                                    },
                                    onClick = {
                                        folderId =
                                            folder.id
                                        showFolderMenu =
                                            false
                                