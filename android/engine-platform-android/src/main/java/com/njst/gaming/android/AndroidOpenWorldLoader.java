package com.njst.gaming.android;

import android.content.Context;

import com.google.gson.Gson;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.OpenWorldTerrainManager;
import com.njst.gaming.OpenWorldTerrainState;
import com.njst.gaming.Scene;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.objects.GameObject;

public class AndroidOpenWorldLoader implements Scene.SceneLoader {
    private static final Gson GSON = new Gson();
    private final Context context;

    public AndroidOpenWorldLoader(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void load(Scene scene) {
        int skyboxTexture = scene.renderer.getGraphicsDevice().loadTexture("desertstorm.jpg");
        int terrainTexture = scene.renderer.getGraphicsDevice().loadTexture("j.jpg");

        GameObject skybox = new GameObject(new SphereGeometry(1, 20, 20), skyboxTexture);
        skybox.ambientlight_multiplier = 5;
        skybox.shininess = 1;
        skybox.scale = new float[] { 500, 500, 500 };
        skybox.updateModelMatrix();
        scene.renderer.skybox = skybox;
        scene.addGameObject(skybox);

        OpenWorldTerrainState state = loadState();
        OpenWorldTerrainManager terrainManager = new OpenWorldTerrainManager(
                scene,
                scene.renderer.getGraphicsDevice(),
                terrainTexture,
                state);
        scene.enableOpenWorld(terrainManager);

        scene.renderer.camera.lookAt(new Vector3(0f, 18f, -20f), new Vector3(0f, 6f, 0f), new Vector3(0f, 1f, 0f));
        terrainManager.update(scene.renderer.camera.cameraPosition);
        float spawnHeight = terrainManager.getHeightAt(0f, 0f);
        scene.renderer.camera.cameraPosition.y = spawnHeight + 18f;
        scene.renderer.camera.targetPosition.y = spawnHeight + 6f;
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
        }
        return defaultState();
    }

    private OpenWorldTerrainState defaultState() {
        OpenWorldTerrainState state = new OpenWorldTerrainState();
        state.seed = 7153246945977311624L;
        state.chunkSize = 32;
        state.renderDistance = 2;
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
            state.renderDistance = 2;
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
