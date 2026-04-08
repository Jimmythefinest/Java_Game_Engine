package com.njst.gaming.collision;

import com.njst.gaming.Math.Vector3;

public class AabbShape implements CollisionShape {
    public static final String TYPE_ID = "aabb";

    private final Vector3 localMin;
    private final Vector3 localMax;

    public AabbShape(Vector3 localMin, Vector3 localMax) {
        this.localMin = new Vector3(localMin);
        this.localMax = new Vector3(localMax);
    }

    @Override
    public String getTypeId() {
        return TYPE_ID;
    }

    public Vector3 getLocalMin() {
        return new Vector3(localMin);
    }

    public Vector3 getLocalMax() {
        return new Vector3(localMax);
    }
}
