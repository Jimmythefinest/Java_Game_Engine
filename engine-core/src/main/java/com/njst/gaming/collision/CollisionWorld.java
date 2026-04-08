package com.njst.gaming.collision;

import java.util.List;

public interface CollisionWorld {
    void addCollider(Collider collider);

    void removeCollider(Collider collider);

    void clear();

    void update(float deltaTime);

    List<Collider> getColliders();

    List<CollisionEvent> getEvents();

    void addListener(CollisionListener listener);

    void removeListener(CollisionListener listener);

    RaycastHit raycast(Ray ray, float maxDistance);
}
