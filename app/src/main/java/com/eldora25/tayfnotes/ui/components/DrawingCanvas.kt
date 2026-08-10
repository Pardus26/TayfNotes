package com.eldora25.tayfnotes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.eldora25.tayfnotes.ui.theme.NeonIcon
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A single vector drawing path.
 *
 * IMPORTANT:
 * Existing serialized DrawPath data is intentionally preserved.
 * New tool types are backward compatible with old PEN/MARKER/ERASER/SHAPE data.
 */
@Serializable
data class DrawPath(
    val points: List<Point>,
    val colorHex: String,
    val strokeWidth: Float,
    val toolType: ToolType = ToolType.PEN,
    val shapeType: ShapeType? = null,
    val isFilled: Boolean = false,
    val fillColorHex: String? = null
)

enum class ToolType {
    PEN,
    PENCIL,
    BRUSH,
    MARKER,
    HIGHLIGHTER,
    ERASER,
    SHAPE
}

enum class ShapeType {
    RECTANGLE,
    CIRCLE,
    TRIANGLE,
    ELLIPSE,
    ARC
}

@Serializable
data class Point(
    val x: Float,
    val y: Float
)

/**
 * Image placed inside the sketch page.
 *
 * Position and size are persisted inside sketchData.
 */
@Serializable
data class SketchImage(
    val uri: String,
    val x: Float = 40f,
    val y: Float = 40f,
    val width: Float = 260f,
    val height: Float = 260f
)

/**
 * New sketch document format.
 *
 * Older TayfNotes versions stored only List<DrawPath>.
 * DrawingCanvas automatically migrates old data into this format.
 */
@Serializable
data class SketchDocument(
    val paths: List<DrawPath> = emptyList(),
    val images: List<SketchImage> = emptyList()
)

@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    initialData: String? = null,
    onDataChanged: (String) -> Unit,
    onRequestImage: () -> Unit = {},
    pendingImageUri: String? = null,
    onPendingImageConsumed: () -> Unit = {}
) {
    /*
     * ------------------------------------------------------------
     * DOCUMENT LOADING
     * ------------------------------------------------------------
     *
     * First try the new SketchDocument format.
     * If that fails, try the old List<DrawPath> format.
     */
    var document by remember(initialData) {
        mutableStateOf(loadSketchDocument(initialData))
    }

    var currentColor by remember {
        mutableStateOf(Color.Black)
    }

    var currentFillColor by remember {
        mutableStateOf(Color.Transparent)
    }

    var currentStrokeWidth by remember {
        mutableStateOf(5f)
    }

    var currentOpacity by remember {
        mutableStateOf(1f)
    }

    var currentTool by remember {
        mutableStateOf(ToolType.PEN)
    }

    var currentShape by remember {
        mutableStateOf(ShapeType.RECTANGLE)
    }

    var isFillEnabled by remember {
        mutableStateOf(false)
    }

    /*
     * First click selects the tool.
     * Second click opens its settings menu.
     */
    var openedSettingsTool by remember {
        mutableStateOf<ToolType?>(null)
    }

    var showColorPicker by remember {
        mutableStateOf(false)
    }

    var showShapePicker by remember {
        mutableStateOf(false)
    }

    var showClearDialog by remember {
        mutableStateOf(false)
    }

    /*
     * Current stroke being drawn.
     */
    val currentPathPoints = remember {
        mutableStateListOf<Point>()
    }

    /*
     * Undo / redo stacks.
     *
     * Each entry represents a complete document state.
     */
    val undoStack = remember {
        mutableStateListOf<SketchDocument>()
    }

    val redoStack = remember {
        mutableStateListOf<SketchDocument>()
    }

    /*
     * ------------------------------------------------------------
     * HELPERS
     * ------------------------------------------------------------
     */

    fun serializeDocument(data: SketchDocument): String {
        return Json.encodeToString(data)
    }

    fun commitDocument(
        newDocument: SketchDocument,
        addToUndo: Boolean = true
    ) {
        if (addToUndo && newDocument != document) {
            undoStack.add(document)

            /*
             * Prevent unlimited memory growth during normal usage.
             */
            if (undoStack.size > 100) {
                undoStack.removeAt(0)
            }

            redoStack.clear()
        }

        document = newDocument
        onDataChanged(serializeDocument(newDocument))
    }

    fun undo() {
        if (undoStack.isEmpty()) return

        val previous = undoStack.removeAt(undoStack.lastIndex)

        redoStack.add(document)
        document = previous

        onDataChanged(serializeDocument(document))
    }

    fun redo() {
        if (redoStack.isEmpty()) return

        val next = redoStack.removeAt(redoStack.lastIndex)

        undoStack.add(document)
        document = next

        onDataChanged(serializeDocument(document))
    }

    fun clearDocument() {
        if (document.paths.isEmpty() && document.images.isEmpty()) {
            return
        }

        commitDocument(
            SketchDocument(),
            addToUndo = true
        )

        currentPathPoints.clear()
    }

    fun addImage(uri: String) {
        if (uri.isBlank()) return

        val newImage = SketchImage(
            uri = uri,
            x = 50f,
            y = 50f,
            width = 300f,
            height = 300f
        )

        commitDocument(
            document.copy(
                images = document.images + newImage
            )
        )
    }

    /*
     * If NoteEditorScreen selected an image, put it into
     * the sketch document.
     */
    LaunchedEffect(pendingImageUri) {
        val uri = pendingImageUri

        if (!uri.isNullOrBlank()) {
            addImage(uri)
            onPendingImageConsumed()
        }
    }

    /*
     * ------------------------------------------------------------
     * UI
     * ------------------------------------------------------------
     */

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        /*
         * ========================================================
         * SKETCH TOOLBAR
         * ========================================================
         */
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
            tonalElevation = 6.dp,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                /*
                 * ------------------------------------------------
                 * TOP ACTION ROW
                 * ------------------------------------------------
                 */
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    /*
                     * UNDO
                     */
                    IconButton(
                        onClick = { undo() },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Undo,
                            contentDescription = "Geri Al"
                        )
                    }

                    /*
                     * REDO
                     */
                    IconButton(
                        onClick = { redo() },
                        enabled = redoStack.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Redo,
                            contentDescription = "Yinele"
                        )
                    }

                    /*
                     * CLEAR PAGE
                     */
                    IconButton(
                        onClick = {
                            showClearDialog = true
                        }
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Sayfayı Temizle"
                        )
                    }

                    /*
                     * ADD IMAGE
                     */
                    IconButton(
                        onClick = {
                            onRequestImage()
                        }
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Sayfaya Resim Ekle"
                        )
                    }

                    Spacer(
                        modifier = Modifier.weight(1f)
                    )

                    /*
                     * COLOR
                     */
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable {
                                showColorPicker = true
                            }
                    ) {
                        NeonIcon(
                            backgroundColor = currentColor,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.ColorLens,
                                contentDescription = "Renk",
                                tint = if (currentColor.luminance() > 0.5f) {
                                    Color.Black
                                } else {
                                    Color.White
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                HorizontalDivider()

                /*
                 * ------------------------------------------------
                 * TOOL ROW
                 * ------------------------------------------------
                 *
                 * IMPORTANT:
                 *
                 * 1st click:
                 *     select tool
                 *
                 * 2nd click:
                 *     open floating settings menu
                 */
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    item {
                        ToolButton(
                            tool = ToolType.PEN,
                            selectedTool = currentTool,
                            openedSettingsTool = openedSettingsTool,
                            icon = Icons.Default.Create,
                            label = "Kalem",
                            onClick = {
                                if (currentTool == ToolType.PEN) {
                                    openedSettingsTool =
                                        if (openedSettingsTool == ToolType.PEN) {
                                            null
                                        } else {
                                            ToolType.PEN
                                        }
                                } else {
                                    currentTool = ToolType.PEN
                                    openedSettingsTool = null
                                }
                            },
                            onDismissSettings = {
                                openedSettingsTool = null
                            },
                            currentStrokeWidth = currentStrokeWidth,
                            currentOpacity = currentOpacity,
                            onStrokeWidthChanged = {
                                currentStrokeWidth = it
                            },
                            onOpacityChanged = {
                                currentOpacity = it
                            }
                        )
                    }

                    item {
                        ToolButton(
                            tool = ToolType.PENCIL,
                            selectedTool = currentTool,
                            openedSettingsTool = openedSettingsTool,
                            icon = Icons.Default.Edit,
                            label = "Kurşun Kalem",
                            onClick = {
                                if (currentTool == ToolType.PENCIL) {
                                    openedSettingsTool =
                                        if (openedSettingsTool == ToolType.PENCIL) {
                                            null
                                        } else {
                                            ToolType.PENCIL
                                        }
                                } else {
                                    currentTool = ToolType.PENCIL
                                    openedSettingsTool = null
                                }
                            },
                            onDismissSettings = {
                                openedSettingsTool = null
                            },
                            currentStrokeWidth = currentStrokeWidth,
                            currentOpacity = currentOpacity,
                            onStrokeWidthChanged = {
                                currentStrokeWidth = it
                            },
                            onOpacityChanged = {
                                currentOpacity = it
                            }
                        )
                    }

                    item {
                        ToolButton(
                            tool = ToolType.BRUSH,
                            selectedTool = currentTool,
                            openedSettingsTool = openedSettingsTool,
                            icon = Icons.Default.Brush,
                            label = "Fırça",
                            onClick = {
                                if (currentTool == ToolType.BRUSH) {
                                    openedSettingsTool =
                                        if (openedSettingsTool == ToolType.BRUSH) {
                                            null
                                        } else {
                                            ToolType.BRUSH
                                        }
                                } else {
                                    currentTool = ToolType.BRUSH
                                    openedSettingsTool = null
                                }
                            },
                            onDismissSettings = {
                                openedSettingsTool = null
                            },
                            currentStrokeWidth = currentStrokeWidth,
                            currentOpacity = currentOpacity,
                            onStrokeWidthChanged = {
                                currentStrokeWidth = it
                            },
                            onOpacityChanged = {
                                currentOpacity = it
                            }
                        )
                    }

                    item {
                        ToolButton(
                            tool = ToolType.MARKER,
                            selectedTool = currentTool,
                            openedSettingsTool = openedSettingsTool,
                            icon = Icons.Default.Brush,
                            label = "Marker",
                            onClick = {
                                if (currentTool == ToolType.MARKER) {
                                    openedSettingsTool =
                                        if (openedSettingsTool == ToolType.MARKER) {
                                            null
                                        } else {
                                            ToolType.MARKER
                                        }
                                } else {
                                    currentTool = ToolType.MARKER
                                    openedSettingsTool = null
                                }
                            },
                            onDismissSettings = {
                                openedSettingsTool = null
                            },
                            currentStrokeWidth = currentStrokeWidth,
                            currentOpacity = currentOpacity,
                            onStrokeWidthChanged = {
                                currentStrokeWidth = it
                            },
                            onOpacityChanged = {
                                currentOpacity = it
                            }
                        )
                    }

                    item {
                        ToolButton(
                            tool = ToolType.HIGHLIGHTER,
                            selectedTool = currentTool,
                            openedSettingsTool = openedSettingsTool,
                            icon = Icons.Default.Highlight,
                            label = "Fosforlu",
                            onClick = {
                                if (currentTool == ToolType.HIGHLIGHTER) {
                                    openedSettingsTool =
                                        if (openedSettingsTool == ToolType.HIGHLIGHTER) {
                                            null
                                        } else {
                                            ToolType.HIGHLIGHTER
                                        }
                                } else {
                                    currentTool = ToolType.HIGHLIGHTER
                                    openedSettingsTool = null
                                }
                            },
                            onDismissSettings = {
                                openedSettingsTool = null
                            },
                            currentStrokeWidth = currentStrokeWidth,
                            currentOpacity = currentOpacity,
                            onStrokeWidthChanged = {
                                curre