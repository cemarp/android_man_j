with open("app/src/main/java/com/example/floorplan/MainActivity.kt", "r") as f:
    content = f.read()

diff = """
<<<<<<< SEARCH
import androidx.compose.ui.Modifier
=======
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
>>>>>>> REPLACE
"""
with open("patch_bg.patch", "w") as f: f.write(diff)
