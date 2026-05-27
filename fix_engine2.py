with open("app/src/main/java/com/example/floorplan/MainActivity.kt", "r") as f:
    content = f.read()

diff = """
<<<<<<< SEARCH
                        when (currentScanMode) {
                            ScanMode.FLOOR_PERIMETER -> {
                                capturedFloorPoints.add(point)
                            }
                            ScanMode.CEILING_TAP -> {
                                ceilingAnchor = point
                            }
                            ScanMode.TRACE_3D_WALLS -> {
                                currentTracingWall.add(point)
                                if (currentTracingWall.size == 4) {
                                    captured3DWalls.add(currentTracingWall.toList())
                                    currentTracingWall.clear()
                                }
                            }
                        }
                    }
                }
            ) {
=======
                        when (currentScanMode) {
                            ScanMode.FLOOR_PERIMETER -> {
                                capturedFloorPoints.add(point)
                            }
                            ScanMode.CEILING_TAP -> {
                                ceilingAnchor = point
                            }
                            ScanMode.TRACE_3D_WALLS -> {
                                currentTracingWall.add(point)
                                if (currentTracingWall.size == 4) {
                                    captured3DWalls.add(currentTracingWall.toList())
                                    currentTracingWall.clear()
                                }
                            }
                        }

                        // Add a 3D Sphere in AR to highlight the anchor
                        if (engine != null) {
                            // Convert point3d to float3
                            val position = Float3(point.x, point.y, point.z)
                            // Sceneview node
                            val sphereNode = SphereNode(
                                engine = engine!!,
                                radius = 0.05f, // 5cm radius
                                center = Float3(0f, 0f, 0f)
                            ).apply {
                                this.position = position
                            }
                            childNodes.add(sphereNode)
                        }
                    }
                }
            ) {
>>>>>>> REPLACE
"""
print(diff)
