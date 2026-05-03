package com.njst.gaming.ri.battlearena;

public final class BattleArenaAnimationTiming {
    public final String animationKey;
    public final float framesPerSecond;
    public final float durationFrames;
    public final int frameCount;
    public final int lockTicks;

    public BattleArenaAnimationTiming(String animationKey,
                                      float framesPerSecond,
                                      float durationFrames,
                                      int frameCount,
                                      int lockTicks) {
        this.animationKey = animationKey;
        this.framesPerSecond = framesPerSecond;
        this.durationFrames = durationFrames;
        this.frameCount = frameCount;
        this.lockTicks = lockTicks;
    }
}
