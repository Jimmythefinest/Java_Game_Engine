package com.njst.gaming.Loaders;

import com.njst.gaming.Scene;
import com.njst.gaming.data;
import com.njst.gaming.Geometries.PlantConfig;
import com.njst.gaming.Geometries.PlantGeometry;
import com.njst.gaming.Geometries.PlantSeed;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Geometries.*;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.objects.ImposterGenerator;
import com.njst.gaming.objects.LODGameObject;

/**
 * Visual test for the LOD (Level of Detail) system.
 * It places a single high-poly plant in the scene and automatically 
 * generates an imposter (2D sprite) for it. 
 * Move the camera away to watch it swap to the 2D version.
 */
public class LODTestLoader implements Scene.SceneLoader {

    @Override
    public void load(Scene scene) {
        // ---- Skybox --------------------------------------------------------
        int skyTex = ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg");
        GameObject skybox = new GameObject(new SphereGeometry(1, 20, 20), skyTex);
        skybox.ambientlight_multiplier = 5;
        skybox.scale = new float[]{500, 500, 500};
        skybox.updateModelMatrix();
        scene.renderer.skybox = skybox;
        scene.addGameObject(skybox);

        // ---- Terrain -------------------------------------------------------
        int terrainTex = ShaderProgram.loadTexture(data.rootDirectory + "/images (2).jpeg");
        GameObject floor = new GameObject(new TerrainGeometry(100,100,new float[100][100]), terrainTex);
        floor.setPosition(0, -0.1f, 0);
        floor.setScale(100f, 0.1f, 100f);
        scene.addGameObject(floor);

        // ---- Create High-Poly Plant -----------------------------------------
        System.out.println("[LODTest] Generating high-poly mesh...");
        int meshTex = ShaderProgram.loadTexture(data.rootDirectory + "/images (2).jpeg");
        PlantConfig config = new PlantSeed(42).generateConfig();
        PlantGeometry highPoly = new PlantGeometry(config);
        
        // Dummy object for baking
        GameObject dummy = new GameObject(highPoly, meshTex);
        dummy.generateBuffers();

        // ---- Bake Imposter --------------------------------------------------
        System.out.println("[LODTest] Baking imposter texture...");
        float[] impScale = new float[1];
        int imposterTexture = ImposterGenerator.bake(dummy, scene.renderer, 256, impScale);
        float scale = impScale[0];
        ImposterGeometry imposterGeo = new ImposterGeometry(scale, scale);

        // ---- Create LOD GameObject -----------------------------------------
        // Distance threshold = 10 units for easy testing
        float lodDistance = 10.0f;
        
        LODGameObject lodObj = new LODGameObject(
            highPoly, meshTex, 
            imposterGeo, imposterTexture, 
            lodDistance
        );
        
        lodObj.setPosition(0, 0, 0);
        lodObj.name = "LOD_Test_Plant";
        scene.addGameObject(lodObj);

        System.out.println("[LODTest] Done. Plant placed at origin. LOD swap distance: " + lodDistance);
    }
}
