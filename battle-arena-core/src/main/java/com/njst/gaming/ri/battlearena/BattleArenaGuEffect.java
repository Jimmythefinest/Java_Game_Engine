package com.njst.gaming.ri.battlearena;

public final class BattleArenaGuEffect {
    public enum Type {
        RELEASE_MATTER,
        ADD_HEAT,
        REMOVE_HEAT,
        PUSH,
        SHAPE
    }

    public final Type type;
    public final BattleArenaGuMaterial material;
    public final float amount;
    public final float x;
    public final float y;
    public final float z;
    public final float halfX;
    public final float halfY;
    public final float halfZ;

    private BattleArenaGuEffect(Type type,
                                BattleArenaGuMaterial material,
                                float amount,
                                float x,
                                float y,
                                float z,
                                float halfX,
                                float halfY,
                                float halfZ) {
        this.type = type;
        this.material = material;
        this.amount = amount;
        this.x = x;
        this.y = y;
        this.z = z;
        this.halfX = halfX;
        this.halfY = halfY;
        this.halfZ = halfZ;
    }

    public static BattleArenaGuEffect releaseMatter(BattleArenaGuMaterial material,
                                                    float halfX,
                                                    float halfY,
                                                    float halfZ) {
        return new BattleArenaGuEffect(Type.RELEASE_MATTER, material, 0f, 0f, 0f, 0f, halfX, halfY, halfZ);
    }

    public static BattleArenaGuEffect addHeat(float amount) {
        return new BattleArenaGuEffect(Type.ADD_HEAT, null, amount, 0f, 0f, 0f, 0f, 0f, 0f);
    }

    public static BattleArenaGuEffect removeHeat(float amount) {
        return new BattleArenaGuEffect(Type.REMOVE_HEAT, null, amount, 0f, 0f, 0f, 0f, 0f, 0f);
    }

    public static BattleArenaGuEffect push(float x, float y, float z, float amount) {
        return new BattleArenaGuEffect(Type.PUSH, null, amount, x, y, z, 0f, 0f, 0f);
    }

    public static BattleArenaGuEffect shape(float halfX, float halfY, float halfZ) {
        return new BattleArenaGuEffect(Type.SHAPE, null, 0f, 0f, 0f, 0f, halfX, halfY, halfZ);
    }
}
