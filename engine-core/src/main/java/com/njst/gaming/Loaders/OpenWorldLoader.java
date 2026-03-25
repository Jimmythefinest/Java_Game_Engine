package com.njst.gaming.Loaders;

import com.njst.gaming.OpenWorldTerrainManager;
import com.njst.gaming.OpenWorldTerrainState;
import com.njst.gaming.Scene;
import com.njst.gaming.data;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.objects.GameObject;

public class OpenWorldLoader implements Scene.SceneLoader {
    private static final String WORLD_STATE_PATH = data.rootDirectory + "/world/open_world.json";

    @Override
    public void load(Scene scene) {
        int skyboxTexture = scene.renderer.getGraphicsDevice().loadTexture(data.rootDirectory + "/desertstorm.jpg");
        int terrainTexture = scene.renderer.getGraphicsDevice().loadTexture(data.rootDirectory + "/j.jpg");

        GameObject skybox = new GameObject(new SphereGeometry(1, 20, 20), skyboxTexture);
        skybox.ambientlight_multiplier = 5;
        skybox.shininess = 1;
        skybox.scale = new float[] { 500, 500, 500 };
        skybox.updateModelMatrix();
        scene.renderer.skybox = skybox;
        scene.addGameObject(skybox);

        OpenWorldTerrainState state = OpenWorldTerrainState.loadOrCreate(WORLD_STATE_PATH);
        OpenWorldTerrainManager terrainManager = new OpenWorldTerrainManager(scene,
                scene.renderer.getGraphicsDevice(), terrainTexture, state);
        scene.enableOpenWorld(terrainManager);

        scene.renderer.camera.lookAt(new Vector3(0f, 18f, -20f), new Vector3(0f, 6f, 0f), new Vector3(0f, 1f, 0f));
        terrainManager.update(scene.renderer.camera.cameraPosition);
        float spawnHeight = terrainManager.getHeightAt(0f, 0f);
        scene.renderer.camera.cameraPosition.y = spawnHeight + 18f;
        scene.renderer.camera.targetPosition.y = spawnHeight + 6f;
    }
}
