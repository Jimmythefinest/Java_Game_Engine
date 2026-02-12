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
import com.njst.gaming.simulation.entities.GraphNPC;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GraphSocietySimulationLoader implements Scene.SceneLoader {
    private Scene scene;
    private List<GraphNPC> npcs = new ArrayList<>();
    private List<GameObject> foodItems = new ArrayList<>();
    private List<GameObject> waterItems = new ArrayList<>();
    private List<Vector3> foodPositions = new ArrayList<>();
    private List<Vector3> waterPositions = new ArrayList<>();
    private Random rnd = new Random();
    
    private int populationSize = 10;
    private int foodCount = 15;
    private int waterCount = 10;
    
    private int generation = 1;
    private float totalGenerationTime = 0;
    private static final float MAX_GEN_TIME = 60.0f;
    private float simulationSpeed = 1.0f;
    private com.njst.gaming.simulation.ui.SimulationControlPanel controlPanel;

    private final Geometry foodGeometry = new SphereGeometry(0.3f, 10, 10);
    private final Geometry waterGeometry = new SphereGeometry(0.4f, 12, 12);
    private int npcTex, foodTex, waterTex;

    @Override
    public void load(Scene s) {
        this.scene = s;
        
        // Initialize UI Control Panel
        this.controlPanel = new com.njst.gaming.simulation.ui.SimulationControlPanel(speed -> {
            this.simulationSpeed = speed;
        });

        // Load textures
        int skyboxTex = ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg");
        int groundTex = ShaderProgram.loadTexture(data.rootDirectory + "/j.jpg");
        this.npcTex = ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg");
        this.foodTex = ShaderProgram.loadTexture(data.rootDirectory + "/kj.jpg");
        this.waterTex = ShaderProgram.loadTexture(data.rootDirectory + "/blade.png");

        // Skybox
        GameObject skybox = new GameObject(new SphereGeometry(1, 20, 20), skyboxTex);
        skybox.scale = new float[] { 500, 500, 500 };
        skybox.ambientlight_multiplier = 5;
        s.addGameObject(skybox);

        // Ground
        GameObject ground = new GameObject(new TerrainGeometry(100, 100, new float[100][100]), groundTex);
        ground.move(-50, -0.5f, -50);
        s.addGameObject(ground);

        // Initial Spawn
        for (int i = 0; i < populationSize; i++) {
            float rx = rnd.nextFloat() * 80 - 40;
            float rz = rnd.nextFloat() * 80 - 40;
            spawnNPC(rx, 1, rz, new com.njst.gaming.simulation.brain.SurvivalBrain());
        }

        spawnFoodItems();
        spawnWaterItems();

        // Animation loop
        s.animations.add(new Animation() {
            @Override
            public void animate() {
                float baseDt = 1.0f / 60.0f;
                float dt = baseDt * simulationSpeed;
                totalGenerationTime += dt;
                
                foodPositions.clear();
                for (GameObject food : foodItems) foodPositions.add(food.position);
                waterPositions.clear();
                for (GameObject water : waterItems) waterPositions.add(water.position);

                int aliveCount = 0;
                List<Vector3> npcPositions = new ArrayList<>();
                for (GraphNPC npc : npcs) {
                    if (npc.isAlive) npcPositions.add(npc.position);
                }

                for (GraphNPC npc : npcs) {
                    if (npc.isAlive) {
                        npc.update(foodPositions, waterPositions, npcPositions, dt);
                        handleResourceConsumption(npc);
                        aliveCount++;
                    } else {
                        npc.skin.move(0, -10, 0); // Hide dead ones
                    }
                }

                // Update UI Stats
                controlPanel.updateStats(generation, aliveCount);

                if (aliveCount == 0 || totalGenerationTime > MAX_GEN_TIME) {
                    runNextGeneration();
                }
            }
        });

        System.out.println("Evolutionary Graph Simulation started.");
    }

    private void spawnNPC(float x, float y, float z, com.njst.gaming.simulation.brain.NPCBrain brain) {
        GameObject npcSkin = new GameObject(new CubeGeometry(), npcTex);
        npcSkin.move(x, y, z);
        npcSkin.scale = new float[] { 1, 1, 1 };
        
        GraphNPC npc = new GraphNPC(npcSkin, brain);
        npcs.add(npc);
        scene.addGameObject(npcSkin);
    }

    private void runNextGeneration() {
        generation++;
        System.out.println("--- Generation " + generation + " ---");
        
        // Selection: Sort by lifetime
        npcs.sort((n1, n2) -> Float.compare(n2.lifetime, n1.lifetime));
        
        float avgLifetime = 0;
        for(GraphNPC npc : npcs) avgLifetime += npc.lifetime;
        avgLifetime /= populationSize;
        System.out.println("Avg Lifetime: " + avgLifetime);

        // Keep top survivals for reproduction
        int topCount = Math.max(1, populationSize / 5);
        List<GraphNPC> topNPCs = new ArrayList<>(npcs.subList(0, topCount));

        // Cleanup scene
        for (GraphNPC npc : npcs) {
            scene.removeGameObject(npc.skin);
        }
        npcs.clear();

        // Populate new generation
        for (int i = 0; i < populationSize; i++) {
            GraphNPC parent = topNPCs.get(rnd.nextInt(topCount));
            float rx = rnd.nextFloat() * 80 - 40;
            float rz = rnd.nextFloat() * 80 - 40;
            spawnNPC(rx, 1, rz, parent.getBrain().reproduce());
        }

        totalGenerationTime = 0;
        
        // Reset resources
        for (GameObject food : foodItems) relocateResource(food);
        for (GameObject water : waterItems) relocateResource(water);
    }

    private void spawnFoodItems() {
        for (int i = 0; i < foodCount; i++) {
            GameObject food = new GameObject(foodGeometry, foodTex);
            relocateResource(food);
            foodItems.add(food);
            scene.addGameObject(food);
        }
    }

    private void spawnWaterItems() {
        for (int i = 0; i < waterCount; i++) {
            GameObject water = new GameObject(waterGeometry, waterTex);
            relocateResource(water);
            waterItems.add(water);
            scene.addGameObject(water);
        }
    }

    private void handleResourceConsumption(GraphNPC npc) {
        for (GameObject food : foodItems) {
            if (npc.position.distance(food.position) < 1.0f) {
                npc.hunger = 0;
                relocateResource(food);
                break;
            }
        }
        for (GameObject water : waterItems) {
            if (npc.position.distance(water.position) < 1.0f) {
                npc.thirst = 0;
                relocateResource(water);
                break;
            }
        }
    }

    private void relocateResource(GameObject obj) {
        float rx = rnd.nextFloat() * 90 - 45;
        float rz = rnd.nextFloat() * 90 - 45;
        obj.setPosition(rx, 1, rz);
        obj.updateModelMatrix();
    }
}
