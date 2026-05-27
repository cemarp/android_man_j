with open("app/src/main/java/com/example/floorplan/FloorPlanCanvas.kt", "r") as f:
    content = f.read()

diff = """
<<<<<<< SEARCH
        features.forEach { allPoints.add(it.pose) }
=======
        features.forEach {
            allPoints.add(it.pose1)
            allPoints.add(it.pose2)
        }
>>>>>>> REPLACE
<<<<<<< SEARCH
                // 2. Draw Captured Features (Windows/Doors)
                features.forEach { feature ->
                    // Apply the same 3D projection logic to features so they map correctly onto the tilted 3D walls
                    val heightProjectionFactor = 150f
                    val projectedZOffset = feature.pose.y * heightProjectionFactor

                    val normalizedX = (feature.pose.x - minX) * baseScale + padding
                    val normalizedZ = ((feature.pose.z - minZ) * baseScale + padding) - projectedZOffset

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
=======
                // 2. Draw Captured Features (Windows/Doors)
                features.forEach { feature ->
                    val heightProjectionFactor = 150f
                    val projectedZOffset1 = feature.pose1.y * heightProjectionFactor

                    val normalizedX1 = (feature.pose1.x - minX) * baseScale + padding
                    val normalizedZ1 = ((feature.pose1.z - minZ) * baseScale + padding) - projectedZOffset1

                    // Draw a green square representing the feature
                    drawRect(
                        color = Color.Green,
                        topLeft = Offset(normalizedX1 - 15f, normalizedZ1 - 15f),
                        size = androidx.compose.ui.geometry.Size(30f, 30f)
                    )

                    val textLayoutResult = textMeasurer.measure(
                        text = feature.label,
                        style = TextStyle(fontSize = 12.sp, color = Color.White, background = Color.Black.copy(alpha = 0.5f))
                    )

                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(normalizedX1 - (textLayoutResult.size.width / 2), normalizedZ1 + 20f)
                    )
                }
>>>>>>> REPLACE
"""
with open("patch_canvas.patch", "w") as f: f.write(diff)
