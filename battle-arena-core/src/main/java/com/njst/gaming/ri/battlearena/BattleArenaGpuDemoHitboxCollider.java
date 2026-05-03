package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.collision.AabbShape;
import com.njst.gaming.collision.Bounds3;
import com.njst.gaming.collision.Collider;
import com.njst.gaming.collision.CollisionShape;

final class BattleArenaGpuDemoHitboxCollider implements Collider {
    enum Type {
        HURTBOX,
        HITBOX
    }

    private static final Bounds3 INACTIVE_BOUNDS =
            new Bounds3(new Vector3(100000f, 100000f, 100000f), new Vector3(100001f, 100001f, 100001f));

    private final BattleArenaGpuDemoCombatController combatController;
    private final String playerId;
    private final Type type;
    private final String name;
    private final String activeWhen;
    private final String onHitAnimation;
    private final Vector3 fallbackRootRelativeCenter;
    private final Vector3 halfExtents;
    private final CollisionShape shape;

    BattleArenaGpuDemoHitboxCollider(BattleArenaGpuDemoCombatController combatController,
                                     String playerId,
                                     Type type,
                                     String name,
                                     String activeWhen,
                                     String onHitAnimation,
                                     Vector3 fallbackRootRelativeCenter,
                                     Vector3 halfExtents) {
        this.combatController = combatController;
        this.playerId = playerId;
        this.type = type;
        this.name = name;
        this.activeWhen = activeWhen;
        this.onHitAnimation = onHitAnimation;
        this.fallbackRootRelativeCenter = new Vector3(fallbackRootRelativeCenter);
        this.halfExtents = new Vector3(halfExtents);
        this.shape = new AabbShape(new Vector3(fallbackRootRelativeCenter).sub(new Vector3(halfExtents)),
                new Vector3(fallbackRootRelativeCenter).add(new Vector3(halfExtents)));
    }

    String playerId() {
        return playerId;
    }

    Type type() {
        return type;
    }

    String name() {
        return name;
    }

    String onHitAnimation() {
        return onHitAnimation;
    }

    @Override
    public Object getOwner() {
        return this;
    }

    @Override
    public CollisionShape getShape() {
        return shape;
    }

    @Override
    public Bounds3 getWorldBounds() {
        if (!isEnabled()) {
            return INACTIVE_BOUNDS;
        }
        Vector3 center = combatController.resolveWorldCenter(this, fallbackRootRelativeCenter);
        Vector3 min = new Vector3(
                center.x - halfExtents.x,
                center.y - halfExtents.y,
                center.z - halfExtents.z);
        Vector3 max = new Vector3(
                center.x + halfExtents.x,
                center.y + halfExtents.y,
                center.z + halfExtents.z);
        return new Bounds3(min, max);
    }

    @Override
    public int getLayer() {
        return type == Type.HITBOX ? BattleArenaHitboxCollider.LAYER_HITBOX
                : BattleArenaHitboxCollider.LAYER_HURTBOX;
    }

    @Override
    public int getMask() {
        if (!isEnabled()) {
            return 0;
        }
        return type == Type.HITBOX ? BattleArenaHitboxCollider.LAYER_HURTBOX
                : BattleArenaHitboxCollider.LAYER_HITBOX;
    }

    @Override
    public boolean isTrigger() {
        return true;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public boolean canCollideWith(Collider other) {
        if (!isEnabled() || other == null || other == this) {
            return false;
        }
        if (!(other instanceof BattleArenaGpuDemoHitboxCollider)) {
            return false;
        }
        BattleArenaGpuDemoHitboxCollider otherHitbox = (BattleArenaGpuDemoHitboxCollider) other;
        if (!otherHitbox.isEnabled() || playerId.equals(otherHitbox.playerId)) {
            return false;
        }
        return Collider.super.canCollideWith(other);
    }

    private boolean isEnabled() {
        boolean fallbackActive = type == Type.HURTBOX;
        if (activeWhen == null || activeWhen.trim().isEmpty()) {
            fallbackActive = true;
        } else {
            BattleArenaPlayerState state = combatController.stateForPlayer(playerId);
            fallbackActive = state != null && activeWhen.equals(state.animationKey);
        }
        return combatController.isBoxActive(this, fallbackActive);
    }
}
