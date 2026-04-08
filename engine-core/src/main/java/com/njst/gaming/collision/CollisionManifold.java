package com.njst.gaming.collision;

import com.njst.gaming.Math.Vector3;

public class CollisionManifold {
    private static final CollisionManifold NO_COLLISION =
            new CollisionManifold(false, new Vector3(), 0f, new Vector3());

    private final boolean colliding;
    private final Vector3 normal;
    private final float penetrationDepth;
    private final Vector3 contactPoint;

    public CollisionManifold(boolean colliding, Vector3 normal, float penetrationDepth, Vector3 contactPoint) {
        this.colliding = colliding;
        this.normal = new Vector3(normal);
        this.penetrationDepth = penetrationDepth;
        this.contactPoint = new Vector3(contactPoint);
    }

    public static CollisionManifold none() {
        return NO_COLLISION;
    }

    public boolean isColliding() {
        return colliding;
    }

    public Vector3 getNormal() {
        return new Vector3(normal);
    }

    public float getPenetrationDepth() {
        return penetrationDepth;
    }

    public Vector3 getContactPoint() {
        return new Vector3(contactPoint);
    }
}
