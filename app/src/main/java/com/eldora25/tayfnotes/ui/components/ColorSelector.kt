package com.eldora25.tayfnotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ColorSelector(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit
) {
    val colors = listOf(
        "#FFFFFF", // White
        "#FF5252", // Spectrum Red
        "#FFAB40", // Spectrum Orange
        "#FFD740", // Spectrum Yellow
        "#69F0AE", // Spectrum Green
        "#40C4FF", // Spectrum Blue
        "#B388FF", // Spectrum Purple
        "#D4AF37"  // Premium Gold
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(colors) { colorHex ->
            val color = Color(android.graphics.Color.parseColor(colorHex))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (selectedColorHex == colorHex) 3.dp else 1.dp,
                        color = if (selectedColorHex == colorHex) Color.Black else Color.Gray.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(colorHex) }
            )
        }
    }
}
