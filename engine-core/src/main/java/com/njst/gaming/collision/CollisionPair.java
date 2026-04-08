package com.njst.gaming.collision;

public class CollisionPair {
    private final Collider first;
    private final Collider second;

    public CollisionPair(Collider first, Collider second) {
        this.first = first;
        this.second = second;
    }

    public Collider getFirst() {
        return first;
    }

    public Collider getSecond() {
        return second;
    }
}
