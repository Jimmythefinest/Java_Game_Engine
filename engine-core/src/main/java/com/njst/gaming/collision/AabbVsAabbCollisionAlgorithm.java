package com.njst.gaming.collision;

import com.njst.gaming.Math.Vector3;

public class AabbVsAabbCollisionAlgorithm implements CollisionAlgorithm {
    @Override
    public boolean supports(CollisionShape first, CollisionShape second) {
        return first instanceof AabbShape && second instanceof AabbShape;
    }

    @Override
    public CollisionManifold test(Collider first, Collider second) {
        Bounds3 firstBounds = first.getWorldBounds();
        Bounds3 secondBounds = second.getWorldBounds();
        if (!firstBounds.overlaps(secondBounds)) {
            return CollisionManifold.none();
        }

        Vector3 firstMin = firstBounds.getMin();
        Vector3 firstMax = firstBounds.getMax();
        Vector3 secondMin = secondBounds.getMin();
        Vector3 secondMax = secondBounds.getMax();

        float overlapX = Math.min(firstMax.x, secondMax.x) - Math.max(firstMin.x, secondMin.x);
        float overlapY = Math.min(firstMax.y, secondMax.y) - Math.max(firstMin.y, secondMin.y);
        float overlapZ = Math.min(firstMax.z, secondMax.z) - Math.max(firstMin.z, secondMin.z);

        Vector3 firstCenter = firstBounds.getCenter();
        Vector3 secondCenter = secondBounds.getCenter();
        float deltaX = secondCenter.x - firstCenter.x;
        float deltaY = secondCenter.y - firstCenter.y;
        float deltaZ = secondCenter.z - firstCenter.z;

        float penetration = overlapX;
        Vector3 normal = new Vector3(deltaX >= 0f ? 1f : -1f, 0f, 0f);

        if (overlapY < penetration) {
            penetration = overlapY;
            normal.set(0f, deltaY >= 0f ? 1f : -1f, 0f);
        }
        if (overlapZ < penetration) {
            penetration = overlapZ;
            normal.set(0f, 0f, deltaZ >= 0f ? 1f : -1f);
        }

        Vector3 contactPoint = new Vector3(
                (Math.max(firstMin.x, secondMin.x) + Math.min(firstMax.x, secondMax.x)) * 0.5f,
                (Math.max(firstMin.y, secondMin.y) + Math.min(firstMax.y, secondMax.y)) * 0.5f,
                (Math.max(firstMin.z, secondMin.z) + Math.min(firstMax.z, secondMax.z)) * 0.5f);
        return new CollisionManifold(true, normal, penetration, contactPoint);
    }
}
