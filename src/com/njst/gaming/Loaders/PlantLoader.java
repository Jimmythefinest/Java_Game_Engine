package com.njst.gaming.Loaders;

import com.njst.gaming.Scene;
import com.njst.gaming.data;
import com.njst.gaming.Geometries.PlantConfig;
import com.njst.gaming.Geometries.PlantGeometry;
import com.njst.gaming.Geometries.PlantSeed;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Geometries.TerrainGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.Geometries.ImposterGeometry;
import com.njst.gaming.objects.ImposterGenerator;
import com.njst.gaming.objects.LODGameObject;

/**
 * Scene loader: generates a Perlin-noise terrain, then scatters procedural
 * L-System plants across it. Uses an Imposter system (LOD) to maintain 
 * high performance by swapping 3D meshes for 2D sprites at a distance.
 */
public class PlantLoader implements Scene.SceneLoader {

    // ---- Terrain settings ---------------------------------------------------
    private static final int TERRAIN_SIZE = 128;
    private static final float MIN_PLANT_HEIGHT = 0.5f;

    // ---- LOD settings -------------------------------------------------------
    private static final float LOD_SWITCH_DISTANCE = 50.0f;
    private static final int IMPOSTER_RESOLUTION = 256;

    // ---- Plant scatter settings ---------------------------------------------
    private static final int PLANT_GRID_STEP = 8;
    private static final long SCATTER_SEED = 0xDEADBEEFL;

    private static final float TERRAIN_OFFSET_X = -TERRAIN_SIZE / 2f;
    private static final float TERRAIN_OFFSET_Z = -TERRAIN_SIZE / 2f;

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
        TerrainGeometry terrainGeo = new TerrainGeometry(TERRAIN_SIZE, TERRAIN_SIZE);
        GameObject terrain = new GameObject(terrainGeo, terrainTex);
        terrain.translate(new Vector3(TERRAIN_OFFSET_X, 0, TERRAIN_OFFSET_Z));
        terrain.name = "Terrain";
        scene.addGameObject(terrain);

        float[][] heightMap = terrainGeo.heightMap;

        // ---- Setup Imposter Pool --------------------------------------------
        // We'll bake one imposter for each of the 5 main grammars to save memory
        System.out.println("[PlantLoader] Baking imposters...");
        int plantTex = ShaderProgram.loadTexture(data.rootDirectory + "/images (2).jpeg");
        int[] imposterTextures = new int[5];
        ImposterGeometry[] imposterGeos = new ImposterGeometry[5];
        
        for (int i = 0; i < 5; i++) {
            System.out.println("[PlantLoader] Baking imposter " + i + "/5...");
            // Generate a representative plant for this grammar index
            long repSeed = i + 100;
            PlantConfig config = new PlantSeed(repSeed).generateConfig();
            PlantGeometry meshGeo = new PlantGeometry(config);
            GameObject dummy = new GameObject(meshGeo, plantTex);
            dummy.generateBuffers();
            
            // Bake it
            float[] impScale = new float[1];
            imposterTextures[i] = ImposterGenerator.bake(dummy, scene.renderer, IMPOSTER_RESOLUTION, impScale);
            float scale = impScale[0];
            
            // Create a quad that fits the plant scale
            imposterGeos[i] = new ImposterGeometry(scale, scale); 
        }

        // ---- Scatter plants -------------------------------------------------
        java.util.Random rng = new java.util.Random(SCATTER_SEED);
        int planted = 0;

        for (int ix = 0; ix < TERRAIN_SIZE - 1; ix += PLANT_GRID_STEP) {
            for (int iz = 0; iz < TERRAIN_SIZE - 1; iz += PLANT_GRID_STEP) {

                float h = heightMap[ix][iz];
                if (h < MIN_PLANT_HEIGHT) continue;

                float jx = (rng.nextFloat() - 0.5f) * PLANT_GRID_STEP * 0.8f;
                float jz = (rng.nextFloat() - 0.5f) * PLANT_GRID_STEP * 0.8f;

                float worldX = ix + jx + TERRAIN_OFFSET_X;
                float worldY = h;
                float worldZ = iz + jz + TERRAIN_OFFSET_Z;

                long plantSeed = ((long) ix * 73856093L) ^ ((long) iz * 19349663L) ^ SCATTER_SEED;
                PlantConfig config = new PlantSeed(plantSeed).generateConfig();
                PlantGeometry geo = new PlantGeometry(config);
                
                // Pick a pre-baked imposter based on something deterministic (like seed % 5)
                int impIndex = (int)(Math.abs(plantSeed) % 5);
                
                LODGameObject plant = new LODGameObject(
                    geo, plantTex, 
                    imposterGeos[impIndex], imposterTextures[impIndex], 
                    LOD_SWITCH_DISTANCE
                );

                plant.setPosition(worldX, worldY, worldZ);
                plant.name = "Plant_" + ix + "_" + iz;
                scene.addGameObject(plant);
                planted++;
            }
        }

        System.out.println("[PlantLoader] Done — " + planted + " plants with LOD system enabled.");
    }
}
