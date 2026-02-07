package com.njst.gaming.simulation.entities;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.ai.NeuralNetwork;
import com.njst.gaming.objects.GameObject;

public class NPC {

    // ===== Tunables =====
    private static final float MAX_SPEED = 5.0f;
    private static final float ACCELERATION = 12.0f;
    private static final float MAX_DIST_NORM = 30.0f;

    // ===== State =====
    public final GameObject skin;
    public final NeuralNetwork brain;

    public final Vector3 position;
    private final Vector3 velocity = new Vector3();

    // ===== Constructors =====
    public NPC(GameObject skin) {
        // 5 inputs → 2 outputs, linear output layer
        this(skin, new NeuralNetwork(
                new int[] { 5, 12, 12, 2 },
                0.01f,
                true // linear outputs (IMPORTANT)
        ));
    }

    public NPC(GameObject skin, NeuralNetwork brain) {
        this.skin = skin;
        this.position = skin.position;
        this.brain = brain;
    }

    // ===== Evolution =====
    public static float mutationRate = 0.1f;
    public static float mutationStrength = 0.3f;

    public NPC reproduce(GameObject newSkin) {
        NeuralNetwork child = brain.copy();
        child.mutate(mutationRate, mutationStrength); // dynamic mutations from control panel
        return new NPC(newSkin, child);
    }

    // ===== Update =====
    public void update(Vector3 target, float dt) {

        // --- Relative target vector ---
        Vector3 toTarget = target.clone().sub(position);
        float distance = toTarget.length();

        // Normalize direction safely
        if (distance > 0.0001f) {
            toTarget.mul(1.0f / distance);
        }

        // --- Build inputs ---
        float[] inputs = new float[] {
                clamp(toTarget.x, -1f, 1f),
                clamp(toTarget.z, -1f, 1f),
                clamp(velocity.x / MAX_SPEED, -1f, 1f),
                clamp(velocity.z / MAX_SPEED, -1f, 1f),
                clamp(distance / MAX_DIST_NORM, 0f, 1f)
        };

        // --- NN forward ---
        float[] out = brain.feedForward(inputs);

        // --- Acceleration output ---
        float ax = clamp(out[0], -1f, 1f);
        float az = clamp(out[1], -1f, 1f);

        // --- Physics integration ---
        velocity.x += ax * ACCELERATION * dt;
        velocity.z += az * ACCELERATION * dt;

        clampVelocity();

        position.x += velocity.x * dt;
        position.z += velocity.z * dt;

        skin.updateModelMatrix();
    }

    // ===== Helpers =====
    private void clampVelocity() {
        float speedSq = velocity.x * velocity.x + velocity.z * velocity.z;
        float maxSq = MAX_SPEED * MAX_SPEED;

        if (speedSq > maxSq) {
            float invLen = (float) (1.0 / Math.sqrt(speedSq));
            velocity.x *= invLen * MAX_SPEED;
            velocity.z *= invLen * MAX_SPEED;
        }
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}