package com.njst.gaming.Loaders;

import com.njst.gaming.Scene;
import com.njst.gaming.data;
import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.objects.CollisionBoxGameObject;

public class CollisionBoxDemoLoader implements Scene.SceneLoader {

    @Override
    public void load(Scene scene) {
        int cubeTexture = ShaderProgram.loadTexture(data.rootDirectory + "/images (2).jpeg");
        int skyboxTexture = ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg");

        GameObject skybox = new GameObject(new SphereGeometry(1, 20, 20), skyboxTexture);
        skybox.ambientlight_multiplier = 5;
        skybox.shininess = 1;
        skybox.scale = new float[] { 500, 500, 500 };
        skybox.updateModelMatrix();
        scene.renderer.skybox = skybox;
        scene.addGameObject(skybox);

        GameObject cube = new GameObject(new CubeGeometry(), cubeTexture);
        cube.setPosition(0, 0, 0);
        scene.animations.add(new com.njst.gaming.Animations.Animation() {
            float angle = 0f;

            @Override
            public void animate() {
                angle += 0.5f;
                cube.setRotation(0, angle, 0);
            }
        });
        scene.addGameObject(cube);

        CollisionBoxGameObject box = new CollisionBoxGameObject(cube, cubeTexture);
        scene.addGameObject(box);
    }
}
