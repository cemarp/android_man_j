# Floorplan and Manual J App

## Goal
Build an Android app which uses the phone's camera and other sensors (ARCore) to create an accurate floorplan of a home from videos/pictures with reasonably accurate dimensions, and notes where windows/doors/skylights are. Use this data to generate a manual J heating/cooling load calculation.
The app should support:
- On-device real-time AR room scanning.
- Polycam integration (utilizing free-tier accessible exports).
- Multi-room stitching (continuous scan + joining separate scans).
- Automatic and manual feature tagging/sizing.
- Manual J data override for aspects like insulation, roofing, etc.
- Wall scanning capability (crucial for accurate Manual J load calculations).

## Technical Approach
- Native Android app using Kotlin and Jetpack Compose.
- ARCore for on-device room scanning and tracking (horizontal and vertical planes). *Experimental update: restricted strictly to vertical planes to improve wall-boundary targeting accuracy.*
- Modular architecture to support both real-time scanning and Polycam import workflows.
- Feature Capture Architecture: Windows and doors are captured by aiming the AR camera at the feature and saving the exact 3D camera pose (and an image frame). In a later step, the user draws a bounding box on the 2D image, which the app projects onto the 3D wall plane to calculate real-world dimensions for the Manual J workflow.
- **Outdoor Mode Strategy**: ARCore struggles with large-scale outdoor tracking due to drift, sunlight, and depth limitations. The app uses a hybrid approach for exterior envelopes:
  1. **Google Maps SDK** (Satellite View) is used to trace the 2D physical footprint of the building.
  2. **ARCore** is used in targeted, short-range sessions ("Facade Capture") to measure vertical heights and tag exterior windows/doors on individual walls.

## Known Issues / Next Steps
- Currently refining the AR room scanning MVP. Wall boundaries are captured using a 3D coordinate system and flattened to 2D for drawing the floorplan, which helps merge vertical taps.

## Debugging Guide for Agents (Device Debugging)
If you need to debug AR functionality or crashes while the user is running the app on a physical device connected via Android Studio (or USB debugging), instruct the user to run the following `adb` commands to extract logs. You can then use this data to troubleshoot:

1.  **Capture Custom AR Instrumentation Logs:**
    The app uses the `ARDebug` tag to broadcast real-time hit-test statuses, plane tracking counts, and coordinate data.
    `adb logcat -d -s ARDebug > ar_debug_log.txt`

2.  **Capture Crash/Fatal Logs:**
    To get stacktraces of any crashes:
    `adb logcat -d AndroidRuntime:E *:S > crash_log.txt`

3.  **Capture Screenshots (if visual bugs occur):**
    `adb shell screencap -p /sdcard/screen.png`
    `adb pull /sdcard/screen.png .`

4.  **Capture Screen Recordings (for AR tracking analysis):**
    `adb shell screenrecord --time-limit 10 /sdcard/ar_record.mp4`
    `adb pull /sdcard/ar_record.mp4 .`

When switching agents, you should read these outputs (if provided by the user) to understand exactly why a raycast failed (e.g. `Hit discarded: Trackable is not a Plane`) or why a tracking state degraded.

## Critical Dependency Constraints
- **Kotlin 2.2.10 + Coroutines 1.11.0**: The project uses Kotlin 2.2.10. It **REQUIRES** `kotlinx-coroutines` version **1.11.0** or higher. Using older versions will cause a `java.lang.NoClassDefFoundError: Lkotlin/coroutines/jvm/internal/SpillingKt` crash when interacting with the `FloorPlanCanvas` (specifically during pan/zoom gestures). **Do not downgrade these versions.**
- **Android 15 (SDK 35)**: Ensure `compileSdk` and `targetSdk` remain at 35 to support 16 KB page size compatibility and latest platform features.

## Context / Decisions
- Built from scratch targeting modern Android devices (Pixel 7 onwards).
- Emphasizing AR scanning first, followed by manual J logic.
- Relying on local on-device capabilities for AR scanning and allowing user overrides for automatic feature tagging.
- Set ARCore Focus Mode to `FIXED` to prevent the camera from getting stuck in a blurred state during low-light sessions (specifically observed on Pixel 7 Pro).
- Using `android:extractNativeLibs="true"` to bypass 16kb page size compatibility issues for legacy native libraries like Filament and ARCore.


## 2026-05-27: 3D Walls, 2-Tap Features, and AR Highlights
- **Fixed Hit Testing**: Changed ARCore `hitTest` logic from `isPoseInPolygon` to `isPoseInExtents` and enabled `Config.DepthMode.AUTOMATIC`. This fixes the issue where walls and ceilings couldn't be tracked properly.
- **AR Feedback Highlights**: Added Filament `SphereNode` to spawn small 3D spheres (radius 5cm) at every successfully tapped AR anchor, giving the user immediate visual confirmation in the AR viewfinder.
- **2-Tap Bounding Box for Features**: Updated the window/door capture flow to require tapping two diagonal corners (`pose1` and `pose2`) to define the spatial extent of a feature, allowing realistic bounding box dimensions on walls.
- **Isometric 2D Canvas**: Updated `FloorPlanCanvas.kt` to draw vertical walls pointing 'upwards' using the Y coordinates of trackables to simulate an isometric 3D perspective. 2-tap captured features are correctly projected onto these translucent wall planes.
- **Branching Strategy**: Migrated changes from `feature/ar-floorplan-3d-walls` and ensured `main` branch holds the latest production-ready code.

## 3D Sceneview Migration
Migrated the `FloorPlanCanvas` to a full `FloorPlan3DScene` using Filament (`io.github.sceneview`). The UI now renders actual 3D nodes (`CubeNode`) for walls and features. Replaced the 2D pseudo-3D isometric projection with a true 3D orbital viewer. Used `rememberEngine()` to persist the 3D context safely inside the composable and configured Euler rotations for `CubeNode` alignments via `Quaternion.fromEuler`.
