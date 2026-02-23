package com.njst.gaming.Loaders;

import com.njst.gaming.Scene;
import com.njst.gaming.data;
import com.njst.gaming.Geometries.PlantConfig;
import com.njst.gaming.Geometries.PlantGeometry;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.objects.ImposterGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * Loader that builds a small pine-like plant and bakes it to an atlas PNG.
 */
public class PineImposterBakeLoader implements Scene.SceneLoader {

    private static final int TILE_SIZE = 256;
    private static final float CAMERA_DISTANCE = 10.0f;
    private static final String OUTPUT_PATH = data.rootDirectory + "/imposters/pine_atlas.png";

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

        // ---- Pine-like Plant ----------------------------------------------
        int barkTex = ShaderProgram.loadTexture(data.rootDirectory + "/images (2).jpeg");
        PlantConfig pineConfig = makePineConfig();
        PlantGeometry pineGeo = new PlantGeometry(pineConfig);
        GameObject pine = new GameObject(pineGeo, barkTex);
        pine.name = "Pine_Tree";
        pine.setPosition(0, 0, 0);
        scene.addGameObject(pine);

        // ---- Bake Atlas ----------------------------------------------------
        System.out.println("[PineImposterBake] Baking atlas to: " + OUTPUT_PATH);
        boolean ok = ImposterGenerator.bakeSingleViewToFile(
                pine, scene.renderer,
                TILE_SIZE, CAMERA_DISTANCE,
                new Vector3(0f, 0f, 1f),
                OUTPUT_PATH
        );
        System.out.println("[PineImposterBake] Done. Success=" + ok);
    }

    private static PlantConfig makePineConfig() {
        Map<Character, String> rules = new HashMap<>();
        // Tall trunk with downward/side branches and sparse leaves.
        rules.put('F', "FF[+FL][-FL][&FL][^FL]F");
        String axiom = "F";
        int iterations = 4;
        float angle = (float) Math.toRadians(18);
        float stepLength = 0.35f;
        float trunkRadius = 0.08f;
        float branchTaper = 0.70f;
        float leafSize = 0.05f;
        return new PlantConfig(axiom, rules, iterations, angle, stepLength, trunkRadius, branchTaper, leafSize);
    }
}
