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

/**
 * Scene loader: generates a Perlin-noise terrain, then scatters procedural
 * L-System plants across it — but only where the terrain height is at or
 * above {@link #MIN_PLANT_HEIGHT}.  Low areas (water / swamp) stay bare.
 *
 * <p>Wire it in as usual:</p>
 * <pre>
 *   engine.scene.loader = new PlantLoader();
 * </pre>
 *
 * <p>Key tuneable constants at the top of the class:</p>
 * <ul>
 *   <li>{@link #MIN_PLANT_HEIGHT} — height threshold (terrain units)</li>
 *   <li>{@link #PLANT_GRID_STEP} — spacing between plant candidates</li>
 *   <li>{@link #TERRAIN_SIZE}   — resolution of the terrain grid</li>
 * </ul>
 */
public class PlantLoader implements Scene.SceneLoader {

    // ---- Terrain settings ---------------------------------------------------

    /** Terrain grid resolution (vertices per side). */
    private static final int TERRAIN_SIZE = 128;

    /**
     * Minimum terrain height at which a plant may spawn.
     * TerrainGenerator scales Perlin output by 10, so the height range is
     * roughly [-10, 10].  0.5 keeps plants off flat/low ground (water zones).
     * Set to a negative value to allow plants everywhere.
     */
    private static final float MIN_PLANT_HEIGHT = 0.5f;

    // ---- Plant scatter settings ---------------------------------------------

    /** Skip this many terrain cells between each plant candidate.
     *  Lower value = denser forest. */
    private static final int PLANT_GRID_STEP = 8;

    /** Master randomness seed for plant-seed derivation and position jitter. */
    private static final long SCATTER_SEED = 0xDEADBEEFL;

    // Terrain is centred at world origin
    private static final float TERRAIN_OFFSET_X = -TERRAIN_SIZE / 2f;
    private static final float TERRAIN_OFFSET_Z = -TERRAIN_SIZE / 2f;

    // -------------------------------------------------------------------------

    @Override
    public void load(Scene scene) {

        // ---- Skybox --------------------------------------------------------
        int skyTex = ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg");
        GameObject skybox = new GameObject(new SphereGeometry(1, 20, 20), skyTex);
        skybox.ambientlight_multiplier = 5;
        skybox.shininess = 1;
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

        // Keep a reference to the heightmap so we can sample it per-plant
        float[][] heightMap = terrainGeo.heightMap;

        System.out.println("[PlantLoader] Terrain " + TERRAIN_SIZE + "x" + TERRAIN_SIZE + " generated.");

        // ---- Scatter plants -------------------------------------------------
        int plantTex = ShaderProgram.loadTexture(data.rootDirectory + "/bark.jpg");
        java.util.Random rng = new java.util.Random(SCATTER_SEED);

        int planted = 0;

        for (int ix = 0; ix < TERRAIN_SIZE - 1; ix += PLANT_GRID_STEP) {
            for (int iz = 0; iz < TERRAIN_SIZE - 1; iz += PLANT_GRID_STEP) {

                float h = heightMap[ix][iz];

                // Only plant above the height threshold (skip water / lowland)
                if (h < MIN_PLANT_HEIGHT) continue;

                // Random sub-cell jitter so plants don't align to a grid
                float jx = (rng.nextFloat() - 0.5f) * PLANT_GRID_STEP * 0.8f;
                float jz = (rng.nextFloat() - 0.5f) * PLANT_GRID_STEP * 0.8f;

                float worldX = ix + jx + TERRAIN_OFFSET_X;
                float worldY = h;                      // base sits on the terrain
                float worldZ = iz + jz + TERRAIN_OFFSET_Z;

                // Derive a unique deterministic seed from the grid cell
                long plantSeed = ((long) ix * 73856093L) ^ ((long) iz * 19349663L) ^ SCATTER_SEED;

                PlantConfig   config = new PlantSeed(plantSeed).generateConfig();
                PlantGeometry geo    = new PlantGeometry(config);
                GameObject    plant  = new GameObject(geo, plantTex);

                plant.setPosition(worldX, worldY, worldZ);
                plant.name = "Plant_" + ix + "_" + iz;
                scene.addGameObject(plant);
                planted++;
            }
        }

        System.out.println("[PlantLoader] Done — " + planted
                + " plants placed above height " + MIN_PLANT_HEIGHT + ".");
    }
}
