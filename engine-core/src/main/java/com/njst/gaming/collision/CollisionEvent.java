package com.njst.gaming.collision;

public class CollisionEvent {
    private final CollisionEventType type;
    private final Collider first;
    private final Collider second;
    private final CollisionManifold manifold;

    public CollisionEvent(CollisionEventType type, Collider first, Collider second, CollisionManifold manifold) {
        this.type = type;
        this.first = first;
        this.second = second;
        this.manifold = manifold;
    }

    public CollisionEventType getType() {
        return type;
    }

    public Collider getFirst() {
        return first;
    }

    public Collider getSecond() {
        return second;
    }

    public CollisionManifold getManifold() {
        return manifold;
    }
}
