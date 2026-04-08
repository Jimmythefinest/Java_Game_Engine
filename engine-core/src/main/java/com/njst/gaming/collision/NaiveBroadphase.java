package com.njst.gaming.collision;

import java.util.ArrayList;
import java.util.List;

public class NaiveBroadphase implements Broadphase {
    @Override
    public List<CollisionPair> computePairs(List<Collider> colliders) {
        ArrayList<CollisionPair> pairs = new ArrayList<CollisionPair>();
        if (colliders == null) {
            return pairs;
        }

        for (int i = 0; i < colliders.size(); i++) {
            Collider first = colliders.get(i);
            for (int j = i + 1; j < colliders.size(); j++) {
                Collider second = colliders.get(j);
                if (!first.canCollideWith(second)) {
                    continue;
                }
                if (first.isStatic() && second.isStatic()) {
                    continue;
                }
                if (!first.getWorldBounds().overlaps(second.getWorldBounds())) {
                    continue;
                }
                pairs.add(new CollisionPair(first, second));
            }
        }

        return pairs;
    }
}
