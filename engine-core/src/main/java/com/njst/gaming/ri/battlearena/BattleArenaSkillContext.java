package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Scene;

final class BattleArenaSkillContext {
    interface TerrainSampler {
        float sample(float worldX, float worldZ);
    }

    final Scene scene;
    final TerrainSampler terrainSampler;
    final boolean debugEnabled;

    BattleArenaSkillContext(Scene scene,
                            TerrainSampler terrainSampler,
                            boolean debugEnabled) {
        this.scene = scene;
        this.terrainSampler = terrainSampler;
        this.debugEnabled = debugEnabled;
    }

    float sampleTerrainHeight(float worldX, float worldZ) {
        return terrainSampler != null ? terrainSampler.sample(worldX, worldZ) : 0f;
    }
}
