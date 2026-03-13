package com.njst.gaming.Loaders;

import com.njst.gaming.Scene;
import com.njst.gaming.data;
import com.njst.gaming.Geometries.PlantConfig;
import com.njst.gaming.Geometries.PlantGeometry;
import com.njst.gaming.Geometries.PlantSeed;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.objects.LODGameObject;

public class CollisionBoxDemoLoader implements Scene.SceneLoader {

    @Override
    public void load(Scene scene) {
        int plantTexture = scene.renderer.getGraphicsDevice().loadTexture(data.rootDirectory + "/images (2).jpeg");
        int skyboxTexture = scene.renderer.getGraphicsDevice().loadTexture(data.rootDirectory + "/desertstorm.jpg");

        GameObject skybox = new GameObject(new SphereGeometry(1, 20, 20), skyboxTexture);
        skybox.ambientlight_multiplier = 5;
        skybox.shininess = 1;
        skybox.scale = new float[] { 500, 500, 500 };
        skybox.updateModelMatrix();
        scene.renderer.skybox = skybox;
        scene.addGameObject(skybox);

        // 20 procedural trees laid out in a grid, each using imposter LOD.
        int treeRows = 4;
        int treeCols = 5;
        float spacing = 7f;
        float halfX = (treeCols - 1) * spacing * 0.5f;
        float halfZ = (treeRows - 1) * spacing * 0.5f;
        long seedBase = 0x5EEDBEEFL;

        for (int r = 0; r < treeRows; r++) {
            for (int c = 0; c < treeCols; c++) {
                long seed = seedBase + (r * treeCols + c);
                PlantConfig cfg = new PlantSeed(seed).generateConfig();
                GameObject tree = new GameObject(new SphereGeometry(1,20,20), plantTexture);
                tree.setPosition(c * spacing - halfX, 0f, r * spacing - halfZ);

                LODGameObject lodTree = new LODGameObject(tree, plantTexture, 14f);
                lodTree.renderer = scene.renderer;
                lodTree.acceptanceConeDegrees = 25f;
                scene.addGameObject(lodTree);
            }
        }
    }
}
