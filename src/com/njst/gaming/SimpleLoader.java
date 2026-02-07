package com.njst.gaming;

import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.Scene.SceneLoader;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.objects.instancedGameObject;

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

        // Stress test: Create instanced spheres in a grid
        System.out.println("SimpleLoader: Creating instanced spheres for stress testing...");
        int spheresPerSide = 5; // 5x5x5 = 125 spheres
        float spacing = 5.0f; // Distance between spheres
        float offset = (spheresPerSide - 1) * spacing / 2.0f; // Center the grid

        // Create a single instanced game object
        instancedGameObject instancedSpheres = new instancedGameObject(
                new SphereGeometry(0.5f, 80, 80),
                cubeTexture);

        // Generate transformation matrices for each sphere instance
        for (int x = 0; x < spheresPerSide; x++) {
            for (int y = 0; y < spheresPerSide; y++) {
                for (int z = 0; z < spheresPerSide; z++) {
                    Matrix4 instanceMatrix = new Matrix4();
                    instanceMatrix.identity();
                    instanceMatrix.translate(new Vector3(
                            x * spacing - offset,
                            y * spacing - offset,
                            z * spacing - offset));
                    instancedSpheres.matrices.add(instanceMatrix);
                }
            }
        }

        scene.addGameObject(instancedSpheres);

        System.out.println("SimpleLoader: Scene loaded with cube, skybox, and " +
                instancedSpheres.matrices.size() + " instanced spheres");
    }
}
