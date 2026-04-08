package com.njst.gaming.collision;

public interface CollisionAlgorithm {
    boolean supports(CollisionShape first, CollisionShape second);

    CollisionManifold test(Collider first, Collider second);
}
