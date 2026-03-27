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

    private final Scene scene;
    private final GraphicsDevice graphicsDevice;
    private final int[] terrainTextures;
    private final OpenWorldTerrainState state;
    private final HashMap<String, GameObject> activeChunks = new HashMap<>();

    public OpenWorldTerrainManager(Scene scene, GraphicsDevice graphicsDevice, int[] terrainTextures,
            OpenWorldTerrainState state) {
        if (terrainTextures == null || terrainTextures.length != 4) {
            throw new IllegalArgumentException("OpenWorldTerrainManager requires 4 terrain textures.");
        }
        this.scene = scene;
        this.graphicsDevice = graphicsDevice;
        this.terrainTextures = terrainTextures.clone();
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
        int splatTexture = graphicsDevice.createTextureRGBA(
                geometry.heightMap.length,
                geometry.heightMap[0].length,
                buildSplatMap(geometry));
        chunk = new TerrainObject(geometry, terrainTextures, splatTexture, DETAIL_TEXTURE_SCALE, state.chunkSize);
        chunk.name = "terrain_" + chunkX + "_" + chunkZ;
        chunk.setGraphicsDevice(graphicsDevice);
        chunk.setPosition(worldStartX, 0, worldStartZ);
        chunk.generateBuffers();
        scene.addGameObject(chunk);
        activeChunks.put(key, chunk);
        return chunk;
    }

    private byte[] buildSplatMap(TerrainGeometry geometry) {
        float[][] heightMap = geometry.heightMap;
        int width = heightMap.length;
        int depth = heightMap[0].length;
        byte[] rgba = new byte[width * depth * 4];

        float minHeight = Float.MAX_VALUE;
        float maxHeight = -Float.MAX_VALUE;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                float value = heightMap[x][z];
                minHeight = java.lang.Math.min(minHeight, value);
                maxHeight = java.lang.Math.max(maxHeight, value);
            }
        }
        float heightRange = java.lang.Math.max(0.001f, maxHeight - minHeight);

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                float normalizedHeight = (heightMap[x][z] - minHeight) / heightRange;
                float slope = computeSlope(heightMap, x, z);

                float layer0 = clamp01((1f - normalizedHeight * 2.4f) * (1f - slope * 0.65f));
                float layer1 = clamp01((1f - java.lang.Math.abs(normalizedHeight - 0.35f) * 3.0f) * (1f - slope * 0.45f));
                float layer2 = clamp01(slope * 1.15f + normalizedHeight * 0.15f);
                float layer3 = clamp01(smoothstep(0.68f, 0.92f, normalizedHeight) * (0.35f + (1f - slope) * 0.65f));

                float total = layer0 + layer1 + layer2 + layer3;
                if (total <= 0.0001f) {
                    layer1 = 1f;
                    total = 1f;
                }

                int index = (x * depth + z) * 4;
                rgba[index] = toByte(layer0 / total);
                rgba[index + 1] = toByte(layer1 / total);
                rgba[index + 2] = toByte(layer2 / total);
                rgba[index + 3] = toByte(layer3 / total);
            }
        }
        return rgba;
    }

    private float computeSlope(float[][] heightMap, int x, int z) {
        int maxX = heightMap.length - 1;
        int maxZ = heightMap[0].length - 1;
        int left = java.lang.Math.max(0, x - 1);
        int right = java.lang.Math.min(maxX, x + 1);
        int down = java.lang.Math.max(0, z - 1);
        int up = java.lang.Math.min(maxZ, z + 1);

        float dx = heightMap[right][z] - heightMap[left][z];
        float dz = heightMap[x][up] - heightMap[x][down];
        return clamp01((float) java.lang.Math.sqrt((dx * dx) + (dz * dz)) / 6f);
    }

    private float smoothstep(float edge0, float edge1, float value) {
        float t = clamp01((value - edge0) / (edge1 - edge0));
        return t * t * (3f - (2f * t));
    }

    private float clamp01(float value) {
        return java.lang.Math.max(0f, java.lang.Math.min(1f, value));
    }

    private byte toByte(float value) {
        return (byte) java.lang.Math.round(clamp01(value) * 255f);
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
