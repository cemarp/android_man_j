with open("app/src/main/java/com/example/floorplan/MainActivity.kt", "r") as f:
    content = f.read()

diff = """
<<<<<<< SEARCH
import io.github.sceneview.ar.node.AnchorNode
import com.google.ar.core.Config
=======
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.SphereNode
import dev.romainguy.kotlin.math.Float3
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import io.github.sceneview.ar.ARSceneView
import com.google.ar.core.Config
>>>>>>> REPLACE
<<<<<<< SEARCH
        var arSurfaceView: SurfaceView? by remember { mutableStateOf(null) }

        ARScene(
=======
        var arSurfaceView: SurfaceView? by remember { mutableStateOf(null) }
        var engine: Engine? by remember { mutableStateOf(null) }

        ARScene(
>>>>>>> REPLACE
<<<<<<< SEARCH
            onViewCreated = {
                // 'this' is the ARSceneView context
                // Cast to android.view.View first, then check if it's a ViewGroup
                val view = this as? android.view.View
                if (view is ViewGroup) {
                    arSurfaceView = findSurfaceView(view)
                } else if (view is SurfaceView) {
                    arSurfaceView = view
                }
            },
=======
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
>>>>>>> REPLACE
"""
print(diff)
