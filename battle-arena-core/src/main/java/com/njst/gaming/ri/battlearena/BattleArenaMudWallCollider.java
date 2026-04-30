package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.collision.AabbShape;
import com.njst.gaming.collision.Bounds3;
import com.njst.gaming.collision.Collider;
import com.njst.gaming.collision.CollisionShape;
import com.njst.gaming.objects.GameObject;

public final class BattleArenaMudWallCollider implements Collider {
    private static final int LAYER_OBSTACLE = 1 << 2;
    private static final Bounds3 INACTIVE_BOUNDS =
            new Bounds3(new Vector3(100000f, 100000f, 100000f), new Vector3(100001f, 100001f, 100001f));

    private final GameObject mudWallObject;
    private final CollisionShape shape;

    public BattleArenaMudWallCollider(GameObject mudWallObject) {
        this.mudWallObject = mudWallObject;
        this.shape = new AabbShape(new Vector3(-0.5f, -0.5f, -0.5f), new Vector3(0.5f, 0.5f, 0.5f));
    }

    @Override
    public Object getOwner() {
        return mudWallObject;
    }

    @Override
    public CollisionShape getShape() {
        return shape;
    }

    @Override
    public Bounds3 getWorldBounds() {
        if (mudWallObject == null || mudWallObject.collisionBounds == null || mudWallObject.collisionBounds.length < 6) {
            return INACTIVE_BOUNDS;
        }
        return new Bounds3(
                new Vector3(mudWallObject.collisionBounds[0], mudWallObject.collisionBounds[1], mudWallObject.collisionBounds[2]),
                new Vector3(mudWallObject.collisionBounds[3], mudWallObject.collisionBounds[4], mudWallObject.collisionBounds[5]));
    }

    @Override
    public int getLayer() {
        return LAYER_OBSTACLE;
    }

    @Override
    public int getMask() {
        return BattleArenaHitboxCollider.LAYER_HURTBOX;
    }

    @Override
    public boolean isTrigger() {
        return false;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public boolean canCollideWith(Collider other) {
        if (!(other instanceof BattleArenaHitboxCollider)) {
            return false;
        }
        BattleArenaHitboxCollider hitbox = (BattleArenaHitboxCollider) other;
        return hitbox.getType() == BattleArenaHitboxCollider.Type.HURTBOX
                && "torso".equals(hitbox.getName())
                && Collider.super.canCollideWith(other);
    }
}
