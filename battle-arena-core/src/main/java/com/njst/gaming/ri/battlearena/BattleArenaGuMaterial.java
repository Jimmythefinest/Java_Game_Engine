package com.njst.gaming.ri.battlearena;

public enum BattleArenaGuMaterial {
    AIR("air", 22f, 1.2f, 1f, 0.02f, 0f, 0.01f),
    EARTH("earth", 22f, 1800f, 1f, 0.85f, 0.9f, 0.95f),
    WATER("water", 22f, 1000f, 1f, 0.25f, 0.02f, 0.45f),
    ICE("ice", -8f, 920f, 1f, 0.75f, 0.82f, 0.9f),
    MOLTEN_EARTH("molten_earth", 760f, 2400f, 1.2f, 0.18f, 0.05f, 0.75f),
    HOT_GAS("hot_gas", 520f, 0.55f, 1.8f, 0.01f, 0f, 0.02f);

    public final String key;
    public final float defaultTemperature;
    public final float defaultDensity;
    public final float defaultPressure;
    public final float defaultCohesion;
    public final float defaultRigidity;
    public final float defaultViscosity;

    BattleArenaGuMaterial(String key,
                          float defaultTemperature,
                          float defaultDensity,
                          float defaultPressure,
                          float defaultCohesion,
                          float defaultRigidity,
                          float defaultViscosity) {
        this.key = key;
        this.defaultTemperature = defaultTemperature;
        this.defaultDensity = defaultDensity;
        this.defaultPressure = defaultPressure;
        this.defaultCohesion = defaultCohesion;
        this.defaultRigidity = defaultRigidity;
        this.defaultViscosity = defaultViscosity;
    }

    public static BattleArenaGuMaterial fromKey(String key) {
        if (key == null) {
            return AIR;
        }
        for (BattleArenaGuMaterial material : values()) {
            if (material.key.equals(key)) {
                return material;
            }
        }
        return AIR;
    }
}
