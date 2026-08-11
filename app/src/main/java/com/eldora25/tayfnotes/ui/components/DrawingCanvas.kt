package com.eldora25.tayfnotes.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.ui.theme.NeonIcon
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.sqrt

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

@Serializable
enum class ToolType {
    PENCIL,
    PEN,
    BRUSH,
    MARKER,
    HIGHLIGHTER,
    ERASER,
    SHAPE
}

@Serializable
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

@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    initialData: String? = null,
    onDataChanged: (String) -> Unit,
    onImageAdded: (String) -> Unit = {}
) {

    var paths by remember {
        mutableStateOf(
            if (!initialData.isNullOrEmpty()) {
                try {
                    Json.decodeFromString<List<DrawPath>>(initialData)
                } catch (e: Exception) {
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

    var currentColor by remember {
        mutableStateOf(Color.Black)
    }

    var currentFillColor by remember {
        mutableStateOf(Color.Transparent)
    }

    var currentStrokeWidth by remember {
        mutableStateOf(5f)
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
     * Which tool's floating menu is open.
     *
     * null = no floating menu
     */
    var openedToolMenu by remember {
        mutableStateOf<ToolType?>(null)
    }

    var showColorPicker by remember {
        mutableStateOf(false)
    }

    var showShapePicker by remember {
        mutableStateOf(false)
    }

    /*
     * UNDO / REDO
     */
    var undoStack by remember {
        mutableStateOf<List<List<DrawPath>>>(emptyList())
    }

    var redoStack by remember {
        mutableStateOf<List<List<DrawPath>>>(emptyList())
    }

    /*
     * Image picker
     */
    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                onImageAdded(it.toString())
            }
        }

    fun saveState(newPaths: List<DrawPath>) {

        undoStack = undoStack + listOf(paths)

        paths = newPaths

        redoStack = emptyList()

        onDataChanged(
            if (paths.isEmpty()) {
                ""
            } else {
                Json.encodeToString(paths)
            }
        )
    }

    fun undo() {

        if (undoStack.isEmpty()) {
            return
        }

        val previousState = undoStack.last()

        undoStack = undoStack.dropLast(1)

        redoStack = redoStack + listOf(paths)

        paths = previousState

        currentPathPoints.clear()

        onDataChanged(
            if (paths.isEmpty()) {
                ""
            } else {
                Json.encodeToString(paths)
            }
        )
    }

    fun redo() {

        if (redoStack.isEmpty()) {
            return
        }

        val nextState = redoStack.last()

        redoStack = redoStack.dropLast(1)

        undoStack = undoStack + listOf(paths)

        paths = nextState

        currentPathPoints.clear()

        onDataChanged(
            if (paths.isEmpty()) {
                ""
            } else {
                Json.encodeToString(paths)
            }
        )
    }

    fun clearCanvas() {

        if (paths.isEmpty()) {
            return
        }

        undoStack = undoStack + listOf(paths)

        paths = emptyList()

        redoStack = emptyList()

        currentPathPoints.clear()

        onDataChanged("")
    }

    /*
     * TOOL SELECTION
     *
     * First tap:
     *   select tool
     *
     * Second tap:
     *   open floating settings menu
     */
    fun selectTool(tool: ToolType) {

        if (currentTool == tool) {

            openedToolMenu =
                if (openedToolMenu == tool) {
                    null
                } else {
                    tool
                }

        } else {

            currentTool = tool
            openedToolMenu = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        /*
         * ============================================
         * TOP DRAWING TOOLBAR
         * ============================================
         */

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),

            shape = RoundedCornerShape(16.dp),

            color = MaterialTheme.colorScheme.surfaceVariant
                .copy(alpha = 0.92f),

            tonalElevation = 5.dp
        ) {

            Column {

                /*
                 * TOOL ROW
                 */

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 6.dp,
                            vertical = 5.dp
                        ),

                    verticalAlignment = Alignment.CenterVertically,

                    horizontalArrangement =
                        Arrangement.SpaceEvenly
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
                            contentDescription = "İleri Al"
                        )
                    }

                    /*
                     * PENCIL
                     */

                    ToolButton(
                        tool = ToolType.PENCIL,
                        currentTool = currentTool,
                        openedToolMenu = openedToolMenu,
                        icon = Icons.Default.Edit,
                        contentDescription = "Kurşun Kalem",
                        onClick = {
                            selectTool(ToolType.PENCIL)
                        }
                    )

                    /*
                     * PEN
                     */

                    ToolButton(
                        tool = ToolType.PEN,
                        currentTool = currentTool,
                        openedToolMenu = openedToolMenu,
                        icon = Icons.Default.Create,
                        contentDescription = "Kalem",
                        onClick = {
                            selectTool(ToolType.PEN)
                        }
                    )

                    /*
                     * BRUSH
                     */

                    ToolButton(
                        tool = ToolType.BRUSH,
                        currentTool = currentTool,
                        openedToolMenu = openedToolMenu,
                        icon = Icons.Default.Brush,
                        contentDescription = "Fırça",
                        onClick = {
                            selectTool(ToolType.BRUSH)
                        }
                    )

                    /*
                     * MARKER
                     */

                    ToolButton(
                        tool = ToolType.MARKER,
                        currentTool = currentTool,
                        openedToolMenu = openedToolMenu,
                        icon = Icons.Default.Brush,
                        contentDescription = "Marker",
                        onClick = {
                            selectTool(ToolType.MARKER)
                        }
                    )

                    /*
                     * HIGHLIGHTER
                     */

                    ToolButton(
                        tool = ToolType.HIGHLIGHTER,
                        currentTool = currentTool,
                        openedToolMenu = openedToolMenu,
                        icon = Icons.Default.Highlight,
                        contentDescription = "Fosforlu Kalem",
                        onClick = {
                            selectTool(ToolType.HIGHLIGHTER)
                        }
                    )

                    /*
                     * ERASER
                     */

                    ToolButton(
                        tool = ToolType.ERASER,
                        currentTool = currentTool,
                        openedToolMenu = openedToolMenu,
                        icon = Icons.Default.AutoFixNormal,
                        contentDescription = "Silgi",
                        onClick = {
                            selectTool(ToolType.ERASER)
                        }
                    )

                    /*
                     * SHAPES
                     */

                    Box {

                        IconButton(
                            onClick = {

                                if (currentTool ==
                                    ToolType.SHAPE
                                ) {

                                    showShapePicker =
                                        !showShapePicker

                                } else {

                                    currentTool =
                                        ToolType.SHAPE

                                    openedToolMenu =
                                        null

                                }
                            }
                        ) {

                            Icon(
                                Icons.Default.Category,
                                contentDescription = "Şekiller",

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
                    }

                    /*
                     * COLOR
                     */

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clickable {
                                showColorPicker =
                                    !showColorPicker
                            }
                    ) {

                        NeonIcon(
                            backgroundColor =
                                currentColor,

                            modifier =
                                Modifier.size(38.dp)
                        ) {

                            Icon(
                                Icons.Default.Palette,
                                contentDescription =
                                    "Renk",

                                tint =
                                    if (
                                        currentColor
                                            .luminance() > 0.5f
                                    ) {
                                        Color.Black
                                    } else {
                                        Color.White
                                    },

                                modifier =
                                    Modifier.size(21.dp)
                            )
                        }
                    }

                    /*
                     * ADD IMAGE
                     */

                    IconButton(
                        onClick = {
                            imagePickerLauncher.launch(
                                "image/*"
                            )
                        }
                    ) {

                        Icon(
                            Icons.Default.Image,
                            contentDescription =
                                "Sayfaya Resim Ekle"
                        )
                    }

                    /*
                     * CLEAR PAGE
                     */

                    IconButton(
                        onClick = {
                            clearCanvas()
                        }
                    ) {

                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription =
                                "Sayfayı Temizle"
                        )
                    }
                }

                /*
                 * ========================================
                 * FLOATING TOOL SETTINGS
                 * ========================================
                 *
                 * This area appears only when the same
                 * tool is tapped for the second time.
                 */

                if (openedToolMenu != null) {

                    ToolSettingsPanel(
                        tool = openedToolMenu!!,

                        strokeWidth =
                            currentStrokeWidth,

                        onStrokeWidthChanged = {
                            currentStrokeWidth = it
                        }
                    )
                }
            }
        }

        /*
         * ============================================
         * SHAPE PICKER
         * ============================================
         */

        if (showShapePicker) {

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 4.dp
                    ),

                shape =
                    RoundedCornerShape(14.dp),

                tonalElevation = 5.dp
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),

                    horizontalArrangement =
                        Arrangement.SpaceEvenly
                ) {

                    ShapeButton(
                        icon = Icons.Default.Rectangle,
                        label = "Kare"
                    ) {

                        currentShape =
                            ShapeType.RECTANGLE

                        showShapePicker = false
                    }

                    ShapeButton(
                        icon = Icons.Default.Circle,
                        label = "Daire"
                    ) {

                        currentShape =
                            ShapeType.CIRCLE

                        showShapePicker = false
                    }

                    ShapeButton(
                        icon = Icons.Default.ChangeHistory,
                        label = "Üçgen"
                    ) {

                        currentShape =
                            ShapeType.TRIANGLE

                        showShapePicker = false
                    }

                    ShapeButton(
                        icon = Icons.Default.FilterTiltShift,
                        label = "Elips"
                    ) {

                        currentShape =
                            ShapeType.ELLIPSE

                        showShapePicker = false
                    }

                    ShapeButton(
                        icon = Icons.Default.Architecture,
                        label = "Yay"
                    ) {

                        currentShape =
                            ShapeType.ARC

                        showShapePicker = false
                    }
                }
            }
        }

        /*
         * ============================================
         * COLOR PANEL
         * ============================================
         */

        if (showColorPicker) {

            ColorPanel(
                currentColor = currentColor,

                onColorSelected = {
                    currentColor = it
                    showColorPicker = false
                }
            )
        }

        /*
         * ============================================
         * DRAWING AREA
         * ============================================
         */

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)

                .pointerInput(
                    currentTool,
                    currentShape,
                    currentColor,
                    currentStrokeWidth,
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

                            if (
                                currentTool !=
                                ToolType.SHAPE
                            ) {

                                currentPathPoints.add(
                                    Point(
                                        change.position.x,
                                        change.position.y
                                    )
                                )

                            } else {

                                if (
                                    currentPathPoints.size > 1
                                ) {

                                    currentPathPoints.removeAt(
                                        1
                                    )
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

                            if (
                                currentPathPoints.isNotEmpty()
                            ) {

                                val colorString =
                                    colorToHex(
                                        currentColor
                                    )

                                val fillColorString =
                                    if (
                                        isFillEnabled
                                    ) {
                                        colorToHex(
                                            currentFillColor
                                        )
                                    } else {
                                        null
                                    }

                                val newPath =
                                    DrawPath(
                                        points =
                                            currentPathPoints
                                                .toList(),

                                        colorHex =
                                            colorString,

                                        strokeWidth =
                                            currentStrokeWidth,

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
                                            fillColorString
                                    )

                                saveState(
                                    paths +
                                        newPath
                                )

                                currentPathPoints.clear()
                            }
                        }
                    )
                }
        ) {

            /*
             * Existing paths
             */

            paths.forEach {
                drawDataPath(it)
            }

            /*
             * Current preview
             */

            if (
                currentPathPoints.isNotEmpty()
            ) {

                val preview =
                    DrawPath(
                        points =
                            currentPathPoints.toList(),

                        colorHex =
                            colorToHex(
                                currentColor
                            ),

                        strokeWidth =
                            currentStrokeWidth,

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
                                colorToHex(
                                    currentFillColor
                                )
                            } else {
                                null
                            }
                    )

                drawDataPath(preview)
            }
        }
    }
}


/*
 * =====================================================
 * TOOL BUTTON
 * =====================================================
 */

@Composable
private fun ToolButton(
    tool: ToolType,
    currentTool: ToolType,
    openedToolMenu: ToolType?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {

    Box {

        IconButton(
            onClick = onClick
        ) {

            Icon(
                icon,

                contentDescription =
                    contentDescription,

                tint =
                    if (
                        currentTool == tool
                    ) {
                        MaterialTheme
                            .colorScheme
                            .primary
                    } else {
                        LocalContentColor.current
                    }
            )
        }

        /*
         * Small indicator under selected tool
         */

        if (
            currentTool == tool
        ) {

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(22.dp)
                    .height(3.dp)
                    .background(
                        MaterialTheme
                            .colorScheme
                            .primary,
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}


/*
 * =====================================================
 * FLOATING TOOL SETTINGS
 * =====================================================
 */

@Composable
private fun ToolSettingsPanel(
    tool: ToolType,
    strokeWidth: Float,
    onStrokeWidthChanged: (Float) -> Unit
) {

    val title =
        when (tool) {

            ToolType.PENCIL ->
                "Kurşun Kalem"

            ToolType.PEN ->
                "Kalem"

            ToolType.BRUSH ->
                "Fırça"

            ToolType.MARKER ->
                "Marker"

            ToolType.HIGHLIGHTER ->
                "Fosforlu Kalem"

            ToolType.ERASER ->
                "Silgi"

            else ->
                "Araç"
        }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 4.dp
            ),

        shape =
            RoundedCornerShape(14.dp),

        color =
            MaterialTheme
                .colorScheme
                .surface,

        tonalElevation = 8.dp
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    title,

                    style =
                        MaterialTheme
                            .typography
                            .labelLarge,

                    modifier =
                        Modifier.weight(1f)
                )

                Text(
                    "${strokeWidth.toInt()} px",

                    style =
                        MaterialTheme
                            .typography
                            .labelMedium
                )
            }

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Slider(
                value =
                    strokeWidth,

                onValueChange =
                    onStrokeWidthChanged,

                valueRange =
                    when (tool) {

                        ToolType.ERASER ->
                            5f..120f

                        ToolType.BRUSH ->
                            2f..80f

                        ToolType.HIGHLIGHTER ->
                            5f..100f

                        else ->
                            1f..50f
                    }
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

                    AssistChip(
                        onClick = {
                            onStrokeWidthChanged(
                                size
                            )
                        },

                        label = {
                            Text(
                                size.toInt()
                                    .toString()
                            )
                        }
                    )
                }
            }
        }
    }
}


/*
 * =====================================================
 * COLOR PANEL
 * =====================================================
 */

@Composable
private fun ColorPanel(
    currentColor: Color,
    onColorSelected: (Color) -> Unit
) {

    val colors =
        listOf(
            Color.Black,
            Color.DarkGray,
            Color.Gray,
            Color.Red,
            Color(0xFFFF5722),
            Color(0xFFFF9800),
            Color.Yellow,
            Color.Green,
            Color(0xFF00BCD4),
            Color.Blue,
            Color(0xFF3F51B5),
            Color.Magenta,
            Color(0xFF9C27B0),
            Color.White
        )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 4.dp
            ),

        shape =
            RoundedCornerShape(14.dp),

        tonalElevation = 6.dp
    ) {

        Column(
            modifier =
                Modifier.padding(10.dp)
        ) {

            Text(
                "Renk",
                style =
                    MaterialTheme
                        .typography
                        .labelLarge
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceEvenly
            ) {

                colors.take(7).forEach { color ->

                    ColorCircle(
                        color =
                            color,

                        selected =
                            currentColor == color,

                        onClick = {
                            onColorSelected(
                                color
                            )
                        }
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceEvenly
            ) {

                colors.drop(7).forEach { color ->

                    ColorCircle(
                        color =
                            color,

                        selected =
                            currentColor == color,

                        onClick = {
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


@Composable
private fun ColorCircle(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                color,
                CircleShape
            )
            .border(
                width =
                    if (selected) {
                        3.dp
                    } else {
                        1.dp
                    },

                color =
                    if (selected) {
                        MaterialTheme
                            .colorScheme
                            .primary
                    } else {
                        Color.Gray
                    },

                shape =
                    CircleShape
            )
            .clickable {
                onClick()
            }
    )
}


/*
 * =====================================================
 * SHAPE BUTTON
 * =====================================================
 */

@Composable
private fun ShapeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
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
                contentDescription = label
            )
        }

        Text(
            label,
            style =
                MaterialTheme
                    .typography
                    .labelSmall
        )
    }
}


/*
 * =====================================================
 * COLOR -> HEX
 * =====================================================
 */

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


/*
 * =====================================================
 * DRAW PATH
 * =====================================================
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
        } catch (e: Exception) {
            Color.Black
        }

    val color =
        when (drawPath.toolType) {

            ToolType.MARKER ->
                baseColor.copy(alpha = 0.45f)

            ToolType.HIGHLIGHTER ->
                baseColor.copy(alpha = 0.30f)

            ToolType.PENCIL ->
                baseColor.copy(alpha = 0.75f)

            ToolType.BRUSH ->
                baseColor.copy(alpha = 0.85f)

            ToolType.ERASER ->
                Color.White

            else ->
                baseColor
        }

    val effectiveWidth =
        when (drawPath.toolType) {

            ToolType.BRUSH ->
                drawPath.strokeWidth * 1.8f

            ToolType.MARKER ->
                drawPath.strokeWidth * 1.5f

            ToolType.HIGHLIGHTER ->
                drawPath.strokeWidth * 2.2f

            ToolType.PENCIL ->
                drawPath.strokeWidth * 0.75f

            else ->
                drawPath.strokeWidth
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
            } catch (e: Exception) {
                Color.Transparent
            }

        } else {

            Color.Transparent
        }

    /*
     * SHAPES
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

        when (
            drawPath.shapeType
        ) {

            ShapeType.RECTANGLE -> {

                if (
                    drawPath.isFilled
                ) {

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
                                effectiveWidth
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
                        color,

                    radius =
                        radius,

                    center =
                        center,

                    style =
                        Stroke(
                            width =
                                effectiveWidth
                        )
                )
            }

            ShapeType.TRIANGLE -> {

                val trianglePath =
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
                        path =
                            trianglePath,

                        color =
                            fillColor
                    )
                }

                drawPath(
                    path =
                        trianglePath,

                    color =
                        color,

                    style =
                        Stroke(
                            width =
                                effectiveWidth
                        )
                )
            }

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
                        color,

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
                                effectiveWidth
                        )
                )
            }

            ShapeType.ARC -> {

                drawArc(
                    color =
                        color,

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
                                effectiveWidth
                        )
                )
            }

            null -> Unit
        }

    } else {

        /*
         * FREEHAND DRAWING
         */

        val path =
            Path()

        if (
            drawPath.points.isNotEmpty()
        ) {

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
                path =
                    path,

                color =
                    color,

                style =
                    Stroke(
                        width =
                            effectiveWidth,

                        cap =
                            StrokeCap.Round
                    )
            )
        }
    }
}


/*
 * =====================================================
 * LUMINANCE
 * =====================================================
 */

private fun Color.luminance(): Float {

    return (
        0.2126f * red +
            0.7152f * green +
            0.0722f * blue
        )
}