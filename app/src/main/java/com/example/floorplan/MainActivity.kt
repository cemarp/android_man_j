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
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.SphereNode
import dev.romainguy.kotlin.math.Float3
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import io.github.sceneview.ar.ARSceneView
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Point
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
import android.graphics.Bitmap
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.ViewGroup
import android.os.Handler
import android.os.Looper

import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image

import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberMarkerState
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition

// Data models for the different scan types
sealed class RoomScanData {
    abstract val features: List<CapturedFeature>

    data class FloorPerimeter(
        val floorPoints: List<Point3D>,
        var ceilingHeightMeters: Float? = null,
        override val features: List<CapturedFeature> = emptyList()
    ) : RoomScanData()

    data class WallPolygons(
        val walls: List<List<Point3D>>,
        override val features: List<CapturedFeature> = emptyList()
    ) : RoomScanData()
}

data class CapturedFeature(val pose1: Point3D, val pose2: Point3D, val image: Bitmap, val label: String)

fun findSurfaceView(viewGroup: ViewGroup): SurfaceView? {
    for (i in 0 until viewGroup.childCount) {
        val child = viewGroup.getChildAt(i)
        if (child is SurfaceView) {
            return child
        } else if (child is ViewGroup) {
            val result = findSurfaceView(child)
            if (result != null) return result
        }
    }
    return null
}

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
    var isOutdoorMode by remember { mutableStateOf(false) }

    // Store our consolidated scan data instead of just a raw list of points
    var roomScanData by remember { mutableStateOf<RoomScanData?>(null) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    if (isOutdoorMode) {
        OutdoorModeScreen(
            onClose = { isOutdoorMode = false },
            onLaunchARFacadeCapture = {
                isOutdoorMode = false
                isARMode = true
            }
        )
    } else if (isARMode) {
        if (cameraPermissionState.status.isGranted) {
            ARScannerScreen(
                onClose = { isARMode = false },
                onFinishScan = { scanData ->
                    roomScanData = scanData
                    isARMode = false
                    showFloorPlan = true
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

            // Only draw the canvas if we have floor perimeter data
            val currentScan = roomScanData
            if (currentScan is RoomScanData.FloorPerimeter) {
                FloorPlanCanvas(points = currentScan.floorPoints, features = currentScan.features, modifier = Modifier.weight(1f))
                currentScan.ceilingHeightMeters?.let { h ->
                    Text(
                        text = "Estimated Ceiling Height: %.1f ft".format(h * 3.28084),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (currentScan is RoomScanData.WallPolygons) {
                FloorPlanCanvas(points = emptyList(), walls = currentScan.walls, features = currentScan.features, modifier = Modifier.weight(1f))
                Text(
                    text = "3D Wall Polygons Captured: ${currentScan.walls.size} walls.",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = { isARMode = true }) {
                Text("Start AR Room Scan (Indoor)")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { isOutdoorMode = true }) {
                Text("Start Outdoor Mode (Maps + AR)")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* TODO: Polycam Integration */ }) {
                Text("Import Polycam Scan")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { showFloorPlan = true }, enabled = roomScanData != null) {
                Text("View Captured Room Data")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { roomScanData = null }) {
                Text("Clear Data")
            }
        }
    }
}

enum class ScanMode(val title: String) {
    FLOOR_PERIMETER("Floor Perimeter (w/ Default Height)"),
    CEILING_TAP("Ceiling Tap (Height)"),
    TRACE_3D_WALLS("Trace 3D Walls")
}

@Composable
fun OutdoorModeScreen(onClose: () -> Unit, onLaunchARFacadeCapture: () -> Unit) {
    // Example starting coordinate, ideally you'd use FusedLocationProviderClient to get current location
    val startLocation = LatLng(37.4221, -122.0841) // Googleplex
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startLocation, 20f)
    }

    val polygonPoints = remember { mutableStateListOf<LatLng>() }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = MapType.SATELLITE),
            uiSettings = MapUiSettings(zoomControlsEnabled = false),
            onMapClick = { latLng ->
                polygonPoints.add(latLng)
            }
        ) {
            // Draw markers for tapped corners
            polygonPoints.forEach { point ->
                Marker(
                    state = rememberMarkerState(position = point),
                    title = "Corner"
                )
            }

            // Draw perimeter outline connecting the points
            if (polygonPoints.isNotEmpty()) {
                val polylinePoints = polygonPoints.toList() + polygonPoints.first() // Close loop
                Polyline(
                    points = polylinePoints,
                    color = androidx.compose.ui.graphics.Color.Blue,
                    width = 8f
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Outdoor Mode", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. Find your house on the satellite map.\n2. Tap the corners of the roof to trace the footprint.\n3. Launch 'Facade Capture' to measure wall heights with AR.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onClose) {
                Text("Back")
            }
            Button(onClick = { polygonPoints.clear() }) {
                Text("Clear Map")
            }
            Button(onClick = onLaunchARFacadeCapture) {
                Text("Capture Facade Details (AR)")
            }
        }
    }
}

@Composable
fun ARScannerScreen(onClose: () -> Unit, onFinishScan: (RoomScanData) -> Unit) {
    val childNodes = remember { mutableStateListOf<Node>() }
    var currentHitPoint by remember { mutableStateOf<Point3D?>(null) }
    var screenSize by remember { mutableStateOf(IntSize.Zero) }

    var currentScanMode by remember { mutableStateOf(ScanMode.FLOOR_PERIMETER) }
    var scanModeDropdownExpanded by remember { mutableStateOf(false) }

    // State for capturing
    val capturedFloorPoints = remember { mutableStateListOf<Point3D>() }
    var ceilingAnchor by remember { mutableStateOf<Point3D?>(null) }
    val captured3DWalls = remember { mutableStateListOf<List<Point3D>>() }
    val currentTracingWall = remember { mutableStateListOf<Point3D>() }

    // Feature capture state
    var pendingFeatureImage by remember { mutableStateOf<Bitmap?>(null) }
    var pendingFeaturePose1 by remember { mutableStateOf<Point3D?>(null) }
    var pendingFeaturePose2 by remember { mutableStateOf<Point3D?>(null) }
    val capturedFeatures = remember { mutableStateListOf<CapturedFeature>() }

    // State for manual ceiling height input
    var showManualHeightDialog by remember { mutableStateOf(false) }
    var manualHeightInput by remember { mutableStateOf("") }

    val totalAnchorsPlaced = capturedFloorPoints.size + (if (ceilingAnchor != null) 1 else 0) + (captured3DWalls.sumOf { it.size } + currentTracingWall.size)

    // Debug instrumentation states
    var debugTrackingState by remember { mutableStateOf("UNKNOWN") }
    var debugHitStatus by remember { mutableStateOf("No Hit") }
    var debugPlanesCount by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { screenSize = it }
    ) {
        var arSurfaceView: SurfaceView? by remember { mutableStateOf(null) }
        var engine: Engine? by remember { mutableStateOf(null) }

        ARScene(
            modifier = Modifier.fillMaxSize(),
            childNodes = childNodes,
            planeRenderer = true,
            sessionConfiguration = { session, config ->
                // Re-enable HORIZONTAL_AND_VERTICAL since hitting the bottom corner often intersects the floor plane.
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL

                // Enable Depth mode to improve hit testing on featureless walls/ceilings
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    config.depthMode = Config.DepthMode.AUTOMATIC
                }

                // Switch to FIXED focus to prevent "focus hunting" in low light,
                // which often leads to a stuck blurred state on Pixel 7 Pro.
                config.focusMode = Config.FocusMode.FIXED

                // Configure ARCore to prefer the ultra-wide lens or lowest available focal length
                try {
                    val filter = CameraConfigFilter(session)
                    val cameraConfigs = session.getSupportedCameraConfigs(filter)
                    // Sorting by lowest focal length or just picking the first one (often the widest if sorted)
                    val wideConfig = cameraConfigs.maxByOrNull { it.imageSize.width }
                    if (wideConfig != null) {
                        session.cameraConfig = wideConfig
                    }
                } catch (e: Exception) {
                    Log.e("ARDebug", "Failed to configure wide camera: ${e.message}")
                }
            },
            onViewCreated = {
                // 'this' is the ARSceneView context
                // Cast to android.view.View first, then check if it's a ViewGroup
                val view = this as? android.view.View
                if (view is ViewGroup) {
                    arSurfaceView = findSurfaceView(view)
                } else if (view is SurfaceView) {
                    arSurfaceView = view
                }

                // Get Filament Engine to create spheres later
                val arSceneView = this as? ARSceneView
                engine = arSceneView?.engine
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
                            // Use isPoseInExtents instead of isPoseInPolygon to be more forgiving for walls and ceilings
                            // where the fully detected polygon might be jagged or incomplete.
                            if (trackable.isPoseInExtents(hit.hitPose)) {
                                currentHitPoint = Point3D(hit.hitPose.tx(), hit.hitPose.ty(), hit.hitPose.tz())
                                debugHitStatus = "Hit: Plane (${trackable.type.name})"
                                foundHit = true
                                Log.d("ARDebug", "Hit Success on Plane Type: ${trackable.type.name} at ${currentHitPoint}")
                                break
                            } else {
                                Log.d("ARDebug", "Hit discarded: Ray cast hit a plane bounding box but pose is outside extents.")
                            }
                        } else if (trackable is com.google.ar.core.Point || trackable is com.google.ar.core.DepthPoint) {
                            // Accept raw feature points or depth points. This is crucial for capturing windows/doors
                            // because glass and recesses often fail strict Plane detection.
                            currentHitPoint = Point3D(hit.hitPose.tx(), hit.hitPose.ty(), hit.hitPose.tz())
                            debugHitStatus = "Hit: Raw Point (${trackable.javaClass.simpleName})"
                            foundHit = true
                            Log.d("ARDebug", "Hit Success on Raw Point Type: ${trackable.javaClass.simpleName} at ${currentHitPoint}")
                            break
                        } else {
                            Log.d("ARDebug", "Hit discarded: Trackable is not a Plane or Point (it is ${trackable.javaClass.simpleName})")
                        }
                    }
                    if (!foundHit) {
                        currentHitPoint = null
                        debugHitStatus = if (hitResults.isEmpty()) "Hit: None" else "Hit: Outside Polygon/Not Point"
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

        // Top Right: Scanning Options Dropdown
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Button(onClick = { scanModeDropdownExpanded = true }) {
                Text("Mode: ${currentScanMode.title}")
            }
            DropdownMenu(
                expanded = scanModeDropdownExpanded,
                onDismissRequest = { scanModeDropdownExpanded = false }
            ) {
                ScanMode.values().forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.title) },
                        onClick = {
                            currentScanMode = mode
                            scanModeDropdownExpanded = false
                        }
                    )
                }
            }
        }

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
                        text = when (currentScanMode) {
                            ScanMode.FLOOR_PERIMETER -> "1. Point camera at walls.\n2. Aim '+' at the BOTTOM corners.\n3. Tap 'Add Anchor' sequentially."
                            ScanMode.CEILING_TAP -> "1. Point camera at the ceiling.\n2. Aim '+' at a flat point.\n3. Tap to capture ceiling height."
                            ScanMode.TRACE_3D_WALLS -> "1. Trace an individual wall.\n2. Tap all 4 corners (Bottom-Left, Bottom-Right, Top-Right, Top-Left)."
                        },
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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

            Button(
                onClick = {
                    currentHitPoint?.let { point ->
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
                Text(
                    text = when (currentScanMode) {
                        ScanMode.CEILING_TAP -> if (ceilingAnchor == null) "Capture Ceiling Height" else "Retake Ceiling Height"
                        else -> "Add Anchor"
                    }
                )
            }
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

            // Validation for finishing a scan based on the active mode
            val canFinish = capturedFloorPoints.size >= 3 || captured3DWalls.isNotEmpty()

            if (canFinish) {
                Button(onClick = {
                    if (captured3DWalls.isNotEmpty()) {
                        onFinishScan(RoomScanData.WallPolygons(captured3DWalls.toList(), features = capturedFeatures.toList()))
                    } else {
                        // Floor perimeter mode logic
                        if (ceilingAnchor != null && capturedFloorPoints.isNotEmpty()) {
                            // Combine floor and ceiling taps
                            val avgFloorY = capturedFloorPoints.map { it.y }.average().toFloat()
                            val heightMeters = Math.abs((ceilingAnchor?.y ?: 0f) - avgFloorY)
                            onFinishScan(RoomScanData.FloorPerimeter(capturedFloorPoints.toList(), ceilingHeightMeters = heightMeters, features = capturedFeatures.toList()))
                        } else {
                            // Ask for manual input
                            showManualHeightDialog = true
                        }
                    }
                }) {
                    Text("Finish Scan")
                }
            }
        }

        Surface(
            color = Color.White.copy(alpha = 0.7f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(32.dp)
        ) {
            Text(
                text = "Anchors placed: $totalAnchorsPlaced",
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        // Render dialogs on top of ARScene to prevent unmounting and session resets
        if (showManualHeightDialog) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Surface(shape = MaterialTheme.shapes.medium, color = Color.White) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Enter Room Height (Feet)", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = manualHeightInput,
                            onValueChange = { manualHeightInput = it },
                            label = { Text("e.g. 8.5") }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row {
                            Button(onClick = {
                                showManualHeightDialog = false
                            }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(onClick = {
                                val heightFeet = manualHeightInput.toFloatOrNull() ?: 8.0f
                                val heightMeters = heightFeet / 3.28084f
                                val finalData = RoomScanData.FloorPerimeter(capturedFloorPoints.toList(), ceilingHeightMeters = heightMeters, features = capturedFeatures.toList())
                                onFinishScan(finalData)
                                showManualHeightDialog = false
                            }) {
                                Text("Save & Finish")
                            }
                        }
                    }
                }
            }
        }

        if (pendingFeatureImage != null && pendingFeaturePose1 != null && pendingFeaturePose2 != null) {
            var labelText by remember { mutableStateOf("") }
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Surface(shape = MaterialTheme.shapes.medium, color = Color.White) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            bitmap = pendingFeatureImage!!.asImageBitmap(),
                            contentDescription = "Captured Feature",
                            modifier = Modifier.height(200.dp).fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = labelText,
                            onValueChange = { labelText = it },
                            label = { Text("Label (e.g. Window, Door)") }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row {
                            Button(onClick = {
                                pendingFeatureImage = null
                                pendingFeaturePose1 = null
                                pendingFeaturePose2 = null
                            }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(onClick = {
                                capturedFeatures.add(CapturedFeature(pendingFeaturePose1!!, pendingFeaturePose2!!, pendingFeatureImage!!, labelText))
                                pendingFeatureImage = null
                                pendingFeaturePose1 = null
                                pendingFeaturePose2 = null
                            }) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }
}
