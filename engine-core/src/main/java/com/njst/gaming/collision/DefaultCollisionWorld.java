package com.njst.gaming.collision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.njst.gaming.Math.Vector3;

public class DefaultCollisionWorld implements CollisionWorld {
    private final List<Collider> colliders = new ArrayList<Collider>();
    private final List<CollisionListener> listeners = new ArrayList<CollisionListener>();
    private final List<CollisionEvent> events = new ArrayList<CollisionEvent>();
    private final Set<String> activePairs = new HashSet<String>();
    private final Broadphase broadphase;
    private final CollisionDispatcher dispatcher;

    public DefaultCollisionWorld() {
        this(new NaiveBroadphase(), defaultDispatcher());
    }

    public DefaultCollisionWorld(Broadphase broadphase, CollisionDispatcher dispatcher) {
        this.broadphase = broadphase;
        this.dispatcher = dispatcher;
    }

    private static CollisionDispatcher defaultDispatcher() {
        CollisionDispatcher dispatcher = new CollisionDispatcher();
        dispatcher.register(new AabbVsAabbCollisionAlgorithm());
        return dispatcher;
    }

    @Override
    public void addCollider(Collider collider) {
        if (collider != null && !colliders.contains(collider)) {
            colliders.add(collider);
        }
    }

    @Override
    public void removeCollider(Collider collider) {
        colliders.remove(collider);
    }

    @Override
    public void clear() {
        colliders.clear();
        events.clear();
        activePairs.clear();
    }

    @Override
    public void update(float deltaTime) {
        events.clear();
        HashSet<String> currentPairs = new HashSet<String>();

        List<CollisionPair> candidatePairs = broadphase.computePairs(colliders);
        for (CollisionPair pair : candidatePairs) {
            Collider first = pair.getFirst();
            Collider second = pair.getSecond();
            CollisionManifold manifold = dispatcher.test(first, second);
            if (!manifold.isColliding()) {
                continue;
            }

            String key = pairKey(first, second);
            currentPairs.add(key);
            CollisionEventType type = activePairs.contains(key)
                    ? CollisionEventType.STAY
                    : CollisionEventType.ENTER;
            emit(new CollisionEvent(type, first, second, manifold));
        }

        for (String key : activePairs) {
            if (currentPairs.contains(key)) {
                continue;
            }
            Collider[] pair = resolvePair(key);
            if (pair[0] != null && pair[1] != null) {
                emit(new CollisionEvent(CollisionEventType.EXIT, pair[0], pair[1], CollisionManifold.none()));
            }
        }

        activePairs.clear();
        activePairs.addAll(currentPairs);
    }

    @Override
    public List<Collider> getColliders() {
        return Collections.unmodifiableList(colliders);
    }

    @Override
    public List<CollisionEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    @Override
    public void addListener(CollisionListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(CollisionListener listener) {
        listeners.remove(listener);
    }

    @Override
    public RaycastHit raycast(Ray ray, float maxDistance) {
        RaycastHit closest = null;
        for (Collider collider : colliders) {
            float distance = intersectRayAabb(ray, collider.getWorldBounds(), maxDistance);
            if (distance < 0f) {
                continue;
            }
            if (closest == null || distance < closest.getDistance()) {
                Vector3 hitPoint = ray.getOrigin().add(ray.getDirection().mul(distance));
                closest = new RaycastHit(collider, distance, hitPoint);
            }
        }
        return closest;
    }

    private void emit(CollisionEvent event) {
        events.add(event);
        for (CollisionListener listener : listeners) {
            listener.onCollision(event);
        }
    }

    private String pairKey(Collider first, Collider second) {
        int firstHash = System.identityHashCode(first);
        int secondHash = System.identityHashCode(second);
        return firstHash < secondHash
                ? firstHash + ":" + secondHash
                : secondHash + ":" + firstHash;
    }

    private Collider[] resolvePair(String key) {
        String[] parts = key.split(":");
        if (parts.length != 2) {
            return new Collider[] { null, null };
        }
        int firstHash = Integer.parseInt(parts[0]);
        int secondHash = Integer.parseInt(parts[1]);
        Collider first = null;
        Collider second = null;
        for (Collider collider : colliders) {
            int hash = System.identityHashCode(collider);
            if (hash == firstHash) {
                first = collider;
            } else if (hash == secondHash) {
                second = collider;
            }
        }
        return new Collider[] { first, second };
    }

    private float intersectRayAabb(Ray ray, Bounds3 bounds, float maxDistance) {
        Vector3 origin = ray.getOrigin();
        Vector3 direction = ray.getDirection();
        Vector3 min = bounds.getMin();
        Vector3 max = bounds.getMax();

        float tMin = 0f;
        float tMax = maxDistance;

        tMin = updateRayInterval(origin.x, direction.x, min.x, max.x, tMin, tMax, true);
        tMax = updateRayInterval(origin.x, direction.x, min.x, max.x, tMin, tMax, false);
        if (tMin > tMax) {
            return -1f;
        }

        tMin = updateRayInterval(origin.y, direction.y, min.y, max.y, tMin, tMax, true);
        tMax = updateRayInterval(origin.y, direction.y, min.y, max.y, tMin, tMax, false);
        if (tMin > tMax) {
            return -1f;
        }

        tMin = updateRayInterval(origin.z, direction.z, min.z, max.z, tMin, tMax, true);
        tMax = updateRayInterval(origin.z, direction.z, min.z, max.z, tMin, tMax, false);
        if (tMin > tMax) {
            return -1f;
        }

        return tMin;
    }

    private float updateRayInterval(float origin, float direction, float min, float max, float tMin, float tMax,
            boolean lowerBound) {
        if (Math.abs(direction) < 0.000001f) {
            if (origin < min || origin > max) {
                return lowerBound ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
            }
            return lowerBound ? tMin : tMax;
        }

        float invDirection = 1f / direction;
        float t1 = (min - origin) * invDirection;
        float t2 = (max - origin) * invDirection;
        float near = Math.min(t1, t2);
        float far = Math.max(t1, t2);
        return lowerBound ? Math.max(tMin, near) : Math.min(tMax, far);
    }
}
