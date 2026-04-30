package com.njst.gaming.ri.battlearena.gameobjects;

import com.njst.gaming.Camera;
import com.njst.gaming.Geometries.CustomGeometry;
import com.njst.gaming.Geometries.Geometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.graphics.NullGraphicsDevice;
import com.njst.gaming.graphics.ShaderHandle;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.ri.battlearena.BattleArenaControlledCharacter;

public final class BattleArenaHealthBarGameObject extends GameObject {
    private static final int TEXTURE_WIDTH = 96;
    private static final int TEXTURE_HEIGHT = 12;

    private final BattleArenaControlledCharacter character;
    private final Camera camera;
    private final float widthWorldUnits;
    private final float heightWorldUnits;
    private final float verticalOffset;
    private float lastRenderedHealthRatio = -1f;
    private int generatedTextureId = 0;

    public BattleArenaHealthBarGameObject(BattleArenaControlledCharacter character,
                                          Camera camera,
                                          String name,
                                          float widthWorldUnits,
                                          float heightWorldUnits,
                                          float verticalOffset) {
        super(createQuadGeometry(), 0);
        this.character = character;
        this.camera = camera;
        this.widthWorldUnits = widthWorldUnits;
        this.heightWorldUnits = heightWorldUnits;
        this.verticalOffset = verticalOffset;
        this.name = name;
        this.castsShadows = false;
        this.ambientlight_multiplier = 12f;
        this.shininess = 1f;
        setScale(widthWorldUnits, heightWorldUnits, 1f);
    }

    @Override
    public void updateModelMatrix() {
        updatePlacement();
        super.updateModelMatrix();
    }

    @Override
    public void render(ShaderHandle shader, int textureHandle) {
        ensureTexture();
        if (texture == 0) {
            return;
        }
        super.render(shader, textureHandle);
    }

    @Override
    public void cleanup() {
        releaseGeneratedTexture();
        super.cleanup();
    }

    private void updatePlacement() {
        Vector3 position = character.runtime.getPosition();
        setPosition(position.x, position.y + verticalOffset, position.z);

        if (camera == null || camera.cameraPosition == null) {
            return;
        }

        float dx = camera.cameraPosition.x - position.x;
        float dz = camera.cameraPosition.z - position.z;
        if (Math.abs(dx) < 0.0001f && Math.abs(dz) < 0.0001f) {
            return;
        }
        float yawDegrees = (float) Math.toDegrees(Math.atan2(dx, dz));
        setRotation(0f, yawDegrees, 0f);
    }

    private void ensureTexture() {
        if (graphicsDevice instanceof NullGraphicsDevice) {
            return;
        }
        float healthRatio = character.getHealthRatio();
        if (texture != 0 && Math.abs(healthRatio - lastRenderedHealthRatio) < 0.0001f) {
            return;
        }

        byte[] pixels = buildHealthBarTexture(healthRatio);
        int newTextureId = graphicsDevice.createTextureRGBA(TEXTURE_WIDTH, TEXTURE_HEIGHT, pixels);
        releaseGeneratedTexture();
        generatedTextureId = newTextureId;
        texture = newTextureId;
        lastRenderedHealthRatio = healthRatio;
    }

    private void releaseGeneratedTexture() {
        if (generatedTextureId != 0 && !(graphicsDevice instanceof NullGraphicsDevice)) {
            graphicsDevice.releaseTexture(generatedTextureId);
        }
        generatedTextureId = 0;
        texture = 0;
    }

    private byte[] buildHealthBarTexture(float healthRatio) {
        byte[] rgba = new byte[TEXTURE_WIDTH * TEXTURE_HEIGHT * 4];
        int fillWidth = Math.round((TEXTURE_WIDTH - 4) * Math.max(0f, Math.min(1f, healthRatio)));
        int[] fillColor = resolveFillColor(healthRatio);

        for (int y = 0; y < TEXTURE_HEIGHT; y++) {
            for (int x = 0; x < TEXTURE_WIDTH; x++) {
                int index = (y * TEXTURE_WIDTH + x) * 4;
                boolean border = x == 0 || y == 0 || x == TEXTURE_WIDTH - 1 || y == TEXTURE_HEIGHT - 1;
                boolean innerFrame = x == 1 || y == 1 || x == TEXTURE_WIDTH - 2 || y == TEXTURE_HEIGHT - 2;
                if (border) {
                    writePixel(rgba, index, 10, 10, 10, 220);
                } else if (innerFrame) {
                    writePixel(rgba, index, 35, 35, 35, 210);
                } else if ((x - 2) < fillWidth) {
                    writePixel(rgba, index, fillColor[0], fillColor[1], fillColor[2], 235);
                } else {
                    writePixel(rgba, index, 45, 12, 12, 160);
                }
            }
        }

        return rgba;
    }

    private int[] resolveFillColor(float healthRatio) {
        if (healthRatio > 0.6f) {
            return new int[] { 72, 214, 104 };
        }
        if (healthRatio > 0.3f) {
            return new int[] { 234, 193, 70 };
        }
        return new int[] { 220, 73, 73 };
    }

    private void writePixel(byte[] rgba, int index, int r, int g, int b, int a) {
        rgba[index] = (byte) r;
        rgba[index + 1] = (byte) g;
        rgba[index + 2] = (byte) b;
        rgba[index + 3] = (byte) a;
    }

    private static Geometry createQuadGeometry() {
        float[] vertices = {
                -0.5f, -0.5f, 0f,
                 0.5f, -0.5f, 0f,
                 0.5f,  0.5f, 0f,
                -0.5f,  0.5f, 0f
        };
        int[] indices = { 0, 1, 2, 0, 2, 3 };
        float[] normals = {
                0f, 0f, 1f,
                0f, 0f, 1f,
                0f, 0f, 1f,
                0f, 0f, 1f
        };
        float[] uv = {
                0f, 0f,
                1f, 0f,
                1f, 1f,
                0f, 1f
        };
        return new CustomGeometry(vertices, indices, normals, uv);
    }
}
