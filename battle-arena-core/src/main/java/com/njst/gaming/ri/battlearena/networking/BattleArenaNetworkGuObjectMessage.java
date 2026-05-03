package com.njst.gaming.ri.battlearena.networking;

import com.njst.gaming.ri.battlearena.BattleArenaGuObjectState;

public final class BattleArenaNetworkGuObjectMessage {
    public int id;
    public String ownerPlayerId;
    public String material;
    public float x;
    public float y;
    public float z;
    public float headingDegrees;
    public float halfX;
    public float halfY;
    public float halfZ;
    public float velocityX;
    public float velocityY;
    public float velocityZ;
    public float temperature;
    public float pressure;
    public float density;
    public float cohesion;
    public float rigidity;
    public float viscosity;
    public float earthPath;
    public float waterPath;
    public float windPath;
    public float firePath;
    public float coldPath;
    public float rulePath;
    public int lifetimeTicksRemaining;

    public static BattleArenaNetworkGuObjectMessage fromState(BattleArenaGuObjectState state) {
        BattleArenaNetworkGuObjectMessage message = new BattleArenaNetworkGuObjectMessage();
        if (state == null) {
            return message;
        }
        message.id = state.id;
        message.ownerPlayerId = state.ownerPlayerId;
        message.material = state.material;
        message.x = state.x;
        message.y = state.y;
        message.z = state.z;
        message.headingDegrees = state.headingDegrees;
        message.halfX = state.halfX;
        message.halfY = state.halfY;
        message.halfZ = state.halfZ;
        message.velocityX = state.velocityX;
        message.velocityY = state.velocityY;
        message.velocityZ = state.velocityZ;
        message.temperature = state.temperature;
        message.pressure = state.pressure;
        message.density = state.density;
        message.cohesion = state.cohesion;
        message.rigidity = state.rigidity;
        message.viscosity = state.viscosity;
        message.earthPath = state.earthPath;
        message.waterPath = state.waterPath;
        message.windPath = state.windPath;
        message.firePath = state.firePath;
        message.coldPath = state.coldPath;
        message.rulePath = state.rulePath;
        message.lifetimeTicksRemaining = state.lifetimeTicksRemaining;
        return message;
    }

    public BattleArenaGuObjectState toState() {
        return new BattleArenaGuObjectState(
                id,
                ownerPlayerId,
                material,
                x,
                y,
                z,
                headingDegrees,
                halfX,
                halfY,
                halfZ,
                velocityX,
                velocityY,
                velocityZ,
                temperature,
                pressure,
                density,
                cohesion,
                rigidity,
                viscosity,
                earthPath,
                waterPath,
                windPath,
                firePath,
                coldPath,
                rulePath,
                lifetimeTicksRemaining);
    }
}
