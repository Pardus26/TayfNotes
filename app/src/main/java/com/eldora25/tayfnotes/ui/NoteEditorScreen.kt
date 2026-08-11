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

    /*
     * ------------------------------------------------------------
     * NOTE ID
     * ------------------------------------------------------------
     */

    val noteId = remember {
        note?.id ?: System.currentTimeMillis().toString()
    }

    /*
     * ------------------------------------------------------------
     * NOTE STATE
     * ------------------------------------------------------------
     */

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
     * ------------------------------------------------------------
     * NORMAL NOTE IMAGES
     * ------------------------------------------------------------
     *
     * These images belong to the normal note.
     * They are intentionally kept separate from sketch data.
     */

    var imageUris by remember {
        mutableStateOf(
            note?.imageUris ?: emptyList()
        )
    }

    /*
     * ------------------------------------------------------------
     * AUDIO
     * ------------------------------------------------------------
     */

    var audioPath by remember {
        mutableStateOf(note?.audioPath)
    }

    /*
     * ------------------------------------------------------------
     * SKETCH DATA
     * ------------------------------------------------------------
     */

    var sketchData by remember {
        mutableStateOf(note?.sketchData)
    }

    /*
     * ------------------------------------------------------------
     * CHECKLIST
     * ------------------------------------------------------------
     */

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

    /*
     * ------------------------------------------------------------
     * UI STATE
     * ------------------------------------------------------------
     */

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

    /*
     * ------------------------------------------------------------
     * AUDIO RECORDER
     * ------------------------------------------------------------
     */

    val recorder = remember {
        AudioRecorder(context)
    }

    var isRecording by remember {
        mutableStateOf(false)
    }

    /*
     * ------------------------------------------------------------
     * BACKGROUND COLOR
     * ------------------------------------------------------------
     */

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

            /*
             * ----------------------------------------------------
             * FINAL CONTENT
             * ----------------------------------------------------
             */

            var finalContent = content

            if (checklistItems.isNotEmpty()) {

                finalContent =
                    Json.encodeToString(
                        checklistItems
                    )
            }

            /*
             * ----------------------------------------------------
             * AUTOMATIC TITLE
             * ----------------------------------------------------
             */

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
                            .split(
                                "\\s+".toRegex()
                            )
                            .take(5)
                            .joinToString(" ")
                }
            }

            /*
             * ----------------------------------------------------
             * CREATE NOTE
             * ----------------------------------------------------
             */

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

                    folderId =
                        folderId,

                    imageUris =
                        imageUris,

                    audioPath =
                        audioPath,

                    sketchData =
                        sketchData,

                    lastModified =
                        System.currentTimeMillis()
                )

            onSave(finalNote)
        }
    }

    /*
     * ============================================================
     * MAIN SCAFFOLD
     * ============================================================
     */

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
                                contentDescription =
                                    "Geri"
                            )
                        }
                    }
                },

                actions = {

                    /*
                     * =================================================
                     * NORMAL NOTE MODE
                     * =================================================
                     */

                    if (!isSketchMode) {

                        /*
                         * -------------------------------------------------
                         * ENTER SKETCH MODE
                         * -------------------------------------------------
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
                         * -------------------------------------------------
                         * ADD IMAGE TO NORMAL NOTE
                         * -------------------------------------------------
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
                         * -------------------------------------------------
                         * AUDIO
                         * -------------------------------------------------
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

                                    isRecording =
                                        false
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
                         * -------------------------------------------------
                         * PREVIEW
                         * -------------------------------------------------
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
                         * -------------------------------------------------
                         * DELETE
                         * -------------------------------------------------
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
                         * =================================================
                         * SKETCH MODE
                         * =================================================
                         *
                         * DrawingCanvas currently supports drawing,
                         * shapes, colors, fills, marker and eraser.
                         *
                         * Image insertion is intentionally not passed
                         * here because DrawingCanvas does not currently
                         * expose image parameters.
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
                     * -----------------------------------------------------
                     * FINISH / SAVE
                     * -----------------------------------------------------
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
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        backgroundColor.copy(
                            alpha = 0.1f
                        )
                    )
        ) {

            /*
             * =========================================================
             * EDIT MODE
             * =========================================================
             */

            if (!isPreviewMode) {

                /*
                 * -----------------------------------------------------
                 * COMMON NOTE HEADER
                 * -----------------------------------------------------
                 */

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    /*
                     * FOLDER
                     */

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

                            /*
                             * NO FOLDER
                             */

                            DropdownMenuItem(

                                text = {
                                    Text("Klasör Yok")
                                },

                                onClick = {

                                    folderId = null

                                    showFolderMenu =
                                        false
                                }
                            )

                            /*
                             * FOLDERS
                             */

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
                                    }
                                )
                            }
                        }
                    }

                    /*
                     * COLOR
                     */

                    ColorSelector(

                        selectedColorHex =
                            colorHex,

                        onColorSelected = {
                            colorHex = it
                        }
                    )
                }

                /*
                 * -----------------------------------------------------
                 * TITLE
                 * -----------------------------------------------------
                 */

                TextField(

                    value = title,

                    onValueChange = {
                        title = it
                    },

                    placeholder = {

                        Text(
                            "Başlık",
                            style =
                                MaterialTheme.typography.headlineSmall
                        )
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp
                            ),

                    colors =
                        TextFieldDefaults.colors(

                            focusedContainerColor =
                                Color.Transparent,

                            unfocusedContainerColor =
                                Color.Transparent,

                            focusedIndicatorColor =
                                Color.Transparent,

                            unfocusedIndicatorColor =
                                Color.Transparent
                        ),

                    textStyle =
                        MaterialTheme.typography.headlineSmall
                            .copy(
                                fontWeight =
                                    FontWeight.Bold
                            )
                )

                /*
                 * =====================================================
                 * SKETCH MODE
                 * =====================================================
                 */

                if (isSketchMode) {

                    DrawingCanvas(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),

                        initialData =
                            sketchData,

                        onDataChanged = {
                            sketchData = it
                        }
                    )

                    /*
                     * -------------------------------------------------
                     * SKETCH DESCRIPTION
                     * -------------------------------------------------
                     */

                    TextField(

                        value = content,

                        onValueChange = {
                            content = it
                        },

                        placeholder = {

                            Text(
                                "Çizim hakkında not...",
                                style =
                                    MaterialTheme.typography.bodyMedium
                            )
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),

                        colors =
                            TextFieldDefaults.colors(

                                focusedContainerColor =
                                    Color.Transparent,

                                unfocusedContainerColor =
                                    Color.Transparent
                            )
                    )

                } else if (
                    note?.type == NoteType.CHECKLIST ||
                    checklistItems.isNotEmpty()
                ) {

                    /*
                     * =================================================
                     * CHECKLIST
                     * =================================================
                     */

                    ChecklistEditor(

                        items =
                            checklistItems,

                        onItemsChanged = {
                            checklistItems = it
                        }
                    )

                } else {

                    /*
                     * =================================================
                     * NORMAL NOTE
                     * =================================================
                     */

                    /*
                     * -------------------------------------------------
                     * IMAGES
                     * -------------------------------------------------
                     */

                    if (imageUris.isNotEmpty()) {

                        LazyRow(

                            modifier =
                                Modifier.padding(16.dp),

                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            items(imageUris) { uri ->

                                Box {

                                    AsyncImage(

                                        model = uri,

                                        contentDescription =
                                            null,

                                        modifier =
                                            Modifier
                                                .size(120.dp)
                                                .clip(
                                                    RoundedCornerShape(
                                                        8.dp
                                                    )
                                                ),

                                        contentScale =
                                            ContentScale.Crop
                                    )

                                    /*
                                     * DELETE IMAGE
                                     */

                                    IconButton(

                                        onClick = {

                                            imageUris =
                                                imageUris - uri
                                        },

                                        modifier =
                                            Modifier
                                                .align(
                                                    Alignment.TopEnd
                                                )
                                                .size(24.dp)
                                                .background(
                                                    Color.Black.copy(
                                                        alpha = 0.5f
                                                    ),
                                                    CircleShape
                                                )
                                    ) {

                                        Icon(

                                            Icons.Default.Close,

                                            contentDescription =
                                                null,

                                            tint =
                                                Color.White,

                                            modifier =
                                                Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    /*
                     * -------------------------------------------------
                     * NORMAL TEXT
                     * -------------------------------------------------
                     */

                    TextField(

                        value = content,

                        onValueChange = {
                            content = it
                        },

                        placeholder = {

                            Text(
                                "Notunuzu yazın...",
                                style =
                                    MaterialTheme.typography.bodyLarge
                            )
                        },

                        modifier =
                            Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .padding(16.dp),

                        colors =
                            TextFieldDefaults.colors(

                                focusedContainerColor =
                                    Color.Transparent,

                                unfocusedContainerColor =
                                    Color.Transparent,

                                focusedIndicatorColor =
                                    Color.Transparent,

                                unfocusedIndicatorColor =
                                    Color.Transparent
                            ),

                        textStyle =
                            MaterialTheme.typography.bodyLarge
                    )
                }

            } else {

                /*
                 * =====================================================
                 * PREVIEW MODE
                 * =====================================================
                 */

                Column(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(
                                rememberScrollState()
                            )
                            .padding(16.dp)
                ) {

                    /*
                     * DISPLAY TITLE
                     */

                    val displayTitle =
                        if (
                            title.isEmpty() &&
                            checklistItems.isNotEmpty()
                        ) {

                            checklistItems
                                .firstOrNull()
                                ?.text
                                ?: "Başlıksız Not"

                        } else if (
                            title.isEmpty()
                        ) {

                            "Başlıksız Not"

                        } else {

                            title
                        }

                    Text(

                        displayTitle,

                        style =
                            MaterialTheme.typography.headlineSmall,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    /*
                     * CHECKLIST PREVIEW
                     */

                    if (checklistItems.isNotEmpty()) {

                        checklistItems.forEach { item ->

                            Row(

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Checkbox(

                                    checked =
                                        item.isChecked,

                                    onCheckedChange =
                                        null,

                                    enabled =
                                        false
                                )

                                Text(

                                    item.text,

                                    style =
                                        if (item.isChecked) {

                                            MaterialTheme
                                                .typography
                                                .bodyLarge
                                                .copy(
                                                    textDecoration =
                                                        androidx
                                                            .compose
                                                            .ui
                                                            .text
                                                            .style
                                                            .TextDecoration
                                                            .LineThrough
                                                )

                                        } else {

                                            MaterialTheme
                                                .typography
                                                .bodyLarge
                                        }
                                )
                            }
                        }

                    } else {

                        /*
                         * NORMAL TEXT PREVIEW
                         */

                        Text(

                            content,

                            style =
                                MaterialTheme.typography.bodyLarge
                        )
                    }

                    /*
                     * SKETCH INFORMATION
                     */

                    if (
                        sketchData?.isNotEmpty() == true
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(24.dp)
                        )

                        Text(

                            "Çizim içeriyor. Düzenlemek için Sketch moduna geçin.",

                            style =
                                MaterialTheme.typography.labelSmall,

                            color =
                                Color.Gray
                        )
                    }

                    /*
                     * NORMAL NOTE IMAGES
                     */

                    imageUris.forEach { uri ->

                        AsyncImage(

                            model = uri,

                            contentDescription =
                                null,

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        vertical = 8.dp
                                    )
                                    .clip(
                                        RoundedCornerShape(
                                            8.dp
                                        )
                                    ),

                            contentScale =
                                ContentScale.FillWidth
                        )
                    }
                }
            }
        }
    }

    /*
     * ============================================================
     * DELETE NOTE DIALOG
     * ============================================================
     */

    if (
        showDeleteDialog &&
        note != null
    ) {

        AlertDialog(

            onDismissRequest = {

                showDeleteDialog =
                    false
            },

            title = {

                Text("Notu Sil")
            },

            text = {

                Text(
                    "Bu notu silmek istediğinize emin misiniz?"
                )
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        onDelete(note)

                        showDeleteDialog =
                            false
                    }
                ) {

                    Text(
                        "Sil",
                        color =
                            MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {

                        showDeleteDialog =
                            false
                    }
                ) {

                    Text("Vazgeç")
                }
            }
        )
    }
}