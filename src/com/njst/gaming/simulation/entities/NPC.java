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
    public final NeuralNetwork stateUpdateBrain;
    public final NeuralNetwork targetSetterBrain;

    public final Vector3 position;
    private final Vector3 velocity = new Vector3();
    private float[] worldState = new float[8];
    // the world state for now should have the first 3 values be the relative
    // position of best the food
    public final Vector3 personalTarget = new Vector3();

    // ===== Constructors =====
    public NPC(GameObject skin) {
        this(skin,
                new NeuralNetwork(new int[] { 5, 12, 12, 3 }, 0.01f, true),
                new NeuralNetwork(new int[] { 12, 16, 8 }, 0.01f, false),
                new NeuralNetwork(new int[] { 8, 12, 2 }, 0.01f, true));
    }

    public NPC(GameObject skin, NeuralNetwork brain) {
        this(skin,
                brain,
                new NeuralNetwork(new int[] { 12, 16, 8 }, 0.01f, false),
                new NeuralNetwork(new int[] { 8, 12, 2 }, 0.01f, true));
    }

    public NPC(GameObject skin, NeuralNetwork movementBrain, NeuralNetwork stateUpdateBrain,
            NeuralNetwork targetSetterBrain) {
        this.skin = skin;
        this.position = skin.position;
        this.brain = movementBrain;
        this.stateUpdateBrain = stateUpdateBrain;
        this.targetSetterBrain = targetSetterBrain;
        resetWorldState();
    }

    // ===== Evolution =====
    public static float movementMutationRate = 0.1f;
    public static float movementMutationStrength = 0.3f;
    public static float stateMutationRate = 0.1f;
    public static float stateMutationStrength = 0.3f;
    public static float targetMutationRate = 0.1f;
    public static float targetMutationStrength = 0.3f;
    public static boolean useTargetSetterBrain = true;

    public NPC reproduce(GameObject newSkin) {
        NeuralNetwork newMovement = brain.copy();
        newMovement.mutate(movementMutationRate, movementMutationStrength);

        NeuralNetwork newStateUpdate = stateUpdateBrain.copy();
        newStateUpdate.mutate(stateMutationRate, stateMutationStrength);

        NeuralNetwork newTargetSetter = targetSetterBrain.copy();
        newTargetSetter.mutate(targetMutationRate, targetMutationStrength);

        return new NPC(newSkin, newMovement, newStateUpdate, newTargetSetter);
    }

    public void resetWorldState() {
        for (int i = 0; i < worldState.length; i++) {
            worldState[i] = 0.0f; // Initial fixed vector
        }
        velocity.set(0, 0, 0);
    }

    public void observeFood(Vector3 foodPos, float value) {
        float relX = foodPos.x - position.x;
        float relZ = foodPos.z - position.z;
        float dist = (float) Math.sqrt(relX * relX + relZ * relZ);

        float[] inputs = new float[12];
        System.arraycopy(worldState, 0, inputs, 0, 8);
        inputs[8] = clamp(relX / MAX_DIST_NORM, -1f, 1f);
        inputs[9] = clamp(relZ / MAX_DIST_NORM, -1f, 1f);
        inputs[10] = clamp(dist / MAX_DIST_NORM, 0f, 1f);
        inputs[11] = clamp(value, 0f, 1f);

        worldState = stateUpdateBrain.feedForward(inputs);
    }

    public void decideTarget() {
        if (useTargetSetterBrain) {
            float[] output = targetSetterBrain.feedForward(worldState);
            // Map output (linear since useLinearOutput=true) to world coords
            personalTarget.set(output[0] * 50.0f, 2.0f, output[1] * 50.0f);

        } else {
            // Use first 2 values from worldState as target coords
            // (Mapping from worldState range which is usually [-1, 1] or [0, 1] depends on
            // state NN activation)
            personalTarget.set(worldState[0] * 50.0f, 2.0f, worldState[1] * 50.0f);
        }
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
        float ACCELERATION = 12.0f * out[2];

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

    public void updateBrains(NeuralNetwork movementBrain, NeuralNetwork stateUpdateBrain,
            NeuralNetwork targetSetterBrain) {
        this.brain.setWeights(movementBrain.getWeights());
        this.brain.setBiases(movementBrain.getBiases());
        this.stateUpdateBrain.setWeights(stateUpdateBrain.getWeights());
        this.stateUpdateBrain.setBiases(stateUpdateBrain.getBiases());
        this.targetSetterBrain.setWeights(targetSetterBrain.getWeights());
        this.targetSetterBrain.setBiases(targetSetterBrain.getBiases());
        resetWorldState();
        velocity.set(0, 0, 0);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}