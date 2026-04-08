package com.njst.gaming.collision;

import com.njst.gaming.Math.Vector3;

public interface SphericalHeightmapCollider extends Collider {
    Vector3 getWorldCenter();

    Vector3 worldToLocalPoint(Vector3 worldPoint);

    Vector3 localToWorldPoint(Vector3 localPoint);
}
