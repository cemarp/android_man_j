package com.example.floorplan

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.AnchorNode
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import io.github.sceneview.node.Node
import io.github.sceneview.collision.HitResult
import android.Manifest
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Color
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    var isARMode by remember { mutableStateOf(false) }
    var showFloorPlan by remember { mutableStateOf(false) }
    val anchorPoints = remember { mutableStateListOf<Point3D>() }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    if (isARMode) {
        if (cameraPermissionState.status.isGranted) {
            ARScannerScreen(
                onClose = { isARMode = false },
                onFinishScan = {
                    isARMode = false
                    showFloorPlan = true
                },
                onAnchorPlaced = { point ->
                    anchorPoints.add(point)
                }
            )
        } else {
            LaunchedEffect(Unit) {
                cameraPermissionState.launchPermissionRequest()
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Camera permission is required for AR scanning.")
                Button(onClick = { isARMode = false }, modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)) {
                    Text("Back")
                }
            }
        }
    } else if (showFloorPlan) {
        Column(modifier = Modifier.fillMaxSize()) {
            Button(
                onClick = { showFloorPlan = false },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Back to Menu")
            }
            FloorPlanCanvas(points = anchorPoints, modifier = Modifier.weight(1f))
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = { isARMode = true }) {
                Text("Start AR Room Scan")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* TODO: Polycam Integration */ }) {
                Text("Import Polycam Scan")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { showFloorPlan = true }) {
                Text("View 2D Floor Plan (${anchorPoints.size} points)")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { anchorPoints.clear() }) {
                Text("Clear Data")
            }
        }
    }
}

@Composable
fun ARScannerScreen(onClose: () -> Unit, onFinishScan: () -> Unit, onAnchorPlaced: (Point3D) -> Unit) {
    val childNodes = remember { mutableStateListOf<Node>() }
    var anchorsCount by remember { mutableStateOf(0) }
    var currentHitPoint by remember { mutableStateOf<Point3D?>(null) }
    var screenSize by remember { mutableStateOf(IntSize.Zero) }

    // Debug instrumentation states
    var debugTrackingState by remember { mutableStateOf("UNKNOWN") }
    var debugHitStatus by remember { mutableStateOf("No Hit") }
    var debugPlanesCount by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { screenSize = it }
    ) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            childNodes = childNodes,
            planeRenderer = true,
            sessionConfiguration = { session, config ->
                // Re-enable HORIZONTAL_AND_VERTICAL since hitting the bottom corner often intersects the floor plane.
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                config.focusMode = Config.FocusMode.AUTO
            },
            onSessionUpdated = { session, frame ->
                val camera = frame.camera
                debugTrackingState = camera.trackingState.name

                val planes = session.getAllTrackables(Plane::class.java)
                debugPlanesCount = planes.size

                Log.d("ARDebug", "Camera Tracking: ${camera.trackingState.name}, Planes detected: ${planes.size}")

                if (camera.trackingState == TrackingState.TRACKING && screenSize != IntSize.Zero) {
                    // Use center of screen coordinates in raw pixels for accurate hit testing
                    val centerX = screenSize.width / 2f
                    val centerY = screenSize.height / 2f

                    val hitResults = frame.hitTest(centerX, centerY)

                    var foundHit = false
                    for (hit in hitResults) {
                        val trackable = hit.trackable
                        if (trackable is Plane) {
                            if (trackable.isPoseInPolygon(hit.hitPose)) {
                                currentHitPoint = Point3D(hit.hitPose.tx(), hit.hitPose.ty(), hit.hitPose.tz())
                                debugHitStatus = "Hit: Plane (${trackable.type.name})"
                                foundHit = true
                                Log.d("ARDebug", "Hit Success on Plane Type: ${trackable.type.name} at ${currentHitPoint}")
                                break
                            } else {
                                Log.d("ARDebug", "Hit discarded: Ray cast hit a plane bounding box but pose is outside polygon.")
                            }
                        } else {
                            Log.d("ARDebug", "Hit discarded: Trackable is not a Plane (it is ${trackable.javaClass.simpleName})")
                        }
                    }
                    if (!foundHit) {
                        currentHitPoint = null
                        debugHitStatus = if (hitResults.isEmpty()) "Hit: None" else "Hit: Outside Polygon/Not Plane"
                    }
                } else {
                    currentHitPoint = null
                    debugHitStatus = "Not Tracking"
                }
            },
            onTrackingFailureChanged = { trackingFailureReason ->
                // Handle tracking failure
            }
        )

        // Custom crosshair / Instructions overlay
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp, start = 16.dp, end = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "How to scan walls:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Point camera at walls.\n2. Aim the '+' exactly at the BOTTOM corners where the wall meets the floor.\n3. Tap 'Add Anchor' sequentially around the room.",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Crosshair mapping to the exact center of the screen
            Text(
                text = "+",
                style = MaterialTheme.typography.headlineLarge,
                color = if (currentHitPoint != null) Color.Green else Color.Red,
                modifier = Modifier.align(Alignment.Center)
            )

            // Debug Overlay
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("DEBUG INFO", color = Color.Yellow, style = MaterialTheme.typography.labelSmall)
                    Text("Tracking: $debugTrackingState", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    Text("Planes: $debugPlanesCount", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    Text("HitTest: $debugHitStatus", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Button(
            onClick = {
                currentHitPoint?.let { point ->
                    onAnchorPlaced(point)
                    anchorsCount++
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
        ) {
            Text("Add Anchor")
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onClose) {
                Text("Cancel")
            }
            if (anchorsCount >= 2) {
                Button(onClick = onFinishScan) {
                    Text("Finish Scan")
                }
            }
        }

        Text(
            text = "Anchors placed: $anchorsCount",
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(32.dp)
        )
    }
}
