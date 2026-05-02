package com.njst.gaming.ri.battlearena;

public final class BattleArenaPlayerState {
    public final String playerId;
    public final float x;
    public final float y;
    public final float z;
    public final float headingDegrees;
    public final String animationKey;
    public final float animationFrame;

    public BattleArenaPlayerState(String playerId,
                                  float x,
                                  float y,
                                  float z,
                                  float headingDegrees,
                                  String animationKey,
                                  float animationFrame) {
        this.playerId = playerId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.headingDegrees = headingDegrees;
        this.animationKey = animationKey;
        this.animationFrame = animationFrame;
    }
}
