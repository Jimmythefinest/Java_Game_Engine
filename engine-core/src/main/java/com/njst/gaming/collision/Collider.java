package com.njst.gaming.collision;

public interface Collider {
    Object getOwner();

    CollisionShape getShape();

    Bounds3 getWorldBounds();

    int getLayer();

    int getMask();

    boolean isTrigger();

    boolean isStatic();

    default boolean canCollideWith(Collider other) {
        if (other == null || other == this) {
            return false;
        }
        boolean thisAllowsOther = (getMask() & other.getLayer()) != 0;
        boolean otherAllowsThis = (other.getMask() & getLayer()) != 0;
        return thisAllowsOther && otherAllowsThis;
    }
}
