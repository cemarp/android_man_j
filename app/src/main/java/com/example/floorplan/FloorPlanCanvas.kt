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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculateRotation

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
        features.forEach {
            allPoints.add(it.pose1)
            allPoints.add(it.pose2)
        }

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
                    awaitEachGesture {
                        awaitFirstDown()
                        do {
                            val event = awaitPointerEvent()
                            val pointers = event.changes

                            if (pointers.isNotEmpty()) {
                                val zoomChange = event.calculateZoom()
                                val rotationChange = event.calculateRotation()
                                val panChange = event.calculatePan()

                                scale *= zoomChange
                                rotationZ += rotationChange

                                // If exactly 2 fingers are down, we apply the pan to tilt (X rotation)
                                // If 1 finger is down, we apply pan to translation offset
                                if (pointers.size >= 2) {
                                    // Per user request, horizontal (or vertical) 2-finger panning without pinch/rotation triggers tilt
                                    if (zoomChange in 0.95f..1.05f && Math.abs(rotationChange) < 5f) {
                                        // Use whichever axis is moved more to dictate the tilt amount
                                        val tiltMagnitude = if (Math.abs(panChange.x) > Math.abs(panChange.y)) panChange.x else -panChange.y
                                        rotationX += tiltMagnitude * 0.5f
                                    } else {
                                        offset += panChange
                                    }
                                } else {
                                    offset += panChange
                                }

                                pointers.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
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
                            // Project Y onto Z to create a fake 3D depth perception for vertical walls
                            val projectedZOffset = point.y * heightProjectionFactor

                            val nx = size.width / 2f + ((point.x - minX) * baseScale + padding - (maxX - minX) * baseScale / 2)
                            val nz = size.height / 2f - ((point.z - minZ) * baseScale + padding - (maxZ - minZ) * baseScale / 2) - projectedZOffset

                            if (index == 0) {
                                path.moveTo(nx, nz)
                            } else {
                                path.lineTo(nx, nz)
                            }

                            drawCircle(color = Color.Magenta, radius = 8f, center = Offset(nx, nz))
                        }
                        path.close()

                        // Fill the wall polygon to make it translucent, rather than just drawing the stroke edge
                        drawPath(path = path, color = Color.Magenta.copy(alpha = 0.2f))
                        // Draw the border stroke on top of the fill
                        drawPath(path = path, color = Color.Magenta, style = Stroke(width = 4f))
                    }
                }

                // Render Pseudo-3D Isometric Walls (approx 20 degrees tilted up)
                val wallHeight = 120f // pixels representing ceiling height

                // For standard floor perimeter, we draw walls upwards from the perimeter lines
                if (filteredFloorPoints.size > 1) {
                    for (i in 0 until filteredFloorPoints.size) {
                        val p1 = filteredFloorPoints[i]
                        val p2 = filteredFloorPoints[(i + 1) % filteredFloorPoints.size]

                        val x1 = size.width / 2f + ((p1.x - minX) * baseScale + padding - (maxX - minX) * baseScale / 2)
                        val z1 = size.height / 2f - ((p1.z - minZ) * baseScale + padding - (maxZ - minZ) * baseScale / 2)
                        val x2 = size.width / 2f + ((p2.x - minX) * baseScale + padding - (maxX - minX) * baseScale / 2)
                        val z2 = size.height / 2f - ((p2.z - minZ) * baseScale + padding - (maxZ - minZ) * baseScale / 2)

                        // Base points
                        val b1 = Offset(x1, z1)
                        val b2 = Offset(x2, z2)

                        // Top points (projected straight up to simulate a wall standing up in an isometric view)
                        val t1 = Offset(x1, z1 - wallHeight)
                        val t2 = Offset(x2, z2 - wallHeight)

                        // Create path for the wall plane
                        val wallPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(b1.x, b1.y)
                            lineTo(b2.x, b2.y)
                            lineTo(t2.x, t2.y)
                            lineTo(t1.x, t1.y)
                            close()
                        }

                        // Fill the wall with a semi-transparent color
                        drawPath(
                            path = wallPath,
                            color = Color(0xFFFF69B4).copy(alpha = 0.4f) // Hot pink semi-transparent
                        )
                        // Stroke the wall edges
                        drawPath(
                            path = wallPath,
                            color = Color(0xFFFF1493), // Deep pink
                            style = Stroke(width = 2f)
                        )
                    }
                }

                // 2. Draw Captured Features (Windows/Doors) projected onto the walls
                features.forEach { feature ->
                    val heightProjectionFactor = 150f
                    // If they tapped top and bottom corners, y will differ.
                    // Let's project pose1 and pose2 straight up based on their y coordinate relative to the floor.
                    val projectedZOffset1 = feature.pose1.y * heightProjectionFactor
                    val projectedZOffset2 = feature.pose2.y * heightProjectionFactor

                    val normalizedX1 = size.width / 2f + (((feature.pose1.x - minX) * baseScale + padding - (maxX - minX) * baseScale / 2))
                    val normalizedZ1 = size.height / 2f - (((feature.pose1.z - minZ) * baseScale + padding - (maxZ - minZ) * baseScale / 2)) - projectedZOffset1

                    val normalizedX2 = size.width / 2f + (((feature.pose2.x - minX) * baseScale + padding - (maxX - minX) * baseScale / 2))
                    val normalizedZ2 = size.height / 2f - (((feature.pose2.z - minZ) * baseScale + padding - (maxZ - minZ) * baseScale / 2)) - projectedZOffset2

                    // Draw a green filled rectangle representing the window on the wall plane
                    val windowPath = androidx.compose.ui.graphics.Path().apply {
                        // Assuming pose1 is bottom-left and pose2 is top-right, or diagonal.
                        // For a simple wall projection, we draw a box between the two coordinates
                        moveTo(normalizedX1, normalizedZ1)
                        lineTo(normalizedX2, normalizedZ1)
                        lineTo(normalizedX2, normalizedZ2)
                        lineTo(normalizedX1, normalizedZ2)
                        close()
                    }

                    drawPath(
                        path = windowPath,
                        color = Color.Green.copy(alpha = 0.7f)
                    )
                    drawPath(
                        path = windowPath,
                        color = Color.Green,
                        style = Stroke(width = 3f)
                    )

                    val textLayoutResult = textMeasurer.measure(
                        text = feature.label,
                        style = TextStyle(fontSize = 12.sp, color = Color.White, background = Color.Black.copy(alpha = 0.5f))
                    )

                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(normalizedX1, normalizedZ2 - 20f)
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
