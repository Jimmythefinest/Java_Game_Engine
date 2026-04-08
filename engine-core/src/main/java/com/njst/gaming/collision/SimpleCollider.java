package com.njst.gaming.collision;

public class SimpleCollider implements Collider {
    private final Object owner;
    private final CollisionShape shape;
    private final Bounds3 worldBounds;
    private final int layer;
    private final int mask;
    private final boolean trigger;
    private final boolean isStatic;

    public SimpleCollider(Object owner, CollisionShape shape, Bounds3 worldBounds) {
        this(owner, shape, worldBounds, 1, -1, false, false);
    }

    public SimpleCollider(Object owner, CollisionShape shape, Bounds3 worldBounds,
            int layer, int mask, boolean trigger, boolean isStatic) {
        this.owner = owner;
        this.shape = shape;
        this.worldBounds = worldBounds;
        this.layer = layer;
        this.mask = mask;
        this.trigger = trigger;
        this.isStatic = isStatic;
    }

    @Override
    public Object getOwner() {
        return owner;
    }

    @Override
    public CollisionShape getShape() {
        return shape;
    }

    @Override
    public Bounds3 getWorldBounds() {
        return worldBounds;
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
