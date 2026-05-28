import re

with open("app/src/main/java/com/example/floorplan/MainActivity.kt", "r") as f:
    content = f.read()

diff = """
<<<<<<< SEARCH
            Button(
                onClick = {
                    // Feature capture needs a reliable 3D point. Windows/Doors often fail Plane hit-tests because they are recessed or reflective.
                    if (arSurfaceView != null) {
                        val surfaceView = arSurfaceView!!

                        var pointToSave = currentHitPoint
                        if (pointToSave == null) {
                            // Don't fall back to the last floor anchor anymore!
                            // Only capture if we actually hit a feature point or plane.
                            Log.w("ARDebug", "No active hit point for feature corner.")
                            return@Button
                        }

                        if (pendingFeaturePose1 == null) {
                            // Captured the first corner
                            pendingFeaturePose1 = pointToSave
                            Log.d("ARDebug", "Captured feature corner 1")
                        } else {
                            // Captured the second corner, now take the picture
                            pendingFeaturePose2 = pointToSave
                            val bitmap = Bitmap.createBitmap(surfaceView.width, surfaceView.height, Bitmap.Config.ARGB_8888)
                            PixelCopy.request(surfaceView, bitmap, { copyResult ->
                                if (copyResult == PixelCopy.SUCCESS) {
                                    pendingFeatureImage = bitmap
                                } else {
                                    Log.e("ARDebug", "Failed to capture pixel copy")
                                }
                            }, Handler(Looper.getMainLooper()))
                        }
                    }
                }
            ) {
                if (pendingFeaturePose1 == null) {
                    Text("📸 Window/Door: Tap Corner 1")
                } else {
                    Text("📸 Window/Door: Tap Corner 2")
                }
            }
=======
            Column(horizontalAlignment = Alignment.End) {
                if (capturedFeatures.isNotEmpty()) {
                    Text(
                        "${capturedFeatures.size} Features Saved",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp).background(Color.Black.copy(alpha=0.5f)).padding(4.dp)
                    )
                }

                Button(
                    onClick = {
                        // Feature capture needs a reliable 3D point. Windows/Doors often fail Plane hit-tests because they are recessed or reflective.
                        if (arSurfaceView != null) {
                            val surfaceView = arSurfaceView!!

                            var pointToSave = currentHitPoint
                            if (pointToSave == null) {
                                // Don't fall back to the last floor anchor anymore!
                                // Only capture if we actually hit a feature point or plane.
                                Log.w("ARDebug", "No active hit point for feature corner.")
                                return@Button
                            }

                            if (pendingFeaturePose1 == null) {
                                // Captured the first corner
                                pendingFeaturePose1 = pointToSave
                                Log.d("ARDebug", "Captured feature corner 1")
                            } else {
                                // Captured the second corner, now take the picture
                                pendingFeaturePose2 = pointToSave
                                val bitmap = Bitmap.createBitmap(surfaceView.width, surfaceView.height, Bitmap.Config.ARGB_8888)
                                PixelCopy.request(surfaceView, bitmap, { copyResult ->
                                    if (copyResult == PixelCopy.SUCCESS) {
                                        pendingFeatureImage = bitmap
                                    } else {
                                        Log.e("ARDebug", "Failed to capture pixel copy")
                                    }
                                }, Handler(Looper.getMainLooper()))
                            }
                        }
                    }
                ) {
                    if (pendingFeaturePose1 == null) {
                        Text("📸 Add Window/Door: Tap Corner 1")
                    } else {
                        Text("📸 Add Window/Door: Tap Corner 2")
                    }
                }

                if (pendingFeaturePose1 != null && pendingFeatureImage == null) {
                    Button(
                        onClick = { pendingFeaturePose1 = null },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cancel Capture")
                    }
                }
            }
>>>>>>> REPLACE
"""
with open("patch_features_ui.patch", "w") as f: f.write(diff)
