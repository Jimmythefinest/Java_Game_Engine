package com.njst.gaming.collision;

import java.util.List;

public interface Broadphase {
    List<CollisionPair> computePairs(List<Collider> colliders);
}
