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
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity

// Represents a 3D coordinate for an AR anchor in world space
data class Point3D(val x: Float, val y: Float, val z: Float)

@Composable
fun FloorPlanCanvas(points: List<Point3D>, modifier: Modifier = Modifier, walls: List<List<Point3D>> = emptyList(), features: List<CapturedFeature> = emptyList()) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotationZ by remember { mutableStateOf(0f) }
    var rotationX by remember { mutableStateOf(0f) }
    val density = LocalDensity.current.density

    Box(modifier = modifier.background(Color(0xFFE0E0E0))) {
        // Collect all points to determine global canvas bounds
        val allPoints = mutableListOf<Point3D>()
        allPoints.addAll(points)
        walls.forEach { allPoints.addAll(it) }
        features.forEach { allPoints.add(it.pose) }

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
            // Legend Overlay
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                color = Color.White.copy(alpha = 0.8f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    androidx.compose.material3.Text("Legend", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 14.sp)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).background(Color.Blue))
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                        androidx.compose.material3.Text("Floor Perimeter", fontSize = 12.sp)
                    }
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).background(Color.Magenta.copy(alpha = 0.5f)))
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                        androidx.compose.material3.Text("3D Walls", fontSize = 12.sp)
                    }
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).background(Color.Red, shape = androidx.compose.foundation.shape.CircleShape))
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                        androidx.compose.material3.Text("Anchors", fontSize = 12.sp)
                    }
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).background(Color.Green))
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                        androidx.compose.material3.Text("Features (Windows/Doors)", fontSize = 12.sp)
                    }
                }
            }

            Canvas(modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Unified transform gesture handling.
                    // A pan with zoom=1f and rot=0f is typical for a 1-finger drag.
                    // Multi-finger triggers zoom/rotation changes.
                    detectTransformGestures(panZoomLock = false) { _, pan, zoom, rotation ->
                        scale *= zoom
                        rotationZ += rotation

                        // To allow map-style navigation:
                        // If it's purely a 1-finger pan (no zoom/rotation), apply translation.
                        // If it's a 2-finger gesture with primarily vertical motion and little zoom/rotation, treat as tilt.
                        // Otherwise, apply standard translation alongside zoom/rotation.
                        val isMultiFinger = zoom !in 0.99f..1.01f || Math.abs(rotation) > 0.5f

                        if (isMultiFinger) {
                            // If zooming/rotating, check if it's actually a tilt gesture (2 fingers dragging vertically)
                            // This is a rough heuristic. A better approach is tracking pointer counts manually, but this suffices for the MVP.
                            if (zoom in 0.9f..1.1f && Math.abs(rotation) < 5f && Math.abs(pan.y) > Math.abs(pan.x) * 1.5f) {
                                rotationX -= pan.y * 0.5f
                            } else {
                                offset += pan
                            }
                        } else {
                            // Single finger drag strictly pans
                            offset += pan
                        }
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                    rotationZ = rotationZ,
                    rotationX = rotationX,
                    cameraDistance = 8f * density // add perspective depth
                )
            ) {
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
                val baseScale = minOf(scaleX, scaleZ)

                // 1. Draw 3D Walls
                if (walls.isNotEmpty()) {
                    // Simple tilt heuristic mapping to give pseudo-3D height to the walls.
                    // Negative Y in ARCore goes "down" (closer to floor), positive goes "up" (ceiling).
                    // We offset the Z-axis drawing coordinate based on the Y height to create a 3D isometric effect.
                    val heightProjectionFactor = 150f

                    walls.forEach { wall ->
                        val path = Path()
                        // We need to fill the polygon instead of just tracing a stroke to make the walls translucent
                        // and visible against the floorplan.
                        wall.forEachIndexed { index, point ->
                            val normalizedX = (point.x - minX) * baseScale + padding

                            // Project Y onto Z to create a fake 3D depth perception for vertical walls
                            val projectedZOffset = point.y * heightProjectionFactor
                            val normalizedZ = ((point.z - minZ) * baseScale + padding) - projectedZOffset

                            if (index == 0) {
                                path.moveTo(normalizedX, normalizedZ)
                            } else {
                                path.lineTo(normalizedX, normalizedZ)
                            }

                            drawCircle(color = Color.Magenta, radius = 8f, center = Offset(normalizedX, normalizedZ))
                        }
                        path.close()

                        // Fill the wall polygon to make it translucent, rather than just drawing the stroke edge
                        drawPath(path = path, color = Color.Magenta.copy(alpha = 0.2f))
                        // Draw the border stroke on top of the fill
                        drawPath(path = path, color = Color.Magenta, style = Stroke(width = 4f))
                    }
                }

                // 2. Draw Captured Features (Windows/Doors)
                features.forEach { feature ->
                    val normalizedX = (feature.pose.x - minX) * baseScale + padding
                    val normalizedZ = (feature.pose.z - minZ) * baseScale + padding

                    // Draw a green square representing the feature
                    drawRect(
                        color = Color.Green,
                        topLeft = Offset(normalizedX - 15f, normalizedZ - 15f),
                        size = androidx.compose.ui.geometry.Size(30f, 30f)
                    )

                    val textLayoutResult = textMeasurer.measure(
                        text = feature.label,
                        style = TextStyle(fontSize = 12.sp, color = Color.White, background = Color.Black.copy(alpha = 0.5f))
                    )

                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(normalizedX - (textLayoutResult.size.width / 2), normalizedZ + 20f)
                    )
                }

                // 3. Draw Floor Perimeter
                if (filteredFloorPoints.isNotEmpty()) {
                    val path = Path()
                    filteredFloorPoints.forEachIndexed { index, point ->
                        val normalizedX = (point.x - minX) * baseScale + padding
                        val normalizedZ = (point.z - minZ) * baseScale + padding

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

                            val normX1 = (p1.x - minX) * baseScale + padding
                            val normZ1 = (p1.z - minZ) * baseScale + padding
                            val normX2 = (p2.x - minX) * baseScale + padding
                            val normZ2 = (p2.z - minZ) * baseScale + padding

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
