package com.njst.gaming.ri.battlearena.networking;

import com.njst.gaming.ri.battlearena.BattleArenaPlayerState;

public final class BattleArenaNetworkPlayerStateMessage {
    public String playerId;
    public float x;
    public float y;
    public float z;
    public float headingDegrees;
    public String animationKey;
    public float animationFrame;
    public float velocityX;
    public float velocityZ;
    public float strength;
    public float currentHealth;
    public float maxHealth;

    public static BattleArenaNetworkPlayerStateMessage fromState(BattleArenaPlayerState state) {
        BattleArenaNetworkPlayerStateMessage message = new BattleArenaNetworkPlayerStateMessage();
        if (state == null) {
            return message;
        }
        message.playerId = state.playerId;
        message.x = state.x;
        message.y = state.y;
        message.z = state.z;
        message.headingDegrees = state.headingDegrees;
        message.animationKey = state.animationKey;
        message.animationFrame = state.animationFrame;
        message.velocityX = state.velocityX;
        message.velocityZ = state.velocityZ;
        message.strength = state.strength;
        message.currentHealth = state.currentHealth;
        message.maxHealth = state.maxHealth;
        return message;
    }

    public BattleArenaPlayerState toState() {
        return new BattleArenaPlayerState(
                playerId,
                x,
                y,
                z,
                headingDegrees,
                animationKey,
                animationFrame,
                velocityX,
                velocityZ,
                strength,
                currentHealth,
                maxHealth);
    }
}
