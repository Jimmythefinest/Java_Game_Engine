package com.njst.gaming.Loaders;

import com.njst.gaming.Scene;
import com.njst.gaming.data;
import com.njst.gaming.Geometries.ImposterGeometry;
import com.njst.gaming.Geometries.PlantConfig;
import com.njst.gaming.Geometries.PlantGeometry;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.objects.ImposterGenerator;
import com.njst.gaming.objects.LODGameObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Loader that builds a small pine-like plant and uses a pre-baked atlas.
 */
public class PineImposterAtlasLoader implements Scene.SceneLoader {

    private static final int VIEWS_AZIMUTH = 1;
    private static final int VIEWS_ELEVATION = 1;
    private static final int ATLAS_COL = 0;
    private static final int ATLAS_ROW = 0;
    private static final float CAMERA_DISTANCE = 10.0f;
    private static final float LOD_DISTANCE = 10.0f;
    private static final String ATLAS_PATH = data.rootDirectory + "/imposters/pine_atlas.png";

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

        // ---- Load Atlas ----------------------------------------------------
        int atlasTex = ShaderProgram.loadTexture(ATLAS_PATH);
        if (atlasTex == 0) {
            System.err.println("[PineImposterAtlas] Failed to load atlas, falling back to mesh only.");
            GameObject pine = new GameObject(pineGeo, barkTex);
            pine.name = "Pine_Tree";
            pine.setPosition(0, 0, 0);
            scene.addGameObject(pine);
            return;
        }

        GameObject pineTemp = new GameObject(pineGeo, barkTex);
        pineTemp.setPosition(0, 0, 0);
        pineTemp.updateModelMatrix();

        Vector3 center = new Vector3(
                (pineGeo.min.x + pineGeo.max.x) * 0.5f,
                (pineGeo.min.y + pineGeo.max.y) * 0.5f,
                (pineGeo.min.z + pineGeo.max.z) * 0.5f);
        Vector3 camPos = new Vector3(center).add(new Vector3(0f, 0f, 1f).mul(CAMERA_DISTANCE));
        float[] bounds = ImposterGenerator.computeImposterSizeForCamera(
                pineTemp, scene.renderer, camPos, center, new Vector3(0f, 1f, 0f));
        float impWidth = bounds[0];
        float impHeight = bounds[1];

        float u0 = ATLAS_COL / (float) VIEWS_AZIMUTH;
        float v0 = ATLAS_ROW / (float) VIEWS_ELEVATION;
        float u1 = (ATLAS_COL + 1) / (float) VIEWS_AZIMUTH;
        float v1 = (ATLAS_ROW + 1) / (float) VIEWS_ELEVATION;
        ImposterGeometry impGeo = new ImposterGeometry(impWidth, impHeight, u0, v0, u1, v1, true);

        LODGameObject pine = new LODGameObject(pineGeo, barkTex, impGeo, atlasTex, LOD_DISTANCE);
        pine.name = "Pine_Tree_LOD";
        pine.setPosition(0, 0, 0);
        scene.addGameObject(pine);

        // Match the viewing camera to the bake setup (distance 10, +Z direction).
        scene.renderer.camera.lookAt(camPos, center, new Vector3(0f, 1f, 0f));

        System.out.println("[PineImposterAtlas] Loaded atlas: " + ATLAS_PATH);
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
