with open("app/src/main/java/com/example/floorplan/MainActivity.kt", "r") as f:
    content = f.read()

diff = """
<<<<<<< SEARCH
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
                        } else if (trackable is com.google.ar.core.Point || trackable is com.google.ar.core.DepthPoint) {
=======
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
>>>>>>> REPLACE
"""

import sys
print(diff)
