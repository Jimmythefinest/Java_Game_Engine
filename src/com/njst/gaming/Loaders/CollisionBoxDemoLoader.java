package com.njst.gaming.Loaders;

import com.njst.gaming.Scene;
import com.njst.gaming.data;
import com.njst.gaming.Geometries.CustomGeometry;
import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Geometries.Geometry;
import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.objects.CollisionBoxGameObject;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.objects.LODGameObject;

public class CollisionBoxDemoLoader implements Scene.SceneLoader {

    @Override
    public void load(Scene scene) {
        int cubeTexture = scene.renderer.getGraphicsDevice().loadTexture(data.rootDirectory + "/images (2).jpeg");
        int skyboxTexture = scene.renderer.getGraphicsDevice().loadTexture(data.rootDirectory + "/desertstorm.jpg");

        GameObject skybox = new GameObject(new SphereGeometry(1, 20, 20), skyboxTexture);
        skybox.ambientlight_multiplier = 5;
        skybox.shininess = 1;
        skybox.scale = new float[] { 500, 500, 500 };
        skybox.updateModelMatrix();
        scene.renderer.skybox = skybox;
        scene.addGameObject(skybox);

        // Real cube (rendered up close)
        GameObject cube = new GameObject(new CubeGeometry(), cubeTexture);
        cube.setPosition(0, 0, 0);
        scene.animations.add(new com.njst.gaming.Animations.Animation() {
            float angle = 0f;

            @Override
            public void animate() {
                // angle += 0.5f;
                cube.setRotation(0, angle, 0);
            }
        });

        // Wrap cube in LODGameObject — switches to baked billboard beyond 8 units
        LODGameObject lodCube = new LODGameObject(cube, cubeTexture, 8f);
        lodCube.renderer = scene.renderer;  // gives the standard render loop LOD awareness
        lodCube.acceptanceConeDegrees = 25f; // rebake when camera rotates more than 25°
        scene.addGameObject(lodCube);

        // POC overlay quad: separate object that billboards and scales to cover cube bounds.
        GameObject coverageQuad = new GameObject(createUnitQuad(), cubeTexture);
        coverageQuad.ambientlight_multiplier = 5f;
        coverageQuad.shininess = 1f;
        coverageQuad.name = "coverage_quad";
        scene.addGameObject(coverageQuad);

        scene.animations.add(new com.njst.gaming.Animations.Animation() {
            @Override
            public void animate() {
                if (scene.renderer == null || scene.renderer.camera == null) {
                    return;
                }
                cube.updateModelMatrix();
                Vector3 center = getCenter(cube);
                coverageQuad.setPosition(center.x, center.y, center.z);

                Vector3 toCamera = new Vector3(scene.renderer.camera.cameraPosition).sub(center);
                float len = toCamera.length();
                if (len > 0.0001f) {
                    float planarLen = (float) Math.sqrt(toCamera.x * toCamera.x + toCamera.z * toCamera.z);
                    float yawDeg = (float) Math.toDegrees(Math.atan2(toCamera.x, toCamera.z));
                    float pitchDeg = (float) -Math.toDegrees(Math.atan2(toCamera.y, planarLen));
                    coverageQuad.setRotation(pitchDeg, yawDeg, 0f);
                } else {
                    coverageQuad.setRotation(0f, 0f, 0f);
                }

                float[] cover = computeCoverScale(scene, cube, center);
                coverageQuad.setScale(cover[0], cover[1], 1f);
            }
        });

        CollisionBoxGameObject box = new CollisionBoxGameObject(cube, cubeTexture);
        scene.addGameObject(box);
    }

    private static Geometry createUnitQuad() {
        float[] vertices = {
                -0.5f, -0.5f, 0f,
                 0.5f, -0.5f, 0f,
                 0.5f,  0.5f, 0f,
                -0.5f,  0.5f, 0f
        };
        int[] indices = { 0, 1, 2, 0, 2, 3 };
        float[] normals = {
                0f, 0f, 1f,
                0f, 0f, 1f,
                0f, 0f, 1f,
                0f, 0f, 1f
        };
        float[] uv = { 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f };
        return new CustomGeometry(vertices, indices, normals, uv);
    }

    private static Vector3 getCenter(GameObject object) {
        if (object.min == null || object.max == null) {
            return new Vector3(object.position);
        }
        return new Vector3(
                (object.min.x + object.max.x) * 0.5f,
                (object.min.y + object.max.y) * 0.5f,
                (object.min.z + object.max.z) * 0.5f);
    }

    private static float[] computeCoverScale(Scene scene, GameObject target, Vector3 center) {
        if (target.min == null || target.max == null || scene.renderer == null || scene.renderer.camera == null) {
            return new float[] { 1f, 1f };
        }

        Vector3 mn = target.min;
        Vector3 mx = target.max;
        Vector3[] corners = {
                new Vector3(mn.x, mn.y, mn.z), new Vector3(mn.x, mn.y, mx.z),
                new Vector3(mn.x, mx.y, mn.z), new Vector3(mn.x, mx.y, mx.z),
                new Vector3(mx.x, mn.y, mn.z), new Vector3(mx.x, mn.y, mx.z),
                new Vector3(mx.x, mx.y, mn.z), new Vector3(mx.x, mx.y, mx.z)
        };

        Matrix4 view = scene.renderer.camera.getViewMatrix();
        Matrix4 proj = scene.renderer.camera.getProjectionMatrix();
        Matrix4 viewProj = new Matrix4().set(proj.r).multiply(view);

        float minNdcX = Float.POSITIVE_INFINITY;
        float maxNdcX = Float.NEGATIVE_INFINITY;
        float minNdcY = Float.POSITIVE_INFINITY;
        float maxNdcY = Float.NEGATIVE_INFINITY;

        for (Vector3 c : corners) {
            Vector3 ndc = viewProj.multiply(c);
            if (ndc.x < minNdcX) minNdcX = ndc.x;
            if (ndc.x > maxNdcX) maxNdcX = ndc.x;
            if (ndc.y < minNdcY) minNdcY = ndc.y;
            if (ndc.y > maxNdcY) maxNdcY = ndc.y;
        }

        float ndcSpanX = maxNdcX - minNdcX;
        float ndcSpanY = maxNdcY - minNdcY;
        float centerDepth = -view.multiply(center).z;
        float d = centerDepth > 0.0001f ? centerDepth : center.distance(scene.renderer.camera.cameraPosition);

        float tanFov = (float) Math.tan(Math.toRadians(scene.renderer.camera.FOV * 0.5f));
        float aspect = scene.renderer.camera.aspect;
        float worldH = ndcSpanY * d * tanFov;
        float worldW = ndcSpanX * d * tanFov * aspect;
        return new float[] { Math.max(0.001f, worldW), Math.max(0.001f, worldH) };
    }
}
