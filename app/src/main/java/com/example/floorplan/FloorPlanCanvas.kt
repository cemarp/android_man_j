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
fun FloorPlanCanvas(points: List<Point3D>, modifier: Modifier = Modifier, walls: List<List<Point3D>> = emptyList()) {
    Box(modifier = modifier.background(Color(0xFFE0E0E0))) {
        // Collect all points to determine global canvas bounds
        val allPoints = mutableListOf<Point3D>()
        allPoints.addAll(points)
        walls.forEach { allPoints.addAll(it) }

        // Filter points: merge points that are essentially the same XZ floor coordinate
        val filteredFloorPoints = mutableListOf<Point3D>()
        for (point in points) {
            if (filteredFloorPoints.isEmpty()) {
                filteredFloorPoints.add(point)
            } else {
                val lastPoint = filteredFloorPoints.last()
                val distXZ = sqrt(Math.pow((point.x - lastPoint.x).toDouble(), 2.0) + Math.pow((point.z - lastPoint.z).toDouble(), 2.0))
                if (distXZ > 0.2) {
                    filteredFloorPoints.add(point)
                }
            }
        }

        val textMeasurer = rememberTextMeasurer()

        if (allPoints.isEmpty()) {
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
                val minX = allPoints.minOfOrNull { it.x } ?: 0f
                val maxX = allPoints.maxOfOrNull { it.x } ?: 0f
                val minZ = allPoints.minOfOrNull { it.z } ?: 0f
                val maxZ = allPoints.maxOfOrNull { it.z } ?: 0f

                val rangeX = maxX - minX
                val rangeZ = maxZ - minZ

                // Add padding
                val padding = 100f
                val scaleX = if (rangeX > 0) (canvasWidth - padding * 2) / rangeX else 1f
                val scaleZ = if (rangeZ > 0) (canvasHeight - padding * 2) / rangeZ else 1f

                // Maintain aspect ratio
                val scale = minOf(scaleX, scaleZ)

                // 1. Draw 3D Walls
                if (walls.isNotEmpty()) {
                    walls.forEach { wall ->
                        val path = Path()
                        // Sort by X/Z roughly to form a 2D line segment representing the wall from top-down
                        wall.forEachIndexed { index, point ->
                            val normalizedX = (point.x - minX) * scale + padding
                            val normalizedZ = (point.z - minZ) * scale + padding

                            if (index == 0) {
                                path.moveTo(normalizedX, normalizedZ)
                            } else {
                                path.lineTo(normalizedX, normalizedZ)
                            }

                            drawCircle(color = Color.Magenta, radius = 8f, center = Offset(normalizedX, normalizedZ))
                        }
                        path.close()
                        drawPath(path = path, color = Color.Magenta.copy(alpha = 0.5f), style = Stroke(width = 12f))
                    }
                }

                // 2. Draw Floor Perimeter
                if (filteredFloorPoints.isNotEmpty()) {
                    val path = Path()
                    filteredFloorPoints.forEachIndexed { index, point ->
                        val normalizedX = (point.x - minX) * scale + padding
                        val normalizedZ = (point.z - minZ) * scale + padding

                        if (index == 0) {
                            path.moveTo(normalizedX, normalizedZ)
                        } else {
                            path.lineTo(normalizedX, normalizedZ)
                        }

                        drawCircle(color = Color.Red, radius = 10f, center = Offset(normalizedX, normalizedZ))
                    }

                    if (filteredFloorPoints.size > 2) {
                        path.close()
                    }

                    drawPath(path = path, color = Color.Blue, style = Stroke(width = 8f))

                    // Draw dimensions
                    val numPoints = filteredFloorPoints.size
                    if (numPoints > 1) {
                        for (i in 0 until numPoints) {
                            if (i == numPoints - 1 && numPoints <= 2) break

                            val p1 = filteredFloorPoints[i]
                            val p2 = filteredFloorPoints[(i + 1) % numPoints]

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
                                style = TextStyle(fontSize = 14.sp, color = Color.Black, background = Color.White.copy(alpha = 0.7f))
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
}
