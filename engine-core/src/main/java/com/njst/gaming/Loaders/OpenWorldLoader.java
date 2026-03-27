package com.njst.gaming.Loaders;

import com.njst.gaming.OpenWorldTerrainManager;
import com.njst.gaming.OpenWorldTerrainState;
import com.njst.gaming.Scene;
import com.njst.gaming.data;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.objects.GameObject;

public class OpenWorldLoader implements Scene.SceneLoader {
    private static final String WORLD_STATE_PATH = data.rootDirectory + "/world/open_world.json";
    private static final String CONTROL_MAP_PATH = data.rootDirectory + "/terrain_control_map.png";
    
    @Override
    public void load(Scene scene) {
        GraphicsDevice graphicsDevice = scene.renderer.getGraphicsDevice();
        int skyboxTexture = graphicsDevice.loadTexture(data.rootDirectory + "/desertstorm.jpg");
        int[] terrainTextures = new int[] {
                graphicsDevice.loadTexture(data.rootDirectory + "/terrain_texture.jpeg"),
                graphicsDevice.loadTexture(data.rootDirectory + "/j.jpg"),
                graphicsDevice.loadTexture(data.rootDirectory + "/stone.jpeg"),
                graphicsDevice.loadTexture(data.rootDirectory + "/images (2).jpeg")
        };
        int controlMapTexture = loadControlMapTexture(graphicsDevice, CONTROL_MAP_PATH);

        GameObject skybox = new GameObject(new SphereGeometry(1, 20, 20), skyboxTexture);
        skybox.ambientlight_multiplier = 5;
        skybox.shininess = 1;
        skybox.scale = new float[] { 500, 500, 500 };
        skybox.updateModelMatrix();
        scene.renderer.skybox = skybox;
        scene.addGameObject(skybox);

        OpenWorldTerrainState state = OpenWorldTerrainState.loadOrCreate(WORLD_STATE_PATH);
        OpenWorldTerrainManager terrainManager = new OpenWorldTerrainManager(scene,
                graphicsDevice, terrainTextures, controlMapTexture, state);
        scene.enableOpenWorld(terrainManager);

        scene.renderer.camera.lookAt(new Vector3(0f, 18f, -20f), new Vector3(0f, 6f, 0f), new Vector3(0f, 1f, 0f));
        terrainManager.update(scene.renderer.camera.cameraPosition);
        float spawnHeight = terrainManager.getHeightAt(0f, 0f);
        scene.renderer.camera.cameraPosition.y = spawnHeight + 18f;
        scene.renderer.camera.targetPosition.y = spawnHeight + 6f;
    }

    private int loadControlMapTexture(GraphicsDevice graphicsDevice, String path) {
        int texture = graphicsDevice.loadTexture(path);
        if (texture != 0) {
            return texture;
        }
        return graphicsDevice.createTextureRGBA(1, 1, new byte[] { 0, (byte) 255, 0, 0 });
    }
}
