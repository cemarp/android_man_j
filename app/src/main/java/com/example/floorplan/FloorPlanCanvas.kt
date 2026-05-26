package com.example.floorplan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

// Represents a 2D coordinate for a wall corner
data class Point2D(val x: Float, val z: Float)

@Composable
fun FloorPlanCanvas(points: List<Point2D>, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color(0xFFE0E0E0))) {
        if (points.isEmpty()) {
            Text(
                text = "No anchors placed yet.",
                modifier = Modifier.align(Alignment.Center),
                color = Color.Gray
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Find min/max to normalize coordinates and fit them to the canvas
                val minX = points.minOfOrNull { it.x } ?: 0f
                val maxX = points.maxOfOrNull { it.x } ?: 0f
                val minZ = points.minOfOrNull { it.z } ?: 0f
                val maxZ = points.maxOfOrNull { it.z } ?: 0f

                val rangeX = maxX - minX
                val rangeZ = maxZ - minZ

                // Add padding
                val padding = 100f
                val scaleX = if (rangeX > 0) (canvasWidth - padding * 2) / rangeX else 1f
                val scaleZ = if (rangeZ > 0) (canvasHeight - padding * 2) / rangeZ else 1f

                // Maintain aspect ratio
                val scale = minOf(scaleX, scaleZ)

                val path = Path()
                points.forEachIndexed { index, point ->
                    val normalizedX = (point.x - minX) * scale + padding
                    // Z goes negative in AR usually, but we normalize it so it handles any direction
                    val normalizedZ = (point.z - minZ) * scale + padding

                    if (index == 0) {
                        path.moveTo(normalizedX, normalizedZ)
                    } else {
                        path.lineTo(normalizedX, normalizedZ)
                    }

                    // Draw a circle for the anchor
                    drawCircle(
                        color = Color.Red,
                        radius = 10f,
                        center = Offset(normalizedX, normalizedZ)
                    )
                }

                // Close the path to form a room if there are more than 2 points
                if (points.size > 2) {
                    path.close()
                }

                drawPath(
                    path = path,
                    color = Color.Blue,
                    style = Stroke(width = 8f)
                )
            }
        }
    }
}
