package com.njst.gaming;

import com.njst.gaming.Geometries.TerrainGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.objects.TerrainObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class OpenWorldTerrainManager {
    private static final float DETAIL_TEXTURE_SCALE = 12f;
    private static final int CONTROL_TILE_GRID_SIZE = 5;

    private final Scene scene;
    private final GraphicsDevice graphicsDevice;
    private final int[] terrainTextures;
    private final int controlMapTexture;
    private final OpenWorldTerrainState state;
    private final HashMap<String, GameObject> activeChunks = new HashMap<>();

    public OpenWorldTerrainManager(Scene scene, GraphicsDevice graphicsDevice, int[] terrainTextures,
            int controlMapTexture, OpenWorldTerrainState state) {
        if (terrainTextures == null || terrainTextures.length != 4) {
            throw new IllegalArgumentException("OpenWorldTerrainManager requires 4 terrain textures.");
        }
        this.scene = scene;
        this.graphicsDevice = graphicsDevice;
        this.terrainTextures = terrainTextures.clone();
        this.controlMapTexture = controlMapTexture;
        this.state = state;
    }

    public void update(Vector3 cameraPosition) {
        if (cameraPosition == null) {
            return;
        }

        int centerChunkX = worldToChunk(cameraPosition.x, state.chunkSize);
        int centerChunkZ = worldToChunk(cameraPosition.z, state.chunkSize);
        for (int dx = -state.renderDistance; dx <= state.renderDistance; dx++) {
            for (int dz = -state.renderDistance; dz <= state.renderDistance; dz++) {
                ensureChunk(centerChunkX + dx, centerChunkZ + dz);
            }
        }
        unloadFarChunks(centerChunkX, centerChunkZ);
    }

    public float getHeightAt(float worldX, float worldZ) {
        int chunkX = worldToChunk(worldX, state.chunkSize);
        int chunkZ = worldToChunk(worldZ, state.chunkSize);
        GameObject chunk = ensureChunk(chunkX, chunkZ);
        TerrainGeometry geometry = (TerrainGeometry) chunk.geometry;
        float localX = worldX - chunk.position.x;
        float localZ = worldZ - chunk.position.z;
        return chunk.position.y + geometry.sampleHeight(localX, localZ);
    }

    public OpenWorldTerrainState getState() {
        return state;
    }

    private GameObject ensureChunk(int chunkX, int chunkZ) {
        String key = chunkKey(chunkX, chunkZ);
        GameObject chunk = activeChunks.get(key);
        if (chunk != null) {
            return chunk;
        }

        int worldStartX = chunkX * state.chunkSize;
        int worldStartZ = chunkZ * state.chunkSize;
        TerrainGeometry geometry = TerrainGeometry.createChunk(state.chunkSize + 1, state.chunkSize + 1,
                state.seed, worldStartX, worldStartZ, state.noiseScale, state.heightScale,
                state.erosionIterations, state.erosionStrength, state.erosionThreshold, state.erosionPadding);

        int tileX = floorMod(chunkX, CONTROL_TILE_GRID_SIZE);
        int tileZ = floorMod(chunkZ, CONTROL_TILE_GRID_SIZE);
        float tileScale = 1f / CONTROL_TILE_GRID_SIZE;
        float tileOffsetX = tileX * tileScale;
        float tileOffsetY = tileZ * tileScale;

        chunk = new TerrainObject(geometry, terrainTextures, controlMapTexture, DETAIL_TEXTURE_SCALE, state.chunkSize,
                tileOffsetX, tileOffsetY, tileScale);
        chunk.name = "terrain_" + chunkX + "_" + chunkZ;
        chunk.setGraphicsDevice(graphicsDevice);
        chunk.setPosition(worldStartX, 0, worldStartZ);
        chunk.generateBuffers();
        scene.addGameObject(chunk);
        activeChunks.put(key, chunk);
        return chunk;
    }

    private void unloadFarChunks(int centerChunkX, int centerChunkZ) {
        Iterator<Map.Entry<String, GameObject>> iterator = activeChunks.entrySet().iterator();
        ArrayList<GameObject> chunksToRemove = new ArrayList<>();
        while (iterator.hasNext()) {
            Map.Entry<String, GameObject> entry = iterator.next();
            int[] coords = parseChunkKey(entry.getKey());
            if (java.lang.Math.abs(coords[0] - centerChunkX) > state.renderDistance
                    || java.lang.Math.abs(coords[1] - centerChunkZ) > state.renderDistance) {
                chunksToRemove.add(entry.getValue());
                iterator.remove();
            }
        }

        for (GameObject chunk : chunksToRemove) {
            chunk.cleanup();
            scene.removeGameObject(chunk);
        }
    }

    private int floorMod(int value, int divisor) {
        int result = value % divisor;
        return result < 0 ? result + divisor : result;
    }

    private int worldToChunk(float worldCoordinate, int chunkSize) {
        return (int) java.lang.Math.floor(worldCoordinate / chunkSize);
    }

    private String chunkKey(int chunkX, int chunkZ) {
        return chunkX + ":" + chunkZ;
    }

    private int[] parseChunkKey(String key) {
        String[] parts = key.split(":");
        return new int[] { Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) };
    }
}
