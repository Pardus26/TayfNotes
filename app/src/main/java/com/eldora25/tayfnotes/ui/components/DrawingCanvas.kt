package com.eldora25.tayfnotes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DrawPath(
    val points: List<Point>,
    val colorHex: String,
    val strokeWidth: Float,
    val toolType: ToolType = ToolType.PEN,
    val shapeType: ShapeType? = null,
    val isFilled: Boolean = false
)

enum class ToolType { PEN, MARKER, ERASER, SHAPE }
enum class ShapeType { RECTANGLE, CIRCLE, TRIANGLE }

@Serializable
data class Point(val x: Float, val y: Float)

@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    initialData: String? = null,
    onDataChanged: (String) -> Unit
) {
    var paths by remember { 
        mutableStateOf(
            if (initialData != null && initialData.isNotEmpty()) {
                try { Json.decodeFromString<List<DrawPath>>(initialData) } catch(e: Exception) { emptyList() }
            } else emptyList()
        )
    }
    
    val currentPathPoints = remember { mutableStateListOf<Point>() }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var currentStrokeWidth by remember { mutableStateOf(5f) }
    var currentTool by remember { mutableStateOf(ToolType.PEN) }
    var currentShape by remember { mutableStateOf(ShapeType.RECTANGLE) }
    var isFillEnabled by remember { mutableStateOf(false) }
    
    var showColorPicker by remember { mutableStateOf(false) }
    var showShapePicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(Color.White)) {
        // Advanced Toolbar
        Surface(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 4.dp
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { currentTool = ToolType.PEN }) {
                        Icon(Icons.Default.Create, contentDescription = "Kalem", tint = if (currentTool == ToolType.PEN) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                    }
                    IconButton(onClick = { currentTool = ToolType.MARKER }) {
                        Icon(Icons.Default.Brush, contentDescription = "Marker", tint = if (currentTool == ToolType.MARKER) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                    }
                    IconButton(onClick = { currentTool = ToolType.ERASER }) {
                        Icon(Icons.Default.AutoFixNormal, contentDescription = "Silgi", tint = if (currentTool == ToolType.ERASER) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                    }
                    IconButton(onClick = { showShapePicker = true }) {
                        Icon(Icons.Default.Category, contentDescription = "Şekiller", tint = if (currentTool == ToolType.SHAPE) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                    }
                    IconButton(onClick = { showColorPicker = true }) {
                        Icon(Icons.Default.ColorLens, contentDescription = "Renk", tint = currentColor)
                    }
                    IconButton(onClick = { 
                        paths = emptyList()
                        currentPathPoints.clear()
                        onDataChanged("")
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Temizle")
                    }
                }
                
                // Thickness Slider
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LineWeight, contentDescription = null, modifier = Modifier.size(16.dp))
                    Slider(
                        value = currentStrokeWidth,
                        onValueChange = { currentStrokeWidth = it },
                        valueRange = 1f..50f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text("${currentStrokeWidth.toInt()}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .pointerInput(currentTool, currentShape, currentColor, currentStrokeWidth, isFillEnabled) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPathPoints.add(Point(offset.x, offset.y))
                        },
                        onDrag = { change, _ ->
                            if (currentTool != ToolType.SHAPE) {
                                currentPathPoints.add(Point(change.position.x, change.position.y))
                            } else {
                                if (currentPathPoints.size > 1) currentPathPoints.removeAt(1)
                                currentPathPoints.add(Point(change.position.x, change.position.y))
                            }
                        },
                        onDragEnd = {
                            if (currentPathPoints.isNotEmpty()) {
                                val newPath = DrawPath(
                                    points = currentPathPoints.toList(),
                                    colorHex = String.format("#%06X", (0xFFFFFF and currentColor.value.toLong().toInt())),
                                    strokeWidth = currentStrokeWidth,
                                    toolType = currentTool,
                                    shapeType = if (currentTool == ToolType.SHAPE) currentShape else null,
                                    isFilled = isFillEnabled
                                )
                                paths = paths + newPath
                                currentPathPoints.clear()
                                onDataChanged(Json.encodeToString(paths))
                            }
                        }
                    )
                }
        ) {
            paths.forEach { drawDataPath(it) }
            
            if (currentPathPoints.isNotEmpty()) {
                val previewPath = DrawPath(
                    points = currentPathPoints.toList(),
                    colorHex = String.format("#%06X", (0xFFFFFF and currentColor.value.toLong().toInt())),
                    strokeWidth = currentStrokeWidth,
                    toolType = currentTool,
                    shapeType = if (currentTool == ToolType.SHAPE) currentShape else null,
                    isFilled = isFillEnabled
                )
                drawDataPath(previewPath)
            }
        }
    }
    
    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Renk ve Dolgu") },
            text = {
                Column {
                    val colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta, Color.Cyan)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        colors.forEach { color ->
                            Box(modifier = Modifier.size(36.dp).background(color, RoundedCornerShape(18.dp)).clickable { currentColor = color; showColorPicker = false })
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isFillEnabled, onCheckedChange = { isFillEnabled = it })
                        Text("Şekil İçini Doldur")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showColorPicker = false }) { Text("Tamam") } }
        )
    }

    if (showShapePicker) {
        AlertDialog(
            onDismissRequest = { showShapePicker = false },
            title = { Text("Şekil Seç") },
            text = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton(onClick = { currentTool = ToolType.SHAPE; currentShape = ShapeType.RECTANGLE; showShapePicker = false }) {
                        Icon(Icons.Default.Rectangle, contentDescription = "Kare")
                    }
                    IconButton(onClick = { currentTool = ToolType.SHAPE; currentShape = ShapeType.CIRCLE; showShapePicker = false }) {
                        Icon(Icons.Default.Circle, contentDescription = "Daire")
                    }
                    IconButton(onClick = { currentTool = ToolType.SHAPE; currentShape = ShapeType.TRIANGLE; showShapePicker = false }) {
                        Icon(Icons.Default.ChangeHistory, contentDescription = "Üçgen")
                    }
                }
            },
            confirmButton = {}
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDataPath(drawPath: DrawPath) {
    val color = if (drawPath.toolType == ToolType.ERASER) Color.White else Color(android.graphics.Color.parseColor(drawPath.colorHex)).run {
        if (drawPath.toolType == ToolType.MARKER) this.copy(alpha = 0.4f) else this
    }
    
    if (drawPath.toolType == ToolType.SHAPE && drawPath.points.size >= 2) {
        val start = Offset(drawPath.points[0].x, drawPath.points[0].y)
        val end = Offset(drawPath.points[1].x, drawPath.points[1].y)
        val left = minOf(start.x, end.x)
        val top = minOf(start.y, end.y)
        val width = Math.abs(start.x - end.x)
        val height = Math.abs(start.y - end.y)

        when (drawPath.shapeType) {
            ShapeType.RECTANGLE -> {
                if (drawPath.isFilled) drawRect(color, Offset(left, top), Size(width, height))
                drawRect(color, Offset(left, top), Size(width, height), style = Stroke(width = drawPath.strokeWidth))
            }
            ShapeType.CIRCLE -> {
                val radius = Math.sqrt((width * width + height * height).toDouble()).toFloat() / 2
                if (drawPath.isFilled) drawCircle(color, radius, Offset(left + width/2, top + height/2))
                drawCircle(color, radius, Offset(left + width/2, top + height/2), style = Stroke(width = drawPath.strokeWidth))
            }
            ShapeType.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(left + width/2, top)
                    lineTo(left, top + height)
                    lineTo(left + width, top + height)
                    close()
                }
                if (drawPath.isFilled) drawPath(path, color)
                drawPath(path, color, style = Stroke(width = drawPath.strokeWidth))
            }
            else -> {}
        }
    } else {
        val path = Path()
        if (drawPath.points.isNotEmpty()) {
            path.moveTo(drawPath.points[0].x, drawPath.points[0].y)
            drawPath.points.forEach { path.lineTo(it.x, it.y) }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = drawPath.strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}
