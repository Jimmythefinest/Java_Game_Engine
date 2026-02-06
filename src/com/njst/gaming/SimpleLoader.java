package com.njst.gaming;

import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.Scene.SceneLoader;
import com.njst.gaming.objects.GameObject;

/**
 * A simple scene loader that creates a basic scene with a cube and skybox.
 */
public class SimpleLoader implements SceneLoader {

    @Override
    public void load(Scene scene) {
        // Load textures
        int cubeTexture = ShaderProgram.loadTexture(data.rootDirectory + "/images (2).jpeg");
        int skyboxTexture = ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg");

        // Create skybox (large sphere)
        GameObject skybox = new GameObject(new SphereGeometry(1, 20, 20), skyboxTexture);
        skybox.ambientlight_multiplier = 5;
        skybox.shininess = 1;
        skybox.scale = new float[] { 500, 500, 500 };
        skybox.updateModelMatrix();
        scene.renderer.skybox = skybox;
        scene.addGameObject(skybox);

        // Create a simple cube
        GameObject cube = new GameObject(new CubeGeometry(), cubeTexture);
        cube.translate(new Vector3(0, 0, 0));
        scene.addGameObject(cube);

        System.out.println("SimpleLoader: Scene loaded with cube and skybox");
    }
}
