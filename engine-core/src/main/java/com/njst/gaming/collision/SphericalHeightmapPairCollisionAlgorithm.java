package com.njst.gaming.collision;

import com.njst.gaming.Math.Vector3;

public class SphericalHeightmapPairCollisionAlgorithm implements CollisionAlgorithm {
    private static final float EPSILON = 0.0001f;

    @Override
    public boolean supports(CollisionShape first, CollisionShape second) {
        return first instanceof SphericalHeightmapShape && second instanceof SphericalHeightmapShape;
    }

    @Override
    public CollisionManifold test(Collider first, Collider second) {
        if (!(first instanceof SphericalHeightmapCollider) || !(second instanceof SphericalHeightmapCollider)) {
            return CollisionManifold.none();
        }

        SphericalHeightmapCollider firstCollider = (SphericalHeightmapCollider) first;
        SphericalHeightmapCollider secondCollider = (SphericalHeightmapCollider) second;
        SphericalHeightmapShape firstShape = (SphericalHeightmapShape) first.getShape();
        SphericalHeightmapShape secondShape = (SphericalHeightmapShape) second.getShape();

        Vector3 firstCenter = firstCollider.getWorldCenter();
        Vector3 secondCenter = secondCollider.getWorldCenter();
        Vector3 centerDelta = new Vector3(secondCenter).sub(firstCenter);
        float centerDistance = centerDelta.length();

        Vector3 worldNormal = centerDistance > EPSILON
                ? centerDelta.mul(1f / centerDistance)
                : new Vector3(1f, 0f, 0f);

        Vector3 firstSampleDirectionLocal = directionInLocalSpace(firstCollider, firstCenter, worldNormal);
        Vector3 secondSampleDirectionLocal = directionInLocalSpace(secondCollider, secondCenter, new Vector3(worldNormal).mul(-1f));

        float firstSurfaceRadiusLocal = firstShape.sampleRadius(firstSampleDirectionLocal);
        float secondSurfaceRadiusLocal = secondShape.sampleRadius(secondSampleDirectionLocal);

        Vector3 firstSurfacePointWorld = firstCollider.localToWorldPoint(
                localSurfacePoint(firstShape.getLocalCenter(), firstSampleDirectionLocal, firstSurfaceRadiusLocal));
        Vector3 secondSurfacePointWorld = secondCollider.localToWorldPoint(
                localSurfacePoint(secondShape.getLocalCenter(), secondSampleDirectionLocal, secondSurfaceRadiusLocal));

        float firstRadiusWorld = firstCenter.distance(firstSurfacePointWorld);
        float secondRadiusWorld = secondCenter.distance(secondSurfacePointWorld);
        float separation = centerDistance - (firstRadiusWorld + secondRadiusWorld);
        if (separation > 0f) {
            return CollisionManifold.none();
        }

        float penetration = -separation;
        Vector3 contactPoint = new Vector3(firstSurfacePointWorld).add(secondSurfacePointWorld).mul(0.5f);
        return new CollisionManifold(true, worldNormal, penetration, contactPoint);
    }

    private Vector3 directionInLocalSpace(SphericalHeightmapCollider collider, Vector3 worldCenter, Vector3 worldNormal) {
        Vector3 worldPoint = new Vector3(worldCenter).add(worldNormal);
        Vector3 localCenter = collider.worldToLocalPoint(worldCenter);
        Vector3 localPoint = collider.worldToLocalPoint(worldPoint);
        Vector3 localDirection = localPoint.sub(localCenter, new Vector3());
        if (localDirection.length() <= EPSILON) {
            return new Vector3(1f, 0f, 0f);
        }
        return localDirection.normalize();
    }

    private Vector3 localSurfacePoint(Vector3 localCenter, Vector3 localDirection, float localRadius) {
        return new Vector3(localDirection).normalize().mul(localRadius).add(localCenter);
    }
}
