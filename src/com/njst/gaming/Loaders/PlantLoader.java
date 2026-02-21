package com.njst.gaming.Loaders;

import com.njst.gaming.Scene;
import com.njst.gaming.data;
import com.njst.gaming.Geometries.PlantConfig;
import com.njst.gaming.Geometries.PlantGeometry;
import com.njst.gaming.Geometries.PlantSeed;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.objects.GameObject;

/**
 * Demo scene loader that populates the engine with procedurally generated plants.
 *
 * <p>Each plant is grown from a different integer seed, producing unique shapes.
 * Wire this loader into your {@link com.njst.gaming.Engine} exactly as you
 * would any other {@link com.njst.gaming.Scene.SceneLoader}:</p>
 *
 * <pre>
 *   Engine engine = new Engine();
 *   engine.scene.loader = new PlantLoader();
 *   engine.start();
 * </pre>
 *
 * <p>To generate a single plant from code:</p>
 * <pre>
 *   PlantConfig config = new PlantSeed(42).generateConfig();
 *   PlantGeometry geo  = new PlantGeometry(config);
 *   GameObject plant   = new GameObject(geo, textureId);
 *   plant.setPosition(x, 0, z);
 *   scene.addGameObject(plant);
 * </pre>
 */
public class PlantLoader implements Scene.SceneLoader {

    // Seeds for the demo plants — each produces a visually distinct result
    private static final long[] DEMO_SEEDS = { 42L, 1337L, 99999L, 7L, 314159L, 271828L };

    // Grid spacing between plants
    private static final float SPACING = 3.5f;

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

        // ---- Shared plant texture (bark/leaf) -------------------------------
        // Re-use whatever texture is available in the project root.
        // Swap the path for a dedicated bark / leaf atlas if you have one.
        int plantTex = ShaderProgram.loadTexture(data.rootDirectory + "/images (2).jpeg");

        // ---- Generate one plant per seed ------------------------------------
        int count = DEMO_SEEDS.length;
        int cols  = (int) Math.ceil(Math.sqrt(count));

        for (int i = 0; i < count; i++) {
            long seed = DEMO_SEEDS[i];

            System.out.println("[PlantLoader] Generating plant from seed " + seed + " ...");
            long t0 = System.currentTimeMillis();

            PlantConfig  config = new PlantSeed(seed).generateConfig();
            PlantGeometry  geo  = new PlantGeometry(config);

            System.out.println("[PlantLoader] seed=" + seed
                    + "  vertices=" + geo.getVertices().length / 3
                    + "  indices="  + geo.getIndices().length
                    + "  (" + (System.currentTimeMillis() - t0) + " ms)");

            GameObject plant = new GameObject(geo, plantTex);

            // Arrange in a grid
            float px = (i % cols) * SPACING - (cols * SPACING / 2f);
            float pz = (i / cols) * SPACING - (cols * SPACING / 2f);
            plant.setPosition(px, 0, pz);
            plant.name = "Plant_seed_" + seed;

            scene.addGameObject(plant);
        }

        System.out.println("[PlantLoader] Done — " + count + " plants in scene.");
    }
}
