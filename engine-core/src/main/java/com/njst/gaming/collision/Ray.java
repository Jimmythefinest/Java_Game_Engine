package com.njst.gaming.collision;

import com.njst.gaming.Math.Vector3;

public class Ray {
    private final Vector3 origin;
    private final Vector3 direction;

    public Ray(Vector3 origin, Vector3 direction) {
        this.origin = new Vector3(origin);
        this.direction = new Vector3(direction);
    }

    public Vector3 getOrigin() {
        return new Vector3(origin);
    }

    public Vector3 getDirection() {
        return new Vector3(direction);
    }
}
