package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Bone;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Geometries.TerrainGeometry;
import com.njst.gaming.Geometries.WeightedGeometry;
import com.njst.gaming.Loaders.FBXBoneLoader;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Scene;
import com.njst.gaming.objects.GameObject;

import java.util.ArrayList;
import java.util.HashMap;

public class BattleArenaDesktopModelLoader implements Scene.SceneLoader {
    private static final String SKYBOX_PATH = com.njst.gaming.data.rootDirectory + "/desertstorm.jpg";
    private static final String GROUND_PATH = com.njst.gaming.data.rootDirectory + "/j.jpg";
    private static final String MODEL_PATH = com.njst.gaming.data.rootDirectory + "/Defeated.fbx";
    private static final String MODEL_TEXTURE_PATH = com.njst.gaming.data.rootDirectory + "/j.jpg";
    private static final float MODEL_SCALE = 0.01f;
    private static final int GROUND_SIZE = 96;
    private static final int[] MODEL_MESH_IDS = new int[] { 0, 1 };

    @Override
    public void load(Scene scene) {
        int skyboxTexture = scene.renderer.getGraphicsDevice().loadTexture(SKYBOX_PATH);
        int groundTexture = scene.renderer.getGraphicsDevice().loadTexture(GROUND_PATH);

        GameObject skybox = new GameObject(new SphereGeometry(1f, 20, 20), skyboxTexture);
        skybox.ambientlight_multiplier = 5f;
        skybox.shininess = 1f;
        skybox.setScale(100f, 100f, 100f);
        skybox.setPosition(0f, 0f, 0f);
        scene.renderer.skybox = skybox;
        scene.addGameObject(skybox);

        GameObject ground = new GameObject(
                new TerrainGeometry(GROUND_SIZE, GROUND_SIZE, new float[GROUND_SIZE][GROUND_SIZE]),
                groundTexture);
        ground.ambientlight_multiplier = 1.35f;
        ground.shininess = 3f;
        ground.setPosition(-GROUND_SIZE * 0.5f, -0.75f, -GROUND_SIZE * 0.5f);
        scene.addGameObject(ground);

        Bone rootBone = FBXBoneLoader.loadBones(MODEL_PATH, new HashMap<>(), 100.0f);
        ArrayList<Bone> bones = FBXBoneLoader.get_array(rootBone);
        int texture = scene.renderer.getGraphicsDevice().loadTexture(MODEL_TEXTURE_PATH);

        for (int i = 0; i < MODEL_MESH_IDS.length; i++) {
            WeightedGeometry geometry = FBXBoneLoader.loadModel(MODEL_PATH, bones, MODEL_MESH_IDS[i], 100.0f);
            GameObject meshObject = new GameObject(geometry, texture);
            meshObject.name = "DesktopFbxMesh" + MODEL_MESH_IDS[i];
            meshObject.shininess = 18f;
            meshObject.ambientlight_multiplier = 1.2f;
            meshObject.setScale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
            meshObject.setPosition(0f, -0.75f, 0f);
            scene.addGameObject(meshObject);
        }

        scene.renderer.camera.lookAt(new Vector3(0f, 1.8f, -6.5f), new Vector3(0f, 1.2f, 0f), new Vector3(0f, 1f, 0f));
    }
}
