package com.njst.gaming.Loaders;

import com.njst.gaming.Scene;
import com.njst.gaming.data;
import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Geometries.SphereGeometry;
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

        CollisionBoxGameObject box = new CollisionBoxGameObject(cube, cubeTexture);
        scene.addGameObject(box);
    }
}
