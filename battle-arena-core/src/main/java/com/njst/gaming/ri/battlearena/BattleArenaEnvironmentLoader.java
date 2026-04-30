package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Geometries.TerrainGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Scene;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.objects.GameObject;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

final class BattleArenaEnvironmentLoader {
    private static final String SKYBOX_FILE = "desertstorm.jpg";
    private static final String GROUND_FILE = "j.jpg";
    private static final int GROUND_SIZE = 96;

    private TerrainGeometry terrainGeometry;
    private Vector3 terrainOrigin;

    void load(Scene scene, GraphicsDevice graphicsDevice) {
        String skyboxPath = resolveResourcePath(SKYBOX_FILE);
        String groundPath = resolveResourcePath(GROUND_FILE);
        log("loading textures skybox=" + skyboxPath + " ground=" + groundPath);
        int skyboxTexture = graphicsDevice.loadTexture(skyboxPath);
        int groundTexture = graphicsDevice.loadTexture(groundPath);
        log("loaded textures skyboxId=" + skyboxTexture + " groundId=" + groundTexture);

        setupDemoLights(scene);
        setupSkybox(scene, skyboxTexture);
        setupGround(scene, groundTexture);
    }

    float sampleTerrainHeight(float worldX, float worldZ) {
        if (terrainGeometry == null || terrainGeometry.heightMap == null || terrainGeometry.heightMap.length == 0
                || terrainGeometry.heightMap[0].length == 0) {
            return 0f;
        }

        float localX = worldX - terrainOrigin.x;
        float localZ = worldZ - terrainOrigin.z;
        int x0 = clampIndex((int) Math.floor(localX), terrainGeometry.heightMap.length - 1);
        int z0 = clampIndex((int) Math.floor(localZ), terrainGeometry.heightMap[0].length - 1);
        int x1 = clampIndex(x0 + 1, terrainGeometry.heightMap.length - 1);
        int z1 = clampIndex(z0 + 1, terrainGeometry.heightMap[0].length - 1);

        float tx = clamp(localX - x0, 0f, 1f);
        float tz = clamp(localZ - z0, 0f, 1f);

        float h00 = terrainGeometry.heightMap[x0][z0];
        float h10 = terrainGeometry.heightMap[x1][z0];
        float h01 = terrainGeometry.heightMap[x0][z1];
        float h11 = terrainGeometry.heightMap[x1][z1];
        float hx0 = lerp(h00, h10, tx);
        float hx1 = lerp(h01, h11, tx);
        return terrainOrigin.y + lerp(hx0, hx1, tz);
    }

    static String resolveResourcePath(String fileName) {
        String normalized = fileName != null ? fileName.replace('\\', '/') : "";
        Path workDir = Paths.get(System.getProperty("user.dir"));
        Path[] roots = new Path[] {
                workDir.resolve(Paths.get("build", "resources", "main")),
                workDir.resolve(Paths.get("src", "main", "resources")),
                workDir.resolve(Paths.get("..", "battle-arena-desktop", "build", "resources", "main")),
                workDir.resolve(Paths.get("..", "battle-arena-desktop", "src", "main", "resources")),
                workDir.resolve(Paths.get("..", "battle-arena-core", "build", "resources", "main")),
                workDir.resolve(Paths.get("..", "battle-arena-core", "src", "main", "resources")),
                workDir.resolve(Paths.get("..", "engine-platform-desktop", "build", "resources", "main")),
                workDir.resolve(Paths.get("..", "engine-platform-desktop", "src", "main", "resources")),
                workDir.resolve(Paths.get("battle-arena-desktop", "build", "resources", "main")),
                workDir.resolve(Paths.get("battle-arena-core", "build", "resources", "main")),
                workDir.resolve(Paths.get("battle-arena-core", "src", "main", "resources")),
                workDir.resolve(Paths.get("engine-platform-desktop", "build", "resources", "main")),
                workDir.resolve(Paths.get("engine-platform-desktop", "src", "main", "resources")),
                Paths.get(com.njst.gaming.data.rootDirectory)
        };
        for (Path root : roots) {
            File resource = root.resolve(normalized).normalize().toFile();
            if (resource.isFile()) {
                String absolutePath = resource.getAbsolutePath();
                log("resolved resource file=" + normalized + " absolute=" + absolutePath);
                return absolutePath;
            }
        }
        log("resource not found on filesystem file=" + normalized
                + " userDir=" + System.getProperty("user.dir")
                + " root=" + com.njst.gaming.data.rootDirectory);
        // On Android rootDirectory is not set; fall back to asset name directly.
        return fileName;
    }

    private void setupSkybox(Scene scene, int skyboxTexture) {
        GameObject skybox = new GameObject(new SphereGeometry(1f, 20, 20), skyboxTexture);
        skybox.ambientlight_multiplier = 10f;
        skybox.shininess = 1f;
        skybox.setScale(100f, 100f, 100f);
        skybox.setPosition(0f, 0f, 0f);
        scene.renderer.skybox = skybox;
        scene.addGameObject(skybox);
    }

    private void setupGround(Scene scene, int groundTexture) {
        terrainGeometry = new TerrainGeometry(GROUND_SIZE, GROUND_SIZE, new float[GROUND_SIZE][GROUND_SIZE]);
        terrainOrigin = new Vector3(-GROUND_SIZE * 0.5f, -0.75f, -GROUND_SIZE * 0.5f);
        GameObject ground = new GameObject(terrainGeometry, groundTexture);
        ground.ambientlight_multiplier = 3f;
        ground.shininess = 3f;
        ground.setPosition(terrainOrigin.x, terrainOrigin.y, terrainOrigin.z);
        scene.addGameObject(ground);
    }

    private void setupDemoLights(Scene scene) {
        scene.renderer.clearLights();
        scene.renderer.addPointLight(-4.5f, 3.2f, -2.5f, 1.0f, 0.48f, 0.18f, 1.65f, 16f);
        scene.renderer.addPointLight(4.5f, 3.2f, 2.8f, 0.18f, 0.55f, 1.0f, 1.45f, 16f);
        log("demo lights configured count=" + (scene.renderer.getLights().size() + 1));
    }

    private int clampIndex(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float lerp(float a, float b, float t) {
        return a + ((b - a) * t);
    }

    private static void log(String message) {
        BattleArenaDemoLoader.log(message);
    }
}
