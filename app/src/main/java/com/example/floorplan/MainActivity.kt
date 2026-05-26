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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

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
    val anchorPoints = remember { mutableStateListOf<Point2D>() }
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
fun ARScannerScreen(onClose: () -> Unit, onFinishScan: () -> Unit, onAnchorPlaced: (Point2D) -> Unit) {
    val childNodes = remember { mutableStateListOf<Node>() }
    var anchorsCount by remember { mutableStateOf(0) }
    var currentHitPoint by remember { mutableStateOf<Point2D?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth.value
        val screenHeight = maxHeight.value

        ARScene(
            modifier = Modifier.fillMaxSize(),
            childNodes = childNodes,
            planeRenderer = true,
            sessionConfiguration = { session, config ->
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                config.focusMode = Config.FocusMode.AUTO
            },
            onSessionUpdated = { session, frame ->
                val camera = frame.camera
                if (camera.trackingState == TrackingState.TRACKING) {
                    // Use center of screen coordinates for hit testing
                    val hitResults = frame.hitTest(screenWidth / 2f, screenHeight / 2f)
                    for (hit in hitResults) {
                        val trackable = hit.trackable
                        if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                            currentHitPoint = Point2D(hit.hitPose.tx(), hit.hitPose.tz())
                            break
                        }
                    }
                }
            },
            onTrackingFailureChanged = { trackingFailureReason ->
                // Handle tracking failure
            }
        )

        // Custom crosshair / Instructions overlay
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Scan vertical walls and floors.\nAim at corners to place anchors.",
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 64.dp)
                )
                Text("+", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
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
