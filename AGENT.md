# Floorplan and Manual J App

## Goal
Build an Android app which uses the phone's camera and other sensors (ARCore) to create an accurate floorplan of a home from videos/pictures with reasonably accurate dimensions, and notes where windows/doors/skylights are. Use this data to generate a manual J heating/cooling load calculation.
The app should support:
- On-device real-time AR room scanning.
- Polycam integration (utilizing free-tier accessible exports).
- Multi-room stitching (continuous scan + joining separate scans).
- Automatic and manual feature tagging/sizing.
- Manual J data override for aspects like insulation, roofing, etc.

## Technical Approach
- Native Android app using Kotlin and Jetpack Compose.
- ARCore for on-device room scanning and tracking.
- Modular architecture to support both real-time scanning and Polycam import workflows.

## Known Issues / Next Steps
- Currently initializing the project and building the basic AR room scanning MVP.

## Context / Decisions
- Built from scratch targeting modern Android devices (Pixel 7 onwards).
- Updated to target Android 15 (API 35) and added support for 16 KB memory page sizes to ensure compatibility with Pixel 7+ devices. This included upgrading ARCore to 1.54.0 and enabling `pageSizeCompat` in the manifest.
- Emphasizing AR scanning first, followed by manual J logic.
- Relying on local on-device capabilities for AR scanning and allowing user overrides for automatic feature tagging.
