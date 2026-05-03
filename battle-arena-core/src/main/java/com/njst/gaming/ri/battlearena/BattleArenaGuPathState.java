package com.njst.gaming.ri.battlearena;

public final class BattleArenaGuPathState {
    public final float earth;
    public final float water;
    public final float wind;
    public final float fire;
    public final float cold;
    public final float rule;

    public BattleArenaGuPathState(float earth,
                                  float water,
                                  float wind,
                                  float fire,
                                  float cold,
                                  float rule) {
        this.earth = earth;
        this.water = water;
        this.wind = wind;
        this.fire = fire;
        this.cold = cold;
        this.rule = rule;
    }

    public static BattleArenaGuPathState forMaterial(BattleArenaGuMaterial material) {
        if (material == BattleArenaGuMaterial.EARTH || material == BattleArenaGuMaterial.MOLTEN_EARTH) {
            return new BattleArenaGuPathState(1f, 0f, 0f, material == BattleArenaGuMaterial.MOLTEN_EARTH ? 0.65f : 0f, 0f, 0f);
        }
        if (material == BattleArenaGuMaterial.WATER) {
            return new BattleArenaGuPathState(0f, 1f, 0f, 0f, 0f, 0f);
        }
        if (material == BattleArenaGuMaterial.ICE) {
            return new BattleArenaGuPathState(0f, 0.8f, 0f, 0f, 0.85f, 0.25f);
        }
        if (material == BattleArenaGuMaterial.HOT_GAS) {
            return new BattleArenaGuPathState(0f, 0f, 0.45f, 1f, 0f, 0.15f);
        }
        return new BattleArenaGuPathState(0f, 0f, 1f, 0f, 0f, 0f);
    }
}
