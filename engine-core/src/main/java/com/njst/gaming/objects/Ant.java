package com.njst.gaming.objects;

import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Scene;

public class Ant {
    public Vector3 position = new Vector3();
    public float walkCycle = 0;

    private GameObject head, thorax, abdomen;
    private GameObject[] legs = new GameObject[6];

    private final float legLength = 0.3f;
    private final float walkSpeed = 2.0f;

    public Ant(Scene scene, int texture) {
        // Create body parts
        head = createPart(scene, texture, new float[]{0.2f, 0.2f, 0.2f});
        thorax = createPart(scene, texture, new float[]{0.25f, 0.25f, 0.25f});
        abdomen = createPart(scene, texture, new float[]{0.3f, 0.3f, 0.3f});

        // Create legs
        for (int i = 0; i < 6; i++) {
            legs[i] = createPart(scene, texture, new float[]{0.05f, 0.05f, legLength});
        }
    }

    private GameObject createPart(Scene scene, int texture, float[] scale) {
        GameObject go = new GameObject(new CubeGeometry(), texture);
        go.scale = scale;
        scene.addGameObject(go);
        return go;
    }

    public void update(float deltaTime) {
        walkCycle += deltaTime * walkSpeed;

        // Update body part positions
        thorax.position.set(position);
        head.position.set(position.x, position.y, position.z + 0.3f);
        abdomen.position.set(position.x, position.y, position.z - 0.3f);

        // Update legs with sinusoidal swing
        for (int i = 0; i < 6; i++) {
            GameObject leg = legs[i];
            float side = (i % 2 == 0) ? 1 : -1;
            float offsetZ = (i / 2 - 1) * 0.2f;

            float swing = (float) Math.sin(walkCycle + i * 0.5f) * 0.1f;
            leg.position.set(
                position.x + side * 0.2f,
                position.y - 0.1f,
                position.z + offsetZ + swing
            );
        }
    }
}
