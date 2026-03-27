package com.njst.gaming.android;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.OpenWorldTerrainManager;
import com.njst.gaming.OpenWorldTerrainState;
import com.njst.gaming.Scene;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.objects.GameObject;

public class AndroidOpenWorldLoader implements Scene.SceneLoader {
    private static final String TAG = "NJST";
    private static final Gson GSON = new Gson();
    private static final String CONTROL_MAP_PATH = "terrain_control_map.png";
    private final Context context;

    public AndroidOpenWorldLoader(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void load(Scene scene) {
        Log.i(TAG, "AndroidOpenWorldLoader.load start");
        GraphicsDevice graphicsDevice = scene.renderer.getGraphicsDevice();
        int skyboxTexture = graphicsDevice.loadTexture("desertstorm.jpg");
        int[] terrainTextures = new int[] {
                graphicsDevice.loadTexture("terrain_texture.jpeg"),
                graphicsDevice.loadTexture("j.jpg"),
                graphicsDevice.loadTexture("stone.jpeg"),
                graphicsDevice.loadTexture("images (2).jpeg")
        };
        int controlMapTexture = loadControlMapTexture(graphicsDevice, CONTROL_MAP_PATH);
        Log.i(TAG, "Loaded texture handles skybox=" + skyboxTexture + " terrain0=" + terrainTextures[0] + " terrain1=" + terrainTextures[1] + " terrain2=" + terrainTextures[2] + " terrain3=" + terrainTextures[3] + " control=" + controlMapTexture);

        GameObject skybox = new GameObject(new SphereGeometry(1, 20, 20), skyboxTexture);
        skybox.ambientlight_multiplier = 5;
        skybox.shininess = 1;
        skybox.scale = new float[] { 500, 500, 500 };
        skybox.updateModelMatrix();
        scene.renderer.skybox = skybox;
        scene.addGameObject(skybox);

        OpenWorldTerrainState state = loadState();
        Log.i(TAG, "Open world state seed=" + state.seed + " chunkSize=" + state.chunkSize + " renderDistance=" + state.renderDistance + " noiseScale=" + state.noiseScale + " heightScale=" + state.heightScale);
        OpenWorldTerrainManager terrainManager = new OpenWorldTerrainManager(
                scene,
                graphicsDevice,
                terrainTextures,
                controlMapTexture,
                state);
        scene.enableOpenWorld(terrainManager);

        scene.renderer.camera.lookAt(new Vector3(0f, 18f, -20f), new Vector3(0f, 6f, 0f), new Vector3(0f, 1f, 0f));
        terrainManager.update(scene.renderer.camera.cameraPosition);
        float spawnHeight = terrainManager.getHeightAt(0f, 0f);
        scene.renderer.camera.cameraPosition.y = spawnHeight + 18f;
        scene.renderer.camera.targetPosition.y = spawnHeight + 6f;
        Log.i(TAG, "Open world spawnHeight=" + spawnHeight + " cameraY=" + scene.renderer.camera.cameraPosition.y);
    }

    private int loadControlMapTexture(GraphicsDevice graphicsDevice, String path) {
        int texture = graphicsDevice.loadTexture(path);
        if (texture != 0) {
            Log.i(TAG, "Using control map texture asset " + path + " -> id=" + texture);
            return texture;
        }
        Log.e(TAG, "Control map texture missing, creating fallback green RGBA texture for " + path);
        return graphicsDevice.createTextureRGBA(1, 1, new byte[] { 0, (byte) 255, 0, 0 });
    }

    private OpenWorldTerrainState loadState() {
        try {
            String json = AndroidAssetLoader.readText(context.getAssets(), "world/open_world.json");
            OpenWorldTerrainState state = GSON.fromJson(json, OpenWorldTerrainState.class);
            if (state != null) {
                applyDefaults(state);
                return state;
            }
        } catch (RuntimeException ignored) {
            Log.e(TAG, "Failed to read Android open world state asset, falling back to defaults", ignored);
        }
        return defaultState();
    }

    private OpenWorldTerrainState defaultState() {
        OpenWorldTerrainState state = new OpenWorldTerrainState();
        state.seed = 7153246945977311624L;
        state.chunkSize = 32;
        state.renderDistance = 4;
        state.noiseScale = 48f;
        state.heightScale = 12f;
        state.erosionIterations = 18;
        state.erosionStrength = 0.22f;
        state.erosionThreshold = 0.35f;
        state.erosionPadding = 12;
        return state;
    }

    private void applyDefaults(OpenWorldTerrainState state) {
        if (state.chunkSize <= 0) {
            state.chunkSize = 32;
        }
        if (state.renderDistance <= 0) {
            state.renderDistance = 4;
        }
        if (state.noiseScale <= 0f) {
            state.noiseScale = 48f;
        }
        if (state.heightScale <= 0f) {
            state.heightScale = 12f;
        }
        if (state.erosionIterations < 0) {
            state.erosionIterations = 18;
        }
        if (state.erosionStrength <= 0f) {
            state.erosionStrength = 0.22f;
        }
        if (state.erosionThreshold <= 0f) {
            state.erosionThreshold = 0.35f;
        }
        if (state.erosionPadding <= 0) {
            state.erosionPadding = 12;
        }
        if (state.seed == 0L) {
            state.seed = 7153246945977311624L;
        }
    }
}
