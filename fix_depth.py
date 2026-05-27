with open("app/src/main/java/com/example/floorplan/MainActivity.kt", "r") as f:
    content = f.read()

diff = """
<<<<<<< SEARCH
                // Re-enable HORIZONTAL_AND_VERTICAL since hitting the bottom corner often intersects the floor plane.
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL

                // Switch to FIXED focus to prevent "focus hunting" in low light,
                // which often leads to a stuck blurred state on Pixel 7 Pro.
                config.focusMode = Config.FocusMode.FIXED
=======
                // Re-enable HORIZONTAL_AND_VERTICAL since hitting the bottom corner often intersects the floor plane.
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL

                // Enable Depth mode to improve hit testing on featureless walls/ceilings
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    config.depthMode = Config.DepthMode.AUTOMATIC
                }

                // Switch to FIXED focus to prevent "focus hunting" in low light,
                // which often leads to a stuck blurred state on Pixel 7 Pro.
                config.focusMode = Config.FocusMode.FIXED
>>>>>>> REPLACE
"""
print(diff)
