package com.njst.gaming.ri.battlearena.skills;

import com.njst.gaming.Scene;

public final class BattleArenaSkillContext {
    public interface TerrainSampler {
        float sample(float worldX, float worldZ);
    }

    public final Scene scene;
    public final TerrainSampler terrainSampler;
    public final boolean debugEnabled;

    public BattleArenaSkillContext(Scene scene,
                                   TerrainSampler terrainSampler,
                                   boolean debugEnabled) {
        this.scene = scene;
        this.terrainSampler = terrainSampler;
        this.debugEnabled = debugEnabled;
    }

    public float sampleTerrainHeight(float worldX, float worldZ) {
        return terrainSampler != null ? terrainSampler.sample(worldX, worldZ) : 0f;
    }
}
