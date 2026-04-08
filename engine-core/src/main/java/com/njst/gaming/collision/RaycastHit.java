package com.njst.gaming.collision;

import com.njst.gaming.Math.Vector3;

public class RaycastHit {
    private final Collider collider;
    private final float distance;
    private final Vector3 point;

    public RaycastHit(Collider collider, float distance, Vector3 point) {
        this.collider = collider;
        this.distance = distance;
        this.point = new Vector3(point);
    }

    public Collider getCollider() {
        return collider;
    }

    public float getDistance() {
        return distance;
    }

    public Vector3 getPoint() {
        return new Vector3(point);
    }
}
