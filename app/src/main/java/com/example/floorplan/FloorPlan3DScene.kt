package com.example.floorplan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.Scene
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
fun FloorPlan3DScene(
    scanData: RoomScanData,
    onClose: () -> Unit
) {
    val engine = rememberEngine()
    val nodes = remember { mutableStateListOf<Node>() }

    LaunchedEffect(scanData, engine) {
        nodes.clear()

        val ceilingHeight = when (scanData) {
            is RoomScanData.FloorPerimeter -> scanData.ceilingHeightMeters ?: 2.5f
            else -> 2.5f
        }
        val features = when (scanData) {
            is RoomScanData.FloorPerimeter -> scanData.features
            is RoomScanData.WallPolygons -> scanData.features
        }

        if (scanData is RoomScanData.FloorPerimeter) {
            val floorPoints = scanData.floorPoints
            if (floorPoints.size > 1) {
                for (i in floorPoints.indices) {
                    val p1 = floorPoints[i]
                    val p2 = floorPoints[(i + 1) % floorPoints.size]

                    val dx = p2.x - p1.x
                    val dz = p2.z - p1.z
                    val length = sqrt(dx * dx + dz * dz)

                    val midX = (p1.x + p2.x) / 2f
                    val midZ = (p1.z + p2.z) / 2f
                    val midY = ceilingHeight / 2f

                    val angleRad = atan2(dx, dz)
                    val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()

                    val wallNode = CubeNode(
                        engine = engine,
                        size = Float3(length, ceilingHeight, 0.1f),
                        center = Float3(0f, 0f, 0f)
                    ).apply {
                        position = Float3(midX, midY, midZ)
                        quaternion = Quaternion.fromEuler(Float3(0f, angleDeg, 0f))
                    }
                    nodes.add(wallNode)
                }
            }
        } else if (scanData is RoomScanData.WallPolygons) {
            // Calculate a proper solid plane for each wall polygon
            scanData.walls.forEach { wall ->
                if (wall.size > 2) {
                    // Approximate the wall as a large rect based on bounding box
                    // Find min/max Y for height and min/max X/Z for length
                    val minX = wall.minOf { it.x }
                    val maxX = wall.maxOf { it.x }
                    val minY = wall.minOf { it.y }
                    val maxY = wall.maxOf { it.y }
                    val minZ = wall.minOf { it.z }
                    val maxZ = wall.maxOf { it.z }

                    val dx = maxX - minX
                    val dz = maxZ - minZ
                    val length = maxOf(0.1f, sqrt(dx * dx + dz * dz))
                    val height = maxOf(0.1f, maxY - minY)

                    val midX = (minX + maxX) / 2f
                    val midY = (minY + maxY) / 2f
                    val midZ = (minZ + maxZ) / 2f

                    // We can estimate the rotation by looking at the bottom 2 points if ordered properly,
                    // but for a general bounding box we can use the max span vector
                    var maxDist = 0f
                    var pA = wall[0]
                    var pB = wall[1]
                    for (i in 0 until wall.size) {
                        for (j in i+1 until wall.size) {
                            val dist = sqrt((wall[j].x - wall[i].x) * (wall[j].x - wall[i].x) + (wall[j].z - wall[i].z) * (wall[j].z - wall[i].z))
                            if (dist > maxDist) {
                                maxDist = dist
                                pA = wall[i]
                                pB = wall[j]
                            }
                        }
                    }
                    val angleRad = atan2(pB.x - pA.x, pB.z - pA.z)
                    val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()

                    val wallNode = CubeNode(
                        engine = engine,
                        size = Float3(length, height, 0.1f),
                        center = Float3(0f, 0f, 0f)
                    ).apply {
                        position = Float3(midX, midY, midZ)
                        quaternion = Quaternion.fromEuler(Float3(0f, angleDeg, 0f))
                    }
                    nodes.add(wallNode)
                }
            }
        }

        // Create Features
        features.forEach { feature ->
            val minX = minOf(feature.pose1.x, feature.pose2.x)
            val maxX = maxOf(feature.pose1.x, feature.pose2.x)
            val minY = minOf(feature.pose1.y, feature.pose2.y)
            val maxY = maxOf(feature.pose1.y, feature.pose2.y)
            val minZ = minOf(feature.pose1.z, feature.pose2.z)
            val maxZ = maxOf(feature.pose1.z, feature.pose2.z)

            val height = maxOf(0.05f, maxY - minY)

            val midX = (minX + maxX) / 2f
            val midY = (minY + maxY) / 2f
            val midZ = (minZ + maxZ) / 2f

            val dx = feature.pose2.x - feature.pose1.x
            val dz = feature.pose2.z - feature.pose1.z
            val length = maxOf(0.05f, sqrt(dx * dx + dz * dz))
            val angleRad = atan2(dx, dz)
            val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()

            val featureNode = CubeNode(
                engine = engine,
                size = Float3(length, height, 0.15f),
                center = Float3(0f, 0f, 0f)
            ).apply {
                position = Float3(midX, midY, midZ)
                quaternion = Quaternion.fromEuler(Float3(0f, angleDeg, 0f))
            }
            nodes.add(featureNode)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            childNodes = nodes
        )

        Button(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text("Close 3D View")
        }
    }
}
