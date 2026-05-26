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
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

// Represents a 3D coordinate for an AR anchor in world space
data class Point3D(val x: Float, val y: Float, val z: Float)

@Composable
fun FloorPlanCanvas(points: List<Point3D>, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color(0xFFE0E0E0))) {
        // Filter points: merge points that are essentially the same XZ floor coordinate
        // (e.g. if the user tapped top and bottom of the same corner)
        val filteredPoints = mutableListOf<Point3D>()
        for (point in points) {
            if (filteredPoints.isEmpty()) {
                filteredPoints.add(point)
            } else {
                val lastPoint = filteredPoints.last()
                val distXZ = sqrt(Math.pow((point.x - lastPoint.x).toDouble(), 2.0) + Math.pow((point.z - lastPoint.z).toDouble(), 2.0))
                // If the next tapped point is less than 0.2 meters away horizontally, ignore it as a duplicate/vertical mistake
                if (distXZ > 0.2) {
                    filteredPoints.add(point)
                }
            }
        }

        val textMeasurer = rememberTextMeasurer()

        if (filteredPoints.isEmpty()) {
            Text(
                text = "No valid anchors placed yet.",
                modifier = Modifier.align(Alignment.Center),
                color = Color.Gray
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Find min/max to normalize coordinates and fit them to the canvas
                val minX = filteredPoints.minOfOrNull { it.x } ?: 0f
                val maxX = filteredPoints.maxOfOrNull { it.x } ?: 0f
                val minZ = filteredPoints.minOfOrNull { it.z } ?: 0f
                val maxZ = filteredPoints.maxOfOrNull { it.z } ?: 0f

                val rangeX = maxX - minX
                val rangeZ = maxZ - minZ

                // Add padding
                val padding = 100f
                val scaleX = if (rangeX > 0) (canvasWidth - padding * 2) / rangeX else 1f
                val scaleZ = if (rangeZ > 0) (canvasHeight - padding * 2) / rangeZ else 1f

                // Maintain aspect ratio
                val scale = minOf(scaleX, scaleZ)

                val path = Path()
                filteredPoints.forEachIndexed { index, point ->
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
                if (filteredPoints.size > 2) {
                    path.close()
                }

                drawPath(
                    path = path,
                    color = Color.Blue,
                    style = Stroke(width = 8f)
                )

                // Draw dimensions
                val numPoints = filteredPoints.size
                if (numPoints > 1) {
                    for (i in 0 until numPoints) {
                        // If it's the last point and we don't have enough to close, skip
                        if (i == numPoints - 1 && numPoints <= 2) break

                        val p1 = filteredPoints[i]
                        val p2 = filteredPoints[(i + 1) % numPoints]

                        // Distance in meters, converting to feet
                        val distMeters = sqrt(Math.pow((p2.x - p1.x).toDouble(), 2.0) + Math.pow((p2.z - p1.z).toDouble(), 2.0))
                        val distFeet = distMeters * 3.28084

                        val text = String.format("%.1f ft", distFeet)

                        val normX1 = (p1.x - minX) * scale + padding
                        val normZ1 = (p1.z - minZ) * scale + padding
                        val normX2 = (p2.x - minX) * scale + padding
                        val normZ2 = (p2.z - minZ) * scale + padding

                        val midX = (normX1 + normX2) / 2
                        val midZ = (normZ1 + normZ2) / 2

                        val textLayoutResult = textMeasurer.measure(
                            text = text,
                            style = TextStyle(fontSize = 14.sp, color = Color.Black)
                        )

                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(midX - (textLayoutResult.size.width / 2), midZ - (textLayoutResult.size.height / 2))
                        )
                    }
                }
            }
        }
    }
}
