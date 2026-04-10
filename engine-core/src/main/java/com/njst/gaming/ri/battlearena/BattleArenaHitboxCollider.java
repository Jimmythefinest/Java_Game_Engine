package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.collision.AabbShape;
import com.njst.gaming.collision.Bounds3;
import com.njst.gaming.collision.Collider;
import com.njst.gaming.collision.CollisionShape;

final class BattleArenaHitboxCollider implements Collider {
    static final int LAYER_HURTBOX = 1;
    static final int LAYER_HITBOX = 1 << 1;

    enum Type {
        BODY,
        PUNCH
    }

    private static final Bounds3 INACTIVE_BOUNDS =
            new Bounds3(new Vector3(100000f, 100000f, 100000f), new Vector3(100001f, 100001f, 100001f));

    private final BattleArenaCharacterRuntime character;
    private final Type type;
    private final String name;
    private final Vector3 localCenter;
    private final Vector3 halfExtents;
    private final CollisionShape shape;

    BattleArenaHitboxCollider(BattleArenaCharacterRuntime character,
                              Type type,
                              String name,
                              Vector3 localCenter,
                              Vector3 halfExtents) {
        this.character = character;
        this.type = type;
        this.name = name;
        this.localCenter = new Vector3(localCenter);
        this.halfExtents = new Vector3(halfExtents);
        this.shape = new AabbShape(new Vector3(localCenter).sub(new Vector3(halfExtents)),
                new Vector3(localCenter).add(new Vector3(halfExtents)));
    }

    BattleArenaCharacterRuntime getCharacter() {
        return character;
    }

    Type getType() {
        return type;
    }

    String getName() {
        return name;
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
        Vector3 base = character.getPosition().clone();
        Vector3 rotatedOffset = localCenter.rotateY((float) Math.toRadians(character.getHeadingDegrees()));
        return base.add(rotatedOffset);
    }

    @Override
    public int getLayer() {
        return type == Type.PUNCH ? LAYER_HITBOX : LAYER_HURTBOX;
    }

    @Override
    public int getMask() {
        if (!isEnabled()) {
            return 0;
        }
        return type == Type.PUNCH ? LAYER_HURTBOX : LAYER_HITBOX;
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
        if (type == Type.BODY) {
            return true;
        }
        return character.isPunching();
    }
}
