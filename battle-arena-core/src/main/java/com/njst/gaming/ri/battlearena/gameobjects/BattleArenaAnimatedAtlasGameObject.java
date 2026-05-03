package com.njst.gaming.ri.battlearena.gameobjects;

import com.njst.gaming.Camera;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.objects.GameObject;

public final class BattleArenaAnimatedAtlasGameObject extends GameObject {
    private static final float DEFAULT_FRAME_RATE = 18f;

    private final BattleArenaLayeredAtlasGeometry atlasGeometry;
    private final int columns;
    private final int rows;
    private final float frameRate;
    private float elapsedSeconds;
    private int currentFrame = -1;

    public BattleArenaAnimatedAtlasGameObject(int texture, int columns, int rows) {
        this(texture, columns, rows, DEFAULT_FRAME_RATE, 4);
    }

    public BattleArenaAnimatedAtlasGameObject(int texture, int columns, int rows, float frameRate) {
        this(texture, columns, rows, frameRate, 4);
    }

    public BattleArenaAnimatedAtlasGameObject(int texture,
                                             int columns,
                                             int rows,
                                             float frameRate,
                                             int layerCount) {
        super(new BattleArenaLayeredAtlasGeometry(columns, rows, 0, layerCount), texture);
        this.atlasGeometry = (BattleArenaLayeredAtlasGeometry) geometry;
        this.columns = Math.max(1, columns);
        this.rows = Math.max(1, rows);
        this.frameRate = Math.max(1f, frameRate);
        this.castsShadows = false;
        this.ambientlight_multiplier = 8f;
        this.shininess = 1f;
    }

    public void updateVisual(float deltaSeconds, Camera camera) {
        elapsedSeconds += Math.max(0f, deltaSeconds);
        int frameCount = columns * rows;
        int nextFrame = ((int) (elapsedSeconds * frameRate)) % frameCount;
        if (nextFrame != currentFrame) {
            currentFrame = nextFrame;
            atlasGeometry.setFrame(columns, rows, currentFrame);
            cleanup();
        }
        faceCamera(camera);
    }

    private void faceCamera(Camera camera) {
        if (camera == null || camera.cameraPosition == null) {
            return;
        }
        Vector3 cameraPosition = camera.cameraPosition;
        float dx = cameraPosition.x - position.x;
        float dz = cameraPosition.z - position.z;
        if (Math.abs(dx) < 0.0001f && Math.abs(dz) < 0.0001f) {
            return;
        }
        setRotation(0f, (float) Math.toDegrees(Math.atan2(dx, dz)), 0f);
    }
}
