with open("AGENT.md", "a") as f:
    f.write("\n\n## 2026-05-27: 3D Walls, 2-Tap Features, and AR Highlights\n")
    f.write("- **Fixed Hit Testing**: Changed ARCore `hitTest` logic from `isPoseInPolygon` to `isPoseInExtents` and enabled `Config.DepthMode.AUTOMATIC`. This fixes the issue where walls and ceilings couldn't be tracked properly.\n")
    f.write("- **AR Feedback Highlights**: Added Filament `SphereNode` to spawn small 3D spheres (radius 5cm) at every successfully tapped AR anchor, giving the user immediate visual confirmation in the AR viewfinder.\n")
    f.write("- **2-Tap Bounding Box for Features**: Updated the window/door capture flow to require tapping two diagonal corners (`pose1` and `pose2`) to define the spatial extent of a feature, allowing realistic bounding box dimensions on walls.\n")
    f.write("- **Isometric 2D Canvas**: Updated `FloorPlanCanvas.kt` to draw vertical walls pointing 'upwards' using the Y coordinates of trackables to simulate an isometric 3D perspective. 2-tap captured features are correctly projected onto these translucent wall planes.\n")
    f.write("- **Branching Strategy**: Migrated changes from `feature/ar-floorplan-3d-walls` and ensured `main` branch holds the latest production-ready code.\n")
