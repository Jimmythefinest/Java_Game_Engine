package com.njst.gaming.objects;

import com.njst.gaming.Geometries.Geometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.graphics.ShaderHandle;

public class TerrainObject extends GameObject {
    private final int[] terrainTextures;
    private final int controlMapTexture;
    private final float detailTextureScale;
    private final float chunkWorldSize;
    private final float controlTileOffsetX;
    private final float controlTileOffsetY;
    private final float controlTileScale;

    public TerrainObject(Geometry geometry, int[] terrainTextures, int controlMapTexture,
            float detailTextureScale, float chunkWorldSize,
            float controlTileOffsetX, float controlTileOffsetY, float controlTileScale) {
        super(geometry, terrainTextures[0]);
        if (terrainTextures == null || terrainTextures.length != 4) {
            throw new IllegalArgumentException("TerrainObject requires exactly 4 terrain textures.");
        }
        this.terrainTextures = terrainTextures.clone();
        this.controlMapTexture = controlMapTexture;
        this.detailTextureScale = detailTextureScale;
        this.chunkWorldSize = chunkWorldSize;
        this.controlTileOffsetX = controlTileOffsetX;
        this.controlTileOffsetY = controlTileOffsetY;
        this.controlTileScale = controlTileScale;
    }

    @Override
    public void render(ShaderHandle shader, int textureHandle) {
        if (!buffers_generated()) {
            generateBuffers();
        }

        if (this.shaderprogram == null) {
            shaderprogram = shader;
        }
        shaderprogram.setUniformVector3("properties", new Vector3(shininess, ambientlight_multiplier, 0f));
        shaderprogram.setUniformVector3("terrainBlendConfig",
                new Vector3(detailTextureScale, chunkWorldSize, controlTileScale));
        shaderprogram.setUniformVector3("terrainControlOffset",
                new Vector3(controlTileOffsetX, controlTileOffsetY, 0f));
        shaderprogram.setUniformMatrix4fv("uMMatrix", modelMatrix);
        shaderprogram.activateTexture("uTexture0", 0, terrainTextures[0]);
        shaderprogram.activateTexture("uTexture1", 1, terrainTextures[1]);
        shaderprogram.activateTexture("uTexture2", 2, terrainTextures[2]);
        shaderprogram.activateTexture("uTexture3", 3, terrainTextures[3]);
        shaderprogram.activateTexture("uSplatMap", 4, controlMapTexture);

        graphicsDevice.bindVertexArray(vaoIds[0]);
        graphicsDevice.drawElementsTriangles(geometry.getIndices().length);
        graphicsDevice.bindVertexArray(0);
    }

    private boolean buffers_generated() {
        return vaoIds[0] != 0;
    }
}
