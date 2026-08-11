package com.eldora25.tayfnotes.ui.components

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
import androidx.compose.ui.clip
import androidx.compose.ui.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb

import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.awaitEachGesture
import androidx.compose.ui.input.pointer.awaitFirstDown
import androidx.compose.ui.input.pointer.drag

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
import kotlin.math.min
import kotlin.math.sin


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


/**
 * Çizilebilen şekiller.
 */
enum class ShapeType {
    RECTANGLE,
    CIRCLE,
    TRIANGLE,
    ELLIPSE,
    ARC
}


/**
 * =============================================================
 * DRAWING POINT
 * =============================================================
 *
 * pressure:
 *
 * 0.0 -> çok hafif basınç
 * 1.0 -> maksimum basınç
 *
 * Eski kayıtlarla uyumluluk için default = 1f.
 */
@Serializable
data class Point(

    val x: Float,

    val y: Float,

    val pressure: Float = 1f
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
private fun defaultToolSettings():
        Map<ToolType, ToolSettings> {

    return mapOf(

        ToolType.PEN to
                ToolSettings(
                    size = 4f,
                    opacity = 1f
                ),

        ToolType.PENCIL to
                ToolSettings(
                    size = 3f,
                    opacity = 0.75f
                ),

        ToolType.INK to
                ToolSettings(
                    size = 5f,
                    opacity = 1f
                ),

        ToolType.BRUSH to
                ToolSettings(
                    size = 12f,
                    opacity = 0.70f
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


/**
 * =============================================================
 * TOOL DISPLAY NAME
 * =============================================================
 */
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

        ToolType.MARKER ->
            2f..60f

        ToolType.BRUSH ->
            2f..60f

        else ->
            1f..40f
    }
}


/**
 * =============================================================
 * COLOR -> HEX
 * =============================================================
 */
private fun Color.toHex(): String {

    return String.format(
        "#%06X",
        0xFFFFFF and this.toArgb()
    )
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

    /**
     * =========================================================
     * INITIAL DATA
     * =========================================================
     */

    fun decodeInitialData(
        data: String?
    ): SketchDocument {

        if (data.isNullOrBlank()) {

            return SketchDocument()
        }

        /**
         * Yeni format.
         */
        try {

            return Json.decodeFromString<SketchDocument>(
                data
            )

        } catch (_: Exception) {
        }

        /**
         * Eski format.
         */
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


    /**
     * =========================================================
     * UNDO / REDO
     * =========================================================
     */

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


    /**
     * =========================================================
     * CURRENT TOOL
     * =========================================================
     */

    var currentTool by remember {

        mutableStateOf(
            ToolType.PEN
        )
    }


    /**
     * =========================================================
     * CURRENT SHAPE
     * =========================================================
     */

    var currentShape by remember {

        mutableStateOf(
            ShapeType.RECTANGLE
        )
    }


    /**
     * =========================================================
     * TOOL SETTINGS
     * =========================================================
     */

    var toolSettings by remember {

        mutableStateOf(
            defaultToolSettings()
        )
    }


    /**
     * =========================================================
     * CURRENT COLOR
     * =========================================================
     */

    var currentColor by remember {

        mutableStateOf(
            Color.Black
        )
    }


    /**
     * =========================================================
     * FILL
     * =========================================================
     */

    var isFillEnabled by remember {

        mutableStateOf(false)
    }

    var currentFillColor by remember {

        mutableStateOf(
            Color.Transparent
        )
    }


    /**
     * =========================================================
     * EXPANDED TOOL
     * =========================================================
     */

    var expandedTool by remember {

        mutableStateOf<ToolType?>(
            null
        )
    }


    /**
     * =========================================================
     * COLOR MENU
     * =========================================================
     */

    var colorMenuExpanded by remember {

        mutableStateOf(false)
    }


    /**
     * =========================================================
     * CURRENT PATH
     * =========================================================
     */

    val currentPathPoints =
        remember {

            mutableStateListOf<Point>()
        }


    /**
     * =========================================================
     * CANVAS SIZE
     * =========================================================
     */

    var canvasSize by remember {

        mutableStateOf(
            IntSize.Zero
        )
    }


    /**
     * =========================================================
     * IMAGE PICKER
     * =========================================================
     */

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


    /**
     * =========================================================
     * SAVE
     * =========================================================
     */

    fun saveCurrentState() {

        onDataChanged(
            Json.encodeToString(
                document
            )
        )
    }


    /**
     * =========================================================
     * UPDATE TOOL SETTINGS
     * =========================================================
     */

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


    /**
     * =========================================================
     * SELECT TOOL
     * =========================================================
     */

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


    /**
     * =========================================================
     * UNDO
     * =========================================================
     */

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


    /**
     * =========================================================
     * REDO
     * =========================================================
     */

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


    /**
     * =========================================================
     * CLEAR
     * =========================================================
     */

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


    /**
     * =========================================================
     * ROOT
     * =========================================================
     */

    Column(

        modifier =
            modifier
                .fillMaxSize()
                .clipToBounds()
                .background(
                    Color.White
                )
    ) {


        /**
         * =====================================================
         * TOP TOOLBAR
         * =====================================================
         */

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


                /**
                 * =================================================
                 * UNDO
                 * =================================================
                 */

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
                            "Geri Al",

                        tint =
                            if (
                                undoStack.isNotEmpty()
                            ) {

                                LocalContentColor.current

                            } else {

                                Color.Gray
                            }
                    )
                }


                /**
                 * =================================================
                 * REDO
                 * =================================================
                 */

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
                            "İleri Al",

                        tint =
                            if (
                                redoStack.isNotEmpty()
                            ) {

                                LocalContentColor.current

                            } else {

                                Color.Gray
                            }
                    )
                }


                /**
                 * =================================================
                 * PENCIL
                 * =================================================
                 */

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


                /**
                 * =================================================
                 * PEN
                 * =================================================
                 */

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


                /**
                 * =================================================
                 * INK
                 * =================================================
                 */

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


                /**
                 * =================================================
                 * BRUSH
                 * =================================================
                 */

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


                /**
                 * =================================================
                 * MARKER
                 * =================================================
                 */

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


                /**
                 * =================================================
                 * SHAPES
                 * =================================================
                 */

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

                                    contentDescription =
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

                                    contentDescription =
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

                                    contentDescription =
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

                                    contentDescription =
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

                                    contentDescription =
                                        null
                                )
                            }
                        )
                    }
                }


                /**
                 * =================================================
                 * COLOR PALETTE
                 * =================================================
                 *
                 * Daha önceki daire renk butonu kaldırıldı.
                 *
                 * Artık tekrar Palette ikonu kullanılıyor.
                 */

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

                            imageVector =
                                Icons.Default.Palette,

                            contentDescription =
                                "Renk paleti",

                            tint =
                                LocalContentColor.current
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


                /**
                 * =================================================
                 * IMAGE
                 * =================================================
                 */

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
                            "Sayfaya resim ekle"
                    )
                }


                /**
                 * =================================================
                 * ERASER
                 * =================================================
                 */

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


                /**
                 * =================================================
                 * CLEAR
                 * =================================================
                 */

                IconButton(

                    onClick = {

                        clearCanvas()
                    }

                ) {

                    Icon(

                        Icons.Default.DeleteSweep,

                        contentDescription =
                            "Sayfayı tamamen temizle"
                    )
                }
            }
        }


        /**
         * =========================================================
         * DRAWING AREA
         * =========================================================
         */

        Box(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .background(
                        Color.White
                    )
                    .padding(4.dp)
                    .onSizeChanged {

                        canvasSize =
                            it
                    }
        ) {


            /**
             * =====================================================
             * IMAGES
             * =====================================================
             */

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


            /**
             * =====================================================
             * CANVAS
             * =====================================================
             */

            Canvas(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .pointerInput(

                            currentTool,
                            currentShape,
                            currentColor,
                            toolSettings,
                            isFillEnabled,
                            currentFillColor

                        ) {

                            /**
                             * =================================================
                             * STYLUS PRESSURE INPUT
                             * =================================================
                             *
                             * detectDragGestures yerine manuel gesture
                             * sistemi kullanılıyor.
                             *
                             * Böylece PointerInputChange.pressure
                             * değerini okuyabiliyoruz.
                             */

                            awaitEachGesture {

                                val down =
                                    awaitFirstDown(
                                        requireUnconsumed = false
                                    )


                                /**
                                 * İlk noktanın basıncı.
                                 */
                                val initialPressure =

                                    if (
                                        down.type ==
                                        PointerType.Stylus
                                    ) {

                                        down.pressure
                                            .coerceIn(
                                                0.05f,
                                                1f
                                            )

                                    } else {

                                        1f
                                    }


                                currentPathPoints.clear()


                                currentPathPoints.add(

                                    Point(

                                        x =
                                            down.position.x,

                                        y =
                                            down.position.y,

                                        pressure =
                                            initialPressure
                                    )
                                )


                                /**
                                 * =================================================
                                 * DRAG
                                 * =================================================
                                 */

                                drag(
                                    down.id
                                ) { change ->

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

                                            1f
                                        }


                                    /**
                                     * Şekiller için sadece
                                     * başlangıç ve bitiş tutulur.
                                     */

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
                                }


                                /**
                                 * =================================================
                                 * DRAG END
                                 * =================================================
                                 */

                                if (
                                    currentPathPoints.isEmpty()
                                ) {

                                    return@awaitEachGesture
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


                                /**
                                 * =================================================
                                 * SAVE UNDO STATE
                                 * =================================================
                                 */

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
                        }

            ) {


                /**
                 * =================================================
                 * SAVED PATHS
                 * =================================================
                 */

                document.paths.forEach {

                    drawDataPath(it)
                }


                /**
                 * =================================================
                 * CURRENT PREVIEW
                 * =================================================
                 */

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


                    drawDataPath(
                        preview
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


        /**
         * TOOL ICON
         */

        IconButton(

            onClick =
                onClick

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


        /**
         * TOOL SETTINGS MENU
         */

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
                        toolDisplayName(
                            tool
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .titleSmall
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )


                /**
                 * =================================================
                 * SIZE
                 * =================================================
                 */

                Text(

                    text =
                        "Boyut: " +
                                "${settings.size.toInt()} px",

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


                /**
                 * =================================================
                 * OPACITY
                 * =================================================
                 */

                if (
                    showOpacity
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(
                                4.dp
                            )
                    )


                    Text(

                        text =
                            "Opaklık: " +
                                    "${
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
                        Modifier.height(
                            8.dp
                        )
                )


                /**
                 * =================================================
                 * QUICK SIZE
                 * =================================================
                 */

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
                                    toolSizeRange(
                                        tool
                                    )


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


                                /**
                                 * Hızlı seçimden sonra
                                 * menü kapanır.
                                 */

                                onDismissRequest()
                            },

                            contentPadding =
                                PaddingValues(

                                    horizontal =
                                        10.dp,

                                    vertical =
                                        0.dp
                                )

                        ) {

                            Text(

                                text =
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
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            HorizontalDivider()


            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )


            /**
             * QUICK COLORS
             */

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

                        Color(
                            0xFFFF9800
                        ),

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


/**
 * =============================================================
 * PRESSURE HELPERS
 * =============================================================
 */

/**
 * Basıncı güvenli şekilde normalize eder.
 */
private fun normalizedPressure(
    pressure: Float
): Float {

    return pressure.coerceIn(
        0.05f,
        1f
    )
}


/**
 * Basınca göre kalınlık.
 *
 * minimum = hafif dokunuş
 * maximum = güçlü basınç
 */
private fun pressureWidth(

    baseWidth: Float,

    pressure: Float,

    minFactor: Float = 0.45f,

    maxFactor: Float = 1.45f

): Float {

    val p =
        normalizedPressure(
            pressure
        )


    return baseWidth *
            (
                minFactor +
                        (
                            maxFactor -
                                    minFactor
                        ) * p
                )
}


/**
 * Basınca göre opaklık.
 */
private fun pressureAlpha(

    baseAlpha: Float,

    pressure: Float,

    minAlpha: Float = 0.35f

): Float {

    val p =
        normalizedPressure(
            pressure
        )


    return (
        minAlpha +
                (
                    1f -
                            minAlpha
                ) * p
        ) * baseAlpha
}


/**
 * =============================================================
 * PATH RENDERER
 * =============================================================
 */
private fun DrawScope.drawDataPath(

    drawPath: DrawPath

) {

    if (
        drawPath.points.isEmpty()
    ) {

        return
    }


    /**
     * =========================================================
     * ERASER
     * =========================================================
     */

    if (
        drawPath.toolType ==
        ToolType.ERASER
    ) {

        drawPressureStroke(

            points =
                drawPath.points,

            color =
                Color.White,

            baseWidth =
                drawPath.strokeWidth,

            opacity =
                1f,

            pressureMin =
                0.75f,

            pressureMax =
                1.35f
        )

        return
    }


    /**
     * =========================================================
     * BASE COLOR
     * =========================================================
     */

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


    /**
     * =========================================================
     * BASE OPACITY
     * =========================================================
     */

    val alpha =
        drawPath.opacity.coerceIn(
            0.05f,
            1f
        )


    /**
     * =========================================================
     * FILL COLOR
     * =========================================================
     */

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


    /**
     * =========================================================
     * SHAPES
     * =========================================================
     */

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


        /**
         * Şeklin basıncı.
         *
         * Şekillerde başlangıç basıncını
         * kullanıyoruz.
         */

        val shapePressure =
            normalizedPressure(
                drawPath.points
                    .first()
                    .pressure
            )


        val strokeWidth =
            pressureWidth(

                baseWidth =
                    drawPath.strokeWidth,

                pressure =
                    shapePressure,

                minFactor =
                    0.65f,

                maxFactor =
                    1.35f
            )


        val finalColor =
            baseColor.copy(
                alpha =
                    alpha
            )


        when (
            drawPath.shapeType
        ) {

            /**
             * RECTANGLE
             */
            ShapeType.RECTANGLE -> {

                if (
                    drawPath.isFilled
                ) {

                    drawRect(

                        color =
                            fillColor.copy(
                                alpha =
                                    alpha
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


            /**
             * CIRCLE
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


                if (
                    drawPath.isFilled
                ) {

                    drawCircle(

                        color =
                            fillColor.copy(
                                alpha =
                                    alpha
                            ),

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
                                strokeWidth
                        )
                )
            }


            /**
             * TRIANGLE
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


                if (
                    drawPath.isFilled
                ) {

                    drawPath(

                        path,

                        fillColor.copy(
                            alpha =
                                alpha
                        )
                    )
                }


                drawPath(

                    path,

                    finalColor,

                    style =
                        Stroke(

                            width =
                                strokeWidth
                        )
                )
            }


            /**
             * ELLIPSE
             */
            ShapeType.ELLIPSE -> {

                if (
                    drawPath.isFilled
                ) {

                    drawOval(

                        color =
                            fillColor.copy(
                                alpha =
                                    alpha
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


            /**
             * ARC
             */
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
                            width,
                            height
                        ),

                    style =
                        Stroke(

                            width =
                                strokeWidth,

                            cap =
                                StrokeCap.Round
                        )
                )
            }


            null -> Unit
        }


        return
    }


    /**
     * =========================================================
     * NORMAL TOOLS
     * =========================================================
     */

    when (
        drawPath.toolType
    ) {

        /**
         * =====================================================
         * PENCIL
         * =====================================================
         *
         * Kurşun kalem dokusu:
         *
         * - Basınca göre kalınlık
         * - Birden fazla ince tekstür katmanı
         * - Hafif opacity değişimi
         */
        ToolType.PENCIL -> {

            drawPencilStroke(

                points =
                    drawPath.points,

                color =
                    baseColor,

                baseWidth =
                    drawPath.strokeWidth,

                opacity =
                    alpha
            )
        }


        /**
         * =====================================================
         * PEN
         * =====================================================
         */
        ToolType.PEN -> {

            drawPressureStroke(

                points =
                    drawPath.points,

                color =
                    baseColor,

                baseWidth =
                    drawPath.strokeWidth,

                opacity =
                    alpha,

                pressureMin =
                    0.65f,

                pressureMax =
                    1.30f
            )
        }


        /**
         * =====================================================
         * INK
         * =====================================================
         *
         * Mürekkep kalem:
         *
         * Basınç arttıkça belirgin şekilde kalınlaşır.
         */
        ToolType.INK -> {

            drawInkStroke(

                points =
                    drawPath.points,

                color =
                    baseColor,

                baseWidth =
                    drawPath.strokeWidth,

                opacity =
                    alpha
            )
        }


        /**
         * =====================================================
         * BRUSH
         * =====================================================
         *
         * Fırça:
         *
         * - geniş yumuşak tabaka
         * - basınca göre değişen genişlik
         * - merkezde daha yoğun renk
         */
        ToolType.BRUSH -> {

            drawBrushStroke(

                points =
                    drawPath.points,

                color =
                    baseColor,

                baseWidth =
                    drawPath.strokeWidth,

                opacity =
                    alpha
            )
        }


        /**
         * =====================================================
         * MARKER
         * =====================================================
         */
        ToolType.MARKER -> {

            drawMarkerStroke(

                points =
                    drawPath.points,

                color =
                    baseColor,

                baseWidth =
                    drawPath.strokeWidth,

                opacity =
                    alpha
            )
        }


        else -> Unit
    }
}


/**
 * =============================================================
 * PRESSURE STROKE
 * =============================================================
 */
private fun DrawScope.drawPressureStroke(

    points: List<Point>,

    color: Color,

    baseWidth: Float,

    opacity: Float,

    pressureMin: Float,

    pressureMax: Float

) {

    if (
        points.size == 1
    ) {

        val point =
            points.first()


        drawCircle(

            color =
                color.copy(
                    alpha =
                        opacity
                ),

            radius =
                pressureWidth(

                    baseWidth,
                    point.pressure,
                    pressureMin,
                    pressureMax
                ) / 2f,

            center =
                Offset(
                    point.x,
                    point.y
                )
        )

        return
    }


    for (
        i in 1 until points.size
    ) {

        val previous =
            points[i - 1]

        val current =
            points[i]


        val pressure =
            (
                previous.pressure +
                        current.pressure
                ) / 2f


        val width =
            pressureWidth(

                baseWidth =
                    baseWidth,

                pressure =
                    pressure,

                minFactor =
                    pressureMin,

                maxFactor =
                    pressureMax
            )


        drawLine(

            color =
                color.copy(
                    alpha =
                        opacity
                ),

            start =
                Offset(
                    previous.x,
                    previous.y
                ),

            end =
                Offset(
                    current.x,
                    current.y
                ),

            strokeWidth =
                width,

            cap =
                StrokeCap.Round
        )
    }
}


/**
 * =============================================================
 * PENCIL TEXTURE
 * =============================================================
 */
private fun DrawScope.drawPencilStroke(

    points: List<Point>,

    color: Color,

    baseWidth: Float,

    opacity: Float

) {

    if (
        points.isEmpty()
    ) {

        return
    }


    /**
     * Ana kurşun çizgisi.
     */
    drawPressureStroke(

        points =
            points,

        color =
            color,

        baseWidth =
            baseWidth,

        opacity =
            opacity * 0.70f,

        pressureMin =
            0.35f,

        pressureMax =
            1.15f
    )


    /**
     * =========================================================
     * TEXTURE LAYERS
     * =========================================================
     *
     * Gerçek kurşun kalem hissini taklit etmek için
     * ana çizginin çevresine çok ince ve düşük opaklıklı
     * çizgiler ekleniyor.
     */

    val textureOffsets =
        listOf(

            -0.85f,

            0.55f,

            1.10f
        )


    for (
        layer in textureOffsets.indices
    ) {

        val offset =
            textureOffsets[layer]


        val texturePath =
            Path()


        val first =
            points.first()


        texturePath.moveTo(

            first.x +
                    offset,

            first.y +
                    offset
        )


        points.drop(1).forEachIndexed {
                index,
                point ->

            val wobble =
                sin(
                    (
                        index +
                                layer
                        ) * 1.73
                ).toFloat() *
                        0.65f


            texturePath.lineTo(

                point.x +
                        offset +
                        wobble,

                point.y +
                        offset -
                        wobble
            )
        }


        drawPath(

            path =
                texturePath,

            color =
                color.copy(
                    alpha =
                        opacity *
                                (
                                    0.12f +
                                            layer *
                                            0.035f
                                    )
                ),

            style =
                Stroke(

                    width =
                        baseWidth *
                                0.28f,

                    cap =
                        StrokeCap.Round
                )
        )
    }


    /**
     * Kurşun kalemin koyu çekirdek izi.
     */
    drawPressureStroke(

        points =
            points,

        color =
            color,

        baseWidth =
            baseWidth * 0.42f,

        opacity =
            opacity * 0.45f,

        pressureMin =
            0.30f,

        pressureMax =
            1.10f
    )
}


/**
 * =============================================================
 * INK STROKE
 * =============================================================
 */
private fun DrawScope.drawInkStroke(

    points: List<Point>,

    color: Color,

    baseWidth: Float,

    opacity: Float

) {

    /**
     * Hafif mürekkep gövdesi.
     */
    drawPressureStroke(

        points =
            points,

        color =
            color,

        baseWidth =
            baseWidth,

        opacity =
            opacity,

        pressureMin =
            0.35f,

        pressureMax =
            1.55f
    )


    /**
     * Mürekkep merkez çizgisi.
     */
    drawPressureStroke(

        points =
            points,

        color =
            color,

        baseWidth =
            baseWidth * 0.35f,

        opacity =
            opacity * 0.60f,

        pressureMin =
            0.45f,

        pressureMax =
            1.15f
    )
}


/**
 * =============================================================
 * BRUSH STROKE
 * =============================================================
 */
private fun DrawScope.drawBrushStroke(

    points: List<Point>,

    color: Color,

    baseWidth: Float,

    opacity: Float

) {

    /**
     * Fırçanın dış yumuşak tabakası.
     */
    drawPressureStroke(

        points =
            points,

        color =
            color,

        baseWidth =
            baseWidth * 1.35f,

        opacity =
            opacity * 0.22f,

        pressureMin =
            0.30f,

        pressureMax =
            1.55f
    )


    /**
     * Ana fırça gövdesi.
     */
    drawPressureStroke(

        points =
            points,

        color =
            color,

        baseWidth =
            baseWidth,

        opacity =
            opacity * 0.70f,

        pressureMin =
            0.25f,

        pressureMax =
            1.50f
    )


    /**
     * Fırçanın yoğun merkezi.
     */
    drawPressureStroke(

        points =
            points,

        color =
            color,

        baseWidth =
            baseWidth * 0.45f,

        opacity =
            opacity * 0.45f,

        pressureMin =
            0.30f,

        pressureMax =
            1.30f
    )
}


/**
 * =============================================================
 * MARKER STROKE
 * =============================================================
 */
private fun DrawScope.drawMarkerStroke(

    points: List<Point>,

    color: Color,

    baseWidth: Float,

    opacity: Float

) {

    /**
     * Marker geniş dış katmanı.
     */
    drawPressureStroke(

        points =
            points,

        color =
            color,

        baseWidth =
            baseWidth * 1.12f,

        opacity =
            opacity * 0.45f,

        pressureMin =
            0.60f,

        pressureMax =
            1.20f
    )


    /**
     * Marker ana gövdesi.
     */
    drawPressureStroke(

        points =
            points,

        color =
            color,

        baseWidth =
            baseWidth,

        opacity =
            opacity * 0.60f,

        pressureMin =
            0.65f,

        pressureMax =
            1.25f
    )
}