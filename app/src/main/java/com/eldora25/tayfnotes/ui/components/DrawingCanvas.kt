@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.eldora25.tayfnotes.ui.components

import android.graphics.Color as AndroidColor
import android.view.MotionEvent

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

import coil.compose.AsyncImage

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

import kotlin.random.Random


/**
 * =============================================================
 * TAYFNOTES DRAWING TOOLS
 * =============================================================
 */

enum class ToolType {
    PEN,
    PENCIL,
    INK,
    BRUSH,
    MARKER,
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


/**
 * =============================================================
 * POINT
 * =============================================================
 */

@Serializable
data class Point(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
    val tilt: Float = 0f,
    val orientation: Float = 0f
)


/**
 * =============================================================
 * DRAW PATH
 * =============================================================
 */

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


/**
 * =============================================================
 * SKETCH IMAGE
 * =============================================================
 */

@Serializable
data class SketchImage(
    val id: String,
    val uri: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)


/**
 * =============================================================
 * SKETCH DOCUMENT
 * =============================================================
 */

@Serializable
data class SketchDocument(
    val paths: List<DrawPath> = emptyList(),
    val images: List<SketchImage> = emptyList()
)


/**
 * =============================================================
 * TOOL SETTINGS
 * =============================================================
 */

data class ToolSettings(
    val size: Float,
    val opacity: Float
)


/**
 * =============================================================
 * DEFAULT TOOL SETTINGS
 * =============================================================
 */

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


/**
 * =============================================================
 * TOOL DISPLAY NAME
 * =============================================================
 */

private fun toolDisplayName(tool: ToolType): String {

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


/**
 * =============================================================
 * TOOL SIZE RANGE
 * =============================================================
 */

private fun toolSizeRange(
    tool: ToolType
): ClosedFloatingPointRange<Float> {

    return when (tool) {

        ToolType.ERASER ->
            5f..100f

        ToolType.MARKER,
        ToolType.BRUSH ->
            2f..60f

        else ->
            1f..40f
    }
}


/**
 * =============================================================
 * COLOR -> HEX
 *
 * IMPORTANT:
 * No Color.toArgb() dependency.
 * =============================================================
 */

private fun Color.toHex(): String {

    val red =
        (red.coerceIn(0f, 1f) * 255f).toInt()

    val green =
        (green.coerceIn(0f, 1f) * 255f).toInt()

    val blue =
        (blue.coerceIn(0f, 1f) * 255f).toInt()

    return String.format(
        "#%06X",
        AndroidColor.rgb(
            red,
            green,
            blue
        ) and 0xFFFFFF
    )
}


/**
 * =============================================================
 * HEX -> COLOR
 * =============================================================
 */

private fun parseColor(hex: String): Color {

    return try {

        Color(
            AndroidColor.parseColor(hex)
        )

    } catch (_: Exception) {

        Color.Black
    }
}


/**
 * =============================================================
 * DRAWING CANVAS
 * =============================================================
 */

@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    initialData: String? = null,
    onDataChanged: (String) -> Unit
) {

    fun decodeInitialData(data: String?): SketchDocument {

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


    var currentTool by remember {

        mutableStateOf(
            ToolType.PEN
        )
    }


    var currentShape by remember {

        mutableStateOf(
            ShapeType.RECTANGLE
        )
    }


    var toolSettings by remember {

        mutableStateOf(
            defaultToolSettings()
        )
    }


    var currentColor by remember {

        mutableStateOf(
            Color.Black
        )
    }


    var isFillEnabled by remember {

        mutableStateOf(false)
    }


    var currentFillColor by remember {

        mutableStateOf(
            Color.Transparent
        )
    }


    var expandedTool by remember {

        mutableStateOf<ToolType?>(null)
    }


    var colorMenuExpanded by remember {

        mutableStateOf(false)
    }


    val currentPathPoints =
        remember {

            mutableStateListOf<Point>()
        }


    var activePointerId by remember {

        mutableStateOf(-1)
    }


    var canvasSize by remember {

        mutableStateOf(
            IntSize.Zero
        )
    }


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

            undoStack =
                undoStack + document

            redoStack =
                emptyList()

            document =
                document.copy(
                    images =
                        document.images +
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
                )

            onDataChanged(
                Json.encodeToString(document)
            )
        }


    fun saveCurrentState() {

        onDataChanged(
            Json.encodeToString(document)
        )
    }


    fun updateToolSettings(
        tool: ToolType,
        settings: ToolSettings
    ) {

        toolSettings =
            toolSettings
                .toMutableMap()
                .apply {

                    this[tool] =
                        settings
                }
    }


    fun selectTool(tool: ToolType) {

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


    fun performUndo() {

        if (undoStack.isEmpty()) {
            return
        }

        redoStack =
            redoStack + document

        document =
            undoStack.last()

        undoStack =
            undoStack.dropLast(1)

        currentPathPoints.clear()

        expandedTool =
            null

        saveCurrentState()
    }


    fun performRedo() {

        if (redoStack.isEmpty()) {
            return
        }

        undoStack =
            undoStack + document

        document =
            redoStack.last()

        redoStack =
            redoStack.dropLast(1)

        currentPathPoints.clear()

        expandedTool =
            null

        saveCurrentState()
    }


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

        saveCurrentState()
    }


    fun finishStroke() {

        if (currentPathPoints.isEmpty()) {
            return
        }

        val settings =
            toolSettings[currentTool]
                ?: ToolSettings(
                    4f,
                    1f
                )

        val newPath =
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
                    if (isFillEnabled) {
                        currentFillColor.toHex()
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
    }


    Column(

        modifier =
            modifier
                .fillMaxSize()
                .clipToBounds()
                .background(Color.White)

    ) {

        Surface(

            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(16.dp),

            color =
                MaterialTheme
                    .colorScheme
                    .surfaceVariant
                    .copy(alpha = 0.95f),

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

                IconButton(

                    enabled =
                        undoStack.isNotEmpty(),

                    onClick =
                        ::performUndo

                ) {

                    Icon(
                        Icons.Default.Undo,
                        contentDescription =
                            "Geri Al"
                    )
                }


                IconButton(

                    enabled =
                        redoStack.isNotEmpty(),

                    onClick =
                        ::performRedo

                ) {

                    Icon(
                        Icons.Default.Redo,
                        contentDescription =
                            "İleri Al"
                    )
                }


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
                        ]!!,

                    onSettingsChanged = {
                        updateToolSettings(
                            ToolType.PENCIL,
                            it
                        )
                    },

                    onDismissRequest = {
                        expandedTool = null
                    }
                )


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
                        ]!!,

                    onSettingsChanged = {
                        updateToolSettings(
                            ToolType.PEN,
                            it
                        )
                    },

                    onDismissRequest = {
                        expandedTool = null
                    }
                )


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
                        ]!!,

                    onSettingsChanged = {
                        updateToolSettings(
                            ToolType.INK,
                            it
                        )
                    },

                    onDismissRequest = {
                        expandedTool = null
                    }
                )


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
                        ]!!,

                    onSettingsChanged = {
                        updateToolSettings(
                            ToolType.BRUSH,
                            it
                        )
                    },

                    onDismissRequest = {
                        expandedTool = null
                    }
                )


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
                        ]!!,

                    onSettingsChanged = {
                        updateToolSettings(
                            ToolType.MARKER,
                            it
                        )
                    },

                    onDismissRequest = {
                        expandedTool = null
                    }
                )


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
                                    null
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
                            expandedTool = null
                        }

                    ) {

                        DropdownMenuItem(

                            text = {
                                Text("Dikdörtgen")
                            },

                            leadingIcon = {
                                Icon(
                                    Icons.Default.Rectangle,
                                    null
                                )
                            },

                            onClick = {

                                currentShape =
                                    ShapeType.RECTANGLE

                                currentTool =
                                    ToolType.SHAPE

                                expandedTool =
                                    null
                            }
                        )


                        DropdownMenuItem(

                            text = {
                                Text("Daire")
                            },

                            leadingIcon = {
                                Icon(
                                    Icons.Default.Circle,
                                    null
                                )
                            },

                            onClick = {

                                currentShape =
                                    ShapeType.CIRCLE

                                currentTool =
                                    ToolType.SHAPE

                                expandedTool =
                                    null
                            }
                        )


                        DropdownMenuItem(

                            text = {
                                Text("Üçgen")
                            },

                            leadingIcon = {
                                Icon(
                                    Icons.Default.ChangeHistory,
                                    null
                                )
                            },

                            onClick = {

                                currentShape =
                                    ShapeType.TRIANGLE

                                currentTool =
                                    ToolType.SHAPE

                                expandedTool =
                                    null
                            }
                        )


                        DropdownMenuItem(

                            text = {
                                Text("Elips")
                            },

                            leadingIcon = {
                                Icon(
                                    Icons.Default.FilterTiltShift,
                                    null
                                )
                            },

                            onClick = {

                                currentShape =
                                    ShapeType.ELLIPSE

                                currentTool =
                                    ToolType.SHAPE

                                expandedTool =
                                    null
                            }
                        )


                        DropdownMenuItem(

                            text = {
                                Text("Yay")
                            },

                            leadingIcon = {
                                Icon(
                                    Icons.Default.Architecture,
                                    null
                                )
                            },

                            onClick = {

                                currentShape =
                                    ShapeType.ARC

                                currentTool =
                                    ToolType.SHAPE

                                expandedTool =
                                    null
                            }
                        )
                    }
                }


                Box {

                    IconButton(

                        onClick = {

                            colorMenuExpanded =
                                !colorMenuExpanded

                            expandedTool =
                                null
                        }

                    ) {

                        Icon(

                            Icons.Default.Palette,

                            contentDescription =
                                "Renk paleti",

                            tint =
                                if (
                                    colorMenuExpanded
                                ) {

                                    MaterialTheme
                                        .colorScheme
                                        .primary

                                } else {

                                    LocalContentColor.current
                                }
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
                        ]!!,

                    onSettingsChanged = {
                        updateToolSettings(
                            ToolType.ERASER,
                            it
                        )
                    },

                    onDismissRequest = {
                        expandedTool = null
                    },

                    showOpacity =
                        false
                )


                IconButton(

                    onClick =
                        ::clearCanvas

                ) {

                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription =
                            "Temizle"
                    )
                }
            }
        }


        Box(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .background(Color.White)
                    .padding(4.dp)
                    .onSizeChanged {
                        canvasSize = it
                    }

        ) {

            val density =
                androidx.compose.ui.platform
                    .LocalDensity.current


            document.images.forEach { image ->

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


            Canvas(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .clipToBounds()

                        /*
                         * IMPORTANT:
                         *
                         * pointerInteropFilter is an
                         * ExperimentalComposeUiApi.
                         *
                         * The file-level OptIn at the top
                         * of this file authorizes its use.
                         */

                        .pointerInteropFilter { event ->

                            val action =
                                event.actionMasked


                            fun isAcceptedTool(
                                index: Int
                            ): Boolean {

                                val type =
                                    event.getToolType(
                                        index
                                    )

                                return type ==
                                    MotionEvent.TOOL_TYPE_STYLUS ||
                                    type ==
                                    MotionEvent.TOOL_TYPE_ERASER ||
                                    type ==
                                    MotionEvent.TOOL_TYPE_FINGER
                            }


                            fun addPoint(

                                index: Int,

                                x: Float,

                                y: Float,

                                pressure: Float,

                                tilt: Float,

                                orientation: Float

                            ) {

                                val safePressure =
                                    pressure.coerceIn(
                                        0.05f,
                                        1f
                                    )

                                val safeTilt =
                                    tilt.coerceIn(
                                        0f,
                                        (PI / 2f).toFloat()
                                    )

                                currentPathPoints.add(

                                    Point(

                                        x = x,

                                        y = y,

                                        pressure =
                                            safePressure,

                                        tilt =
                                            safeTilt,

                                        orientation =
                                            orientation
                                    )
                                )
                            }


                            fun processHistorical(
                                index: Int
                            ) {

                                for (
                                    historyIndex
                                    in 0 until event.historySize
                                ) {

                                    val x =
                                        event.getHistoricalX(
                                            index,
                                            historyIndex
                                        )

                                    val y =
                                        event.getHistoricalY(
                                            index,
                                            historyIndex
                                        )

                                    val pressure =
                                        event.getHistoricalPressure(
                                            index,
                                            historyIndex
                                        )

                                    val tilt =
                                        event.getHistoricalAxisValue(
                                            MotionEvent.AXIS_TILT,
                                            index,
                                            historyIndex
                                        )

                                    val orientation =
                                        event.getHistoricalOrientation(
                                            index,
                                            historyIndex
                                        )

                                    addPoint(

                                        index,

                                        x,

                                        y,

                                        pressure,

                                        tilt,

                                        orientation
                                    )
                                }
                            }


                            when (action) {

                                MotionEvent.ACTION_DOWN -> {

                                    if (
                                        !isAcceptedTool(0)
                                    ) {
                                        return@pointerInteropFilter false
                                    }

                                    activePointerId =
                                        event.getPointerId(0)

                                    currentPathPoints.clear()

                                    addPoint(

                                        index = 0,

                                        x =
                                            event.getX(0),

                                        y =
                                            event.getY(0),

                                        pressure =
                                            event.getPressure(0),

                                        tilt =
                                            event.getAxisValue(
                                                MotionEvent.AXIS_TILT,
                                                0
                                            ),

                                        orientation =
                                            event.getOrientation(0)
                                    )

                                    true
                                }


                                MotionEvent.ACTION_MOVE -> {

                                    val index =
                                        event.findPointerIndex(
                                            activePointerId
                                        )

                                    if (index < 0) {
                                        return@pointerInteropFilter true
                                    }

                                    processHistorical(index)

                                    addPoint(

                                        index,

                                        event.getX(index),

                                        event.getY(index),

                                        event.getPressure(index),

                                        event.getAxisValue(
                                            MotionEvent.AXIS_TILT,
                                            index
                                        ),

                                        event.getOrientation(
                                            index
                                        )
                                    )

                                    true
                                }


                                MotionEvent.ACTION_UP -> {

                                    val index =
                                        event.findPointerIndex(
                                            activePointerId
                                        )

                                    if (index >= 0) {

                                        addPoint(

                                            index,

                                            event.getX(index),

                                            event.getY(index),

                                            event.getPressure(index),

                                            event.getAxisValue(
                                                MotionEvent.AXIS_TILT,
                                                index
                                            ),

                                            event.getOrientation(
                                                index
                                            )
                                        )
                                    }

                                    finishStroke()

                                    activePointerId =
                                        -1

                                    true
                                }


                                MotionEvent.ACTION_CANCEL -> {

                                    currentPathPoints.clear()

                                    activePointerId =
                                        -1

                                    true
                                }


                                else -> true
                            }
                        }

            ) {

                document.paths.forEach {

                    drawDataPath(it)
                }


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

                    drawDataPath(

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
                    )
                }
            }
        }
    }
}


/**
 * =============================================================
 * TOOL BUTTON
 * =============================================================
 */

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

            onDismissRequest =
                onDismissRequest,

            modifier =
                Modifier.width(300.dp)

        ) {

            Column(

                modifier =
                    Modifier.padding(12.dp)

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
                    Modifier.height(8.dp)
                )


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


                if (showOpacity) {

                    Text(

                        text =
                            "Opaklık: ${
                                (
                                    settings.opacity *
                                        100f
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
                    Modifier.height(8.dp)
                )


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

                                val range =
                                    toolSizeRange(tool)

                                val newSize =
                                    size.coerceIn(
                                        range.start,
                                        range.endInclusive
                                    )

                                onSettingsChanged(

                                    settings.copy(
                                        size =
                                            newSize
                                    )
                                )

                                onDismissRequest()
                            },

                            contentPadding =
                                PaddingValues(
                                    horizontal = 10.dp,
                                    vertical = 0.dp
                                )

                        ) {

                            Text(
                                size.toInt().toString()
                            )
                        }
                    }
                }
            }
        }
    }
}


/**
 * =============================================================
 * COLOR PALETTE
 * =============================================================
 */

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
            Modifier.width(310.dp)

    ) {

        Column(

            modifier =
                Modifier.padding(12.dp)

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
                    Arrangement.spacedBy(4.dp)

            ) {

                values.forEach { value ->

                    Row(

                        horizontalArrangement =
                            Arrangement.spacedBy(4.dp)

                    ) {

                        hues.forEach { hue ->

                            val color =
                                Color.hsv(

                                    hue = hue,

                                    saturation = 1f,

                                    value = value
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
                Modifier.height(12.dp)
            )


            HorizontalDivider()


            Spacer(
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


/**
 * =============================================================
 * MAIN PATH RENDERER
 * =============================================================
 */

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawDataPath(
        drawPath: DrawPath
    ) {

    if (drawPath.points.isEmpty()) {
        return
    }


    if (
        drawPath.toolType ==
        ToolType.ERASER
    ) {

        drawVariableStroke(

            points =
                drawPath.points,

            baseWidth =
                drawPath.strokeWidth,

            color =
                Color.White,

            alpha =
                1f,

            widthProvider = { _, _ ->
                drawPath.strokeWidth
            },

            cap =
                StrokeCap.Round
        )

        return
    }


    val baseColor =
        parseColor(drawPath.colorHex)


    val alpha =
        drawPath.opacity.coerceIn(
            0.05f,
            1f
        )


    if (
        drawPath.toolType ==
        ToolType.SHAPE &&
        drawPath.points.size >= 2
    ) {

        drawShape(

            data =
                drawPath,

            strokeColor =
                baseColor.copy(
                    alpha = alpha
                )
        )

        return
    }


    when (drawPath.toolType) {

        ToolType.PENCIL -> {

            drawPencil(
                data = drawPath,
                color = baseColor,
                alpha = alpha
            )
        }


        ToolType.INK -> {

            drawInk(
                data = drawPath,
                color = baseColor,
                alpha = alpha
            )
        }


        ToolType.BRUSH -> {

            drawBrush(
                data = drawPath,
                color = baseColor,
                alpha = alpha
            )
        }


        ToolType.MARKER -> {

            drawMarker(
                data = drawPath,
                color = baseColor,
                alpha = alpha
            )
        }


        ToolType.PEN -> {

            drawPen(
                data = drawPath,
                color = baseColor,
                alpha = alpha
            )
        }


        else -> Unit
    }
}


/**
 * =============================================================
 * PEN
 * =============================================================
 */

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawPen(

        data: DrawPath,

        color: Color,

        alpha: Float

    ) {

    val points =
        data.points


    if (points.isEmpty()) {
        return
    }


    if (points.size == 1) {

        drawCircle(

            color =
                color.copy(alpha = alpha),

            radius =
                data.strokeWidth / 2f,

            center =
                Offset(
                    points[0].x,
                    points[0].y
                )
        )

        return
    }


    drawVariableStroke(

        points =
            points,

        baseWidth =
            data.strokeWidth,

        color =
            color,

        alpha =
            alpha,

        widthProvider = { point, _ ->

            data.strokeWidth *
                (
                    0.72f +
                        0.28f *
                        point.pressure
                )
        },

        cap =
            StrokeCap.Round
    )
}


/**
 * =============================================================
 * INK
 * =============================================================
 */

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawInk(

        data: DrawPath,

        color: Color,

        alpha: Float

    ) {

    val points =
        data.points


    if (points.isEmpty()) {
        return
    }


    drawVariableStroke(

        points =
            points,

        baseWidth =
            data.strokeWidth,

        color =
            color,

        alpha =
            alpha,

        widthProvider = { point, _ ->

            data.strokeWidth *
                pressureWidth(
                    point.pressure
                )
        },

        cap =
            StrokeCap.Round
    )
}


/**
 * =============================================================
 * PRESSURE WIDTH CURVE
 * =============================================================
 */

private fun pressureWidth(
    pressure: Float
): Float {

    val p =
        pressure.coerceIn(
            0f,
            1f
        )


    val smooth =
        p * p *
            (
                3f -
                    2f * p
            )


    return 0.22f +
        1.18f *
        smooth
}


/**
 * =============================================================
 * PENCIL
 * =============================================================
 */

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawPencil(

        data: DrawPath,

        color: Color,

        alpha: Float

    ) {

    val points =
        data.points


    if (points.isEmpty()) {
        return
    }


    drawVariableStroke(

        points =
            points,

        baseWidth =
            data.strokeWidth,

        color =
            color,

        alpha =
            alpha * 0.58f,

        widthProvider = { point, _ ->

            data.strokeWidth *
                (
                    0.48f +
                        0.62f *
                        point.pressure
                )
        },

        cap =
            StrokeCap.Round
    )


    val seed =
        data.points.hashCode().toLong() *
            31L +
            data.strokeWidth.toBits()


    val random =
        Random(seed)


    val grainCount =
        (
            points.size * 2.2f
        )
            .toInt()
            .coerceIn(
                12,
                2200
            )


    repeat(grainCount) {

        val index =
            random.nextInt(
                points.size
            )


        val point =
            points[index]


        val previous =
            points.getOrNull(
                index - 1
            )


        val next =
            points.getOrNull(
                index + 1
            )


        val tangent =

            if (
                previous != null &&
                next != null
            ) {

                atan2(

                    next.y -
                        previous.y,

                    next.x -
                        previous.x
                )

            } else if (
                next != null
            ) {

                atan2(

                    next.y -
                        point.y,

                    next.x -
                        point.x
                )

            } else if (
                previous != null
            ) {

                atan2(

                    point.y -
                        previous.y,

                    point.x -
                        previous.x
                )

            } else {

                0f
            }


        val grainAngle =
            tangent +
                (
                    random.nextFloat() -
                        0.5f
                ) * 0.65f


        val normal =
            grainAngle +
                PI.toFloat() / 2f


        val spread =
            data.strokeWidth *
                (
                    1f +
                        random.nextFloat() *
                        2.2f
                )


        val offset =
            (
                random.nextFloat() -
                    0.5f
            ) * spread


        val along =
            (
                random.nextFloat() -
                    0.5f
            ) *
            data.strokeWidth *
            0.8f


        val length =
            data.strokeWidth *
                (
                    0.35f +
                        random.nextFloat() *
                        1.35f
                )


        val x0 =
            point.x +
                cos(normal) *
                offset +
                cos(grainAngle) *
                along


        val y0 =
            point.y +
                sin(normal) *
                offset +
                sin(grainAngle) *
                along


        val x1 =
            x0 +
                cos(grainAngle) *
                length


        val y1 =
            y0 +
                sin(grainAngle) *
                length


        val grainAlpha =
            alpha *
                (
                    0.035f +
                        random.nextFloat() *
                        0.13f
                ) *
                (
                    0.55f +
                        0.45f *
                        point.pressure
                )


        drawLine(

            color =
                color.copy(
                    alpha =
                        grainAlpha
                ),

            start =
                Offset(
                    x0,
                    y0
                ),

            end =
                Offset(
                    x1,
                    y1
                ),

            strokeWidth =
                max(

                    0.25f,

                    data.strokeWidth *
                        (
                            0.025f +
                                random.nextFloat() *
                                0.055f
                        )
                ),

            cap =
                StrokeCap.Round
        )
    }
}


/**
 * =============================================================
 * BRUSH
 * =============================================================
 */

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawBrush(

        data: DrawPath,

        color: Color,

        alpha: Float

    ) {

    val points =
        data.points


    if (points.isEmpty()) {
        return
    }


    for (index in points.indices) {

        val point =
            points[index]


        val previous =
            points.getOrNull(
                index - 1
            )


        val pressure =
            point.pressure.coerceIn(
                0.05f,
                1f
            )


        val width =
            data.strokeWidth *
                (
                    0.42f +
                        1.25f *
                        pressure
                )


        drawBrushStamp(

            point =
                point,

            width =
                width,

            color =
                color,

            alpha =
                alpha,

            previous =
                previous
        )
    }
}


/**
 * =============================================================
 * BRUSH STAMP
 * =============================================================
 */

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawBrushStamp(

        point: Point,

        width: Float,

        color: Color,

        alpha: Float,

        previous: Point? = null

    ) {

    val tiltAmount =

        (
            point.tilt /
                (PI / 2f).toFloat()
        )
            .coerceIn(
                0f,
                1f
            )


    val major =
        width *
            (
                1f +
                    1.8f *
                    tiltAmount
            )


    val minor =
        width *
            (
                0.55f +
                    0.15f *
                    (
                        1f -
                            tiltAmount
                    )
            )


    val angle =

        if (
            point.tilt > 0.02f
        ) {

            point.orientation

        } else if (
            previous != null
        ) {

            atan2(

                point.y -
                    previous.y,

                point.x -
                    previous.x
            )

        } else {

            0f
        }


    rotate(

        degrees =
            Math.toDegrees(
                angle.toDouble()
            ).toFloat(),

        pivot =
            Offset(
                point.x,
                point.y
            )

    ) {

        drawOval(

            color =
                color.copy(

                    alpha =
                        alpha *
                            (
                                0.12f +
                                    0.52f *
                                    point.pressure
                            )
                ),

            topLeft =
                Offset(

                    point.x -
                        major / 2f,

                    point.y -
                        minor / 2f
                ),

            size =
                Size(
                    major,
                    minor
                )
        )


        val fibers =
            9


        for (fiber in 0 until fibers) {

            val y =

                -minor / 2f +
                    (
                        fiber + 0.5f
                    ) *
                    minor /
                    fibers


            drawLine(

                color =
                    color.copy(

                        alpha =
                            alpha *
                                (
                                    0.10f +
                                        0.18f *
                                        point.pressure
                                )
                    ),

                start =
                    Offset(

                        point.x -
                            major * 0.38f,

                        point.y + y
                    ),

                end =
                    Offset(

                        point.x +
                            major * 0.38f,

                        point.y + y
                    ),

                strokeWidth =
                    max(

                        0.45f,

                        minor * 0.055f
                    ),

                cap =
                    StrokeCap.Round
            )
        }
    }
}


/**
 * =============================================================
 * MARKER
 * =============================================================
 */

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawMarker(

        data: DrawPath,

        color: Color,

        alpha: Float

    ) {

    val points =
        data.points


    if (points.isEmpty()) {
        return
    }


    for (index in points.indices) {

        val point =
            points[index]


        val previous =
            points.getOrNull(
                index - 1
            )


        val dx =

            if (previous != null) {

                point.x -
                    previous.x

            } else {

                cos(
                    point.orientation
                )
            }


        val dy =

            if (previous != null) {

                point.y -
                    previous.y

            } else {

                sin(
                    point.orientation
                )
            }


        val movementAngle =
            atan2(
                dy,
                dx
            )


        val nibAngle =

            if (
                point.tilt > 0.03f
            ) {

                point.orientation

            } else {

                movementAngle
            }


        val width =
            data.strokeWidth *
                (
                    0.80f +
                        0.20f *
                        point.pressure
                )


        val thickness =
            max(
                1f,
                width * 0.42f
            )


        rotate(

            degrees =
                Math.toDegrees(
                    nibAngle.toDouble()
                ).toFloat(),

            pivot =
                Offset(
                    point.x,
                    point.y
                )

        ) {

            drawRect(

                color =
                    color.copy(
                        alpha =
                            alpha * 0.70f
                    ),

                topLeft =
                    Offset(

                        point.x -
                            width / 2f,

                        point.y -
                            thickness / 2f
                    ),

                size =
                    Size(
                        width,
                        thickness
                    )
            )
        }
    }
}


/**
 * =============================================================
 * VARIABLE STROKE
 *
 * Does not use Compose Stroke.
 * =============================================================
 */

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawVariableStroke(

        points: List<Point>,

        baseWidth: Float,

        color: Color,

        alpha: Float,

        widthProvider:
            (Point, Point?) -> Float,

        cap: StrokeCap

    ) {

    if (points.isEmpty()) {
        return
    }


    if (points.size == 1) {

        drawCircle(

            color =
                color.copy(
                    alpha =
                        alpha
                ),

            radius =
                widthProvider(
                    points[0],
                    null
                ) / 2f,

            center =
                Offset(
                    points[0].x,
                    points[0].y
                )
        )

        return
    }


    for (index in 1 until points.size) {

        val start =
            points[index - 1]


        val end =
            points[index]


        val widthStart =
            widthProvider(

                start,

                points.getOrNull(
                    index - 2
                )
            )


        val widthEnd =
            widthProvider(

                end,

                start
            )


        val width =
            (
                widthStart +
                    widthEnd
            ) * 0.5f


        drawLine(

            color =
                color.copy(
                    alpha =
                        alpha
                ),

            start =
                Offset(
                    start.x,
                    start.y
                ),

            end =
                Offset(
                    end.x,
                    end.y
                ),

            strokeWidth =
                width.coerceAtLeast(
                    0.5f
                ),

            cap =
                cap
        )
    }


    drawCircle(

        color =
            color.copy(
                alpha =
                    alpha
            ),

        radius =
            widthProvider(
                points.first(),
                null
            ) / 2f,

        center =
            Offset(
                points.first().x,
                points.first().y
            )
    )


    drawCircle(

        color =
            color.copy(
                alpha =
                    alpha
            ),

        radius =
            widthProvider(

                points.last(),

                points.getOrNull(
                    points.size - 2
                )

            ) / 2f,

        center =
            Offset(
                points.last().x,
                points.last().y
            )
    )
}


/**
 * =============================================================
 * SHAPE OUTLINE HELPERS
 *
 * These replace androidx.compose.ui.graphics.drawscope.Stroke.
 * =============================================================
 */

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawRectangleOutline(

        left: Float,

        top: Float,

        width: Float,

        height: Float,

        color: Color,

        strokeWidth: Float

    ) {

    val right =
        left + width

    val bottom =
        top + height


    drawLine(

        color = color,

        start =
            Offset(
                left,
                top
            ),

        end =
            Offset(
                right,
                top
            ),

        strokeWidth =
            strokeWidth,

        cap =
            StrokeCap.Butt
    )


    drawLine(

        color = color,

        start =
            Offset(
                right,
                top
            ),

        end =
            Offset(
                right,
                bottom
            ),

        strokeWidth =
            strokeWidth,

        cap =
            StrokeCap.Butt
    )


    drawLine(

        color = color,

        start =
            Offset(
                right,
                bottom
            ),

        end =
            Offset(
                left,
                bottom
            ),

        strokeWidth =
            strokeWidth,

        cap =
            StrokeCap.Butt
    )


    drawLine(

        color = color,

        start =
            Offset(
                left,
                bottom
            ),

        end =
            Offset(
                left,
                top
            ),

        strokeWidth =
            strokeWidth,

        cap =
            StrokeCap.Butt
    )
}


/**
 * =============================================================
 * ELLIPSE OUTLINE
 * =============================================================
 */

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawEllipseOutline(

        topLeft: Offset,

        size: Size,

        color: Color,

        strokeWidth: Float,

        segments: Int = 96

    ) {

    if (
        size.width <= 0f ||
        size.height <= 0f
    ) {
        return
    }


    val centerX =
        topLeft.x +
            size.width / 2f


    val centerY =
        topLeft.y +
            size.height / 2f


    val radiusX =
        size.width / 2f


    val radiusY =
        size.height / 2f


    var previous =
        Offset(

            centerX +
                radiusX,

            centerY
        )


    for (i in 1..segments) {

        val angle =
            2f *
                PI.toFloat() *
                i.toFloat() /
                segments.toFloat()


        val current =
            Offset(

                centerX +
                    radiusX *
                    cos(angle),

                centerY +
                    radiusY *
                    sin(angle)
            )


        drawLine(

            color =
                color,

            start =
                previous,

            end =
                current,

            strokeWidth =
                strokeWidth,

            cap =
                StrokeCap.Round
        )


        previous =
            current
    }
}


/**
 * =============================================================
 * ARC OUTLINE
 * =============================================================
 */

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawArcOutline(

        topLeft: Offset,

        size: Size,

        color: Color,

        strokeWidth: Float,

        startAngleDegrees: Float = 0f,

        sweepAngleDegrees: Float = 180f,

        segments: Int = 64

    ) {

    if (
        size.width <= 0f ||
        size.height <= 0f
    ) {
        return
    }


    val centerX =
        topLeft.x +
            size.width / 2f


    val centerY =
        topLeft.y +
            size.height / 2f


    val radiusX =
        size.width / 2f


    val radiusY =
        size.height / 2f


    val safeSegments =
        segments.coerceAtLeast(2)


    var previousAngle =
        Math.toRadians(
            startAngleDegrees.toDouble()
        ).toFloat()


    var previous =
        Offset(

            centerX +
                radiusX *
                cos(previousAngle),

            centerY +
                radiusY *
                sin(previousAngle)
        )


    for (i in 1..safeSegments) {

        val progress =
            i.toFloat() /
                safeSegments.toFloat()


        val angle =
            Math.toRadians(

                (
                    startAngleDegrees +
                        sweepAngleDegrees *
                        progress
                ).toDouble()

            ).toFloat()


        val current =
            Offset(

                centerX +
                    radiusX *
                    cos(angle),

                centerY +
                    radiusY *
                    sin(angle)
            )


        drawLine(

            color =
                color,

            start =
                previous,

            end =
                current,

            strokeWidth =
                strokeWidth,

            cap =
                StrokeCap.Round
        )


        previous =
            current
    }
}


/**
 * =============================================================
 * SHAPES
 * =============================================================
 */

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawShape(

        data: DrawPath,

        strokeColor: Color

    ) {

    if (data.points.size < 2) {
        return
    }


    val start =
        Offset(

            data.points[0].x,

            data.points[0].y
        )


    val end =
        Offset(

            data.points[1].x,

            data.points[1].y
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


    val fillColor =

        if (
            data.isFilled &&
            data.fillColorHex != null
        ) {

            parseColor(
                data.fillColorHex
            ).copy(

                alpha =
                    data.opacity.coerceIn(
                        0.05f,
                        1f
                    )
            )

        } else {

            Color.Transparent
        }


    when (data.shapeType) {

        /**
         * -----------------------------------------------------
         * RECTANGLE
         * -----------------------------------------------------
         */

        ShapeType.RECTANGLE -> {

            if (data.isFilled) {

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
                            width,
                            height
                        )
                )
            }


            drawRectangleOutline(

                left =
                    left,

                top =
                    top,

                width =
                    width,

                height =
                    height,

                color =
                    strokeColor,

                strokeWidth =
                    data.strokeWidth
            )
        }


        /**
         * -----------------------------------------------------
         * CIRCLE
         * -----------------------------------------------------
         */

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


            if (data.isFilled) {

                drawCircle(

                    color =
                        fillColor,

                    radius =
                        radius,

                    center =
                        center
                )
            }


            drawEllipseOutline(

                topLeft =
                    Offset(

                        center.x -
                            radius,

                        center.y -
                            radius
                    ),

                size =
                    Size(

                        radius * 2f,

                        radius * 2f
                    ),

                color =
                    strokeColor,

                strokeWidth =
                    data.strokeWidth
            )
        }


        /**
         * -----------------------------------------------------
         * TRIANGLE
         * -----------------------------------------------------
         */

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


            if (data.isFilled) {

                drawPath(

                    path,

                    fillColor
                )
            }


            val p1 =
                Offset(

                    left +
                        width / 2f,

                    top
                )


            val p2 =
                Offset(

                    left,

                    top +
                        height
                )


            val p3 =
                Offset(

                    left +
                        width,

                    top +
                        height
                )


            drawLine(

                color =
                    strokeColor,

                start =
                    p1,

                end =
                    p2,

                strokeWidth =
                    data.strokeWidth,

                cap =
                    StrokeCap.Round
            )


            drawLine(

                color =
                    strokeColor,

                start =
                    p2,

                end =
                    p3,

                strokeWidth =
                    data.strokeWidth,

                cap =
                    StrokeCap.Round
            )


            drawLine(

                color =
                    strokeColor,

                start =
                    p3,

                end =
                    p1,

                strokeWidth =
                    data.strokeWidth,

                cap =
                    StrokeCap.Round
            )
        }


        /**
         * -----------------------------------------------------
         * ELLIPSE
         * -----------------------------------------------------
         */

        ShapeType.ELLIPSE -> {

            if (data.isFilled) {

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
                            width,
                            height
                        )
                )
            }


            drawEllipseOutline(

                topLeft =
                    Offset(
                        left,
                        top
                    ),

                size =
                    Size(
                        width,
                        height
                    ),

                color =
                    strokeColor,

                strokeWidth =
                    data.strokeWidth
            )
        }


        /**
         * -----------------------------------------------------
         * ARC
         * -----------------------------------------------------
         */

        ShapeType.ARC -> {

            drawArcOutline(

                topLeft =
                    Offset(
                        left,
                        top
                    ),

                size =
                    Size(
                        width,
                        height
                    ),

                color =
                    strokeColor,

                strokeWidth =
                    data.strokeWidth,

                startAngleDegrees =
                    0f,

                sweepAngleDegrees =
                    180f
            )
        }


        null -> Unit
    }
}