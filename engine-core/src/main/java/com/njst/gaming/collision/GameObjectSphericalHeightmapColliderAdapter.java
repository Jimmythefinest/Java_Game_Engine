package com.njst.gaming.collision;

import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.objects.GameObject;

public class GameObjectSphericalHeightmapColliderAdapter implements SphericalHeightmapCollider {
    private final GameObject gameObject;
    private final SphericalHeightmapShape shape;
    private final int layer;
    private final int mask;
    private final boolean trigger;
    private final boolean isStatic;

    public GameObjectSphericalHeightmapColliderAdapter(GameObject gameObject, SphericalHeightmapShape shape) {
        this(gameObject, shape, 1, -1, false, false);
    }

    public GameObjectSphericalHeightmapColliderAdapter(GameObject gameObject, SphericalHeightmapShape shape,
            int layer, int mask, boolean trigger, boolean isStatic) {
        this.gameObject = gameObject;
        this.shape = shape;
        this.layer = layer;
        this.mask = mask;
        this.trigger = trigger;
        this.isStatic = isStatic;
    }

    @Override
    public Object getOwner() {
        return gameObject;
    }

    @Override
    public CollisionShape getShape() {
        return shape;
    }

    @Override
    public Bounds3 getWorldBounds() {
        float outerRadius = worldDistanceForLocalRadius(shape.getMaxRadius());
        Vector3 center = getWorldCenter();
        Vector3 extents = new Vector3(outerRadius, outerRadius, outerRadius);
        return new Bounds3(new Vector3(center).sub(extents), new Vector3(center).add(extents));
    }

    @Override
    public int getLayer() {
        return layer;
    }

    @Override
    public int getMask() {
        return mask;
    }

    @Override
    public boolean isTrigger() {
        return trigger;
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public Vector3 getWorldCenter() {
        return localToWorldPoint(shape.getLocalCenter());
    }

    @Override
    public Vector3 worldToLocalPoint(Vector3 worldPoint) {
        gameObject.updateModelMatrix();
        float[] model = gameObject.getModelMatrix().clone();
        Matrix4 inverse = new Matrix4().set(model).invert();
        return inverse.multiply(worldPoint);
    }

    @Override
    public Vector3 localToWorldPoint(Vector3 localPoint) {
        gameObject.updateModelMatrix();
        Matrix4 world = new Matrix4().set(gameObject.getModelMatrix().clone());
        return world.multiply(localPoint);
    }

    public float worldDistanceForLocalRadius(float localRadius) {
        Vector3 center = getWorldCenter();
        Vector3 offsetPoint = localToWorldPoint(shape.getLocalCenter().add(new Vector3(localRadius, 0f, 0f)));
        return center.distance(offsetPoint);
    }
}
