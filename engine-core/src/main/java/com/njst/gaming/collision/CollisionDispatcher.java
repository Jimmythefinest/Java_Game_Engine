package com.njst.gaming.collision;

import java.util.ArrayList;
import java.util.List;

public class CollisionDispatcher {
    private final List<CollisionAlgorithm> algorithms = new ArrayList<CollisionAlgorithm>();

    public void register(CollisionAlgorithm algorithm) {
        if (algorithm != null) {
            algorithms.add(algorithm);
        }
    }

    public void registerFirst(CollisionAlgorithm algorithm) {
        if (algorithm != null) {
            algorithms.add(0, algorithm);
        }
    }

    public CollisionManifold test(Collider first, Collider second) {
        for (CollisionAlgorithm algorithm : algorithms) {
            if (algorithm.supports(first.getShape(), second.getShape())) {
                return algorithm.test(first, second);
            }
        }
        return CollisionManifold.none();
    }
}
