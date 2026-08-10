package com.eldora25.tayfnotes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val strokeWidth: Float
)

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
    var showColorPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(Color.White)) {
        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { showColorPicker = true }) {
                Icon(Icons.Default.ColorLens, contentDescription = "Renk", tint = currentColor)
            }
            IconButton(onClick = { currentStrokeWidth = if (currentStrokeWidth < 25f) currentStrokeWidth + 5f else 5f }) {
                Icon(Icons.Default.HorizontalRule, contentDescription = "Kalınlık")
            }
            IconButton(onClick = { 
                paths = emptyList()
                currentPathPoints.clear()
                onDataChanged("")
            }) {
                Icon(Icons.Default.Clear, contentDescription = "Temizle")
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPathPoints.add(Point(offset.x, offset.y))
                        },
                        onDrag = { change, _ ->
                            currentPathPoints.add(Point(change.position.x, change.position.y))
                        },
                        onDragEnd = {
                            val newPath = DrawPath(
                                points = currentPathPoints.toList(),
                                colorHex = String.format("#%06X", (0xFFFFFF and currentColor.value.toLong().toInt())),
                                strokeWidth = currentStrokeWidth
                            )
                            paths = paths + newPath
                            currentPathPoints.clear()
                            onDataChanged(Json.encodeToString(paths))
                        }
                    )
                }
        ) {
            // Draw stored paths
            paths.forEach { drawPath ->
                val path = Path()
                if (drawPath.points.isNotEmpty()) {
                    path.moveTo(drawPath.points[0].x, drawPath.points[0].y)
                    drawPath.points.forEach { path.lineTo(it.x, it.y) }
                    drawPath(
                        path = path,
                        color = try { Color(android.graphics.Color.parseColor(drawPath.colorHex)) } catch(e: Exception) { Color.Black },
                        style = Stroke(width = drawPath.strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
            
            // Draw current path
            if (currentPathPoints.isNotEmpty()) {
                val path = Path()
                path.moveTo(currentPathPoints[0].x, currentPathPoints[0].y)
                currentPathPoints.forEach { path.lineTo(it.x, it.y) }
                drawPath(
                    path = path,
                    color = currentColor,
                    style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Round)
                )
            }
        }
    }
    
    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Renk Seç") },
            text = {
                val colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(color, RoundedCornerShape(20.dp))
                                .clickable { 
                                    currentColor = color
                                    showColorPicker = false
                                }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}
