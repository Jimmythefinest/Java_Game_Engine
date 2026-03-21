package com.njst.gaming;

import com.njst.gaming.Geometries.TerrainGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.objects.GameObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class OpenWorldTerrainManager {
    private final Scene scene;
    private final GraphicsDevice graphicsDevice;
    private final int terrainTexture;
    private final OpenWorldTerrainState state;
    private final HashMap<String, GameObject> activeChunks = new HashMap<>();

    public OpenWorldTerrainManager(Scene scene, GraphicsDevice graphicsDevice, int terrainTexture,
            OpenWorldTerrainState state) {
        this.scene = scene;
        this.graphicsDevice = graphicsDevice;
        this.terrainTexture = terrainTexture;
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
                state.seed, worldStartX, worldStartZ, state.noiseScale, state.heightScale);
        chunk = new GameObject(geometry, terrainTexture);
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
