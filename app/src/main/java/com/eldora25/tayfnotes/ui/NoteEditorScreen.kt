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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

import coil.compose.AsyncImage

import com.eldora25.tayfnotes.shared.model.ChecklistItem
import com.eldora25.tayfnotes.shared.model.Folder
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.model.NoteType

import com.eldora25.tayfnotes.ui.components.ChecklistEditor
import com.eldora25.tayfnotes.ui.components.ColorSelector
import com.eldora25.tayfnotes.ui.components.DrawingCanvas

import com.eldora25.tayfnotes.ui.theme.EditorNeonIcon

import com.eldora25.tayfnotes.util.AudioRecorder
import com.eldora25.tayfnotes.util.FileExportHelper

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
     * ------------------------------------------------------------------------
     * NOTE STATE
     * ------------------------------------------------------------------------
     */

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

    var imageUris by remember {
        mutableStateOf(note?.imageUris ?: emptyList())
    }

    var audioPath by remember {
        mutableStateOf(note?.audioPath)
    }

    var sketchData by remember {
        mutableStateOf(note?.sketchData)
    }

    /*
     * ------------------------------------------------------------------------
     * CHECKLIST
     * ------------------------------------------------------------------------
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
     * ------------------------------------------------------------------------
     * UI STATE
     * ------------------------------------------------------------------------
     */

    var isPreviewMode by remember {
        mutableStateOf(false)
    }

    var isSketchMode by remember {
        mutableStateOf(
            initialSketch || sketchData != null
        )
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var showFolderMenu by remember {
        mutableStateOf(false)
    }

    /*
     * ------------------------------------------------------------------------
     * AUDIO
     * ------------------------------------------------------------------------
     */

    val recorder = remember {
        AudioRecorder(context)
    }

    var isRecording by remember {
        mutableStateOf(false)
    }

    /*
     * ------------------------------------------------------------------------
     * NOTE BACKGROUND
     * ------------------------------------------------------------------------
     */

    val backgroundColor = try {

        Color(
            android.graphics.Color.parseColor(colorHex)
        )

    } catch (_: Exception) {

        MaterialTheme.colorScheme.surface
    }

    /*
     * ------------------------------------------------------------------------
     * IMAGE PICKER
     * ------------------------------------------------------------------------
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
     * ------------------------------------------------------------------------
     * AUTO SAVE
     * ------------------------------------------------------------------------
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
                    Json.encodeToString(checklistItems)
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

            val finalNote = Note(

                id = noteId,

                title = finalTitle,

                content = finalContent,

                colorHex = colorHex,

                type =
                    if (checklistItems.isNotEmpty())
                        NoteType.CHECKLIST
                    else
                        NoteType.TEXT,

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
     * ------------------------------------------------------------------------
     * MAIN SCAFFOLD
     * ------------------------------------------------------------------------
     */

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(

                        if (note == null)
                            "Yeni Ekle"
                        else
                            "Düzenle",

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

                        EditorNeonIcon {

                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Geri"
                            )
                        }
                    }
                },

                actions = {

                    /*
                     * --------------------------------------------------------
                     * NORMAL NOTE MODE
                     * --------------------------------------------------------
                     */

                    if (!isSketchMode) {

                        /*
                         * Sketch mode
                         */

                        IconButton(
                            onClick = {
                                isSketchMode = true
                            }
                        ) {

                            EditorNeonIcon {

                                Icon(
                                    Icons.Default.Gesture,
                                    contentDescription = "Sketch"
                                )
                            }
                        }

                        /*
                         * Gallery
                         */

                        IconButton(
                            onClick = {
                                galleryLauncher.launch("image/*")
                            }
                        ) {

                            EditorNeonIcon {

                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = "Resim"
                                )
                            }
                        }

                        /*
                         * Audio recording
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

                                        isRecording = true

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

                            EditorNeonIcon {

                                Icon(

                                    if (isRecording)
                                        Icons.Default.StopCircle
                                    else
                                        Icons.Default.Mic,

                                    contentDescription =
                                        "Ses",

                                    tint =
                                        if (isRecording)
                                            Color.Red
                                        else
                                            Color(0xFFFFD700)
                                )
                            }
                        }

                        /*
                         * Share
                         */

                        IconButton(

                            onClick = {

                                val currentNote =
                                    Note(

                                        id = noteId,

                                        title = title,

                                        content =
                                            if (checklistItems.isNotEmpty())
                                                Json.encodeToString(
                                                    checklistItems
                                                )
                                            else
                                                content,

                                        colorHex = colorHex
                                    )

                                FileExportHelper
                                    .exportNoteToTxt(
                                        context,
                                        currentNote
                                    )
                            }

                        ) {

                            EditorNeonIcon {

                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Paylaş"
                                )
                            }
                        }

                        /*
                         * Preview
                         */

                        IconButton(

                            onClick = {
                                isPreviewMode =
                                    !isPreviewMode
                            }

                        ) {

                            EditorNeonIcon {

                                Icon(

                                    if (isPreviewMode)
                                        Icons.Default.Edit
                                    else
                                        Icons.Default.Visibility,

                                    contentDescription =
                                        "Önizle"
                                )
                            }
                        }

                        /*
                         * Delete
                         */

                        if (note != null) {

                            IconButton(

                                onClick = {
                                    showDeleteDialog = true
                                }

                            ) {

                                EditorNeonIcon {

                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Sil"
                                    )
                                }
                            }
                        }

                    } else {

                        /*
                         * ----------------------------------------------------
                         * SKETCH MODE
                         * ----------------------------------------------------
                         */

                        IconButton(

                            onClick = {
                                isSketchMode = false
                            }

                        ) {

                            EditorNeonIcon {

                                Icon(
                                    Icons.Default.TextFields,
                                    contentDescription =
                                        "Metin Modu"
                                )
                            }
                        }
                    }

                    /*
                     * Finish
                     */

                    IconButton(
                        onClick = onBack
                    ) {

                        EditorNeonIcon {

                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Bitti"
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

        /*
         * --------------------------------------------------------------------
         * ROOT EDITOR AREA
         * --------------------------------------------------------------------
         *
         * IMPORTANT:
         *
         * The drawing surface is NOT allowed to cover this whole root.
         *
         * The drawing canvas receives its own bounded Box below.
         */

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

            if (!isPreviewMode) {

                /*
                 * ----------------------------------------------------------------
                 * TOP METADATA AREA
                 * ----------------------------------------------------------------
                 */

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            ),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    /*
                     * Folder selector
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
                                    }
                                )
                            }
                        }
                    }

                    /*
                     * Note background color
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
                 * ----------------------------------------------------------------
                 * TITLE AREA
                 * ----------------------------------------------------------------
                 *
                 * Title is completely outside DrawingCanvas.
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
                                MaterialTheme
                                    .typography
                                    .headlineSmall
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
                        MaterialTheme
                            .typography
                            .headlineSmall
                            .copy(
                                fontWeight =
                                    FontWeight.Bold
                            )
                )

                /*
                 * ----------------------------------------------------------------
                 * SKETCH MODE
                 * ----------------------------------------------------------------
                 */

                if (isSketchMode) {

                    /*
                     * IMPORTANT DRAWING BOUNDARY
                     *
                     * DrawingCanvas is isolated inside this Box.
                     *
                     * It cannot draw outside this area.
                     *
                     * Therefore:
                     *
                     * - Title is protected
                     * - Top metadata/menu is protected
                     * - Bottom note field is protected
                     * - Eraser cannot visually extend outside canvas area
                     */

                    Box(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                )
                                .clip(
                                    RoundedCornerShape(12.dp)
                                )
                                .clipToBounds()
                    ) {

                        DrawingCanvas(

                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .clipToBounds(),

                            initialData =
                                sketchData,

                            onDataChanged = {
                                sketchData = it
                            }
                        )
                    }

                    /*
                     * ----------------------------------------------------------------
                     * BOTTOM NOTE / DESCRIPTION AREA
                     * ----------------------------------------------------------------
                     *
                     * This is outside DrawingCanvas.
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
                                    MaterialTheme
                                        .typography
                                        .bodyMedium
                            )
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
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
                            )
                    )

                } else if (

                    note?.type ==
                        NoteType.CHECKLIST ||

                    checklistItems.isNotEmpty()

                ) {

                    /*
                     * ----------------------------------------------------------------
                     * CHECKLIST
                     * ----------------------------------------------------------------
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
                     * ----------------------------------------------------------------
                     * IMAGE AREA
                     * ----------------------------------------------------------------
                     */

                    if (imageUris.isNotEmpty()) {

                        LazyRow(

                            modifier =
                                Modifier.padding(
                                    16.dp
                                ),

                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
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
                                                Modifier.size(
                                                    16.dp
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    /*
                     * ----------------------------------------------------------------
                     * NORMAL TEXT AREA
                     * ----------------------------------------------------------------
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
                                    MaterialTheme
                                        .typography
                                        .bodyLarge
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
                            MaterialTheme
                                .typography
                                .bodyLarge
                    )
                }

            } else {

                /*
                 * --------------------------------------------------------------------
                 * PREVIEW MODE
                 * --------------------------------------------------------------------
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

                    val displayTitle =

                        if (
                            title.isEmpty() &&
                            checklistItems.isNotEmpty()
                        ) {

                            checklistItems
                                .firstOrNull()
                                ?.text
                                ?: "Başlıksız Not"

                        } else if (title.isEmpty()) {

                            "Başlıksız Not"

                        } else {

                            title
                        }

                    Text(

                        displayTitle,

                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    /*
                     * Checklist preview
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

                                    enabled = false
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
                                                        TextDecoration
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

                        Text(

                            content,

                            style =
                                MaterialTheme
                                    .typography
                                    .bodyLarge
                        )
                    }

                    /*
                     * Sketch information
                     */

                    if (
                        sketchData?.isNotEmpty() == true
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(24.dp)
                        )

                        Text(

                            "Çizim İçeriyor " +
                                "(Düzenlemek için Sketch moduna geçin)",

                            style =
                                MaterialTheme
                                    .typography
                                    .labelSmall,

                            color =
                                Color.Gray
                        )
                    }

                    /*
                     * Images
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
     * ------------------------------------------------------------------------
     * DELETE DIALOG
     * ------------------------------------------------------------------------
     */

    if (
        showDeleteDialog &&
        note != null
    ) {

        AlertDialog(

            onDismissRequest = {
                showDeleteDialog = false
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

                        onBack()
                    }

                ) {

                    Text(
                        "Sil",
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {
                        showDeleteDialog = false
                    }

                ) {

                    Text("Vazgeç")
                }
            }
        )
    }
}