package com.njst.gaming.Loaders;

import com.njst.gaming.*;
import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Geometries.Geometry;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Geometries.TerrainGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.simulation.entities.NPC;
import com.njst.gaming.ai.NeuralNetwork;
import com.njst.gaming.ui.SimulationControlPanel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SocietySimulationLoader implements Scene.SceneLoader, SimulationControlPanel.ControlActions {
    private Scene scene;
    private List<NPC> npcs = new ArrayList<>();
    private List<GameObject> foodItems = new ArrayList<>();
    private Vector3 targetPosition = new Vector3(-50, 2, -50);
    private GameObject targetMarker;
    private int npcTex;
    private Random rnd = new Random();
    private float generationTimer = 0;
    private int populationCap = 200; // Configurable population cap
    private boolean shouldDumpMovementBrain = false;
    private boolean shouldDumpStateBrain = false;
    private boolean shouldDumpTargetBrain = false;
    private boolean shouldLoadMovementToAll = false;
    private boolean shouldLoadStateToAll = false;
    private boolean shouldLoadTargetToAll = false;
    private volatile boolean isFastTraining = false;
    private boolean forcedTargetMode = true;
    private Vector3 forcedTargetPosition = new Vector3(0, 1, 0);

    private final Geometry foodGeometry = new SphereGeometry(0.3f, 10, 10);

    @Override
    public void load(Scene s) {
        this.scene = s;

        // Load textures
        int skyboxTex = ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg");
        int groundTex = ShaderProgram.loadTexture(data.rootDirectory + "/j.jpg");
        this.npcTex = ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg");
        int targetTex = ShaderProgram.loadTexture(data.rootDirectory + "/red.png");

        // Skybox
        GameObject skybox = new GameObject(new SphereGeometry(1, 20, 20), skyboxTex);
        skybox.scale = new float[] { 500, 500, 500 };
        skybox.ambientlight_multiplier = 5;
        s.addGameObject(skybox);

        // Ground
        GameObject ground = new GameObject(new TerrainGeometry(100, 100, new float[100][100]), groundTex);
        ground.move(-50, -0.5f, -50);
        s.addGameObject(ground);

        // Visual target (Synchronized sphere in forced mode)
        targetMarker = new GameObject(new SphereGeometry(0.5f, 16, 16), targetTex);
        targetMarker.position = forcedTargetPosition.clone();
        s.addGameObject(targetMarker);

        // Try to load best brain template
        NeuralNetwork movementTemplate = null;
        NeuralNetwork stateTemplate = null;
        NeuralNetwork targetTemplate = null;
        File bFile = new File("best_brain.bin");
        if (bFile.exists()) {
            try {
                movementTemplate = NeuralNetwork.loadFromFile(bFile);
                System.out.println("Loaded initial movement brain from best_brain.bin");

                File sFile = new File("best_state.bin");
                if (sFile.exists())
                    stateTemplate = NeuralNetwork.loadFromFile(sFile);

                File tFile = new File("best_target.bin");
                if (tFile.exists())
                    targetTemplate = NeuralNetwork.loadFromFile(tFile);

            } catch (Exception e) {
                System.err.println("Could not load template brain: " + e.getMessage());
            }
        }

        // Spawn initial NPCs at random locations
        for (int i = 0; i < populationCap; i++) {
            float rx = rnd.nextFloat() * 80 - 40;
            float rz = rnd.nextFloat() * 80 - 40;
            spawnNPC(s, npcTex, rx, 1, rz, movementTemplate, stateTemplate, targetTemplate);
        }

        // Spawn initial food
        spawnFoodItems(s, npcTex); // Using npcTex for simplicity or foodTex

        // Initialize Control UI
        new SimulationControlPanel(this);

        // Animation loop for simulation logic
        s.animations.add(new Animation() {
            @Override
            public void animate() {
                if (isFastTraining)
                    return; // Skip logic when in headless fast training

                for (int i = 0; i < 10; i++) {
                    float dt = 1.0f / 60.0f;
                    generationTimer += dt;

                    // Update NPCs movement
                    for (NPC npc : npcs) {
                        Vector3 target = forcedTargetMode ? forcedTargetPosition : npc.personalTarget;
                        npc.update(target, dt);
                    }

                    if (forcedTargetMode) {
                        targetMarker.position.set(forcedTargetPosition);
                    } else if (!foodItems.isEmpty()) {
                        targetMarker.position.set(foodItems.get(0).position);
                    }

                    if (generationTimer >= 30.0f) {
                        runGeneration(s, npcTex);
                        generationTimer = 0;
                    }
                }
            }
        });

        // Trigger initial perception
        updateNPCPerception();

        System.out.println("Evolutionary Simulation started with " + populationCap + " NPCs.");
    }

    private void spawnFoodItems(Scene s, int tex) {
        // Spawn 10-20 random food items
        int count = 10 + rnd.nextInt(10);

        // Use existing items if available
        int reUsed = 0;
        for (int i = 0; i < Math.min(count, foodItems.size()); i++) {
            GameObject food = foodItems.get(i);
            float fx = (float) (rnd.nextFloat() * 80 - 40);
            float fz = (float) (rnd.nextFloat() * 80 - 40);
            food.setPosition(fx, 1, fz);
            reUsed++;
        }

        // Add new items if needed
        for (int i = reUsed; i < count; i++) {
            GameObject food = new GameObject(foodGeometry, tex);
            float fx = (float) (rnd.nextFloat() * 80 - 40);
            float fz = (float) (rnd.nextFloat() * 80 - 40);
            food.move(fx, 1, fz);
            food.scale = new float[] { 1, 1, 1 };
            foodItems.add(food);
            scene.addGameObject(food);
        }

        // Cleanup surplus items
        while (foodItems.size() > count) {
            GameObject surplus = foodItems.remove(foodItems.size() - 1);
            scene.removeGameObject(surplus);
            surplus.cleanup();
        }

        // Track first food for legacy visual marker
        if (!foodItems.isEmpty()) {
            targetPosition.set(foodItems.get(0).position);
            targetMarker.position.set(targetPosition);
            targetMarker.updateModelMatrix();
        }
    }

    private void updateNPCPerception() {
        for (NPC npc : npcs) {
            npc.resetWorldState();
            for (GameObject food : foodItems) {
                npc.observeFood(food.position, 1.0f); // default value 1.0
            }
            npc.decideTarget();
        }
    }

    @Override
    public void saveMovement() {
        shouldDumpMovementBrain = true;
    }

    @Override
    public void saveState() {
        shouldDumpStateBrain = true;
    }

    @Override
    public void saveTarget() {
        shouldDumpTargetBrain = true;
    }

    @Override
    public void loadMovementToAll() {
        shouldLoadMovementToAll = true;
    }

    @Override
    public void loadStateToAll() {
        shouldLoadStateToAll = true;
    }

    @Override
    public void loadTargetToAll() {
        shouldLoadTargetToAll = true;
    }

    @Override
    public void toggleFastTraining() {
        isFastTraining = !isFastTraining;
        if (isFastTraining) {
            startFastTrainingThread(npcTex);
        } else {
            if (scene.renderer != null)
                scene.renderer.hasError = false;
            System.gc();
        }
    }

    @Override
    public boolean isFastTraining() {
        return isFastTraining;
    }

    @Override
    public void toggleForcedTargetMode() {
        forcedTargetMode = !forcedTargetMode;
        if (forcedTargetMode) {
            System.out.println("Forced Target Mode Enabled: Focus on movement training.");
        } else {
            System.out.println("Forced Target Mode Disabled.");
        }
    }

    @Override
    public boolean isForcedTargetMode() {
        return forcedTargetMode;
    }

    private void runGeneration(Scene s, int npcTex) {
        // Sort NPCs by performance:
        // In Forced Target Mode, we sort by proximity to the synchronized global
        // target.
        // Otherwise, we sort by proximity to the nearest food item.
        if (forcedTargetMode) {
            npcs.sort((n1, n2) -> Float.compare(n1.position.distance(forcedTargetPosition),
                    n2.position.distance(forcedTargetPosition)));
            // Randomize target for the next generation
            forcedTargetPosition.set(rnd.nextFloat() * 80 - 40, 1, rnd.nextFloat() * 80 - 40);
        } else {
            npcs.sort((n1, n2) -> Float.compare(getMinDistToFood(n1), getMinDistToFood(n2)));
        }

        // Dump logic
        if (!npcs.isEmpty()) {
            NPC best = npcs.get(0);
            if (shouldDumpMovementBrain) {
                best.brain.saveToFile(new File("best_brain.bin"));
                System.out.println("Best movement brain saved.");
                shouldDumpMovementBrain = false;
            }
            if (shouldDumpStateBrain) {
                best.stateUpdateBrain.saveToFile(new File("best_state.bin"));
                System.out.println("Best state brain saved.");
                shouldDumpStateBrain = false;
            }
            if (shouldDumpTargetBrain) {
                best.targetSetterBrain.saveToFile(new File("best_target.bin"));
                System.out.println("Best target brain saved.");
                shouldDumpTargetBrain = false;
            }
        }

        int survivorCount = populationCap / 5;

        // Keep survivors, update culled NPCs to become the next generation (pooling)
        List<NPC> survivors = new ArrayList<>(npcs.subList(0, survivorCount));
        List<NPC> culled = npcs.subList(survivorCount, npcs.size());

        // Reproduce survivors into the culled slots
        Random reproductionRnd = new Random();
        for (NPC poolNPC : culled) {
            NPC parent = survivors.get(reproductionRnd.nextInt(survivors.size()));

            // Mutate parent's brains for the pooled NPC
            NeuralNetwork newMovement = parent.brain.copy();
            newMovement.mutate(NPC.movementMutationRate, NPC.movementMutationStrength);
            NeuralNetwork newStateUpdate = parent.stateUpdateBrain.copy();
            newStateUpdate.mutate(NPC.stateMutationRate, NPC.stateMutationStrength);
            NeuralNetwork newTargetSetter = parent.targetSetterBrain.copy();
            newTargetSetter.mutate(NPC.targetMutationRate, NPC.targetMutationStrength);

            poolNPC.updateBrains(newMovement, newStateUpdate, newTargetSetter);
        }

        // Load logic (injected across population if requested)
        if (shouldLoadMovementToAll || shouldLoadStateToAll || shouldLoadTargetToAll) {
            try {
                NeuralNetwork mTmp = shouldLoadMovementToAll ? NeuralNetwork.loadFromFile(new File("best_brain.bin"))
                        : null;
                NeuralNetwork sTmp = shouldLoadStateToAll ? NeuralNetwork.loadFromFile(new File("best_state.bin"))
                        : null;
                NeuralNetwork tTmp = shouldLoadTargetToAll ? NeuralNetwork.loadFromFile(new File("best_target.bin"))
                        : null;

                for (NPC npc : npcs) {
                    npc.updateBrains(
                            mTmp != null ? mTmp.copy() : npc.brain,
                            sTmp != null ? sTmp.copy() : npc.stateUpdateBrain,
                            tTmp != null ? tTmp.copy() : npc.targetSetterBrain);
                }
                System.out.println("Loaded requested brains across population.");
            } catch (Exception e) {
                System.err.println("Load failed: " + e.getMessage());
            }
            shouldLoadMovementToAll = false;
            shouldLoadStateToAll = false;
            shouldLoadTargetToAll = false;
        }

        // All NPCs are now updated correctly in the 'npcs' list

        // Force everyone to new random locations and reset target
        for (NPC npc : npcs) {
            float rx = rnd.nextFloat() * 80 - 40;
            float rz = rnd.nextFloat() * 80 - 40;
            npc.position.set(rx, 1, rz);
            npc.skin.updateModelMatrix();
        }

        // Cycle food
        spawnFoodItems(s, npcTex);

        // Perceptual pass for new generation
        updateNPCPerception();

        System.out
                .println("New generation: Population=" + npcs.size() + ". All reset to random locations. Food cycled.");
    }

    private void startFastTrainingThread(final int npcTex) {
        new Thread(() -> {
            System.out.println("Starting Fast Training Mode (Headless)...");
            while (isFastTraining) {
                // Perform a perceptual pass
                updateNPCPerception();

                // Teleport NPCs to their targets to evaluate proximity (instant movement)
                for (NPC npc : npcs) {
                    Vector3 target = forcedTargetMode ? forcedTargetPosition : npc.personalTarget;
                    npc.position.set(target);
                }

                // Run generation cycle
                runGeneration(scene, npcTex);

                try {
                    Thread.sleep(10); // Small sleep to prevent overwhelming the CPU/UI
                } catch (InterruptedException e) {
                    break;
                }
            }
            System.out.println("Stopped Fast Training Mode.");
        }).start();
    }

    private float getMinDistToFood(NPC npc) {
        float minDist = Float.MAX_VALUE;
        if (foodItems.isEmpty())
            return 1000f; // Penalty if no food
        for (GameObject food : foodItems) {
            float d = npc.position.distance(food.position);
            if (d < minDist)
                minDist = d;
        }
        return minDist;
    }

    private void spawnNPC(Scene s, int tex, float x, float y, float z,
            NeuralNetwork mBrain, NeuralNetwork sBrain, NeuralNetwork tBrain) {
        GameObject npcSkin = new GameObject(new CubeGeometry(), tex);
        npcSkin.move(x, y, z);
        npcSkin.scale = new float[] { 1, 1, 1 };

        NPC npc;
        if (mBrain != null) {
            // Apply initial mutations to the templates
            NeuralNetwork m = mBrain.copy();
            m.mutate(NPC.movementMutationRate, NPC.movementMutationStrength);
            NeuralNetwork st = (sBrain != null) ? sBrain.copy()
                    : new NeuralNetwork(new int[] { 12, 16, 8 }, 0.01f, false);
            if (sBrain != null)
                st.mutate(NPC.stateMutationRate, NPC.stateMutationStrength);

            NeuralNetwork tg = (tBrain != null) ? tBrain.copy()
                    : new NeuralNetwork(new int[] { 8, 12, 2 }, 0.01f, true);
            if (tBrain != null)
                tg.mutate(NPC.targetMutationRate, NPC.targetMutationStrength);

            npc = new NPC(npcSkin, m, st, tg);
        } else {
            npc = new NPC(npcSkin);
        }

        npcs.add(npc);
        s.addGameObject(npcSkin);
    }
}
