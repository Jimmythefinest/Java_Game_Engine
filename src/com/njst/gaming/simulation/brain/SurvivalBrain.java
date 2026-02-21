package com.njst.gaming.simulation.brain;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.ai.NeuralNetwork;
import com.njst.gaming.simulation.entities.GraphNPC;
import java.util.List;
import java.util.Random;

/**
 * SurvivalBrain implements a hierarchical neural network architecture for NPC decision-making.
 * 
 * Architecture Overview:
 * 1. Main Brain: A high-level network that selects between 5 states (IDLE, WANDER, SEEK_FOOD, SEEK_WATER, GO_TO_BOX).
 * 2. Box Brain: A specialized navigation network that selects one of the 9 world grid boxes when directed by the Main Brain.
 * 
 * Input Architecture (40 inputs):
 * - [0-1]: Internal Meters (Hunger, Thirst) -> Normalized 0.0 to 1.0.
 * - [2-28]: Spatial Grid (9 Boxes * 3 Types) -> Counts of Food, Water, and other NPCs in each sector.
 * - [29-37]: Current Location (One-Hot Encoded) -> Exactly one node is 1.0 based on the NPC's current grid position.
 * - [38-39]: Distance to Nearest (Food, Water) -> Normalized 0.0 to 1.0 (capped at 100.0 units).
 */
public class SurvivalBrain implements NPCBrain {
    /** High-level state selection network. Inputs: 40, Outputs: 5. */
    public NeuralNetwork mainBrain;
    /** Spatial navigation network. Inputs: 40, Outputs: 9 (one per grid box). */
    public NeuralNetwork boxBrain;
    
    private float stateTimer = 0;
    private Random rnd = new Random();
    private static final float ARRIVE_DISTANCE = 1.0f;

    public SurvivalBrain() {
        // Architecture: 40 inputs -> 16 hidden -> 5/9 outputs
        this.mainBrain = new NeuralNetwork(new int[]{40, 16, 5}, 0.01f, true);
        this.boxBrain = new NeuralNetwork(new int[]{40, 16, 9}, 0.01f, true);
    }

    public SurvivalBrain(NeuralNetwork mainBrain, NeuralNetwork boxBrain) {
        this.mainBrain = mainBrain;
        this.boxBrain = boxBrain;
    }

    @Override
    public void update(GraphNPC npc, List<Vector3> foodPositions, List<Vector3> waterPositions, List<Vector3> npcPositions, float dt) {
        stateTimer += dt;
        
        // Re-evaluate AI logic every 0.2 seconds or if the NPC has arrived at its current target.
        if (stateTimer > 0.2f || npc.position.distance(npc.getPersonalTarget()) < ARRIVE_DISTANCE) {
            makeAIDecision(npc, foodPositions, waterPositions, npcPositions);
        }

        // Execute behaviors associated with the current high-level state.
        switch (npc.getCurrentState()) {
            case SEEK_FOOD:
                Vector3 nearestFood = findNearest(npc.position, foodPositions);
                if (nearestFood != null) npc.getPersonalTarget().set(nearestFood);
                break;
            case SEEK_WATER:
                Vector3 nearestWater = findNearest(npc.position, waterPositions);
                if (nearestWater != null) npc.getPersonalTarget().set(nearestWater);
                break;
            case GO_TO_BOX:
                // personalTarget is already set to the box center by makeAIDecision via boxBrain.
                break;
        }
    }

    /**
     * Constructs the 40-input vector and queries the neural networks for state and navigation.
     */
    private void makeAIDecision(GraphNPC npc, List<Vector3> foodPositions, List<Vector3> waterPositions, List<Vector3> npcPositions) {
        float[] inputs = new float[40];
        
        // [0-1]: Physical Needs
        inputs[0] = npc.hunger;
        inputs[1] = npc.thirst;

        float boxSize = 100f / 3f;
        
        // [2-28]: Spatial Resource Counts (3x3 Grid)
        // Each box stores: [FoodCount, WaterCount, NPCCount]
        for (Vector3 pos : foodPositions) {
            int idx = getGridIndex(pos, boxSize);
            if (idx != -1) inputs[2 + idx * 3 + 0]++;
        }
        for (Vector3 pos : waterPositions) {
            int idx = getGridIndex(pos, boxSize);
            if (idx != -1) inputs[2 + idx * 3 + 1]++;
        }
        for (Vector3 pos : npcPositions) {
            if (pos == npc.position) continue;
            int idx = getGridIndex(pos, boxSize);
            if (idx != -1) inputs[2 + idx * 3 + 2]++;
        }

        // Normalize counts: Values are divided by 10 and capped at 1.0.
        for (int i = 2; i < 29; i++) {
            inputs[i] = Math.min(1.0f, inputs[i] / 10.0f);
        }

        // [29-37]: Spatial Self-Awareness (One-Hot Box Encoding)
        int currentBox = getGridIndex(npc.position, boxSize);
        if (currentBox != -1) {
            inputs[29 + currentBox] = 1.0f;
        }

        // [38-39]: Direct Resource Distance
        Vector3 nearFood = findNearest(npc.position, foodPositions);
        Vector3 nearWater = findNearest(npc.position, waterPositions);
        inputs[38] = nearFood != null ? Math.min(1.0f, npc.position.distance(nearFood) / 100.0f) : 1.0f;
        inputs[39] = nearWater != null ? Math.min(1.0f, npc.position.distance(nearWater) / 100.0f) : 1.0f;

        // Step 1: Run Main Brain to determine the high-level survival state.
        float[] outputs = mainBrain.feedForward(inputs);
        int best = 0;
        float maxVal = outputs[0];
        for (int i = 1; i < 5; i++) {
            if (outputs[i] > maxVal) {
                maxVal = outputs[i];
                best = i;
            }
        }

        GraphNPC.State nextState = GraphNPC.State.values()[best];
        
        // Step 2: If GO_TO_BOX is chosen, run Box Brain to determine spatial navigation target.
        if (nextState == GraphNPC.State.GO_TO_BOX) {
            float[] boxOutputs = boxBrain.feedForward(inputs);
            int bestBox = 0;
            float maxBoxVal = boxOutputs[0];
            for (int i = 1; i < 9; i++) {
                if (boxOutputs[i] > maxBoxVal) {
                    maxBoxVal = boxOutputs[i];
                    bestBox = i;
                }
            }
            // Set the target to the visual center of the chosen grid box.
            setBoxTarget(npc, bestBox, boxSize);
        }

        // Apply state transition and reset timers if necessary.
        if (nextState != npc.getCurrentState()) {
            npc.transitionTo(nextState);
            stateTimer = 0;
        } else if (npc.getCurrentState() == GraphNPC.State.WANDER && npc.position.distance(npc.getPersonalTarget()) < ARRIVE_DISTANCE) {
            npc.setNewWanderTarget();
        }
    }

    /**
     * Translates a grid index (0-8) into a world-space coordinate (center of the box).
     */
    private void setBoxTarget(GraphNPC npc, int boxIndex, float boxSize) {
        int ix = boxIndex % 3;
        int iz = boxIndex / 3;
        float wx = (ix * boxSize + boxSize / 2f) - 50f;
        float wz = (iz * boxSize + boxSize / 2f) - 50f;
        npc.getPersonalTarget().set(wx, npc.position.y, wz);
    }

    /**
     * Maps a world position to a grid index based on a -50 to 50 coordinate system.
     */
    private int getGridIndex(Vector3 pos, float boxSize) {
        float worldX = pos.x + 50;
        float worldZ = pos.z + 50;
        if (worldX < 0 || worldX >= 100 || worldZ < 0 || worldZ >= 100) return -1;
        int ix = (int)(worldX / boxSize);
        int iz = (int)(worldZ / boxSize);
        ix = Math.min(2, Math.max(0, ix));
        iz = Math.min(2, Math.max(0, iz));
        return iz * 3 + ix;
    }

    private Vector3 findNearest(Vector3 npcPos, List<Vector3> positions) {
        Vector3 nearest = null;
        float minDist = Float.MAX_VALUE;
        for (Vector3 pos : positions) {
            float d = npcPos.distance(pos);
            if (d < minDist) {
                minDist = d;
                nearest = pos;
            }
        }
        return nearest;
    }

    /**
     * Deep clones the hierarchical brain structure with Gaussian mutation for neuroevolution.
     */
    @Override
    public SurvivalBrain reproduce() {
`        NeuralNetwork newMain = mainBrain.copy();
        newMain.mutate(0.1f, 0.2f); // 10% mutation rate, 20% strength
        NeuralNetwork newBox = boxBrain.copy();
        newBox.mutate(0.1f, 0.2f);
        return new SurvivalBrain(newMain, newBox);
    }
}
