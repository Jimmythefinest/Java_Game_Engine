package com.njst.gaming.collision;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.objects.GameObject;

public class GameObjectColliderAdapter implements Collider {
    private final GameObject gameObject;
    private final CollisionShape shape;
    private final int layer;
    private final int mask;
    private final boolean trigger;
    private final boolean isStatic;

    public GameObjectColliderAdapter(GameObject gameObject) {
        this(gameObject, 1, -1, false, false);
    }

    public GameObjectColliderAdapter(GameObject gameObject, int layer, int mask, boolean trigger, boolean isStatic) {
        this.gameObject = gameObject;
        this.shape = new AabbShape(new Vector3(gameObject.localMin), new Vector3(gameObject.localMax));
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
        gameObject.updateModelMatrix();
        return new Bounds3(new Vector3(gameObject.min), new Vector3(gameObject.max));
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
}
