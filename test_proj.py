import math

def project_point_onto_line(px, pz, ax, az, bx, bz):
    # Vector AB
    abx = bx - ax
    abz = bz - az

    # Vector AP
    apx = px - ax
    apz = pz - az

    # Dot product
    dot = apx * abx + apz * abz
    length_sq = abx * abx + abz * abz

    if length_sq == 0:
        return ax, az

    t = dot / length_sq
    t = max(0, min(1, t))

    return ax + t * abx, az + t * abz

# If we have wall segment A to B, and feature point P, we project it onto AB.
