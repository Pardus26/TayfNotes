package com.eldora25.tayfnotes.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb

import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

import coil.compose.AsyncImage

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


// =============================================================
// TOOL TYPES
// =============================================================

enum class ToolType {
    PEN,
    PENCIL,
    INK,
    BRUSH,
    MARKER,
    ERASER,
    SHAPE
}


// =============================================================
// SHAPE TYPES
// =============================================================

enum class ShapeType {
    RECTANGLE,
    CIRCLE,
    TRIANGLE,
    ELLIPSE,
    ARC
}


// =============================================================
// DRAWING POINT
// =============================================================
//
// pressure:
// 0.0f - 1.0f
//
// Eski kayıtlarla uyumluluk için default = 1f.
// =============================================================

@Serializable
data class Point(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f
)


// =============================================================
// DRAW PATH
// =============================================================

@Serializable
data class DrawPath(
    val points: List<Point>,
    val colorHex: String,
    val strokeWidth: Float,
    val toolType: ToolType = ToolType.PEN,
    val shapeType: ShapeType? = null,
    val isFilled: Boolean = false,
    val fillColorHex: String? = null,
    val opacity: Float = 1f
)


// =============================================================
// IMAGE
// =============================================================

@Serializable
data class SketchImage(
    val id: String,
    val uri: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)


// =============================================================
// DOCUMENT
// =============================================================

@Serializable
data class SketchDocument(
    val paths: List<DrawPath> = emptyList(),
    val images: List<SketchImage> = emptyList()
)


// =============================================================
// TOOL SETTINGS
// =============================================================

data class ToolSettings(
    val size: Float,
    val opacity: Float
)


// =============================================================
// DEFAULT SETTINGS
// =============================================================

private fun defaultToolSettings(): Map<ToolType, ToolSettings> {

    return mapOf(

        ToolType.PEN to ToolSettings(
            size = 4f,
            opacity = 1f
        ),

        ToolType.PENCIL to ToolSettings(
            size = 3f,
            opacity = 0.75f
        ),

        ToolType.INK to ToolSettings(
            size = 5f,
            opacity = 1f
        ),

        ToolType.BRUSH to ToolSettings(
            size = 12f,
            opacity = 0.70f
        ),

        ToolType.MARKER to ToolSettings(
            size = 20f,
            opacity = 0.45f
        ),

        ToolType.ERASER to ToolSettings(
            size = 30f,
            opacity = 1f
        ),

        ToolType.SHAPE to ToolSettings(
            size = 4f,
            opacity = 1f
        )
    )
}


// =============================================================
// TOOL NAME
// =============================================================

private fun toolDisplayName(
    tool: ToolType
): String {

    return when (tool) {

        ToolType.PEN -> "Kalem"

        ToolType.PENCIL -> "Kurşun Kalem"

        ToolType.INK -> "Mürekkep"

        ToolType.BRUSH -> "Fırça"

        ToolType.MARKER -> "Marker"

        ToolType.ERASER -> "Silgi"

        ToolType.SHAPE -> "Şekil"
    }
}


// =============================================================
// SIZE RANGE
// =============================================================

private fun toolSizeRange(
    tool: ToolType
): ClosedFloatingPointRange<Float> {

    return when (tool) {

        ToolType.ERASER ->
            5f..100f

        ToolType.MARKER ->
            2f..60f

        ToolType.BRUSH ->
            2f..60f

        else ->
            1f..40f
    }
}


// =============================================================
// COLOR -> HEX
// =============================================================

private fun Color.toHex(): String {

    return String.format(
        "#%06X",
        0xFFFFFF and this.toArgb()
    )
}


// =============================================================
// DRAWING CANVAS
// =============================================================

@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    initialData: String? = null,
    onDataChanged: (String) -> Unit
) {

    // =========================================================
    // INITIAL DATA
    // =========================================================

    fun decodeInitialData(
        data: String?
    ): SketchDocument {

        if (data.isNullOrBlank()) {
            return SketchDocument()
        }

        try {

            return Json.decodeFromString<SketchDocument>(
                data
            )

        } catch (_: Exception) {
        }

        try {

            val oldPaths =
                Json.decodeFromString<List<DrawPath>>(
                    data
                )

            return SketchDocument(
                paths = oldPaths
            )

        } catch (_: Exception) {
        }

        return SketchDocument()
    }


    var document by remember {

        mutableStateOf(
            decodeInitialData(initialData)
        )
    }


    // =========================================================
    // UNDO / REDO
    // =========================================================

    var undoStack by remember {

        mutableStateOf<List<SketchDocument>>(
            emptyList()
        )
    }

    var redoStack by remember {

        mutableStateOf<List<SketchDocument>>(
            emptyList()
        )
    }


    // =========================================================
    // CURRENT TOOL
    // =========================================================

    var currentTool by remember {

        mutableStateOf(
            ToolType.PEN
        )
    }


    // =========================================================
    // CURRENT SHAPE
    // =========================================================

    var currentShape by remember {

        mutableStateOf(
            ShapeType.RECTANGLE
        )
    }


    // =========================================================
    // TOOL SETTINGS
    // =========================================================

    var toolSettings by remember {

        mutableStateOf(
            defaultToolSettings()
        )
    }


    // =========================================================
    // CURRENT COLOR
    // =========================================================

    var currentColor by remember {

        mutableStateOf(
            Color.Black
        )
    }


    // =========================================================
    // FILL
    // =========================================================

    var isFillEnabled by remember {

        mutableStateOf(false)
    }

    var currentFillColor by remember {

        mutableStateOf(
            Color.Transparent
        )
    }


    // =========================================================
    // EXPANDED TOOL
    // =========================================================

    var expandedTool by remember {

        mutableStateOf<ToolType?>(
            null
        )
    }


    // =========================================================
    // COLOR MENU
    // =========================================================

    var colorMenuExpanded by remember {

        mutableStateOf(false)
    }


    // =========================================================
    // CURRENT PATH
    // =========================================================

    val currentPathPoints =
        remember {

            mutableStateListOf<Point>()
        }


    // =========================================================
    // CANVAS SIZE
    // =========================================================

    var canvasSize by remember {

        mutableStateOf(
            IntSize.Zero
        )
    }


    // =========================================================
    // IMAGE PICKER
    // =========================================================

    val imagePicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            uri ?: return@rememberLauncherForActivityResult

            if (
                canvasSize.width <= 0 ||
                canvasSize.height <= 0
            ) {
                return@rememberLauncherForActivityResult
            }


            val imageWidth =
                (
                    canvasSize.width * 0.45f
                ).coerceIn(
                    240f,
                    650f
                )


            val imageHeight =
                imageWidth * 0.65f


            val imageX =
                (
                    canvasSize.width -
                        imageWidth
                ) / 2f


            val imageY =
                (
                    canvasSize.height -
                        imageHeight
                ) / 2f


            val newImage =
                SketchImage(

                    id =
                        System.currentTimeMillis()
                            .toString(),

                    uri =
                        uri.toString(),

                    x =
                        imageX,

                    y =
                        imageY,

                    width =
                        imageWidth,

                    height =
                        imageHeight
                )


            undoStack =
                undoStack + document

            redoStack =
                emptyList()


            document =
                document.copy(
                    images =
                        document.images +
                            newImage
                )


            onDataChanged(
                Json.encodeToString(
                    document
                )
            )
        }


    // =========================================================
    // SAVE
    // =========================================================

    fun saveCurrentState() {

        onDataChanged(
            Json.encodeToString(
                document
            )
        )
    }


    // =========================================================
    // UPDATE SETTINGS
    // =========================================================

    fun updateToolSettings(
        tool: ToolType,
        newSettings: ToolSettings
    ) {

        toolSettings =
            toolSettings
                .toMutableMap()
                .apply {

                    this[tool] =
                        newSettings
                }
    }


    // =========================================================
    // SELECT TOOL
    // =========================================================

    fun selectTool(
        tool: ToolType
    ) {

        if (currentTool == tool) {

            expandedTool =
                if (expandedTool == tool) {

                    null

                } else {

                    tool
                }

        } else {

            currentTool =
                tool

            expandedTool =
                null
        }


        colorMenuExpanded =
            false
    }


    // =========================================================
    // UNDO
    // =========================================================

    fun performUndo() {

        if (undoStack.isEmpty()) {
            return
        }


        val previous =
            undoStack.last()


        redoStack =
            redoStack + document


        undoStack =
            undoStack.dropLast(1)


        document =
            previous


        currentPathPoints.clear()

        expandedTool =
            null

        colorMenuExpanded =
            false


        saveCurrentState()
    }


    // =========================================================
    // REDO
    // =========================================================

    fun performRedo() {

        if (redoStack.isEmpty()) {
            return
        }


        val next =
            redoStack.last()


        undoStack =
            undoStack + document


        redoStack =
            redoStack.dropLast(1)


        document =
            next


        currentPathPoints.clear()

        expandedTool =
            null

        colorMenuExpanded =
            false


        saveCurrentState()
    }


    // =========================================================
    // CLEAR
    // =========================================================

    fun clearCanvas() {

        if (
            document.paths.isEmpty() &&
            document.images.isEmpty()
        ) {
            return
        }


        undoStack =
            undoStack + document


        redoStack =
            emptyList()


        document =
            SketchDocument()


        currentPathPoints.clear()

        expandedTool =
            null

        colorMenuExpanded =
            false


        saveCurrentState()
    }


    // =========================================================
    // ROOT
    // =========================================================

    Column(

        modifier =
            modifier
                .fillMaxSize()
                .background(Color.White)
    ) {


        // =====================================================
        // TOOLBAR
        // =====================================================

        Surface(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),

            shape =
                RoundedCornerShape(
                    16.dp
                ),

            color =
                MaterialTheme
                    .colorScheme
                    .surfaceVariant
                    .copy(
                        alpha = 0.95f
                    ),

            tonalElevation =
                4.dp
        ) {


            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(
                            rememberScrollState()
                        )
                        .padding(
                            horizontal = 10.dp,
                            vertical = 4.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                // =================================================
                // UNDO
                // =================================================

                IconButton(

                    enabled =
                        undoStack.isNotEmpty(),

                    onClick = {
                        performUndo()
                    }
                ) {

                    Icon(

                        Icons.Default.Undo,

                        contentDescription =
                            "Geri Al"
                    )
                }


                // =================================================
                // REDO
                // =================================================

                IconButton(

                    enabled =
                        redoStack.isNotEmpty(),

                    onClick = {
                        performRedo()
                    }
                ) {

                    Icon(

                        Icons.Default.Redo,

                        contentDescription =
                            "İleri Al"
                    )
                }


                // =================================================
                // PENCIL
                // =================================================

                ToolButton(

                    tool =
                        ToolType.PENCIL,

                    icon =
                        Icons.Default.Create,

                    selected =
                        currentTool ==
                            ToolType.PENCIL,

                    expanded =
                        expandedTool ==
                            ToolType.PENCIL,

                    onClick = {

                        selectTool(
                            ToolType.PENCIL
                        )
                    },

                    settings =
                        toolSettings[
                            ToolType.PENCIL
                        ] ?: ToolSettings(
                            3f,
                            0.75f
                        ),

                    onSettingsChanged = {

                        updateToolSettings(
                            ToolType.PENCIL,
                            it
                        )
                    },

                    onDismissRequest = {

                        expandedTool =
                            null
                    }
                )


                // =================================================
                // PEN
                // =================================================

                ToolButton(

                    tool =
                        ToolType.PEN,

                    icon =
                        Icons.Default.Edit,

                    selected =
                        currentTool ==
                            ToolType.PEN,

                    expanded =
                        expandedTool ==
                            ToolType.PEN,

                    onClick = {

                        selectTool(
                            ToolType.PEN
                        )
                    },

                    settings =
                        toolSettings[
                            ToolType.PEN
                        ] ?: ToolSettings(
                            4f,
                            1f
                        ),

                    onSettingsChanged = {

                        updateToolSettings(
                            ToolType.PEN,
                            it
                        )
                    },

                    onDismissRequest = {

                        expandedTool =
                            null
                    }
                )


                // =================================================
                // INK
                // =================================================

                ToolButton(

                    tool =
                        ToolType.INK,

                    icon =
                        Icons.Default.Colorize,

                    selected =
                        currentTool ==
                            ToolType.INK,

                    expanded =
                        expandedTool ==
                            ToolType.INK,

                    onClick = {

                        selectTool(
                            ToolType.INK
                        )
                    },

                    settings =
                        toolSettings[
                            ToolType.INK
                        ] ?: ToolSettings(
                            5f,
                            1f
                        ),

                    onSettingsChanged = {

                        updateToolSettings(
                            ToolType.INK,
                            it
                        )
                    },

                    onDismissRequest = {

                        expandedTool =
                            null
                    }
                )


                // =================================================
                // BRUSH
                // =================================================

                ToolButton(

                    tool =
                        ToolType.BRUSH,

                    icon =
                        Icons.Default.Brush,

                    selected =
                        currentTool ==
                            ToolType.BRUSH,

                    expanded =
                        expandedTool ==
                            ToolType.BRUSH,

                    onClick = {

                        selectTool(
                            ToolType.BRUSH
                        )
                    },

                    settings =
                        toolSettings[
                            ToolType.BRUSH
                        ] ?: ToolSettings(
                            12f,
                            0.70f
                        ),

                    onSettingsChanged = {

                        updateToolSettings(
                            ToolType.BRUSH,
                            it
                        )
                    },

                    onDismissRequest = {

                        expandedTool =
                            null
                    }
                )


                // =================================================
                // MARKER
                // =================================================

                ToolButton(

                    tool =
                        ToolType.MARKER,

                    icon =
                        Icons.Default.Brush,

                    selected =
                        currentTool ==
                            ToolType.MARKER,

                    expanded =
                        expandedTool ==
                            ToolType.MARKER,

                    onClick = {

                        selectTool(
                            ToolType.MARKER
                        )
                    },

                    settings =
                        toolSettings[
                            ToolType.MARKER
                        ] ?: ToolSettings(
                            20f,
                            0.45f
                        ),

                    onSettingsChanged = {

                        updateToolSettings(
                            ToolType.MARKER,
                            it
                        )
                    },

                    onDismissRequest = {

                        expandedTool =
                            null
                    }
                )


                // =================================================
                // SHAPES
                // =================================================

                Box {

                    IconButton(

                        onClick = {

                            if (
                                currentTool ==
                                ToolType.SHAPE
                            ) {

                                expandedTool =
                                    if (
                                        expandedTool ==
                                        ToolType.SHAPE
                                    ) {

                                        null

                                    } else {

                                        ToolType.SHAPE
                                    }

                            } else {

                                currentTool =
                                    ToolType.SHAPE

                                expandedTool =
                                    ToolType.SHAPE
                            }

                            colorMenuExpanded =
                                false
                        }
                    ) {

                        Icon(

                            Icons.Default.Category,

                            contentDescription =
                                "Şekiller",

                            tint =
                                if (
                                    currentTool ==
                                    ToolType.SHAPE
                                ) {

                                    MaterialTheme
                                        .colorScheme
                                        .primary

                                } else {

                                    LocalContentColor.current
                                }
                        )
                    }


                    DropdownMenu(

                        expanded =
                            expandedTool ==
                                ToolType.SHAPE,

                        onDismissRequest = {

                            expandedTool =
                                null
                        }
                    ) {

                        DropdownMenuItem(

                            text = {
                                Text("Dikdörtgen")
                            },

                            onClick = {

                                currentShape =
                                    ShapeType.RECTANGLE

                                currentTool =
                                    ToolType.SHAPE

                                expandedTool =
                                    null
                            },

                            leadingIcon = {

                                Icon(
                                    Icons.Default.Rectangle,
                                    null
                                )
                            }
                        )


                        DropdownMenuItem(

                            text = {
                                Text("Daire")
                            },

                            onClick = {

                                currentShape =
                                    ShapeType.CIRCLE

                                currentTool =
                                    ToolType.SHAPE

                                expandedTool =
                                    null
                            },

                            leadingIcon = {

                                Icon(
                                    Icons.Default.Circle,
                                    null
                                )
                            }
                        )


                        DropdownMenuItem(

                            text = {
                                Text("Üçgen")
                            },

                            onClick = {

                                currentShape =
                                    ShapeType.TRIANGLE

                                currentTool =
                                    ToolType.SHAPE

                                expandedTool =
                                    null
                            },

                            leadingIcon = {

                                Icon(
                                    Icons.Default.ChangeHistory,
                                    null
                                )
                            }
                        )


                        DropdownMenuItem(

                            text = {
                                Text("Elips")
                            },

                            onClick = {

                                currentShape =
                                    ShapeType.ELLIPSE

                                currentTool =
                                    ToolType.SHAPE

                                expandedTool =
                                    null
                            },

                            leadingIcon = {

                                Icon(
                                    Icons.Default.FilterTiltShift,
                                    null
                                )
                            }
                        )


                        DropdownMenuItem(

                            text = {
                                Text("Yay")
                            },

                            onClick = {

                                currentShape =
                                    ShapeType.ARC

                                currentTool =
                                    ToolType.SHAPE

                                expandedTool =
                                    null
                            },

                            leadingIcon = {

                                Icon(
                                    Icons.Default.Architecture,
                                    null
                                )
                            }
                        )
                    }
                }


                // =================================================
                // COLOR PALETTE
                // =================================================

                Box {

                    IconButton(

                        onClick = {

                            colorMenuExpanded =
                                !colorMenuExpanded

                            expandedTool =
                                null
                        }
                    ) {

                        /*
                         * DAİRE YERİNE PALET İKONU
                         */
                        Icon(

                            imageVector =
                                Icons.Default.Palette,

                            contentDescription =
                                "Renk paleti",

                            tint =
                                currentColor
                        )
                    }


                    ColorPalettePopup(

                        expanded =
                            colorMenuExpanded,

                        currentColor =
                            currentColor,

                        onColorSelected = {

                            currentColor =
                                it

                            colorMenuExpanded =
                                false
                        },

                        onDismiss = {

                            colorMenuExpanded =
                                false
                        }
                    )
                }


                // =================================================
                // IMAGE
                // =================================================

                IconButton(

                    onClick = {

                        expandedTool =
                            null

                        colorMenuExpanded =
                            false

                        imagePicker.launch(
                            "image/*"
                        )
                    }
                ) {

                    Icon(

                        Icons.Default.Image,

                        contentDescription =
                            "Resim ekle"
                    )
                }


                // =================================================
                // ERASER
                // =================================================

                ToolButton(

                    tool =
                        ToolType.ERASER,

                    icon =
                        Icons.Default.AutoFixNormal,

                    selected =
                        currentTool ==
                            ToolType.ERASER,

                    expanded =
                        expandedTool ==
                            ToolType.ERASER,

                    onClick = {

                        selectTool(
                            ToolType.ERASER
                        )
                    },

                    settings =
                        toolSettings[
                            ToolType.ERASER
                        ] ?: ToolSettings(
                            30f,
                            1f
                        ),

                    onSettingsChanged = {

                        updateToolSettings(
                            ToolType.ERASER,
                            it
                        )
                    },

                    onDismissRequest = {

                        expandedTool =
                            null
                    },

                    showOpacity =
                        false
                )


                // =================================================
                // CLEAR
                // =================================================

                IconButton(

                    onClick = {

                        clearCanvas()
                    }
                ) {

                    Icon(

                        Icons.Default.DeleteSweep,

                        contentDescription =
                            "Sayfayı temizle"
                    )
                }
            }
        }


        // =========================================================
        // DRAWING AREA
        // =========================================================

        Box(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White)
                    .padding(4.dp)
                    .onSizeChanged {

                        canvasSize =
                            it
                    }
        ) {


            // =====================================================
            // IMAGES
            // =====================================================

            document.images.forEach { image ->

                val density =
                    androidx.compose.ui.platform
                        .LocalDensity.current


                AsyncImage(

                    model =
                        image.uri,

                    contentDescription =
                        "Sketch resmi",

                    modifier =
                        Modifier
                            .offset {

                                IntOffset(

                                    image.x.toInt(),

                                    image.y.toInt()
                                )
                            }
                            .size(

                                with(density) {
                                    image.width.toDp()
                                },

                                with(density) {
                                    image.height.toDp()
                                }
                            ),

                    contentScale =
                        ContentScale.Fit
                )
            }


            // =====================================================
            // CANVAS
            // =====================================================

            Canvas(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .pointerInput(

                            currentTool,
                            currentShape,
                            currentColor,
                            toolSettings,
                            isFillEnabled,
                            currentFillColor
                        ) {

                            detectDragGestures(

                                // =================================
                                // START
                                // =================================

                                onDragStart = { offset ->

                                    currentPathPoints.clear()

                                    currentPathPoints.add(

                                        Point(

                                            x =
                                                offset.x,

                                            y =
                                                offset.y,

                                            pressure =
                                                1f
                                        )
                                    )
                                },


                                // =================================
                                // DRAG
                                // =================================

                                onDrag = {
                                    change,
                                    _ ->

                                    val pressure =
                                        if (
                                            change.type ==
                                            PointerType.Stylus
                                        ) {

                                            change.pressure
                                                .coerceIn(
                                                    0.05f,
                                                    1f
                                                )

                                        } else {

                                            /*
                                             * Parmak için sabit
                                             * maksimum basınç.
                                             */
                                            1f
                                        }


                                    if (
                                        currentTool ==
                                        ToolType.SHAPE
                                    ) {

                                        if (
                                            currentPathPoints.size >
                                            1
                                        ) {

                                            currentPathPoints
                                                .removeAt(
                                                    1
                                                )
                                        }


                                        currentPathPoints.add(

                                            Point(

                                                x =
                                                    change.position.x,

                                                y =
                                                    change.position.y,

                                                pressure =
                                                    pressure
                                            )
                                        )

                                    } else {

                                        currentPathPoints.add(

                                            Point(

                                                x =
                                                    change.position.x,

                                                y =
                                                    change.position.y,

                                                pressure =
                                                    pressure
                                            )
                                        )
                                    }


                                    change.consume()
                                },


                                // =================================
                                // END
                                // =================================

                                onDragEnd = {

                                    if (
                                        currentPathPoints.isEmpty()
                                    ) {

                                        return@detectDragGestures
                                    }


                                    val settings =
                                        toolSettings[
                                            currentTool
                                        ] ?: ToolSettings(
                                            4f,
                                            1f
                                        )


                                    val newPath =
                                        DrawPath(

                                            points =
                                                currentPathPoints
                                                    .toList(),

                                            colorHex =
                                                currentColor
                                                    .toHex(),

                                            strokeWidth =
                                                settings.size,

                                            toolType =
                                                currentTool,

                                            shapeType =
                                                if (
                                                    currentTool ==
                                                    ToolType.SHAPE
                                                ) {

                                                    currentShape

                                                } else {

                                                    null
                                                },

                                            isFilled =
                                                isFillEnabled,

                                            fillColorHex =
                                                if (
                                                    isFillEnabled
                                                ) {

                                                    currentFillColor
                                                        .toHex()

                                                } else {

                                                    null
                                                },

                                            opacity =
                                                settings.opacity
                                        )


                                    undoStack =
                                        undoStack + document

                                    redoStack =
                                        emptyList()


                                    document =
                                        document.copy(

                                            paths =
                                                document.paths +
                                                    newPath
                                        )


                                    currentPathPoints.clear()

                                    saveCurrentState()
                                },


                                onDragCancel = {

                                    currentPathPoints.clear()
                                }
                            )
                        }
            ) {

                // =================================================
                // SAVED
                // =================================================

                document.paths.forEach {

                    drawDataPath(it)
                }


                // =================================================
                // PREVIEW
                // =================================================

                if (
                    currentPathPoints.isNotEmpty()
                ) {

                    val settings =
                        toolSettings[
                            currentTool
                        ] ?: ToolSettings(
                            4f,
                            1f
                        )


                    val preview =
                        DrawPath(

                            points =
                                currentPathPoints.toList(),

                            colorHex =
                                currentColor.toHex(),

                            strokeWidth =
                                settings.size,

                            toolType =
                                currentTool,

                            shapeType =
                                if (
                                    currentTool ==
                                    ToolType.SHAPE
                                ) {

                                    currentShape

                                } else {

                                    null
                                },

                            isFilled =
                                isFillEnabled,

                            fillColorHex =
                                if (
                                    isFillEnabled
                                ) {

                                    currentFillColor.toHex()

                                } else {

                                    null
                                },

                            opacity =
                                settings.opacity
                        )


                    drawDataPath(
                        preview
                    )
                }
            }
        }
    }
}


// =============================================================
// TOOL BUTTON
// =============================================================

@Composable
private fun ToolButton(

    tool: ToolType,

    icon:
        androidx.compose.ui.graphics.vector.ImageVector,

    selected: Boolean,

    expanded: Boolean,

    onClick: () -> Unit,

    settings: ToolSettings,

    onSettingsChanged:
        (ToolSettings) -> Unit,

    onDismissRequest:
        () -> Unit,

    showOpacity: Boolean = true
) {

    Box {

        IconButton(
            onClick = onClick
        ) {

            Icon(

                imageVector =
                    icon,

                contentDescription =
                    toolDisplayName(tool),

                tint =
                    if (selected) {

                        MaterialTheme
                            .colorScheme
                            .primary

                    } else {

                        LocalContentColor.current
                    }
            )
        }


        DropdownMenu(

            expanded =
                expanded,

            onDismissRequest = {

                onDismissRequest()
            },

            modifier =
                Modifier.width(
                    300.dp
                )
        ) {

            Column(

                modifier =
                    Modifier.padding(
                        12.dp
                    )
            ) {

                Text(

                    text =
                        toolDisplayName(tool),

                    style =
                        MaterialTheme
                            .typography
                            .titleSmall
                )


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                // =================================================
                // SIZE
                // =================================================

                Text(

                    text =
                        "Boyut: ${settings.size.toInt()} px",

                    style =
                        MaterialTheme
                            .typography
                            .labelMedium
                )


                Slider(

                    value =
                        settings.size,

                    onValueChange = {

                        onSettingsChanged(
                            settings.copy(
                                size = it
                            )
                        )
                    },

                    valueRange =
                        toolSizeRange(tool)
                )


                // =================================================
                // OPACITY
                // =================================================

                if (showOpacity) {

                    Text(

                        text =
                            "Opaklık: ${
                                (
                                    settings.opacity *
                                    100
                                ).toInt()
                            }%",

                        style =
                            MaterialTheme
                                .typography
                                .labelMedium
                    )


                    Slider(

                        value =
                            settings.opacity,

                        onValueChange = {

                            onSettingsChanged(
                                settings.copy(
                                    opacity = it
                                )
                            )
                        },

                        valueRange =
                            0.05f..1f
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                // =================================================
                // PRESSURE INFO
                // =================================================

                if (
                    tool != ToolType.ERASER
                ) {

                    Text(

                        text =
                            "Stylus basıncı: Aktif",

                        style =
                            MaterialTheme
                                .typography
                                .labelSmall
                    )


                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )
                }


                // =================================================
                // QUICK SIZE
                // =================================================

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceEvenly
                ) {

                    listOf(
                        2f,
                        5f,
                        10f,
                        20f,
                        40f
                    ).forEach { size ->

                        OutlinedButton(

                            onClick = {

                                val allowed =
                                    toolSizeRange(tool)

                                val newSize =
                                    size.coerceIn(
                                        allowed.start,
                                        allowed.endInclusive
                                    )

                                onSettingsChanged(

                                    settings.copy(
                                        size =
                                            newSize
                                    )
                                )

                                /*
                                 * KRİTİK:
                                 * Hızlı boyut seçildikten
                                 * sonra menüyü kapat.
                                 */
                                onDismissRequest()
                            },

                            contentPadding =
                                PaddingValues(
                                    horizontal = 10.dp,
                                    vertical = 0.dp
                                )
                        ) {

                            Text(
                                size.toInt()
                                    .toString()
                            )
                        }
                    }
                }
            }
        }
    }
}


// =============================================================
// COLOR PALETTE
// =============================================================

@Composable
private fun ColorPalettePopup(

    expanded: Boolean,

    currentColor: Color,

    onColorSelected:
        (Color) -> Unit,

    onDismiss: () -> Unit
) {

    DropdownMenu(

        expanded =
            expanded,

        onDismissRequest =
            onDismiss,

        modifier =
            Modifier.width(
                310.dp
            )
    ) {

        Column(

            modifier =
                Modifier.padding(
                    12.dp
                )
        ) {

            Text(

                text =
                    "Renk",

                style =
                    MaterialTheme
                        .typography
                        .titleSmall
            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            val hues =
                listOf(
                    0f,
                    30f,
                    60f,
                    90f,
                    120f,
                    150f,
                    180f,
                    210f,
                    240f,
                    270f,
                    300f,
                    330f
                )


            val values =
                listOf(
                    1f,
                    0.85f,
                    0.70f,
                    0.55f,
                    0.40f,
                    0.25f
                )


            Column(

                verticalArrangement =
                    Arrangement.spacedBy(
                        4.dp
                    )
            ) {

                values.forEach { value ->

                    Row(

                        horizontalArrangement =
                            Arrangement.spacedBy(
                                4.dp
                            )
                    ) {

                        hues.forEach { hue ->

                            val color =
                                Color.hsv(

                                    hue =
                                        hue,

                                    saturation =
                                        1f,

                                    value =
                                        value
                                )


                            Box(

                                modifier =
                                    Modifier
                                        .size(20.dp)
                                        .background(color)
                                        .border(

                                            if (
                                                currentColor ==
                                                color
                                            ) {
                                                2.dp
                                            } else {
                                                0.dp
                                            },

                                            Color.Black
                                        )
                                        .clickable {

                                            onColorSelected(
                                                color
                                            )
                                        }
                            )
                        }
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            HorizontalDivider()


            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )


            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceEvenly
            ) {

                val quickColors =
                    listOf(

                        Color.Blue,

                        Color.Red,

                        Color(0xFFFF9800),

                        Color.Yellow,

                        Color.Green,

                        Color.Black
                    )


                quickColors.forEach { color ->

                    Box(

                        modifier =
                            Modifier
                                .size(28.dp)
                                .background(
                                    color,
                                    CircleShape
                                )
                                .border(
                                    if (
                                        currentColor ==
                                        color
                                    ) {
                                        2.dp
                                    } else {
                                        0.dp
                                    },
                                    Color.Gray,
                                    CircleShape
                                )
                                .clickable {

                                    onColorSelected(
                                        color
                                    )
                                }
                    )
                }
            }
        }
    }
}


// =============================================================
// PATH RENDERER
// =============================================================

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawDataPath(
        drawPath: DrawPath
    ) {

    if (
        drawPath.points.isEmpty()
    ) {
        return
    }


    // =========================================================
    // ERASER
    // =========================================================

    if (
        drawPath.toolType ==
        ToolType.ERASER
    ) {

        val eraserPath =
            Path()


        eraserPath.moveTo(

            drawPath.points.first().x,

            drawPath.points.first().y
        )


        drawPath.points
            .drop(1)
            .forEach {

                eraserPath.lineTo(
                    it.x,
                    it.y
                )
            }


        drawPath(

            path =
                eraserPath,

            color =
                Color.White,

            style =
                Stroke(

                    width =
                        drawPath.strokeWidth,

                    cap =
                        StrokeCap.Round
                )
        )

        return
    }


    // =========================================================
    // BASE COLOR
    // =========================================================

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


    // =========================================================
    // OPACITY
    // =========================================================

    val alpha =
        drawPath.opacity.coerceIn(
            0.05f,
            1f
        )


    // =========================================================
    // FINAL COLOR
    // =========================================================

    val finalColor =
        when (drawPath.toolType) {

            ToolType.PENCIL ->
                baseColor.copy(
                    alpha =
                        alpha * 0.80f
                )

            ToolType.MARKER ->
                baseColor.copy(
                    alpha =
                        alpha * 0.45f
                )

            ToolType.BRUSH ->
                baseColor.copy(
                    alpha =
                        alpha
                )

            else ->
                baseColor.copy(
                    alpha =
                        alpha
                )
        }


    // =========================================================
    // FILL
    // =========================================================

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


    // =========================================================
    // SHAPES
    // =========================================================

    if (

        drawPath.toolType ==
        ToolType.SHAPE &&

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


        val left =
            min(
                start.x,
                end.x
            )


        val top =
            min(
                start.y,
                end.y
            )


        val width =
            abs(
                start.x -
                    end.x
            )


        val height =
            abs(
                start.y -
                    end.y
            )


        val shapeWidth =
            max(
                width,
                1f
            )


        val shapeHeight =
            max(
                height,
                1f
            )


        val pressure =
            drawPath.points
                .lastOrNull()
                ?.pressure
                ?.coerceIn(
                    0.05f,
                    1f
                )
                ?: 1f


        val shapeStrokeWidth =
            drawPath.strokeWidth *
                pressureWidthFactor(
                    drawPath.toolType,
                    pressure
                )


        when (
            drawPath.shapeType
        ) {

            // =====================================================
            // RECTANGLE
            // =====================================================

            ShapeType.RECTANGLE -> {

                if (
                    drawPath.isFilled
                ) {

                    drawRect(

                        color =
                            fillColor,

                        topLeft =
                            Offset(
                                left,
                                top
                            ),

                        size =
                            Size(
                                shapeWidth,
                                shapeHeight
                            )
                    )
                }


                drawRect(

                    color =
                        finalColor,

                    topLeft =
                        Offset(
                            left,
                            top
                        ),

                    size =
                        Size(
                            shapeWidth,
                            shapeHeight
                        ),

                    style =
                        Stroke(
                            width =
                                shapeStrokeWidth
                        )
                )
            }


            // =====================================================
            // CIRCLE
            // =====================================================

            ShapeType.CIRCLE -> {

                val radius =
                    min(
                        width,
                        height
                    ) / 2f


                val center =
                    Offset(

                        left +
                            width / 2f,

                        top +
                            height / 2f
                    )


                if (
                    drawPath.isFilled
                ) {

                    drawCircle(

                        color =
                            fillColor,

                        radius =
                            radius,

                        center =
                            center
                    )
                }


                drawCircle(

                    color =
                        finalColor,

                    radius =
                        radius,

                    center =
                        center,

                    style =
                        Stroke(
                            width =
                                shapeStrokeWidth
                        )
                )
            }


            // =====================================================
            // TRIANGLE
            // =====================================================

            ShapeType.TRIANGLE -> {

                val path =
                    Path().apply {

                        moveTo(

                            left +
                                width / 2f,

                            top
                        )

                        lineTo(

                            left,

                            top +
                                height
                        )

                        lineTo(

                            left +
                                width,

                            top +
                                height
                        )

                        close()
                    }


                if (
                    drawPath.isFilled
                ) {

                    drawPath(
                        path,
                        fillColor
                    )
                }


                drawPath(

                    path,

                    finalColor,

                    style =
                        Stroke(
                            width =
                                shapeStrokeWidth
                        )
                )
            }


            // =====================================================
            // ELLIPSE
            // =====================================================

            ShapeType.ELLIPSE -> {

                if (
                    drawPath.isFilled
                ) {

                    drawOval(

                        color =
                            fillColor,

                        topLeft =
                            Offset(
                                left,
                                top
                            ),

                        size =
                            Size(
                                shapeWidth,
                                shapeHeight
                            )
                    )
                }


                drawOval(

                    color =
                        finalColor,

                    topLeft =
                        Offset(
                            left,
                            top
                        ),

                    size =
                        Size(
                            shapeWidth,
                            shapeHeight
                        ),

                    style =
                        Stroke(
                            width =
                                shapeStrokeWidth
                        )
                )
            }


            // =====================================================
            // ARC
            // =====================================================

            ShapeType.ARC -> {

                drawArc(

                    color =
                        finalColor,

                    startAngle =
                        0f,

                    sweepAngle =
                        180f,

                    useCenter =
                        false,

                    topLeft =
                        Offset(
                            left,
                            top
                        ),

                    size =
                        Size(
                            shapeWidth,
                            shapeHeight
                        ),

                    style =
                        Stroke(
                            width =
                                shapeStrokeWidth
                        )
                )
            }


            null -> Unit
        }


        return
    }


    // =========================================================
    // NORMAL STROKE
    // =========================================================

    if (
        drawPath.points.size == 1
    ) {

        val point =
            drawPath.points.first()


        val pressure =
            point.pressure.coerceIn(
                0.05f,
                1f
            )


        val width =
            drawPath.strokeWidth *
                pressureWidthFactor(
                    drawPath.toolType,
                    pressure
                )


        drawCircle(

            color =
                finalColor,

            radius =
                width / 2f,

            center =
                Offset(
                    point.x,
                    point.y
                )
        )


        return
    }


    // =========================================================
    // PRESSURE-SENSITIVE STROKE
    // =========================================================

    /*
     * Her iki nokta arasındaki segment ayrı çiziliyor.
     *
     * Böylece Stylus basıncı değiştikçe
     * çizgi kalınlığı da yumuşak şekilde değişiyor.
     */

    for (
        i in 1 until drawPath.points.size
    ) {

        val previous =
            drawPath.points[i - 1]

        val current =
            drawPath.points[i]


        val p1 =
            previous.pressure.coerceIn(
                0.05f,
                1f
            )


        val p2 =
            current.pressure.coerceIn(
                0.05f,
                1f
            )


        val averagePressure =
            (
                p1 +
                    p2
            ) / 2f


        val pressureFactor =
            pressureWidthFactor(

                drawPath.toolType,

                averagePressure
            )


        val width =
            drawPath.strokeWidth *
                pressureFactor


        val segmentPath =
            Path()


        segmentPath.moveTo(

            previous.x,

            previous.y
        )


        segmentPath.lineTo(

            current.x,

            current.y
        )


        drawPath(

            path =
                segmentPath,

            color =
                finalColor,

            style =
                Stroke(

                    width =
                        width,

                    cap =
                        StrokeCap.Round
                )
        )
    }


    // =========================================================
    // TEXTURE
    // =========================================================

    /*
     * Pencil:
     * Hafif kuru / kırık görünüm.
     *
     * Brush:
     * Kenarların daha yumuşak hissedilmesi.
     *
     * Marker:
     * Daha düz ve yarı saydam.
     */

    when (
        drawPath.toolType
    ) {

        ToolType.PENCIL -> {

            drawPencilTexture(
                drawPath,
                finalColor
            )
        }


        ToolType.BRUSH -> {

            drawBrushTexture(
                drawPath,
                finalColor
            )
        }


        ToolType.MARKER -> {

            drawMarkerTexture(
                drawPath,
                finalColor
            )
        }


        else -> Unit
    }
}


// =============================================================
// PRESSURE WIDTH
// =============================================================

private fun pressureWidthFactor(
    tool: ToolType,
    pressure: Float
): Float {

    val p =
        pressure.coerceIn(
            0.05f,
            1f
        )


    return when (tool) {

        ToolType.PENCIL -> {

            /*
             * Pencil basınca oldukça duyarlı.
             */
            0.35f +
                p * 1.15f
        }


        ToolType.PEN -> {

            /*
             * Gerçek tükenmez kalem gibi
             * daha kontrollü değişim.
             */
            0.60f +
                p * 0.65f
        }


        ToolType.INK -> {

            /*
             * Mürekkep kaleminde basınç
             * daha belirgin.
             */
            0.45f +
                p * 0.95f
        }


        ToolType.BRUSH -> {

            /*
             * Fırça basınçtan en fazla etkilenen araç.
             */
            0.25f +
                p * 1.50f
        }


        ToolType.MARKER -> {

            /*
             * Marker daha stabil.
             */
            0.75f +
                p * 0.40f
        }


        else -> {

            1f
        }
    }
}


// =============================================================
// PENCIL TEXTURE
// =============================================================

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawPencilTexture(
        drawPath: DrawPath,
        color: Color
    ) {

    if (
        drawPath.points.size < 2
    ) {
        return
    }


    /*
     * Çok hafif ikinci katman.
     *
     * Gerçek raster pencil texture yerine
     * Compose Canvas üzerinde performanslı
     * bir yaklaşım kullanıyoruz.
     */

    val textureColor =
        color.copy(
            alpha =
                color.alpha * 0.16f
        )


    for (
        i in 1 until drawPath.points.size step 2
    ) {

        val p =
            drawPath.points[i]


        val previous =
            drawPath.points[
                i - 1
            ]


        val path =
            Path()


        path.moveTo(
            previous.x,
            previous.y
        )


        path.lineTo(
            p.x,
            p.y
        )


        drawPath(

            path =
                path,

            color =
                textureColor,

            style =
                Stroke(

                    width =
                        drawPath.strokeWidth *
                            0.45f,

                    cap =
                        StrokeCap.Round
                )
        )
    }
}


// =============================================================
// BRUSH TEXTURE
// =============================================================

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawBrushTexture(
        drawPath: DrawPath,
        color: Color
    ) {

    if (
        drawPath.points.size < 2
    ) {
        return
    }


    val textureColor =
        color.copy(
            alpha =
                color.alpha * 0.10f
        )


    /*
     * Fırça hissini güçlendirmek için
     * ana stroke'un iki yanında çok hafif
     * yardımcı stroke'lar çiziyoruz.
     */

    val offset =
        drawPath.strokeWidth *
            0.22f


    for (
        i in 1 until drawPath.points.size
    ) {

        val a =
            drawPath.points[i - 1]

        val b =
            drawPath.points[i]


        val dx =
            b.x - a.x

        val dy =
            b.y - a.y


        val length =
            max(
                kotlin.math.sqrt(
                    dx * dx +
                        dy * dy
                ),
                0.001f
            )


        val nx =
            -dy / length

        val ny =
            dx / length


        val path1 =
            Path()


        path1.moveTo(
            a.x + nx * offset,
            a.y + ny * offset
        )


        path1.lineTo(
            b.x + nx * offset,
            b.y + ny * offset
        )


        drawPath(

            path =
                path1,

            color =
                textureColor,

            style =
                Stroke(

                    width =
                        drawPath.strokeWidth *
                            0.35f,

                    cap =
                        StrokeCap.Round
                )
        )
    }
}


// =============================================================
// MARKER TEXTURE
// =============================================================

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawMarkerTexture(
        drawPath: DrawPath,
        color: Color
    ) {

    if (
        drawPath.points.size < 2
    ) {
        return
    }


    /*
     * Marker için hafif bir ikinci geçiş.
     * Marker'ın dijital çizgi yerine
     * daha yumuşak görünmesini sağlar.
     */

    val markerColor =
        color.copy(
            alpha =
                color.alpha * 0.12f
        )


    val path =
        Path()


    path.moveTo(

        drawPath.points
            .first()
            .x,

        drawPath.points
            .first()
            .y
    )


    drawPath.points
        .drop(1)
        .forEach {

            path.lineTo(
                it.x,
                it.y
            )
        }


    drawPath(

        path =
            path,

        color =
            markerColor,

        style =
            Stroke(

                width =
                    drawPath.strokeWidth *
                        1.12f,

                cap =
                    StrokeCap.Round
            )
    )
}