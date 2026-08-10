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
                                currentStrokeWidth = it
                            },
                            onOpacityChanged = {
                                currentOpacity = it
                            }
                        )
                    }

                    item {
                        ToolButton(
                            tool = ToolType.ERASER,
                            selectedTool = currentTool,
                            openedSettingsTool = openedSettingsTool,
                            icon = Icons.Default.AutoFixNormal,
                            label = "Silgi",
                            onClick = {
                                if (currentTool == ToolType.ERASER) {
                                    openedSettingsTool =
                                        if (openedSettingsTool == ToolType.ERASER) {
                                            null
                                        } else {
                                            ToolType.ERASER
                                        }
                                } else {
                                    currentTool = ToolType.ERASER
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
                        Box {
                            IconButton(
                                onClick = {
                                    if (currentTool == ToolType.SHAPE) {
                                        showShapePicker = !showShapePicker
                                    } else {
                                        currentTool = ToolType.SHAPE
                                        openedSettingsTool = null
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Category,
                                    contentDescription = "Şekiller",
                                    tint = if (currentTool == ToolType.SHAPE) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        LocalContentColor.current
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        /*
         * ========================================================
         * DRAWING PAGE
         * ========================================================
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White)
        ) {

            /*
             * ----------------------------------------------------
             * VECTOR CANVAS
             * ----------------------------------------------------
             */
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(
                        currentTool,
                        currentShape,
                        currentColor,
                        currentStrokeWidth,
                        currentOpacity,
                        isFillEnabled,
                        currentFillColor
                    ) {
                        detectDragGestures(

                            onDragStart = { offset ->
                                currentPathPoints.clear()

                                currentPathPoints.add(
                                    Point(
                                        offset.x,
                                        offset.y
                                    )
                                )
                            },

                            onDrag = { change, _ ->

                                if (currentTool != ToolType.SHAPE) {

                                    currentPathPoints.add(
                                        Point(
                                            change.position.x,
                                            change.position.y
                                        )
                                    )

                                } else {

                                    if (currentPathPoints.size > 1) {
                                        currentPathPoints.removeAt(1)
                                    }

                                    currentPathPoints.add(
                                        Point(
                                            change.position.x,
                                            change.position.y
                                        )
                                    )
                                }
                            },

                            onDragEnd = {

                                if (currentPathPoints.isNotEmpty()) {

                                    val colorString =
                                        currentColor.toHex()

                                    val fillColorString =
                                        if (isFillEnabled) {
                                            currentFillColor.toHex()
                                        } else {
                                            null
                                        }

                                    val newPath =
                                        DrawPath(
                                            points = currentPathPoints.toList(),
                                            colorHex = colorString,
                                            strokeWidth = currentStrokeWidth,
                                            toolType = currentTool,
                                            shapeType =
                                                if (currentTool == ToolType.SHAPE) {
                                                    currentShape
                                                } else {
                                                    null
                                                },
                                            isFilled = isFillEnabled,
                                            fillColorHex = fillColorString
                                        )

                                    commitDocument(
                                        document.copy(
                                            paths =
                                                document.paths + newPath
                                        )
                                    )

                                    currentPathPoints.clear()
                                }
                            }
                        )
                    }
            ) {

                /*
                 * Existing completed paths.
                 */
                document.paths.forEach {
                    drawDataPath(it)
                }

                /*
                 * Current live preview.
                 */
                if (currentPathPoints.isNotEmpty()) {

                    val previewPath =
                        DrawPath(
                            points =
                                currentPathPoints.toList(),
                            colorHex =
                                currentColor.toHex(),
                            strokeWidth =
                                currentStrokeWidth,
                            toolType =
                                currentTool,
                            shapeType =
                                if (currentTool == ToolType.SHAPE) {
                                    currentShape
                                } else {
                                    null
                                },
                            isFilled =
                                isFillEnabled,
                            fillColorHex =
                                if (isFillEnabled) {
                                    currentFillColor.toHex()
                                } else {
                                    null
                                }
                        )

                    drawDataPath(previewPath)
                }
            }

            /*
             * ----------------------------------------------------
             * IMAGE LAYER
             * ----------------------------------------------------
             *
             * Images are drawn above the vector canvas.
             *
             * They are currently displayed as movable sketch
             * elements and their positions are persisted.
             */
            document.images.forEachIndexed { index, image ->

                AsyncImage(
                    model = image.uri,
                    contentDescription = "Sketch resmi",
                    modifier = Modifier
                        .offset(
                            x = image.x.dp,
                            y = image.y.dp
                        )
                        .size(
                            width = image.width.dp,
                            height = image.height.dp
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.35f
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentScale = ContentScale.Fit
                )
            }

            /*
             * Empty canvas hint.
             */
            if (
                document.paths.isEmpty() &&
                document.images.isEmpty() &&
                currentPathPoints.isEmpty()
            ) {
                Text(
                    text = "Çizime başlamak için kalem veya fırça seçin",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    /*
     * ============================================================
     * CLEAR PAGE DIALOG
     * ============================================================
     */
    if (showClearDialog) {

        AlertDialog(
            onDismissRequest = {
                showClearDialog = false
            },

            title = {
                Text("Sayfayı Temizle")
            },

            text = {
                Text(
                    "Bu işlem sayfadaki tüm çizimleri ve eklenen resimleri kaldıracaktır. İşlem geri alınabilir."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        clearDocument()
                        showClearDialog = false
                    }
                ) {
                    Text(
                        "Temizle",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showClearDialog = false
                    }
                ) {
                    Text("Vazgeç")
                }
            }
        )
    }

    /*
     * ============================================================
     * COLOR PICKER
     * ============================================================
     */
    if (showColorPicker) {

        AlertDialog(
            onDismissRequest = {
                showColorPicker = false
            },

            title = {
                Text("Renk ve Dolgu")
            },

            text = {

                Column {

                    val colors =
                        listOf(
                            Color.Black,
                            Color.DarkGray,
                            Color.Gray,
                            Color.Red,
                            Color.Blue,
                            Color.Green,
                            Color.Yellow,
                            Color.Magenta,
                            Color.Cyan,
                            Color(0xFFFF9800),
                            Color(0xFF9C27B0),
                            Color.White
                        )

                    Text(
                        "Çizgi Rengi",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {

                        colors.forEach { color ->

                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(34.dp)
                                    .background(
                                        color,
                                        CircleShape
                                    )
                                    .border(
                                        width =
                                            if (currentColor == color) {
                                                3.dp
                                            } else {
                                                1.dp
                                            },
                                        color =
                                            if (currentColor == color) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.Gray
                                            },
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        currentColor = color
                                    }
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Checkbox(
                            checked = isFillEnabled,
                            onCheckedChange = {
                                isFillEnabled = it
                            }
                        )

                        Text("Şekil dolgusu")
                    }

                    if (isFillEnabled) {

                        Text(
                            "Dolgu Rengi",
                            style = MaterialTheme.typography.labelMedium
                        )

                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {

                            colors.forEach { color ->

                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(30.dp)
                                        .background(
                                            color,
                                            CircleShape
                                        )
                                        .border(
                                            width =
                                                if (currentFillColor == color) {
                                                    3.dp
                                                } else {
                                                    1.dp
                                                },
                                            color =
                                                if (currentFillColor == color) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    Color.Gray
                                                },
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            currentFillColor = color
                                        }
                                )
                            }
                        }
                    }
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        showColorPicker = false
                    }
                ) {
                    Text("Tamam")
                }
            }
        )
    }

    /*
     * ============================================================
     * SHAPE PICKER
     * ============================================================
     */
    if (showShapePicker) {

        AlertDialog(
            onDismissRequest = {
                showShapePicker = false
            },

            title = {
                Text("Şekil Seç")
            },

            text = {

                Column {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceEvenly
                    ) {

                        ShapeButton(
                            icon = Icons.Default.Rectangle,
                            description = "Dikdörtgen",
                            onClick = {
                                currentTool = ToolType.SHAPE
                                currentShape =
                                    ShapeType.RECTANGLE
                                showShapePicker = false
                            }
                        )

                        ShapeButton(
                            icon = Icons.Default.Circle,
                            description = "Daire",
                            onClick = {
                                currentTool = ToolType.SHAPE
                                currentShape =
                                    ShapeType.CIRCLE
                                showShapePicker = false
                            }
                        )

                        ShapeButton(
                            icon = Icons.Default.ChangeHistory,
                            description = "Üçgen",
                            onClick = {
                                currentTool = ToolType.SHAPE
                                currentShape =
                                    ShapeType.TRIANGLE
                                showShapePicker = false
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceEvenly
                    ) {

                        ShapeButton(
                            icon = Icons.Default.FilterTiltShift,
                            description = "Elips",
                            onClick = {
                                currentTool = ToolType.SHAPE
                                currentShape =
                                    ShapeType.ELLIPSE
                                showShapePicker = false
                            }
                        )

                        ShapeButton(
                            icon = Icons.Default.Architecture,
                            description = "Yay",
                            onClick = {
                                currentTool = ToolType.SHAPE
                                currentShape =
                                    ShapeType.ARC
                                showShapePicker = false
                            }
                        )
                    }
                }
            },

            confirmButton = {}
        )
    }
}

/**
 * Tool button with the requested two-stage interaction:
 *
 * First press  -> select
 * Second press -> settings menu
 */
@Composable
private fun ToolButton(
    tool: ToolType,
    selectedTool: ToolType,
    openedSettingsTool: ToolType?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    onDismissSettings: () -> Unit,
    currentStrokeWidth: Float,
    currentOpacity: Float,
    onStrokeWidthChanged: (Float) -> Unit,
    onOpacityChanged: (Float) -> Unit
) {
    Box {

        IconButton(
            onClick = onClick
        ) {

            Icon(
                imageVector = icon,
                contentDescription = label,
                tint =
                    if (selectedTool == tool) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        LocalContentColor.current
                    }
            )
        }

        /*
         * Floating settings menu.
         */
        DropdownMenu(
            expanded = openedSettingsTool == tool,
            onDismissRequest = onDismissSettings
        ) {

            Column(
                modifier = Modifier
                    .width(260.dp)
                    .padding(14.dp)
            ) {

                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = "Boyut: ${currentStrokeWidth.roundToInt()} px",
                    style = MaterialTheme.typography.labelMedium
                )

                Slider(
                    value = currentStrokeWidth,
                    onValueChange = onStrokeWidthChanged,
                    valueRange = 1f..100f
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "Opaklık: ${(currentOpacity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium
                )

                Slider(
                    value = currentOpacity,
                    onValueChange = onOpacityChanged,
                    valueRange = 0.05f..1f
                )
            }
        }
    }
}

@Composable
private fun ShapeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick
    ) {
        Icon(
            icon,
            contentDescription = description
        )
    }
}

/**
 * Loads both old and new sketch formats.
 */
private fun loadSketchDocument(
    initialData: String?
): SketchDocument {

    if (initialData.isNullOrBlank()) {
        return SketchDocument()
    }

    /*
     * New format.
     */
    try {
        return Json.decodeFromString<SketchDocument>(
            initialData
        )
    } catch (_: Exception) {
        // Continue with legacy migration.
    }

    /*
     * Legacy TayfNotes format:
     *
     * List<DrawPath>
     */
    return try {

        val oldPaths =
            Json.decodeFromString<List<DrawPath>>(
                initialData
            )

        SketchDocument(
            paths = oldPaths
        )

    } catch (_: Exception) {

        SketchDocument()
    }
}

/**
 * Converts Compose Color into #RRGGBB.
 */
private fun Color.toHex(): String {

    val r = (red * 255f).roundToInt().coerceIn(0, 255)
    val g = (green * 255f).roundToInt().coerceIn(0, 255)
    val b = (blue * 255f).roundToInt().coerceIn(0, 255)

    return String.format(
        "#%02X%02X%02X",
        r,
        g,
        b
    )
}

private fun Color.luminance(): Float {
    return (
        0.2126f * red +
            0.7152f * green +
            0.0722f * blue
        )
}

/**
 * Draws a serialized DrawPath.
 *
 * Tool visual characteristics:
 *
 * PEN         = precise
 * PENCIL      = softer
 * BRUSH       = thicker
 * MARKER      = semi transparent
 * HIGHLIGHTER = wide + transparent
 * ERASER      = white
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDataPath(
    drawPath: DrawPath
) {

    val baseColor =
        try {
            Color(
                android.graphics.Color.parseColor(
                    drawPath.colorHex
                )
            )
        } catch (_: Exception) {
            Color.Black
        }

    val color: Color
    val effectiveWidth: Float
    val cap: StrokeCap

    when (drawPath.toolType) {

        ToolType.PEN -> {
            color = baseColor
            effectiveWidth = drawPath.strokeWidth
            cap = StrokeCap.Round
        }

        ToolType.PENCIL -> {
            color = baseColor.copy(alpha = 0.72f)
            effectiveWidth =
                max(
                    1f,
                    drawPath.strokeWidth * 0.75f
                )
            cap = StrokeCap.Round
        }

        ToolType.BRUSH -> {
            color = baseColor
            effectiveWidth =
                max(
                    2f,
                    drawPath.strokeWidth * 1.8f
                )
            cap = StrokeCap.Round
        }

        ToolType.MARKER -> {
            color = baseColor.copy(alpha = 0.45f)
            effectiveWidth =
                max(
                    3f,
                    drawPath.strokeWidth * 1.35f
                )
            cap = StrokeCap.Round
        }

        ToolType.HIGHLIGHTER -> {
            color = baseColor.copy(alpha = 0.28f)
            effectiveWidth =
                max(
                    8f,
                    drawPath.strokeWidth * 2.8f
                )
            cap = StrokeCap.Round
        }

        ToolType.ERASER -> {
            color = Color.White
            effectiveWidth =
                max(
                    5f,
                    drawPath.strokeWidth * 2f
                )
            cap = StrokeCap.Round
        }

        ToolType.SHAPE -> {
            color = baseColor
            effectiveWidth = drawPath.strokeWidth
            cap = StrokeCap.Round
        }
    }

    val fillColor =
        if (
            drawPath.isFilled &&
            drawPath.fillColorHex != null
        ) {
            try {
                Color(
                    android.graphics.Color.parseColor(
                        drawPath.fillColorHex
                    )
                )
            } catch (_: Exception) {
                Color.Transparent
            }
        } else {
            Color.Transparent
        }

    /*
     * ------------------------------------------------------------
     * SHAPES
     * ------------------------------------------------------------
     */
    if (
        drawPath.toolType == ToolType.SHAPE &&
        drawPath.points.size >= 2
    ) {

        val start =
            Offset(
                drawPath.points[0].x,
                drawPath.points[0].y
            )

        val end =
            Offset(
                drawPath.points[1].x,
                drawPath.points[1].y
            )

        val left = min(start.x, end.x)
        val top = min(start.y, end.y)

        val width = abs(start.x - end.x)
        val height = abs(start.y - end.y)

        when (drawPath.shapeType) {

            ShapeType.RECTANGLE -> {

                if (drawPath.isFilled) {
                    drawRect(
                        color = fillColor,
                        topLeft = Offset(left, top),
                        size = Size(width, height)
                    )
                }

                drawRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(
                        width = effectiveWidth
                    )
                )
            }

            ShapeType.CIRCLE -> {

                val radius =
                    min(width, height) / 2f

                val center =
                    Offset(
                        left + width / 2f,
                        top + height / 2f
                    )

                if (drawPath.isFilled) {
                    drawCircle(
                        color = fillColor,
                        radius = radius,
                        center = center
                    )
                }

                drawCircle(
                    color = color,
                    radius = radius,
                    center = center,
                    style = Stroke(
                        width = effectiveWidth
                    )
                )
            }

            ShapeType.TRIANGLE -> {

                val triangle =
                    Path().apply {

                        moveTo(
                            left + width / 2f,
                            top
                        )

                        lineTo(
                            left,
                            top + height
                        )

                        lineTo(
                            left + width,
                            top + height
                        )

                        close()
                    }

                if (drawPath.isFilled) {
                    drawPath(
                        path = triangle,
                        color = fillColor
                    )
                }

                drawPath(
                    path = triangle,
                    color = color,
                    style = Stroke(
                        width = effectiveWidth
                    )
                )
            }

            ShapeType.ELLIPSE -> {

                if (drawPath.isFilled) {
                    drawOval(
                        color = fillColor,
                        topLeft = Offset(left, top),
                        size = Size(width, height)
                    )
                }

                drawOval(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(
                        width = effectiveWidth
                    )
                )
            }

            ShapeType.ARC -> {

                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(
                        width = effectiveWidth
                    )
                )
            }

            null -> Unit
        }

        return
    }

    /*
     * ------------------------------------------------------------
     * FREEHAND PATH
     * ------------------------------------------------------------
     */
    if (drawPath.points.isEmpty()) {
        return
    }

    val path = Path()

    path.moveTo(
        drawPath.points[0].x,
        drawPath.points[0].y
    )

    drawPath.points
        .drop(1)
        .forEach { point ->

            path.lineTo(
                point.x,
                point.y
            )
        }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = effectiveWidth,
            cap = cap
        )
    )
}