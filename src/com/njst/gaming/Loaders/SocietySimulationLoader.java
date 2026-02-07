package com.njst.gaming.Loaders;

import com.njst.gaming.*;
import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Geometries.TerrainGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.simulation.entities.NPC;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SocietySimulationLoader implements Scene.SceneLoader {
    private Scene scene;
    private List<NPC> npcs = new ArrayList<>();
    private Vector3 targetPosition = new Vector3(-50, 2, -50);
    private GameObject targetMarker;
    private Random rnd = new Random();
    private float generationTimer = 0;
    private int populationCap = 200; // Configurable population cap
    private boolean shouldDumpBestBrain = false;

    @Override
    public void load(Scene s) {
        this.scene = s;

        // Load textures
        int skyboxTex = ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg");
        int groundTex = ShaderProgram.loadTexture(data.rootDirectory + "/j.jpg");
        int npcTex = ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg");
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

        // Target visual marker
        targetMarker = new GameObject(new CubeGeometry(), targetTex);
        targetMarker.position = targetPosition.clone();
        targetMarker.scale = new float[] { 0.5f, 0.5f, 0.5f };
        s.addGameObject(targetMarker);

        // Spawn initial NPCs at origin
        for (int i = 0; i < populationCap; i++) {
            spawnNPC(s, npcTex, 0, 1, 0);
        }

        // Initialize Control UI
        initControlPanel();

        // Animation loop for simulation logic
        s.animations.add(new Animation() {
            @Override
            public void animate() {
                for (int i = 0; i < 10; i++) {
                    float dt = 1.0f / 60.0f;
                    generationTimer += dt;

                    // Update NPCs movement
                    for (NPC npc : npcs) {
                        npc.update(targetPosition, dt);
                    }

                    if (generationTimer >= 30.0f) {
                        runGeneration(s, npcTex);
                        generationTimer = 0;
                    }
                }
            }
        });

        System.out.println("Evolutionary Simulation started with " + populationCap + " NPCs starting at origin.");
    }

    private void initControlPanel() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Simulation Controls");
            frame.setLayout(new GridLayout(4, 1));
            frame.setSize(300, 250);
            frame.setAlwaysOnTop(true);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Mutation Rate Slider (0 - 100, maps to 0.0 - 1.0)
            JPanel p1 = new JPanel(new BorderLayout());
            JLabel l1 = new JLabel("Mutation Rate: " + NPC.mutationRate);
            JSlider s1 = new JSlider(0, 100, (int) (NPC.mutationRate * 100));
            s1.addChangeListener(e -> {
                NPC.mutationRate = s1.getValue() / 100.0f;
                l1.setText("Mutation Rate: " + String.format("%.2f", NPC.mutationRate));
            });
            p1.add(l1, BorderLayout.NORTH);
            p1.add(s1, BorderLayout.CENTER);

            // Mutation Strength Slider (0 - 100, maps to 0.0 - 1.0)
            JPanel p2 = new JPanel(new BorderLayout());
            JLabel l2 = new JLabel("Mutation Strength: " + NPC.mutationStrength);
            JSlider s2 = new JSlider(0, 100, (int) (NPC.mutationStrength * 100));
            s2.addChangeListener(e -> {
                NPC.mutationStrength = s2.getValue() / 100.0f;
                l2.setText("Mutation Strength: " + String.format("%.2f", NPC.mutationStrength));
            });
            p2.add(l2, BorderLayout.NORTH);
            p2.add(s2, BorderLayout.CENTER);

            // Dump Button
            JButton dumpBtn = new JButton("Dump Closest Brain (Next Cycle)");
            dumpBtn.addActionListener(e -> {
                shouldDumpBestBrain = true;
                dumpBtn.setText("Wait for Cycle End...");
                dumpBtn.setEnabled(false);
            });

            frame.add(p1);
            frame.add(p2);
            frame.add(dumpBtn);
            frame.setVisible(true);

            // To update button state after dump
            scene.animations.add(new Animation() {
                @Override
                public void animate() {
                    if (!shouldDumpBestBrain && !dumpBtn.isEnabled()) {
                        dumpBtn.setText("Dump Closest Brain (Next Cycle)");
                        dumpBtn.setEnabled(true);
                    }
                }
            });
        });
    }

    private void runGeneration(Scene s, int npcTex) {
        // Sort NPCs by distance to target (closest first)
        npcs.sort(
                (n1, n2) -> Float.compare(n1.position.distance(targetPosition), n2.position.distance(targetPosition)));

        // Dump logic
        if (shouldDumpBestBrain && !npcs.isEmpty()) {
            File dumpFile = new File("best_brain.bin");
            npcs.get(0).brain.saveToFile(dumpFile);
            System.out.println("Best brain dumped to: " + dumpFile.getAbsolutePath());
            shouldDumpBestBrain = false;
        }

        int survivorCount = populationCap / 5;

        // Keep top half as survivors, cull bottom half
        List<NPC> survivors = new ArrayList<>(npcs.subList(0, survivorCount));
        List<NPC> culled = new ArrayList<>(npcs.subList(survivorCount, npcs.size()));

        // Remove culled from scene
        for (NPC c : culled) {
            scene.removeGameObject(c.skin);
        }

        // Reproduce survivors to fill up to populationCap
        List<NPC> nextGen = new ArrayList<>(survivors);
        Random reproductionRnd = new Random();
        while (nextGen.size() < populationCap) {
            NPC survivor = survivors.get(reproductionRnd.nextInt(survivors.size()));
            GameObject childSkin = new GameObject(new CubeGeometry(), npcTex);
            childSkin.scale = new float[] { 1, 1, 1 };

            NPC child = survivor.reproduce(childSkin);
            nextGen.add(child);
            scene.addGameObject(childSkin);
        }

        npcs = nextGen;

        // Force everyone back to origin for the next generation
        for (NPC npc : npcs) {
            npc.position.set(0, 1, 0);
            npc.skin.updateModelMatrix();
        }

        // Move target randomly within ground bounds
        targetPosition.set((float) (rnd.nextFloat() * 20 - 10), 2, (float) (rnd.nextFloat() * 20 - 10));
        targetMarker.position.set(targetPosition);
        targetMarker.updateModelMatrix();

        System.out.println("New generation: Population=" + npcs.size() + ". All reset to origin. Target moved to "
                + targetPosition);
    }

    private void spawnNPC(Scene s, int tex, float x, float y, float z) {
        GameObject npcSkin = new GameObject(new CubeGeometry(), tex);
        npcSkin.move(x, y, z);
        npcSkin.scale = new float[] { 1, 1, 1 };
        NPC npc = new NPC(npcSkin);
        npcs.add(npc);
        s.addGameObject(npcSkin);
    }
}
