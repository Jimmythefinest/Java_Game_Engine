package com.njst.gaming.ri.battlearena;

public final class BattleArenaPlayerState {
    public final String playerId;
    public final float x;
    public final float y;
    public final float z;
    public final float headingDegrees;
    public final String animationKey;
    public final float animationFrame;
    public final float currentHealth;
    public final float maxHealth;

    public BattleArenaPlayerState(String playerId,
                                  float x,
                                  float y,
                                  float z,
                                  float headingDegrees,
                                  String animationKey,
                                  float animationFrame) {
        this(playerId, x, y, z, headingDegrees, animationKey, animationFrame, 100f, 100f);
    }

    public BattleArenaPlayerState(String playerId,
                                  float x,
                                  float y,
                                  float z,
                                  float headingDegrees,
                                  String animationKey,
                                  float animationFrame,
                                  float currentHealth,
                                  float maxHealth) {
        this.playerId = playerId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.headingDegrees = headingDegrees;
        this.animationKey = animationKey;
        this.animationFrame = animationFrame;
        this.currentHealth = currentHealth;
        this.maxHealth = maxHealth;
    }

    public float healthRatio() {
        if (maxHealth <= 0f) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, currentHealth / maxHealth));
    }
}
