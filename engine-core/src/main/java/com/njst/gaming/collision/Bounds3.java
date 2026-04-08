package com.njst.gaming.collision;

import com.njst.gaming.Math.Vector3;

public class Bounds3 {
    private final Vector3 min;
    private final Vector3 max;

    public Bounds3(Vector3 min, Vector3 max) {
        this.min = new Vector3(min);
        this.max = new Vector3(max);
    }

    public Vector3 getMin() {
        return new Vector3(min);
    }

    public Vector3 getMax() {
        return new Vector3(max);
    }

    public Vector3 getCenter() {
        return new Vector3(
                (min.x + max.x) * 0.5f,
                (min.y + max.y) * 0.5f,
                (min.z + max.z) * 0.5f);
    }

    public Vector3 getExtents() {
        return new Vector3(
                (max.x - min.x) * 0.5f,
                (max.y - min.y) * 0.5f,
                (max.z - min.z) * 0.5f);
    }

    public boolean overlaps(Bounds3 other) {
        if (other == null) {
            return false;
        }
        return min.x <= other.max.x && max.x >= other.min.x
                && min.y <= other.max.y && max.y >= other.min.y
                && min.z <= other.max.z && max.z >= other.min.z;
    }
}
