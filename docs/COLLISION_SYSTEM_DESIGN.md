# Collision System Design

This document defines the collision architecture that should sit beside rendering and physics rather than inside them.

## Goals

- Keep collision detection modular and engine-core friendly.
- Allow users to write their own collision world, broadphase, collider, or pair algorithm.
- Separate detection, query, and response so physics does not own every collision concern.
- Reuse existing `GameObject` bounds as an adapter path during migration.

## Package Layout

The scaffolding lives in `com.njst.gaming.collision` and is split by responsibility:

- `CollisionShape`: shape marker contract.
- `Collider`: runtime collision participant contract.
- `CollisionWorld`: registry and update/query API.
- `Broadphase`: candidate-pair generation.
- `CollisionAlgorithm`: narrowphase pair test.
- `CollisionDispatcher`: shape-pair routing.
- `CollisionEvent`, `CollisionListener`: event stream.
- `Ray`, `RaycastHit`: query models.
- `GameObjectColliderAdapter`: compatibility layer for existing `GameObject`.

## Ownership Model

`GameObject` is not the collision API.

That is intentional. A renderable object may expose colliders, but the collision system should also work for:

- invisible triggers
- editor gizmos
- AI/query-only volumes
- future rigid bodies or character controllers

## Default First Version

The default implementation is intentionally small:

- `DefaultCollisionWorld`
- `NaiveBroadphase`
- `AabbShape`
- `AabbVsAabbCollisionAlgorithm`

This gives the engine a stable seam now, while leaving room for:

- sweep-and-prune or grid broadphase
- spheres/capsules
- trigger-only colliders
- collision layers and masks
- separate response/solver systems

## Optional Spherical Heightmap Extension

For workflows that bake object surfaces into radial height data, the collision package can be extended with:

- `SphericalHeightmapShape`
- `GameObjectSphericalHeightmapColliderAdapter`
- `SphericalHeightmapPairCollisionAlgorithm`

This is intended as an optional narrow-phase path rather than a default engine feature.

The algorithm currently compares two baked surfaces along the center-to-center direction:

1. find each collider center in world space
2. convert the opposing direction into each collider's local space
3. sample local surface radius from the baked spherical heightmap
4. transform those two sampled surface points back into world space
5. compare center distance to sampled world radii and build a manifold

That makes it a practical first detector for roughly star-shaped baked objects.

Recommended setup:

```java
CollisionDispatcher dispatcher = new CollisionDispatcher();
dispatcher.registerFirst(new SphericalHeightmapPairCollisionAlgorithm());
dispatcher.register(new AabbVsAabbCollisionAlgorithm());

scene.setCollisionWorld(new DefaultCollisionWorld(new NaiveBroadphase(), dispatcher));
```

Each baked object then registers a `GameObjectSphericalHeightmapColliderAdapter` that references its own `SphericalHeightmapShape`.

## How Scene Should Use It

`Scene` should own a `CollisionWorld` instance and treat it as an engine service.

Recommended usage:

1. loaders create `GameObject`
2. scene creates or receives matching colliders
3. scene registers colliders with `CollisionWorld`
4. scene updates `CollisionWorld` once per frame
5. physics/gameplay systems consume collision events or queries

## Migration Strategy

Keep the current `PhysicsEngine` working while moving toward the new contracts.

Recommended sequence:

1. add `CollisionWorld` to `Scene`
2. register `GameObjectColliderAdapter` for collision-enabled objects
3. replace direct overlap loops in `PhysicsEngine` with `CollisionWorld.getEvents()` or `raycast(...)`
4. move collision response out of the dispatcher and into dedicated gameplay/physics handlers
