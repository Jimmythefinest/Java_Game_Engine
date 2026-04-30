package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Bone;
import com.njst.gaming.collision.AabbShape;
import com.njst.gaming.collision.Bounds3;
import com.njst.gaming.collision.Collider;
import com.njst.gaming.collision.CollisionShape;

public final class BattleArenaHitboxCollider implements Collider {
    public static final int LAYER_HURTBOX = 1;
    static final int LAYER_HITBOX = 1 << 1;

    public enum Type {
        HURTBOX,
        HITBOX
    }

    private static final Bounds3 INACTIVE_BOUNDS =
            new Bounds3(new Vector3(100000f, 100000f, 100000f), new Vector3(100001f, 100001f, 100001f));

    private final BattleArenaCharacterRuntime character;
    private final Type type;
    private final String name;
    private final String activeWhen;
    private final String onHitAnimation;
    private final Vector3 fallbackRootRelativeCenter;
    private final Vector3 halfExtents;
    private final CollisionShape shape;

    BattleArenaHitboxCollider(BattleArenaCharacterRuntime character,
                              Type type,
                              String name,
                              String activeWhen,
                              String onHitAnimation,
                              Bone anchorBone,
                              Vector3 localCenter,
                              Vector3 halfExtents) {
        this.character = character;
        this.type = type;
        this.name = name;
        this.activeWhen = activeWhen;
        this.onHitAnimation = onHitAnimation;
        this.fallbackRootRelativeCenter = resolveFallbackRootRelativeCenter(character, anchorBone, localCenter);
        this.halfExtents = new Vector3(halfExtents);
        this.shape = new AabbShape(new Vector3(localCenter).sub(new Vector3(halfExtents)),
                new Vector3(localCenter).add(new Vector3(halfExtents)));
    }

    public BattleArenaCharacterRuntime getCharacter() {
        return character;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getOnHitAnimation() {
        return onHitAnimation;
    }

    public boolean isDebugVisible() {
        return isEnabled();
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

        Vector3 center = getWorldCenter();
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

    Vector3 getWorldCenter() {
        return character.resolveHitboxCenter(name, fallbackRootRelativeCenter);
    }

    @Override
    public int getLayer() {
        return type == Type.HITBOX ? LAYER_HITBOX : LAYER_HURTBOX;
    }

    @Override
    public int getMask() {
        if (!isEnabled()) {
            return 0;
        }
        return type == Type.HITBOX ? LAYER_HURTBOX : LAYER_HITBOX;
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
        if (other instanceof BattleArenaMudWallCollider) {
            return type == Type.HURTBOX && "torso".equals(name);
        }
        if (!(other instanceof BattleArenaHitboxCollider)) {
            return false;
        }
        BattleArenaHitboxCollider otherHitbox = (BattleArenaHitboxCollider) other;
        if (!otherHitbox.isEnabled()) {
            return false;
        }
        if (character == otherHitbox.character) {
            return false;
        }
        return Collider.super.canCollideWith(other);
    }

    private boolean isEnabled() {
        boolean fallbackActive = type == Type.HURTBOX;
        if (activeWhen == null || activeWhen.trim().isEmpty()) {
            fallbackActive = true;
        } else {
            fallbackActive = character.isAnimationActive(activeWhen);
        }
        return character.isHitboxTrackActive(name, fallbackActive);
    }

    private Vector3 resolveFallbackRootRelativeCenter(BattleArenaCharacterRuntime character,
                                                      Bone anchorBone,
                                                      Vector3 localCenter) {
        if (anchorBone != null && character != null && character.rootBone != null) {
            return new Vector3(anchorBone.global_position)
                    .add(new Vector3(localCenter))
                    .sub(new Vector3(character.rootBone.global_position));
        }
        return new Vector3(localCenter);
    }
}
