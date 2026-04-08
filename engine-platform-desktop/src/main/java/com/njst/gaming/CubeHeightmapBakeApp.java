package com.njst.gaming;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Utils.GameObjectRenderUtil;
import com.njst.gaming.collision.SphericalHeightmapShape;
import com.njst.gaming.objects.GameObject;

public class CubeHeightmapBakeApp extends Engine {
    private final CubeBakeLoader loader = new CubeBakeLoader();
    private boolean dumped = false;

    @Override
    protected void onKey(int key, int action) {
    }

    @Override
    protected void onInit() {
        title = "Cube Heightmap Bake";
        scene.loader = loader;
    }

    @Override
    protected void onUpdate() {
        if (dumped || loader.cube == null) {
            return;
        }

        SphericalHeightmapShape baked = renderer.bakeSphericalHeightmap(loader.cube, 512, 256, new Vector3(0f, 0f, 0f));
        if (baked == null) {
            System.err.println("Failed to bake cube spherical heightmap.");
            dumped = true;
            System.exit(1);
            return;
        }

        File faceDumpDirectory = new File(data.rootDirectory, "spherical_bake_faces");
        GameObjectRenderUtil.dumpSphericalHeightmapCubeFaces(renderer, loader.cube, 256, new Vector3(0f, 0f, 0f),
                faceDumpDirectory);
        File renderDumpDirectory = new File(data.rootDirectory, "spherical_render_faces");
        GameObjectRenderUtil.dumpSphericalHeightmapCubeFaceColorRenders(renderer, loader.cube, 256,
                new Vector3(0f, 0f, 0f), renderDumpDirectory);

        File output = new File(data.rootDirectory, "baked_cube_heightmap.png");
        writeHeightmapPng(baked, output);
        System.out.println("Wrote spherical heightmap bake to " + output.getAbsolutePath());
        System.out.println("Wrote cube-face debug images to " + faceDumpDirectory.getAbsolutePath());
        System.out.println("Wrote cube-face color renders to " + renderDumpDirectory.getAbsolutePath());
        dumped = true;
        System.exit(0);
    }

    private void writeHeightmapPng(SphericalHeightmapShape baked, File output) {
        float[][] samples = baked.getHeightSamplesCopy();
        int height = baked.getSampleHeight();
        int width = baked.getSampleWidth();
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float sample = samples[y][x];
                if (sample < min) {
                    min = sample;
                }
                if (sample > max) {
                    max = sample;
                }
            }
        }

        float range = max - min;
        if (range <= 0.000001f) {
            range = 1f;
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float normalized = (samples[y][x] - min) / range;
                int gray = Math.max(0, Math.min(255, Math.round(normalized * 255f)));
                int argb = (255 << 24) | (gray << 16) | (gray << 8) | gray;
                image.setRGB(x, y, argb);
            }
        }

        try {
            ImageIO.write(image, "png", output);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write heightmap PNG: " + output.getAbsolutePath(), e);
        }
    }

    private static class CubeBakeLoader implements Scene.SceneLoader {
        private GameObject cube;

        @Override
        public void load(Scene scene) {
            cube = new GameObject(new CubeGeometry(), 0);
            cube.name = "BakeCube";
            cube.setScale(1f, 1f, 1f);
            cube.setPosition(0f, 0f, 0f);
            cube.updateModelMatrix();
            scene.addGameObject(cube);
        }
    }

    public static void main(String[] args) {
        new CubeHeightmapBakeApp().run();
    }
}
