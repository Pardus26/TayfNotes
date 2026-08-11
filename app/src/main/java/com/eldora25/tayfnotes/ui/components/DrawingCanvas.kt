package com.eldora25.tayfnotes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.ui.theme.NeonIcon
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.sqrt

/* ============================================================
 * DRAWING DATA
 * ============================================================ */

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

enum class ToolType {
    PEN,
    BALLPOINT,
    PENCIL,
    MARKER,
    BRUSH,
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

/* ============================================================
 * TOOL CONFIGURATION
 * ============================================================ */

private data class ToolConfig(
    val name: String,
    val defaultSize: Float,
    val defaultOpacity: Float,
    val minSize: Float = 1f,
    val maxSize: Float = 80f
)

private fun toolConfig(tool: ToolType): ToolConfig {
    return when (tool) {
        ToolType.PEN -> ToolConfig(
            name = "Kalem",
            defaultSize = 3f,
            defaultOpacity = 1f,
            maxSize = 30f
        )

        ToolType.BALLPOINT -> ToolConfig(
            name = "Mürekkepli Kalem",
            defaultSize = 2.5f,
            defaultOpacity = 0.92f,
            maxSize = 25f
        )

        ToolType.PENCIL -> ToolConfig(
            name = "Kurşun Kalem",
            defaultSize = 4f,
            defaultOpacity = 0.65f,
            maxSize = 35f
        )

        ToolType.MARKER -> ToolConfig(
            name = "Marker",
            defaultSize = 14f,
            defaultOpacity = 0.38f,
            maxSize = 60f
        )

        ToolType.BRUSH -> ToolConfig(
            name = "Fırça",
            defaultSize = 8f,
            defaultOpacity = 0.78f,
            maxSize = 80f
        )

        ToolType.ERASER -> ToolConfig(
            name = "Silgi",
            defaultSize = 24f,
            defaultOpacity = 1f,
            maxSize = 100f
        )

        ToolType.SHAPE -> ToolConfig(
            name = "Şekil",
            defaultSize = 4f,
            defaultOpacity = 1f,
            maxSize = 30f
        )
    }
}

/* ============================================================
 * MAIN CANVAS
 * ============================================================ */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    initialData: String? = null,
    onDataChanged: (String) -> Unit,
    onAddImage: (() -> Unit)? = null
) {

    var paths by remember {
        mutableStateOf(
            if (!initialData.isNullOrEmpty()) {
                try {
                    Json.decodeFromString<List<DrawPath>>(initialData)
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        )
    }

    val currentPathPoints = remember {
        mutableStateListOf<Point>()
    }

    var currentTool by remember {
        mutableStateOf(ToolType.PEN)
    }

    var currentShape by remember {
        mutableStateOf(ShapeType.RECTANGLE)
    }

    var currentColor by remember {
        mutableStateOf(Color.Black)
    }

    var currentFillColor by remember {
        mutableStateOf(Color.Transparent)
    }

    var isFillEnabled by remember {
        mutableStateOf(false)
    }

    /*
     * Her araç kendi boyut ve opaklık değerini saklar.
     */
    val toolSizes = remember {
        mutableStateMapOf<ToolType, Float>().apply {
            ToolType.values().forEach {
                this[it] = toolConfig(it).defaultSize
            }
        }
    }

    val toolOpacities = remember {
        mutableStateMapOf<ToolType, Float>().apply {
            ToolType.values().forEach {
                this[it] = toolConfig(it).defaultOpacity
            }
        }
    }

    var settingsTool by remember {
        mutableStateOf<ToolType?>(null)
    }

    var showColorPicker by remember {
        mutableStateOf(false)
    }

    var showShapePicker by remember {
        mutableStateOf(false)
    }

    /*
     * Undo / Redo
     */
    val undoStack = remember {
        mutableStateListOf<List<DrawPath>>()
    }

    val redoStack = remember {
        mutableStateListOf<List<DrawPath>>()
    }

    /*
     * Silme işleminden önceki durum.
     */
    var eraseStartSnapshot by remember {
        mutableStateOf<List<DrawPath>?>(null)
    }

    fun serialize(data: List<DrawPath>) {
        onDataChanged(Json.encodeToString(data))
    }

    fun commit(newPaths: List<DrawPath>) {
        undoStack.add(paths)

        /*
         * Çok büyük history oluşmasını engelle.
         */
        if (undoStack.size > 50) {
            undoStack.removeAt(0)
        }

        paths = newPaths
        redoStack.clear()
        serialize(paths)
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.lastIndex)

            redoStack.add(paths)

            paths = previous
            currentPathPoints.clear()

            serialize(paths)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)

            undoStack.add(paths)

            paths = next
            currentPathPoints.clear()

            serialize(paths)
        }
    }

    fun clearPage() {
        if (paths.isNotEmpty()) {
            commit(emptyList())
        }
    }

    fun selectTool(tool: ToolType) {

        /*
         * Birinci basış:
         * aracı seç.
         *
         * İkinci basış:
         * aynı aracın ayar panelini aç/kapat.
         */
        if (currentTool == tool) {
            settingsTool =
                if (settingsTool == tool) null else tool
        } else {
            currentTool = tool
            settingsTool = null
        }

        if (tool == ToolType.SHAPE) {
            showShapePicker = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        /* ====================================================
         * TOOLBAR
         * ==================================================== */

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.95f
            ),
            tonalElevation = 5.dp
        ) {

            Column {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    /* UNDO */

                    IconButton(
                        enabled = undoStack.isNotEmpty(),
                        onClick = { undo() }
                    ) {
                        Icon(
                            Icons.Default.Undo,
                            contentDescription = "Geri Al"
                        )
                    }

                    /* REDO */

                    IconButton(
                        enabled = redoStack.isNotEmpty(),
                        onClick = { redo() }
                    ) {
                        Icon(
                            Icons.Default.Redo,
                            contentDescription = "Yinele"
                        )
                    }

                    /* KALEM */

                    ToolButton(
                        icon = Icons.Default.Create,
                        label = "Kalem",
                        selected = currentTool == ToolType.PEN,
                        onClick = {
                            selectTool(ToolType.PEN)
                        }
                    )

                    /* MÜREKKEPLİ KALEM */

                    ToolButton(
                        icon = Icons.Default.Edit,
                        label = "Mürekkep",
                        selected = currentTool == ToolType.BALLPOINT,
                        onClick = {
                            selectTool(ToolType.BALLPOINT)
                        }
                    )

                    /* KURŞUN KALEM */

                    ToolButton(
                        icon = Icons.Default.Brush,
                        label = "Kurşun",
                        selected = currentTool == ToolType.PENCIL,
                        onClick = {
                            selectTool(ToolType.PENCIL)
                        }
                    )

                    /* MARKER */

                    ToolButton(
                        icon = Icons.Default.BorderColor,
                        label = "Marker",
                        selected = currentTool == ToolType.MARKER,
                        onClick = {
                            selectTool(ToolType.MARKER)
                        }
                    )

                    /* FIRÇA */

                    ToolButton(
                        icon = Icons.Default.Brush,
                        label = "Fırça",
                        selected = currentTool == ToolType.BRUSH,
                        onClick = {
                            selectTool(ToolType.BRUSH)
                        }
                    )

                    /* SİLGİ */

                    ToolButton(
                        icon = Icons.Default.AutoFixNormal,
                        label = "Silgi",
                        selected = currentTool == ToolType.ERASER,
                        onClick = {
                            selectTool(ToolType.ERASER)
                        }
                    )

                    /* ŞEKİL */

                    ToolButton(
                        icon = Icons.Default.Category,
                        label = "Şekil",
                        selected = currentTool == ToolType.SHAPE,
                        onClick = {
                            selectTool(ToolType.SHAPE)

                            if (currentTool == ToolType.SHAPE) {
                                showShapePicker = true
                            }
                        }
                    )

                    /* RENK */

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clickable {
                                showColorPicker = !showColorPicker
                                settingsTool = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        NeonIcon(
                            backgroundColor = currentColor,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = "Renk",
                                tint =
                                    if (currentColor.luminance() > 0.5f)
                                        Color.Black
                                    else
                                        Color.White
                            )
                        }
                    }

                    /* RESİM */

                    if (onAddImage != null) {
                        IconButton(
                            onClick = {
                                onAddImage()
                            }
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "Resim Ekle"
                            )
                        }
                    }

                    /* SAYFAYI TEMİZLE */

                    IconButton(
                        onClick = {
                            clearPage()
                        }
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Sayfayı Temizle"
                        )
                    }
                }

                /* =================================================
                 * TOOL SETTINGS
                 * ================================================= */

                if (settingsTool != null) {

                    val tool = settingsTool!!
                    val config = toolConfig(tool)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 14.dp,
                                end = 14.dp,
                                bottom = 10.dp
                            )
                            .background(
                                MaterialTheme.colorScheme.surface.copy(
                                    alpha = 0.92f
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(
                                    alpha = 0.25f
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                config.name,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                "${toolSizes[tool]!!.toInt()} px",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        if (tool == ToolType.ERASER)
                                            Color.LightGray
                                        else
                                            currentColor,
                                        RoundedCornerShape(8.dp)
                                    )
                            )

                            Slider(
                                value = toolSizes[tool] ?: config.defaultSize,
                                onValueChange = {
                                    toolSizes[tool] = it
                                },
                                valueRange =
                                    config.minSize..config.maxSize,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        /* OPACITY */

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                "Opaklık",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.width(70.dp)
                            )

                            Slider(
                                value =
                                    toolOpacities[tool]
                                        ?: config.defaultOpacity,
                                onValueChange = {
                                    toolOpacities[tool] = it
                                },
                                valueRange = 0.05f..1f,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                "${((toolOpacities[tool] ?: 1f) * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(45.dp)
                            )
                        }

                        /* QUICK SIZES */

                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                        toolSizes[tool] =
                                            size.coerceIn(
                                                config.minSize,
                                                config.maxSize
                                            )
                                    }
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

        /* ====================================================
         * CANVAS
         * ==================================================== */

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(
                    currentTool,
                    currentColor,
                    currentShape,
                    settingsTool,
                    toolSizes,
                    toolOpacities
                ) {

                    detectDragGestures(

                        onDragStart = { offset ->

                            if (currentTool == ToolType.ERASER) {

                                eraseStartSnapshot = paths

                                paths = eraseAtPoint(
                                    paths,
                                    Point(
                                        offset.x,
                                        offset.y
                                    ),
                                    toolSizes[ToolType.ERASER]
                                        ?: 24f
                                )

                            } else {

                                currentPathPoints.clear()

                                currentPathPoints.add(
                                    Point(
                                        offset.x,
                                        offset.y
                                    )
                                )
                            }
                        },

                        onDrag = { change, _ ->

                            val point = Point(
                                change.position.x,
                                change.position.y
                            )

                            if (currentTool == ToolType.ERASER) {

                                paths = eraseAtPoint(
                                    paths,
                                    point,
                                    toolSizes[ToolType.ERASER]
                                        ?: 24f
                                )

                            } else if (currentTool == ToolType.SHAPE) {

                                if (currentPathPoints.size > 1) {
                                    currentPathPoints.removeAt(1)
                                }

                                currentPathPoints.add(point)

                            } else {

                                currentPathPoints.add(point)
                            }
                        },

                        onDragEnd = {

                            /* =============================
                             * ERASER
                             * ============================= */

                            if (currentTool == ToolType.ERASER) {

                                val before =
                                    eraseStartSnapshot

                                if (
                                    before != null &&
                                    before != paths
                                ) {

                                    undoStack.add(before)

                                    if (undoStack.size > 50) {
                                        undoStack.removeAt(0)
                                    }

                                    redoStack.clear()

                                    serialize(paths)
                                }

                                eraseStartSnapshot = null

                            } else {

                                if (currentPathPoints.isNotEmpty()) {

                                    val size =
                                        toolSizes[currentTool]
                                            ?: toolConfig(
                                                currentTool
                                            ).defaultSize

                                    val opacity =
                                        toolOpacities[currentTool]
                                            ?: toolConfig(
                                                currentTool
                                            ).defaultOpacity

                                    val colorString =
                                        colorToHex(currentColor)

                                    val fillColorString =
                                        if (isFillEnabled)
                                            colorToHex(
                                                currentFillColor
                                            )
                                        else null

                                    val newPath =
                                        DrawPath(
                                            points =
                                                currentPathPoints
                                                    .toList(),
                                            colorHex =
                                                colorString,
                                            strokeWidth = size,
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
                                                fillColorString,
                                            opacity = opacity
                                        )

                                    commit(
                                        paths + newPath
                                    )

                                    currentPathPoints.clear()
                                }
                            }
                        }
                    )
                }
        ) {

            /* EXISTING PATHS */

            paths.forEach { drawDataPath(it) }

            /* CURRENT PREVIEW */

            if (currentPathPoints.isNotEmpty()) {

                val size =
                    toolSizes[currentTool]
                        ?: toolConfig(
                            currentTool
                        ).defaultSize

                val opacity =
                    toolOpacities[currentTool]
                        ?: toolConfig(
                            currentTool
                        ).defaultOpacity

                val preview =
                    DrawPath(
                        points =
                            currentPathPoints.toList(),
                        colorHex =
                            colorToHex(currentColor),
                        strokeWidth = size,
                        toolType = currentTool,
                        shapeType =
                            if (
                                currentTool ==
                                ToolType.SHAPE
                            ) {
                                currentShape
                            } else {
                                null
                            },
                        isFilled = isFillEnabled,
                        fillColorHex =
                            if (isFillEnabled)
                                colorToHex(
                                    currentFillColor
                                )
                            else null,
                        opacity = opacity
                    )

                drawDataPath(preview)
            }
        }
    }

    /* ========================================================
     * COLOR PALETTE
     * ======================================================== */

    if (showColorPicker) {

        AlertDialog(
            onDismissRequest = {
                showColorPicker = false
            },

            title = {
                Text("Renk")
            },

            text = {

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    /*
                     * Fotoğraf 3'teki ana renk matrisi
                     */

                    val palette = listOf(
                        listOf(
                            "#003B4D",
                            "#003C8F",
                            "#28004F",
                            "#51006B",
                            "#720000",
                            "#990000",
                            "#7A3200",
                            "#806000",
                            "#777000",
                            "#263000",
                            "#202020"
                        ),

                        listOf(
                            "#00657F",
                            "#0057C7",
                            "#4700A8",
                            "#7800A8",
                            "#B00058",
                            "#D00000",
                            "#D85A00",
                            "#C98700",
                            "#C7BD00",
                            "#607A00",
                            "#555555"
                        ),

                        listOf(
                            "#008FB5",
                            "#1476E8",
                            "#7624D8",
                            "#B328D7",
                            "#ED287C",
                            "#F13B24",
                            "#F36D00",
                            "#F2A400",
                            "#E6DF00",
                            "#87B400",
                            "#808080"
                        ),

                        listOf(
                            "#00BFEF",
                            "#4296F5",
                            "#A04BE5",
                            "#D45CE4",
                            "#F56BA4",
                            "#F57A6D",
                            "#F59B55",
                            "#F6C45C",
                            "#F1ED73",
                            "#B4D878",
                            "#AAAAAA"
                        ),

                        listOf(
                            "#8EDFF2",
                            "#A7CBFA",
                            "#D0A7F5",
                            "#E5B5EB",
                            "#F7C7D9",
                            "#F8C8C2",
                            "#F8D8B4",
                            "#F8E4B8",
                            "#F4F0B4",
                            "#D9EAB8",
                            "#D0D0D0"
                        ),

                        listOf(
                            "#D8F3FA",
                            "#D9E8FC",
                            "#EBDDF9",
                            "#F3DDF3",
                            "#F8E5EC",
                            "#F9E7E4",
                            "#FAECDD",
                            "#FAF1D8",
                            "#F7F4D8",
                            "#EDF4D9",
                            "#EEEEEE"
                        )
                    )

                    palette.forEach { row ->

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(2.dp)
                        ) {

                            row.forEach { hex ->

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(
                                            Color(
                                                android.graphics.Color.parseColor(
                                                    hex
                                                )
                                            )
                                        )
                                        .clickable {

                                            currentColor =
                                                Color(
                                                    android.graphics.Color.parseColor(
                                                        hex
                                                    )
                                                )

                                            showColorPicker =
                                                false
                                        }
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    /*
                     * Hızlı renkler
                     */

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceEvenly
                    ) {

                        listOf(
                            Color(0xFF2196F3),
                            Color(0xFFF44336),
                            Color(0xFFFF9800),
                            Color(0xFFFFEB3B),
                            Color(0xFF4CAF50),
                            Color(0xFF222222)
                        ).forEach { color ->

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color,
                                        CircleShape
                                    )
                                    .border(
                                        2.dp,
                                        if (
                                            currentColor ==
                                            color
                                        )
                                            Color.Black
                                        else
                                            Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable {

                                        currentColor =
                                            color

                                        showColorPicker =
                                            false
                                    }
                            )
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

    /* ========================================================
     * SHAPE PICKER
     * ======================================================== */

    if (showShapePicker) {

        AlertDialog(
            onDismissRequest = {
                showShapePicker = false
            },

            title = {
                Text("Şekil")
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
                            name = "Dikdörtgen"
                        ) {
                            currentShape =
                                ShapeType.RECTANGLE

                            currentTool =
                                ToolType.SHAPE

                            showShapePicker =
                                false
                        }

                        ShapeButton(
                            icon = Icons.Default.Circle,
                            name = "Daire"
                        ) {
                            currentShape =
                                ShapeType.CIRCLE

                            currentTool =
                                ToolType.SHAPE

                            showShapePicker =
                                false
                        }

                        ShapeButton(
                            icon = Icons.Default.ChangeHistory,
                            name = "Üçgen"
                        ) {
                            currentShape =
                                ShapeType.TRIANGLE

                            currentTool =
                                ToolType.SHAPE

                            showShapePicker =
                                false
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceEvenly
                    ) {

                        ShapeButton(
                            icon =
                                Icons.Default.FilterTiltShift,
                            name = "Elips"
                        ) {
                            currentShape =
                                ShapeType.ELLIPSE

                            currentTool =
                                ToolType.SHAPE

                            showShapePicker =
                                false
                        }

                        ShapeButton(
                            icon =
                                Icons.Default.Architecture,
                            name = "Yay"
                        ) {
                            currentShape =
                                ShapeType.ARC

                            currentTool =
                                ToolType.SHAPE

                            showShapePicker =
                                false
                        }
                    }
                }
            },

            confirmButton = {}
        )
    }
}

/* ============================================================
 * TOOL BUTTON
 * ============================================================ */

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        IconButton(
            onClick = onClick
        ) {

            Icon(
                icon,
                contentDescription = label,
                tint =
                    if (selected)
                        MaterialTheme.colorScheme.primary
                    else
                        LocalContentColor.current
            )
        }

        if (selected) {

            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(2.dp)
                    .background(
                        MaterialTheme.colorScheme.primary
                    )
            )
        }
    }
}

/* ============================================================
 * SHAPE BUTTON
 * ============================================================ */

@Composable
private fun ShapeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    onClick: () -> Unit
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        IconButton(
            onClick = onClick
        ) {
            Icon(
                icon,
                contentDescription = name
            )
        }

        Text(
            name,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/* ============================================================
 * ERASER
 * ============================================================ */

private fun eraseAtPoint(
    source: List<DrawPath>,
    point: Point,
    radius: Float
): List<DrawPath> {

    val threshold = radius + 4f

    return source.filterNot { path ->

        path.points.any { p ->

            distance(
                p,
                point
            ) <= threshold
        }
    }
}

private fun distance(
    a: Point,
    b: Point
): Float {

    val dx = a.x - b.x
    val dy = a.y - b.y

    return sqrt(
        dx * dx + dy * dy
    )
}

/* ============================================================
 * DRAW PATH
 * ============================================================ */

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawDataPath(
        drawPath: DrawPath
    ) {

    if (drawPath.points.isEmpty()) {
        return
    }

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

    val color =
        baseColor.copy(
            alpha = drawPath.opacity
        )

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
            minOf(
                start.x,
                end.x
            )

        val top =
            minOf(
                start.y,
                end.y
            )

        val width =
            abs(
                start.x - end.x
            )

        val height =
            abs(
                start.y - end.y
            )

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
                    ).copy(
                        alpha = drawPath.opacity
                    )
                } catch (_: Exception) {
                    Color.Transparent
                }

            } else {
                Color.Transparent
            }

        when (drawPath.shapeType) {

            ShapeType.RECTANGLE -> {

                if (drawPath.isFilled) {
                    drawRect(
                        color = fillColor,
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
                    color = color,
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
                                drawPath.strokeWidth
                        )
                )
            }

            ShapeType.CIRCLE -> {

                val radius =
                    sqrt(
                        width * width +
                            height * height
                    ) / 2f

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
                    style =
                        Stroke(
                            width =
                                drawPath.strokeWidth
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
                    style =
                        Stroke(
                            width =
                                drawPath.strokeWidth
                        )
                )
            }

            ShapeType.ELLIPSE -> {

                if (drawPath.isFilled) {

                    drawOval(
                        color = fillColor,
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
                    color = color,
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
                                drawPath.strokeWidth
                        )
                )
            }

            ShapeType.ARC -> {

                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
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
                                drawPath.strokeWidth
                        )
                )
            }

            null -> Unit
        }

    } else {

        val path =
            Path()

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

        val cap =
            when (drawPath.toolType) {

                ToolType.BRUSH ->
                    StrokeCap.Round

                ToolType.PENCIL ->
                    StrokeCap.Round

                ToolType.MARKER ->
                    StrokeCap.Square

                ToolType.BALLPOINT ->
                    StrokeCap.Round

                else ->
                    StrokeCap.Round
            }

        drawPath(
            path = path,
            color = color,
            style =
                Stroke(
                    width =
                        drawPath.strokeWidth,
                    cap = cap
                )
        )
    }
}

/* ============================================================
 * COLOR → HEX
 * ============================================================ */

private fun colorToHex(
    color: Color
): String {

    val r =
        (color.red * 255)
            .toInt()
            .coerceIn(0, 255)

    val g =
        (color.green * 255)
            .toInt()
            .coerceIn(0, 255)

    val b =
        (color.blue * 255)
            .toInt()
            .coerceIn(0, 255)

    return String.format(
        "#%02X%02X%02X",
        r,
        g,
        b
    )
}