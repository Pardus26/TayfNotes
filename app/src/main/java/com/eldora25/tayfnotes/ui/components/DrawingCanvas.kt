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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb

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

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt


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


enum class ShapeType {
    RECTANGLE,
    CIRCLE,
    TRIANGLE,
    ELLIPSE,
    ARC
}


// =============================================================
// POINT
// =============================================================
//
// Yeni alanlar:
//
// pressure    -> 0..1 stylus pressure
// tilt        -> 0..PI/2
// orientation -> stylus orientation
// isStylus    -> gerçek stylus mu?
//
// Eski kayıtlarla uyumluluk için default değerler vardır.
// =============================================================

@Serializable
data class Point(
    val x: Float,
    val y: Float,

    val pressure: Float = 1f,

    val tilt: Float = 0f,

    val orientation: Float = 0f,

    val isStylus: Boolean = false
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

        ToolType.PEN to
            ToolSettings(
                size = 4f,
                opacity = 1f
            ),

        ToolType.PENCIL to
            ToolSettings(
                size = 3f,
                opacity = 0.78f
            ),

        ToolType.INK to
            ToolSettings(
                size = 4f,
                opacity = 1f
            ),

        ToolType.BRUSH to
            ToolSettings(
                size = 12f,
                opacity = 0.72f
            ),

        ToolType.MARKER to
            ToolSettings(
                size = 20f,
                opacity = 0.45f
            ),

        ToolType.ERASER to
            ToolSettings(
                size = 30f,
                opacity = 1f
            ),

        ToolType.SHAPE to
            ToolSettings(
                size = 4f,
                opacity = 1f
            )
    )
}


// =============================================================
// DISPLAY NAME
// =============================================================

private fun toolDisplayName(
    tool: ToolType
): String {

    return when (tool) {

        ToolType.PEN ->
            "Kalem"

        ToolType.PENCIL ->
            "Kurşun Kalem"

        ToolType.INK ->
            "Mürekkep"

        ToolType.BRUSH ->
            "Fırça"

        ToolType.MARKER ->
            "Marker"

        ToolType.ERASER ->
            "Silgi"

        ToolType.SHAPE ->
            "Şekil"
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
            2f..80f

        ToolType.BRUSH ->
            2f..70f

        ToolType.PENCIL ->
            1f..20f

        ToolType.INK ->
            1f..30f

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
// HEX -> COLOR
// =============================================================

private fun colorFromHex(
    value: String
): Color {

    return try {

        Color(
            AndroidColor.parseColor(value)
        )

    } catch (_: Exception) {

        Color.Black
    }
}


// =============================================================
// PRESSURE NORMALIZATION
// =============================================================
//
// Bazı digitizer'lar 0..1 dışında değer verebilir.
// Android dokümantasyonuna göre pressure değerinin
// normalize edilmesi gerekir.
// =============================================================

private fun normalizePressure(
    pressure: Float
): Float {

    if (!pressure.isFinite()) {
        return 1f
    }

    return pressure.coerceIn(
        0.02f,
        1f
    )
}


// =============================================================
// INK PRESSURE CURVE
// =============================================================
//
// Hafif basınç:
// ince
//
// Orta basınç:
// kontrollü kalınlık
//
// Güçlü basınç:
// daha kalın
//
// Smooth-step benzeri eğri kullanılıyor.
// =============================================================

private fun inkPressureCurve(
    pressure: Float
): Float {

    val p =
        normalizePressure(
            pressure
        )

    val curved =
        p * p * (3f - 2f * p)

    return 0.28f + curved * 1.15f
}


// =============================================================
// PENCIL PRESSURE
// =============================================================

private fun pencilPressure(
    pressure: Float
): Float {

    val p =
        normalizePressure(
            pressure
        )

    return 0.55f + p * 0.65f
}


// =============================================================
// BRUSH PRESSURE
// =============================================================

private fun brushPressure(
    pressure: Float
): Float {

    val p =
        normalizePressure(
            pressure
        )

    return 0.35f + p * 1.25f
}


// =============================================================
// TILT FACTOR
// =============================================================
//
// tilt:
//
// 0 rad    = dik
// PI/2     = yüzeye yakın
//
// Fırça ve marker daha geniş olur.
// =============================================================

private fun tiltFactor(
    tilt: Float
): Float {

    val normalized =
        (tilt / (Math.PI.toFloat() / 2f))
            .coerceIn(
                0f,
                1f
            )

    return 1f + normalized * 1.75f
}


// =============================================================
// DETERMINISTIC GRAIN NOISE
// =============================================================
//
// Gerçek random kullanmıyoruz.
//
// Böylece çizim her redraw olduğunda
// aynı texture korunur.
// =============================================================

private fun grainNoise(
    value: Int
): Float {

    var x =
        value * 0x45d9f3b

    x =
        (x xor (x shr 16)) *
                0x45d9f3b

    x =
        (x xor (x shr 16)) *
                0x45d9f3b

    x =
        x xor (x shr 16)

    return (
        (x and 0x7fffffff) /
            2147483647f
        )
}


// =============================================================
// BUILD PATH
// =============================================================

private fun buildPath(
    points: List<Point>,
    offsetX: Float = 0f,
    offsetY: Float = 0f
): Path {

    val path =
        Path()

    if (points.isEmpty()) {
        return path
    }

    path.moveTo(
        points.first().x + offsetX,
        points.first().y + offsetY
    )

    if (points.size == 1) {

        path.lineTo(
            points.first().x + 0.01f + offsetX,
            points.first().y + 0.01f + offsetY
        )

        return path
    }

    for (
        i in 1 until points.size
    ) {

        val previous =
            points[i - 1]

        val current =
            points[i]

        val midpoint =
            Offset(

                (
                    previous.x +
                        current.x
                    ) / 2f + offsetX,

                (
                    previous.y +
                        current.y
                    ) / 2f + offsetY
            )

        path.quadraticTo(

            previous.x + offsetX,
            previous.y + offsetY,

            midpoint.x,
            midpoint.y
        )
    }

    val last =
        points.last()

    path.lineTo(
        last.x + offsetX,
        last.y + offsetY
    )

    return path
}


// =============================================================
// DRAWING CANVAS
// =============================================================

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    initialData: String? = null,
    onDataChanged: (String) -> Unit
) {


    // =========================================================
    // DECODE
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
            decodeInitialData(
                initialData
            )
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
    // TOOL
    // =========================================================

    var currentTool by remember {

        mutableStateOf(
            ToolType.PEN
        )
    }


    // =========================================================
    // SHAPE
    // =========================================================

    var currentShape by remember {

        mutableStateOf(
            ShapeType.RECTANGLE
        )
    }


    // =========================================================
    // SETTINGS
    // =========================================================

    var toolSettings by remember {

        mutableStateOf(
            defaultToolSettings()
        )
    }


    // =========================================================
    // COLOR
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
    // MENUS
    // =========================================================

    var expandedTool by remember {

        mutableStateOf<ToolType?>(
            null
        )
    }

    var colorMenuExpanded by remember {

        mutableStateOf(false)
    }


    // =========================================================
    // CURRENT STROKE
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
    // STYLUS STATE
    // =========================================================

    var activePointerId by remember {

        mutableStateOf(
            -1
        )
    }

    var drawingActive by remember {

        mutableStateOf(
            false
        )
    }

    var multiTouchDetected by remember {

        mutableStateOf(
            false
        )


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
                    canvasSize.width *
                        0.45f
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

            val image =
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
                            image
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
    // TOOL SELECT
    // =========================================================

    fun selectTool(
        tool: ToolType
    ) {

        if (
            currentTool == tool
        ) {

            expandedTool =
                if (
                    expandedTool == tool
                ) {

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

        if (
            undoStack.isEmpty()
        ) {
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

        saveCurrentState()
    }


    // =========================================================
    // REDO
    // =========================================================

    fun performRedo() {

        if (
            redoStack.isEmpty()
        ) {
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

        saveCurrentState()
    }


    // =========================================================
    // FINALIZE STROKE
    // =========================================================

    fun finalizeStroke() {

        if (
            !drawingActive
        ) {
            return
        }

        if (
            currentPathPoints.isEmpty()
        ) {
            drawingActive =
                false

            return
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

        drawingActive =
            false

        activePointerId =
            -1

        multiTouchDetected =
            false

        saveCurrentState()
    }


    // =========================================================
    // CANCEL STROKE
    // =========================================================

    fun cancelStroke() {

        currentPathPoints.clear()

        drawingActive =
            false

        activePointerId =
            -1

        multiTouchDetected =
            false
    }


    // =========================================================
    // START POINT
    // =========================================================

    fun addPointFromEvent(
        event: MotionEvent,
        pointerIndex: Int
    ) {

        if (
            pointerIndex < 0 ||
            pointerIndex >= event.pointerCount
        ) {
            return
        }

        val tool =
            event.getToolType(
                pointerIndex
            )

        val isStylus =
            tool ==
                MotionEvent.TOOL_TYPE_STYLUS ||
                tool ==
                MotionEvent.TOOL_TYPE_ERASER

        val pressure =
            if (isStylus) {

                normalizePressure(
                    event.getAxisValue(
                        MotionEvent.AXIS_PRESSURE,
                        pointerIndex
                    )
                )

            } else {

                1f
            }

        val tilt =
            if (isStylus) {

                event.getAxisValue(
                    MotionEvent.AXIS_TILT,
                    pointerIndex
                ).coerceIn(
                    0f,
                    Math.PI.toFloat() / 2f
                )

            } else {

                0f
            }

        val orientation =
            if (isStylus) {

                event.getAxisValue(
                    MotionEvent.AXIS_ORIENTATION,
                    pointerIndex
                )

            } else {

                0f
            }

        currentPathPoints.add(

            Point(

                x =
                    event.getX(
                        pointerIndex
                    ),

                y =
                    event.getY(
                        pointerIndex
                    ),

                pressure =
                    pressure,

                tilt =
                    tilt,

                orientation =
                    orientation,

                isStylus =
                    isStylus
            )
        )
    }


    // =========================================================
    // ROOT
    // =========================================================

    Column(

        modifier =
            modifier
                .fillMaxSize()
                .clipToBounds()
                .background(
                    Color.White
                )
    ) {


        // =====================================================
        // TOOLBAR
        // =====================================================

        Surface(

            modifier =
                Modifier
                    .fillMaxWidth(),

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

                    onClick =
                        ::performUndo
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

                    onClick =
                        ::performRedo
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
                        ]!!,

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
                        ]!!,

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
                        ]!!,

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
                        ]!!,

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
                        ]!!,

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

                            expandedTool =
                                null
                        }
                    ) {

                        ShapeMenuItem(
                            "Dikdörtgen",
                            Icons.Default.Rectangle
                        ) {

                            currentShape =
                                ShapeType.RECTANGLE

                            currentTool =
                                ToolType.SHAPE

                            expandedTool =
                                null
                        }

                        ShapeMenuItem(
                            "Daire",
                            Icons.Default.Circle
                        ) {

                            currentShape =
                                ShapeType.CIRCLE

                            currentTool =
                                ToolType.SHAPE

                            expandedTool =
                                null
                        }

                        ShapeMenuItem(
                            "Üçgen",
                            Icons.Default.ChangeHistory
                        ) {

                            currentShape =
                                ShapeType.TRIANGLE

                            currentTool =
                                ToolType.SHAPE

                            expandedTool =
                                null
                        }

                        ShapeMenuItem(
                            "Elips",
                            Icons.Default.FilterTiltShift
                        ) {

                            currentShape =
                                ShapeType.ELLIPSE

                            currentTool =
                                ToolType.SHAPE

                            expandedTool =
                                null
                        }

                        ShapeMenuItem(
                            "Yay",
                            Icons.Default.Architecture
                        ) {

                            currentShape =
                                ShapeType.ARC

                            currentTool =
                                ToolType.SHAPE

                            expandedTool =
                                null
                        }
                    }
                }


                // =================================================
                // COLOR PALETTE
                // =================================================
                //
                // Daire yerine tekrar Palette ikonu.
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

                        Icon(

                            Icons.Default.Palette,

                            contentDescription =
                                "Renk Paleti",

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
                            "Resim"
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
                        ]!!,

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


        // =========================================================
        // DRAWING AREA
        // =========================================================

        Box(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .background(
                        Color.White
                    )
                    .onSizeChanged {

                        canvasSize =
                            it
                    }
        ) {


            // =====================================================
            // IMAGES
            // =====================================================

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


            // =====================================================
            // CANVAS
            // =====================================================

            Canvas(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .clipToBounds()

                        // -------------------------------------------------
                        // IMPORTANT:
                        //
                        // Compose -> Android MotionEvent
                        //
                        // Pressure / tilt / orientation burada okunuyor.
                        // -------------------------------------------------

                        .pointerInteropFilter { event ->

                            when (
                                event.actionMasked
                            ) {


                                // =========================================
                                // DOWN
                                // =========================================

                                MotionEvent.ACTION_DOWN -> {

                                    activePointerId =
                                        event.getPointerId(
                                            event.actionIndex
                                        )

                                    multiTouchDetected =
                                        false

                                    currentPathPoints.clear()

                                    drawingActive =
                                        true

                                    addPointFromEvent(

                                        event,

                                        event.actionIndex
                                    )

                                    true
                                }


                                // =========================================
                                // SECOND FINGER / POINTER
                                // =========================================

                                MotionEvent.ACTION_POINTER_DOWN -> {

                                    multiTouchDetected =
                                        true

                                    // İki parmak başladığında
                                    // çizimi iptal ediyoruz.
                                    //
                                    // Böylece ileride pan/zoom sistemi
                                    // bu alanı kullanabilir.

                                    cancelStroke()

                                    true
                                }


                                // =========================================
                                // MOVE
                                // =========================================

                                MotionEvent.ACTION_MOVE -> {

                                    if (
                                        multiTouchDetected
                                    ) {
                                        return@pointerInteropFilter true
                                    }

                                    if (
                                        !drawingActive
                                    ) {
                                        return@pointerInteropFilter true
                                    }

                                    val index =
                                        event.findPointerIndex(
                                            activePointerId
                                        )

                                    if (
                                        index < 0
                                    ) {
                                        return@pointerInteropFilter true
                                    }


                                    // -------------------------------------
                                    // HISTORICAL EVENTS
                                    // -------------------------------------
                                    //
                                    // Android input batching yapabilir.
                                    // Historical points çizgiyi daha
                                    // pürüzsüz yapar.
                                    // -------------------------------------

                                    val historySize =
                                        event.historySize

                                    for (
                                        h in 0 until historySize
                                    ) {

                                        val tool =
                                            event.getToolType(
                                                index
                                            )

                                        val isStylus =
                                            tool ==
                                                MotionEvent.TOOL_TYPE_STYLUS ||
                                                tool ==
                                                MotionEvent.TOOL_TYPE_ERASER

                                        val pressure =
                                            if (
                                                isStylus
                                            ) {

                                                normalizePressure(
                                                    event.getHistoricalPressure(
                                                        index,
                                                        h
                                                    )
                                                )

                                            } else {

                                                1f
                                            }

                                        val tilt =
                                            if (
                                                isStylus
                                            ) {

                                                event.getHistoricalAxisValue(
                                                    MotionEvent.AXIS_TILT,
                                                    index,
                                                    h
                                                ).coerceIn(
                                                    0f,
                                                    Math.PI.toFloat() / 2f
                                                )

                                            } else {

                                                0f
                                            }

                                        val orientation =
                                            if (
                                                isStylus
                                            ) {

                                                event.getHistoricalAxisValue(
                                                    MotionEvent.AXIS_ORIENTATION,
                                                    index,
                                                    h
                                                )

                                            } else {

                                                0f
                                            }

                                        currentPathPoints.add(

                                            Point(

                                                x =
                                                    event.getHistoricalX(
                                                        index,
                                                        h
                                                    ),

                                                y =
                                                    event.getHistoricalY(
                                                        index,
                                                        h
                                                    ),

                                                pressure =
                                                    pressure,

                                                tilt =
                                                    tilt,

                                                orientation =
                                                    orientation,

                                                isStylus =
                                                    isStylus
                                            )
                                        )
                                    }


                                    // -------------------------------------
                                    // CURRENT EVENT
                                    // -------------------------------------

                                    addPointFromEvent(
                                        event,
                                        index
                                    )

                                    true
                                }


                                // =========================================
                                // POINTER UP
                                // =========================================

                                MotionEvent.ACTION_POINTER_UP -> {

                                    if (
                                        !multiTouchDetected
                                    ) {

                                        val index =
                                            event.actionIndex

                                        val id =
                                            event.getPointerId(
                                                index
                                            )

                                        if (
                                            id ==
                                            activePointerId
                                        ) {

                                            finalizeStroke()
                                        }
                                    }

                                    true
                                }


                                // =========================================
                                // UP
                                // =========================================

                                MotionEvent.ACTION_UP -> {

                                    if (
                                        !multiTouchDetected
                                    ) {

                                        finalizeStroke()

                                    } else {

                                        cancelStroke()
                                    }

                                    true
                                }


                                // =========================================
                                // CANCEL
                                // =========================================

                                MotionEvent.ACTION_CANCEL -> {

                                    cancelStroke()

                                    true
                                }


                                else -> {

                                    true
                                }
                            }
                        }
            ) {

                // =====================================================
                // SAVED PATHS
                // =====================================================

                document.paths.forEach {

                    drawDataPath(
                        it
                    )
                }


                // =====================================================
                // LIVE PREVIEW
                // =====================================================

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
// SHAPE MENU ITEM
// =============================================================

@Composable
private fun ShapeMenuItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {

    DropdownMenuItem(

        text = {
            Text(title)
        },

        onClick = onClick,

        leadingIcon = {

            Icon(
                icon,
                contentDescription = null
            )
        }
    )
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
                    toolDisplayName(
                        tool
                    ),

                tint =
                    if (
                        selected
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
                expanded,

            onDismissRequest =
                onDismissRequest,

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
                        toolDisplayName(
                            tool
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .titleSmall
                )


                Spacer(
                    Modifier.height(
                        8.dp
                    )
                )


                // =================================================
                // SIZE
                // =================================================

                Text(

                    text =
                        "Boyut: ${
                            settings.size.toInt()
                        } px",

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
                        toolSizeRange(
                            tool
                        )
                )


                // =================================================
                // OPACITY
                // =================================================

                if (
                    showOpacity
                ) {

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
                    Modifier.height(
                        8.dp
                    )
                )


                // =================================================
                // QUICK SIZES
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

                                val range =
                                    toolSizeRange(
                                        tool
                                    )

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

                                // Menü otomatik kapanır.
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
                Modifier.height(
                    8.dp
                )
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
                                        .size(
                                            20.dp
                                        )
                                        .background(
                                            color
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
                Modifier.height(
                    12.dp
                )
            )


            HorizontalDivider()


            Spacer(
                Modifier.height(
                    10.dp
                )
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
                                .size(
                                    28.dp
                                )
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

        val path =
            buildPath(
                drawPath.points
            )

        drawPath(

            path =
                path,

            color =
                Color.White,

            style =
                Stroke(

                    width =
                        drawPath.strokeWidth,

                    cap =
                        StrokeCap.Round,

                    join =
                        StrokeJoin.Round
                )
        )

        return
    }


    // =========================================================
    // BASE COLOR
    // =========================================================

    val baseColor =
        colorFromHex(
            drawPath.colorHex
        )


    val baseAlpha =
        drawPath.opacity.coerceIn(
            0.03f,
            1f
        )


    // =========================================================
    // SHAPES
    // =========================================================

    if (

        drawPath.toolType ==
        ToolType.SHAPE &&

        drawPath.points.size >= 2
    ) {

        drawShape(
            drawPath,
            baseColor,
            baseAlpha
        )

        return
    }


    // =========================================================
    // NORMAL DRAWING
    // =========================================================

    when (
        drawPath.toolType
    ) {

        ToolType.PENCIL -> {

            drawPencilStroke(
                drawPath,
                baseColor,
                baseAlpha
            )
        }


        ToolType.INK -> {

            drawInkStroke(
                drawPath,
                baseColor,
                baseAlpha
            )
        }


        ToolType.BRUSH -> {

            drawBrushStroke(
                drawPath,
                baseColor,
                baseAlpha
            )
        }


        ToolType.MARKER -> {

            drawMarkerStroke(
                drawPath,
                baseColor,
                baseAlpha
            )
        }


        ToolType.PEN -> {

            drawPenStroke(
                drawPath,
                baseColor,
                baseAlpha
            )
        }


        else -> Unit
    }
}


// =============================================================
// PEN
// =============================================================

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawPenStroke(
        data: DrawPath,
        color: Color,
        alpha: Float
    ) {

    if (
        data.points.size == 1
    ) {

        val p =
            data.points.first()

        drawCircle(

            color =
                color.copy(
                    alpha = alpha
                ),

            radius =
                data.strokeWidth / 2f,

            center =
                Offset(
                    p.x,
                    p.y
                )
        )

        return
    }


    for (
        i in 1 until data.points.size
    ) {

        val a =
            data.points[i - 1]

        val b =
            data.points[i]

        val pressure =
            if (
                a.isStylus
            ) {

                (
                    a.pressure +
                        b.pressure
                    ) / 2f

            } else {

                1f
            }

        val width =
            data.strokeWidth *
                (
                    0.72f +
                        pressure * 0.48f
                    )


        drawLine(

            color =
                color.copy(
                    alpha = alpha
                ),

            start =
                Offset(
                    a.x,
                    a.y
                ),

            end =
                Offset(
                    b.x,
                    b.y
                ),

            strokeWidth =
                width,

            cap =
                StrokeCap.Round
        )
    }
}


// =============================================================
// INK
// =============================================================
//
// Pressure-to-width curve.
// =============================================================

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawInkStroke(
        data: DrawPath,
        color: Color,
        alpha: Float
    ) {

    if (
        data.points.size == 1
    ) {

        val p =
            data.points.first()

        drawCircle(

            color =
                color.copy(
                    alpha = alpha
                ),

            radius =
                data.strokeWidth *
                    inkPressureCurve(
                        p.pressure
                    ) / 2f,

            center =
                Offset(
                    p.x,
                    p.y
                )
        )

        return
    }


    for (
        i in 1 until data.points.size
    ) {

        val a =
            data.points[i - 1]

        val b =
            data.points[i]

        val p =
            (
                a.pressure +
                    b.pressure
                ) / 2f

        val width =
            max(
                0.35f,
                data.strokeWidth *
                    inkPressureCurve(
                        p
                    )
            )

        val inkAlpha =
            (
                alpha *
                    (
                        0.70f +
                            p * 0.30f
                        )
                ).coerceIn(
                    0f,
                    1f
                )

        drawLine(

            color =
                color.copy(
                    alpha =
                        inkAlpha
                ),

            start =
                Offset(
                    a.x,
                    a.y
                ),

            end =
                Offset(
                    b.x,
                    b.y
                ),

            strokeWidth =
                width,

            cap =
                StrokeCap.Round,

            )

    }
}


// =============================================================
// PENCIL
// =============================================================
//
// Gerçekçi yaklaşım:
//
// 1. yumuşak graphite base
// 2. basınca bağlı core
// 3. deterministic grain
//
// Daire damgalama YOK.
// =============================================================

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawPencilStroke(
        data: DrawPath,
        color: Color,
        alpha: Float
    ) {

    if (
        data.points.size == 1
    ) {

        val p =
            data.points.first()

        drawCircle(

            color =
                color.copy(
                    alpha =
                        alpha * 0.45f
                ),

            radius =
                data.strokeWidth *
                    pencilPressure(
                        p.pressure
                    ) / 2f,

            center =
                Offset(
                    p.x,
                    p.y
                )
        )

        return
    }


    // =========================================================
    // BASE GRAPHITE
    // =========================================================

    val basePath =
        buildPath(
            data.points
        )

    drawPath(

        path =
            basePath,

        color =
            color.copy(
                alpha =
                    alpha * 0.22f
            ),

        style =
            Stroke(

                width =
                    data.strokeWidth *
                        0.95f,

                cap =
                    StrokeCap.Round,

                join =
                    StrokeJoin.Round
            )
    )


    // =========================================================
    // PRESSURE CORE
    // =========================================================

    for (
        i in 1 until data.points.size
    ) {

        val a =
            data.points[i - 1]

        val b =
            data.points[i]

        val pressure =
            (
                a.pressure +
                    b.pressure
                ) / 2f

        val width =
            data.strokeWidth *
                pencilPressure(
                    pressure
                )

        drawLine(

            color =
                color.copy(
                    alpha =
                        alpha *
                            (
                                0.18f +
                                    pressure * 0.35f
                                )
                ),

            start =
                Offset(
                    a.x,
                    a.y
                ),

            end =
                Offset(
                    b.x,
                    b.y
                ),

            strokeWidth =
                width,

            cap =
                StrokeCap.Round
        )
    }


    // =========================================================
    // GRAIN
    // =========================================================
    //
    // Birkaç paralel mikro graphite çizgisi.
    //
    // Nokta değil.
    // =========================================================

    for (
        grainLayer in 0..2
    ) {

        val offset =
            (
                grainLayer - 1
                ) *
                data.strokeWidth *
                0.22f

        val grainPath =
            Path()

        var started =
            false

        for (
            i in 1 until data.points.size
        ) {

            val a =
                data.points[i - 1]

            val b =
                data.points[i]

            val dx =
                b.x - a.x

            val dy =
                b.y - a.y

            val length =
                sqrt(
                    dx * dx +
                        dy * dy
                )

            if (
                length < 0.01f
            ) {
                continue
            }

            val nx =
                -dy / length

            val ny =
                dx / length

            val ox =
                nx * offset

            val oy =
                ny * offset

            val noise =
                grainNoise(
                    i * 17 +
                        grainLayer * 791
                )

            if (
                noise > 0.27f
            ) {

                if (!started) {

                    grainPath.moveTo(
                        a.x + ox,
                        a.y + oy
                    )

                    started =
                        true
                }

                grainPath.lineTo(
                    b.x + ox,
                    b.y + oy
                )

            } else {

                started =
                    false
            }
        }


        drawPath(

            path =
                grainPath,

            color =
                color.copy(
                    alpha =
                        alpha *
                            0.10f
                ),

            style =
                Stroke(

                    width =
                        max(
                            0.35f,
                            data.strokeWidth *
                                0.22f
                        ),

                    cap =
                        StrokeCap.Butt,

                    join =
                        StrokeJoin.Miter
                )
        )
    }
}


// =============================================================
// BRUSH
// =============================================================
//
// Pressure + Tilt
//
// Tilt arttıkça brush genişler.
// Pressure arttıkça yoğunluk ve kalınlık artar.
// =============================================================

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawBrushStroke(
        data: DrawPath,
        color: Color,
        alpha: Float
    ) {

    if (
        data.points.size == 1
    ) {

        val p =
            data.points.first()

        val width =
            data.strokeWidth *
                brushPressure(
                    p.pressure
                ) *
                tiltFactor(
                    p.tilt
                )

        drawCircle(

            color =
                color.copy(
                    alpha =
                        alpha * 0.70f
                ),

            radius =
                width / 2f,

            center =
                Offset(
                    p.x,
                    p.y
                )
        )

        return
    }


    for (
        i in 1 until data.points.size
    ) {

        val a =
            data.points[i - 1]

        val b =
            data.points[i]

        val pressure =
            (
                a.pressure +
                    b.pressure
                ) / 2f

        val tilt =
            (
                a.tilt +
                    b.tilt
                ) / 2f

        val width =
            data.strokeWidth *
                brushPressure(
                    pressure
                ) *
                tiltFactor(
                    tilt
                )


        val brushAlpha =
            (
                alpha *
                    (
                        0.48f +
                            pressure * 0.45f
                        )
                ).coerceIn(
                    0.05f,
                    1f
                )


        drawLine(

            color =
                color.copy(
                    alpha =
                        brushAlpha
                ),

            start =
                Offset(
                    a.x,
                    a.y
                ),

            end =
                Offset(
                    b.x,
                    b.y
                ),

            strokeWidth =
                width,

            cap =
                StrokeCap.Round,

            )
    }
}


// =============================================================
// MARKER
// =============================================================
//
// Marker:
//
// - flat/butt cap
// - pressure width
// - tilt width
// - daha düşük opacity
// =============================================================

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawMarkerStroke(
        data: DrawPath,
        color: Color,
        alpha: Float
    ) {

    if (
        data.points.size == 1
    ) {

        val p =
            data.points.first()

        val width =
            data.strokeWidth *
                (
                    0.72f +
                        p.pressure * 0.55f
                    ) *
                (
                    1f +
                        (
                            p.tilt /
                                (
                                    Math.PI.toFloat() /
                                        2f
                                    )
                            ).coerceIn(
                                0f,
                                1f
                            ) *
                            0.8f
                    )

        drawLine(

            color =
                color.copy(
                    alpha =
                        alpha * 0.72f
                ),

            start =
                Offset(
                    p.x - 0.1f,
                    p.y
                ),

            end =
                Offset(
                    p.x + 0.1f,
                    p.y
                ),

            strokeWidth =
                width,

            cap =
                StrokeCap.Butt
        )

        return
    }


    for (
        i in 1 until data.points.size
    ) {

        val a =
            data.points[i - 1]

        val b =
            data.points[i]

        val pressure =
            (
                a.pressure +
                    b.pressure
                ) / 2f

        val tilt =
            (
                a.tilt +
                    b.tilt
                ) / 2f

        val tiltNormalized =
            (
                tilt /
                    (
                        Math.PI.toFloat() /
                            2f
                        )
                ).coerceIn(
                    0f,
                    1f
                )

        val width =
            data.strokeWidth *
                (
                    0.72f +
                        pressure * 0.55f
                    ) *
                (
                    1f +
                        tiltNormalized *
                        0.8f
                    )

        drawLine(

            color =
                color.copy(
                    alpha =
                        alpha * 0.72f
                ),

            start =
                Offset(
                    a.x,
                    a.y
                ),

            end =
                Offset(
                    b.x,
                    b.y
                ),

            strokeWidth =
                width,

            // MARKER FLAT TIP
            cap =
                StrokeCap.Butt
        )
    }
}


// =============================================================
// SHAPE RENDERER
// =============================================================

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawShape(
        data: DrawPath,
        color: Color,
        alpha: Float
    ) {

    if (
        data.points.size < 2
    ) {
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

            colorFromHex(
                data.fillColorHex
            )

        } else {

            Color.Transparent
        }

    val strokeWidth =
        data.strokeWidth


    when (
        data.shapeType
    ) {

        // =====================================================
        // RECTANGLE
        // =====================================================

        ShapeType.RECTANGLE -> {

            if (
                data.isFilled
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
                            width,
                            height
                        )
                )
            }

            drawRect(

                color =
                    color.copy(
                        alpha = alpha
                    ),

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

                style =
                    Stroke(
                        width =
                            strokeWidth
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
                data.isFilled
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
                    color.copy(
                        alpha = alpha
                    ),

                radius =
                    radius,

                center =
                    center,

                style =
                    Stroke(
                        width =
                            strokeWidth
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
                data.isFilled
            ) {

                drawPath(
                    path,
                    fillColor
                )
            }

            drawPath(

                path =
                    path,

                color =
                    color.copy(
                        alpha = alpha
                    ),

                style =
                    Stroke(
                        width =
                            strokeWidth
                    )
            )
        }


        // =====================================================
        // ELLIPSE
        // =====================================================

        ShapeType.ELLIPSE -> {

            if (
                data.isFilled
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
                            width,
                            height
                        )
                )
            }

            drawOval(

                color =
                    color.copy(
                        alpha = alpha
                    ),

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

                style =
                    Stroke(
                        width =
                            strokeWidth
                    )
            )
        }


        // =====================================================
        // ARC
        // =====================================================

        ShapeType.ARC -> {

            drawArc(

                color =
                    color.copy(
                        alpha = alpha
                    ),

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
                        width,
                        height
                    ),

                style =
                    Stroke(
                        width =
                            strokeWidth
                    )
            )
        }


        null -> Unit
    }
}