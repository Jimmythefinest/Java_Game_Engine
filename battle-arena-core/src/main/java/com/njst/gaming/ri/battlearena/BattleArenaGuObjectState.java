package com.njst.gaming.ri.battlearena;

public final class BattleArenaGuObjectState {
    public final int id;
    public final String ownerPlayerId;
    public final String material;
    public final float x;
    public final float y;
    public final float z;
    public final float headingDegrees;
    public final float halfX;
    public final float halfY;
    public final float halfZ;
    public final float velocityX;
    public final float velocityY;
    public final float velocityZ;
    public final float temperature;
    public final float pressure;
    public final float density;
    public final float cohesion;
    public final float rigidity;
    public final float viscosity;
    public final float earthPath;
    public final float waterPath;
    public final float windPath;
    public final float firePath;
    public final float coldPath;
    public final float rulePath;
    public final int lifetimeTicksRemaining;

    public BattleArenaGuObjectState(int id,
                                    String ownerPlayerId,
                                    String material,
                                    float x,
                                    float y,
                                    float z,
                                    float headingDegrees,
                                    float halfX,
                                    float halfY,
                                    float halfZ,
                                    float velocityX,
                                    float velocityY,
                                    float velocityZ,
                                    float temperature,
                                    float pressure,
                                    float density,
                                    float cohesion,
                                    float rigidity,
                                    float viscosity,
                                    float earthPath,
                                    float waterPath,
                                    float windPath,
                                    float firePath,
                                    float coldPath,
                                    float rulePath,
                                    int lifetimeTicksRemaining) {
        this.id = id;
        this.ownerPlayerId = ownerPlayerId;
        this.material = material;
        this.x = x;
        this.y = y;
        this.z = z;
        this.headingDegrees = headingDegrees;
        this.halfX = halfX;
        this.halfY = halfY;
        this.halfZ = halfZ;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.temperature = temperature;
        this.pressure = pressure;
        this.density = density;
        this.cohesion = cohesion;
        this.rigidity = rigidity;
        this.viscosity = viscosity;
        this.earthPath = earthPath;
        this.waterPath = waterPath;
        this.windPath = windPath;
        this.firePath = firePath;
        this.coldPath = coldPath;
        this.rulePath = rulePath;
        this.lifetimeTicksRemaining = lifetimeTicksRemaining;
    }
}
