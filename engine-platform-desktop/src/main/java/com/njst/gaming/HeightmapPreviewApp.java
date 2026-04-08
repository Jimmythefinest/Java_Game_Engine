package com.njst.gaming;

import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Geometries.SphericalHeightmapGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.collision.SphericalHeightmapShape;
import com.njst.gaming.objects.GameObject;

public class HeightmapPreviewApp extends Engine {
    private final HeightmapPreviewLoader loader = new HeightmapPreviewLoader();
    private boolean bakedPreviewCreated = false;

    public HeightmapPreviewApp() {
        title = "Heightmap Preview";
        width = 1280;
        height = 720;
    }

    @Override
    protected void onKey(int key, int action) {
    }

    @Override
    protected void onInit() {
        scene.loader = loader;
        renderer.camera.cameraPosition.set(0f, 0.2f, -6.5f);
        renderer.camera.targetPosition.set(0f, 0f, 0f);
    }

    @Override
    protected void onUpdate() {
        if (bakedPreviewCreated || loader.sourceCube == null) {
            return;
        }

        SphericalHeightmapShape baked = renderer.bakeSphericalHeightmap(loader.sourceCube, 512, 256, new Vector3(0f, 0f, 0f));
        if (baked == null) {
            throw new IllegalStateException("Failed to bake source cube for heightmap preview.");
        }

        GameObject preview = new GameObject(
                new SphericalHeightmapGeometry(baked.getHeightSamplesCopy(), baked.getBaseRadius()),
                loader.textureId);
        preview.name = "HeightmapPreview";
        preview.setPosition(1.6f, 0f, 0f);
        preview.setScale(2f, 2f, 2f);
        preview.updateModelMatrix();
        scene.addGameObject(preview);
        bakedPreviewCreated = true;
    }

    private class HeightmapPreviewLoader implements Scene.SceneLoader {
        private GameObject sourceCube;
        private int textureId;

        @Override
        public void load(Scene scene) {
            textureId = scene.renderer.getGraphicsDevice().loadTexture("generated:heightmap-preview");
            int skyboxTexture = scene.renderer.getGraphicsDevice().loadTexture(data.rootDirectory + "/desertstorm.jpg");

            GameObject skybox = new GameObject(new SphereGeometry(1f, 20, 20), skyboxTexture);
            skybox.ambientlight_multiplier = 3f;
            skybox.shininess = 1f;
            skybox.setScale(50f, 50f, 50f);
            skybox.updateModelMatrix();
            scene.renderer.skybox = skybox;

            sourceCube = new GameObject(new CubeGeometry(), textureId);
            sourceCube.name = "SourceCube";
            sourceCube.setPosition(-1.6f, 0f, 0f);
            sourceCube.setScale(2f, 2f, 2f);
            sourceCube.updateModelMatrix();
            scene.addGameObject(sourceCube);
        }
    }

    public static void main(String[] args) {
        new HeightmapPreviewApp().run();
    }
}
