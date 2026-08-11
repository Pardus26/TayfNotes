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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
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
import kotlin.math.min


/**
 * TayfNotes çizim araçları.
 *
 * PEN:
 * Normal tükenmez/kalem
 *
 * PENCIL:
 * Kurşun kalem
 *
 * INK:
 * Mürekkep kalem
 *
 * BRUSH:
 * Fırça
 *
 * MARKER:
 * Fosforlu/marker
 *
 * ERASER:
 * Silgi
 *
 * SHAPE:
 * Şekiller
 *
 * PEN değeri eski sürümlerle uyumluluk için korunmuştur.
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


@Serializable
data class Point(
    val x: Float,
    val y: Float
)


/**
 * Çizilmiş tek bir stroke.
 *
 * opacity alanı yeni sürümde eklenmiştir.
 * Default değer 1f olduğu için eski kayıtlarla uyumludur.
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
 * Sketch içine eklenen resim.
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
 * Yeni sketch veri formatı.
 *
 * Eski sürüm:
 * List<DrawPath>
 *
 * Yeni sürüm:
 * SketchDocument
 *
 * DrawingCanvas eski formatı da okuyabilir.
 */
@Serializable
data class SketchDocument(
    val paths: List<DrawPath> = emptyList(),
    val images: List<SketchImage> = emptyList()
)


/**
 * Her aracın kendi ayarları vardır.
 *
 * Opacity ayrı bir araç değildir.
 * Her kalem/fırça/marker kendi opacity değerine sahiptir.
 */
data class ToolSettings(
    val size: Float,
    val opacity: Float
)


private fun defaultToolSettings(): Map<ToolType, ToolSettings> {
    return mapOf(
        ToolType.PEN to ToolSettings(4f, 1f),
        ToolType.PENCIL to ToolSettings(3f, 0.75f),
        ToolType.INK to ToolSettings(5f, 1f),
        ToolType.BRUSH to ToolSettings(12f, 0.70f),
        ToolType.MARKER to ToolSettings(20f, 0.45f),
        ToolType.ERASER to ToolSettings(30f, 1f),
        ToolType.SHAPE to ToolSettings(4f, 1f)
    )
}


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


private fun Color.toHex(): String {

    return String.format(
        "#%06X",
        0xFFFFFF and this.toArgb()
    )
}


@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    initialData: String? = null,
    onDataChanged: (String) -> Unit
) {

    /*
     * ------------------------------------------------------------------------
     * INITIAL DATA
     * ------------------------------------------------------------------------
     */

    fun decodeInitialData(
        data: String?
    ): SketchDocument {

        if (data.isNullOrBlank()) {
            return SketchDocument()
        }

        /*
         * Yeni format
         */
        try {

            return Json.decodeFromString<SketchDocument>(
                data
            )

        } catch (_: Exception) {
        }

        /*
         * Eski format
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


    /*
     * ------------------------------------------------------------------------
     * UNDO / REDO
     * ------------------------------------------------------------------------
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


    /*
     * ------------------------------------------------------------------------
     * ACTIVE TOOL
     * ------------------------------------------------------------------------
     */

    var currentTool by remember {
        mutableStateOf(ToolType.PEN)
    }


    /*
     * ------------------------------------------------------------------------
     * ACTIVE SHAPE
     * ------------------------------------------------------------------------
     */

    var currentShape by remember {
        mutableStateOf(ShapeType.RECTANGLE)
    }


    /*
     * ------------------------------------------------------------------------
     * TOOL SETTINGS
     * ------------------------------------------------------------------------
     */

    var toolSettings by remember {
        mutableStateOf(
            defaultToolSettings()
        )
    }


    /*
     * ------------------------------------------------------------------------
     * ACTIVE COLOR
     * ------------------------------------------------------------------------
     */

    var currentColor by remember {
        mutableStateOf(Color.Black)
    }


    /*
     * ------------------------------------------------------------------------
     * FILL
     * ------------------------------------------------------------------------
     */

    var isFillEnabled by remember {
        mutableStateOf(false)
    }

    var currentFillColor by remember {
        mutableStateOf(Color.Transparent)
    }


    /*
     * ------------------------------------------------------------------------
     * TOOL MENU
     * ------------------------------------------------------------------------
     */

    var expandedTool by remember {
        mutableStateOf<ToolType?>(null)
    }

    var colorMenuExpanded by remember {
        mutableStateOf(false)
    }


    /*
     * ------------------------------------------------------------------------
     * CURRENT DRAWING POINTS
     * ------------------------------------------------------------------------
     */

    val currentPathPoints =
        remember {
            mutableStateListOf<Point>()
        }


    /*
     * ------------------------------------------------------------------------
     * CANVAS SIZE
     * ------------------------------------------------------------------------
     */

    var canvasSize by remember {
        mutableStateOf(IntSize.Zero)
    }


    /*
     * ------------------------------------------------------------------------
     * IMAGE PICKER
     * ------------------------------------------------------------------------
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
                (canvasSize.width * 0.45f)
                    .coerceIn(
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


    /*
     * ------------------------------------------------------------------------
     * HELPERS
     * ------------------------------------------------------------------------
     */

    fun saveCurrentState() {

        onDataChanged(
            Json.encodeToString(
                document
            )
        )
    }


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


    fun selectTool(
        tool: ToolType
    ) {

        /*
         * İlk dokunuş:
         * sadece aracı seç.
         *
         * Aynı seçili araca tekrar dokunulursa:
         * ayar menüsünü aç.
         */

        if (currentTool == tool) {

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
    }


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

        saveCurrentState()
    }


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

        saveCurrentState()
    }


    /*
     * ------------------------------------------------------------------------
     * MAIN DRAWING CANVAS
     * ------------------------------------------------------------------------
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

        /*
         * --------------------------------------------------------------------
         * TOP TOOLBAR
         * --------------------------------------------------------------------
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

                /*
                 * UNDO
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
                            )
                                LocalContentColor.current
                            else
                                Color.Gray
                    )
                }


                /*
                 * REDO
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
                            )
                                LocalContentColor.current
                            else
                                Color.Gray
                    )
                }


                /*
                 * KURŞUN KALEM
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
                    }
                )


                /*
                 * KALEM
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
                    }
                )


                /*
                 * MÜREKKEP
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
                    }
                )


                /*
                 * FIRÇA
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
                    }
                )


                /*
                 * MARKER
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
                    }
                )


                /*
                 * ŞEKİLLER
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
                                    )
                                        null
                                    else
                                        ToolType.SHAPE

                            } else {

                                currentTool =
                                    ToolType.SHAPE

                                expandedTool =
                                    null
                            }
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
                                )
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                                else
                                    LocalContentColor.current
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
                                Text(
                                    "Dikdörtgen"
                                )
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
                                Text(
                                    "Daire"
                                )
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
                                Text(
                                    "Üçgen"
                                )
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
                                Text(
                                    "Elips"
                                )
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
                                Text(
                                    "Yay"
                                )
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


                /*
                 * RENK
                 */

                Box {

                    IconButton(

                        onClick = {

                            colorMenuExpanded =
                                !colorMenuExpanded
                        }

                    ) {

                        Box(

                            modifier =
                                Modifier
                                    .size(32.dp)
                                    .background(
                                        currentColor,
                                        CircleShape
                                    )
                                    .border(
                                        2.dp,
                                        Color.Black,
                                        CircleShape
                                    )
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


                /*
                 * RESİM EKLE
                 */

                IconButton(

                    onClick = {
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


                /*
                 * SİLGİ
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

                    showOpacity =
                        false
                )


                /*
                 * TEMİZLE
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


        /*
         * --------------------------------------------------------------------
         * REAL DRAWING AREA
         * --------------------------------------------------------------------
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

            /*
             * ----------------------------------------------------------------
             * IMAGES
             * ----------------------------------------------------------------
             */

            document.images.forEach { image ->

                val density =
                    androidx.compose.ui.platform
                        .LocalDensity
                        .current

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


            /*
             * ----------------------------------------------------------------
             * DRAWING CANVAS
             * ----------------------------------------------------------------
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

                            detectDragGestures(

                                onDragStart = { offset ->

                                    currentPathPoints
                                        .clear()

                                    currentPathPoints
                                        .add(

                                            Point(
                                                offset.x,
                                                offset.y
                                            )
                                        )
                                },


                                onDrag = {
                                    change, _ ->

                                    /*
                                     * Şekiller:
                                     * yalnızca başlangıç
                                     * ve bitiş noktası.
                                     */

                                    if (
                                        currentTool ==
                                        ToolType.SHAPE
                                    ) {

                                        if (
                                            currentPathPoints
                                                .size > 1
                                        ) {

                                            currentPathPoints
                                                .removeAt(1)
                                        }

                                        currentPathPoints
                                            .add(

                                                Point(
                                                    change.position.x,
                                                    change.position.y
                                                )
                                            )

                                    } else {

                                        currentPathPoints
                                            .add(

                                                Point(
                                                    change.position.x,
                                                    change.position.y
                                                )
                                            )
                                    }
                                },


                                onDragEnd = {

                                    if (
                                        currentPathPoints
                                            .isEmpty()
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
                                                )
                                                    currentShape
                                                else
                                                    null,

                                            isFilled =
                                                isFillEnabled,

                                            fillColorHex =
                                                if (
                                                    isFillEnabled
                                                )
                                                    currentFillColor
                                                        .toHex()
                                                else
                                                    null,

                                            opacity =
                                                settings.opacity
                                        )


                                    /*
                                     * Yeni çizim yapılınca
                                     * mevcut state undo'ya gider.
                                     */

                                    undoStack =
                                        undoStack +
                                            document

                                    redoStack =
                                        emptyList()

                                    document =
                                        document.copy(

                                            paths =
                                                document.paths +
                                                    newPath
                                        )

                                    currentPathPoints
                                        .clear()

                                    saveCurrentState()
                                }
                            )
                        }

            ) {

                /*
                 * Önceden kaydedilmiş çizimler.
                 */

                document.paths.forEach {

                    drawDataPath(it)
                }


                /*
                 * O anda çizilen preview.
                 */

                if (
                    currentPathPoints
                        .isNotEmpty()
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
                                )
                                    currentShape
                                else
                                    null,

                            isFilled =
                                isFillEnabled,

                            fillColorHex =
                                if (
                                    isFillEnabled
                                )
                                    currentFillColor
                                        .toHex()
                                else
                                    null,

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
 * ---------------------------------------------------------------------------
 * TOOL BUTTON
 * ---------------------------------------------------------------------------
 *
 * 1. dokunuş = seç
 *
 * 2. dokunuş = ayar menüsünü aç
 */
@Composable
private fun ToolButton(
    tool: ToolType,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    settings: ToolSettings,
    onSettingsChanged: (ToolSettings) -> Unit,
    showOpacity: Boolean = true
) {

    Box {

        IconButton(
            onClick = onClick
        ) {

            Icon(

                icon,

                contentDescription =
                    toolDisplayName(tool),

                tint =
                    if (selected)
                        MaterialTheme
                            .colorScheme
                            .primary
                    else
                        LocalContentColor.current
            )
        }


        /*
         * İkinci tıklamada açılan
         * floating ayar menüsü.
         */

        DropdownMenu(

            expanded =
                expanded,

            onDismissRequest = {
                /*
                 * Menü ToolButton dışındaki
                 * state tarafından kontrol ediliyor.
                 *
                 * Burada boş bırakılması mevcut
                 * davranışı korur.
                 */
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


                /*
                 * BOYUT
                 */

                Text(

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
                        toolSizeRange(tool)
                )


                /*
                 * OPACITY
                 */

                if (showOpacity) {

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(

                        "Opaklık: " +
                            "${(settings.opacity * 100).toInt()}%",

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


                /*
                 * Hızlı boyutlar
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
                                    toolSizeRange(tool)

                                onSettingsChanged(

                                    settings.copy(

                                        size =
                                            size.coerceIn(
                                                allowed.start,
                                                allowed.endInclusive
                                            )
                                    )
                                )
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


/**
 * ---------------------------------------------------------------------------
 * COLOR PALETTE
 * ---------------------------------------------------------------------------
 */
@Composable
private fun ColorPalettePopup(
    expanded: Boolean,
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
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
                                        .background(
                                            color
                                        )
                                        .border(

                                            if (
                                                currentColor ==
                                                color
                                            )
                                                2.dp
                                            else
                                                0.dp,

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


            /*
             * Hızlı renkler
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
                                .size(28.dp)
                                .background(
                                    color,
                                    CircleShape
                                )
                                .border(

                                    if (
                                        currentColor ==
                                        color
                                    )
                                        2.dp
                                    else
                                        0.dp,

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
 * ---------------------------------------------------------------------------
 * PATH RENDERER
 * ---------------------------------------------------------------------------
 */
private fun androidx.compose.ui.graphics
    .drawscope.DrawScope.drawDataPath(
        drawPath: DrawPath
    ) {

    if (
        drawPath.points.isEmpty()
    ) {
        return
    }


    /*
     * ------------------------------------------------------------------------
     * ERASER
     * ------------------------------------------------------------------------
     *
     * Silgi yalnızca DrawingCanvas içerisinde
     * beyaz çizgi olarak davranır.
     *
     * Toolbar DrawingCanvas'ın dışında olduğu için
     * silinemez.
     */

    if (
        drawPath.toolType ==
        ToolType.ERASER
    ) {

        val eraserPath =
            Path()


        eraserPath.moveTo(

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


    /*
     * ------------------------------------------------------------------------
     * BASE COLOR
     * ------------------------------------------------------------------------
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


    /*
     * ------------------------------------------------------------------------
     * OPACITY
     * ------------------------------------------------------------------------
     */

    val alpha =
        drawPath.opacity
            .coerceIn(
                0.05f,
                1f
            )


    val finalColor =

        when (
            drawPath.toolType
        ) {

            ToolType.PENCIL ->

                baseColor.copy(
                    alpha =
                        alpha * 0.80f
                )

            ToolType.PEN ->

                baseColor.copy(
                    alpha =
                        alpha
                )

            ToolType.INK ->

                baseColor.copy(
                    alpha =
                        alpha
                )

            ToolType.BRUSH ->

                baseColor.copy(
                    alpha =
                        alpha
                )

            ToolType.MARKER ->

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


    /*
     * ------------------------------------------------------------------------
     * STROKE WIDTH
     * ------------------------------------------------------------------------
     */

    val strokeWidth =

        when (
            drawPath.toolType
        ) {

            ToolType.PENCIL ->

                drawPath.strokeWidth *
                    0.85f

            ToolType.PEN ->

                drawPath.strokeWidth

            ToolType.INK ->

                drawPath.strokeWidth *
                    1.10f

            ToolType.BRUSH ->

                drawPath.strokeWidth

            ToolType.MARKER ->

                drawPath.strokeWidth

            else ->

                drawPath.strokeWidth
        }


    /*
     * ------------------------------------------------------------------------
     * FILL COLOR
     * ------------------------------------------------------------------------
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


    /*
     * ------------------------------------------------------------------------
     * SHAPES
     * ------------------------------------------------------------------------
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


        when (
            drawPath.shapeType
        ) {

            /*
             * --------------------------------------------------------------
             * RECTANGLE
             * --------------------------------------------------------------
             */

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


            /*
             * --------------------------------------------------------------
             * CIRCLE
             * --------------------------------------------------------------
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
                                strokeWidth
                        )
                )
            }


            /*
             * --------------------------------------------------------------
             * TRIANGLE
             * --------------------------------------------------------------
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
                        fillColor
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


            /*
             * --------------------------------------------------------------
             * ELLIPSE
             * --------------------------------------------------------------
             */

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


            /*
             * --------------------------------------------------------------
             * ARC
             * --------------------------------------------------------------
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
                                strokeWidth
                        )
                )
            }


            null -> Unit
        }

        return
    }


    /*
     * ------------------------------------------------------------------------
     * NORMAL STROKE
     * ------------------------------------------------------------------------
     */

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
            finalColor,

        style =
            Stroke(

                width =
                    strokeWidth,

                cap =
                    StrokeCap.Round
            )
    )
}