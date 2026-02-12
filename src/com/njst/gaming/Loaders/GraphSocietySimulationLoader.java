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
    private int populationSize = 50;
    private int foodCount = 15;
    private int waterCount = 10;

    private final Geometry foodGeometry = new SphereGeometry(0.3f, 10, 10);
    private final Geometry waterGeometry = new SphereGeometry(0.4f, 12, 12);
    private int npcTex;
    private int foodTex;
    private int waterTex;

    @Override
    public void load(Scene s) {
        this.scene = s;

        // Load textures
        int skyboxTex = ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg");
        int groundTex = ShaderProgram.loadTexture(data.rootDirectory + "/j.jpg");
        this.npcTex = ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg");
        this.foodTex = ShaderProgram.loadTexture(data.rootDirectory + "/red.png");
        this.waterTex = ShaderProgram.loadTexture(data.rootDirectory + "/blue.png");

        // Skybox
        GameObject skybox = new GameObject(new SphereGeometry(1, 20, 20), skyboxTex);
        skybox.scale = new float[] { 500, 500, 500 };
        skybox.ambientlight_multiplier = 5;
        s.addGameObject(skybox);

        // Ground
        GameObject ground = new GameObject(new TerrainGeometry(100, 100, new float[100][100]), groundTex);
        ground.move(-50, -0.5f, -50);
        s.addGameObject(ground);

        // Spawn NPCs
        for (int i = 0; i < populationSize; i++) {
            float rx = rnd.nextFloat() * 80 - 40;
            float rz = rnd.nextFloat() * 80 - 40;
            spawnNPC(rx, 1, rz);
        }

        // Spawn Resources
        spawnFoodItems();
        spawnWaterItems();

        // Animation loop for simulation logic
        s.animations.add(new Animation() {
            @Override
            public void animate() {
                float dt = 1.0f / 60.0f;
                
                // Update resource positions list for NPCs to sense
                foodPositions.clear();
                for (GameObject food : foodItems) foodPositions.add(food.position);
                
                waterPositions.clear();
                for (GameObject water : waterItems) waterPositions.add(water.position);

                // Update NPCs
                for (GraphNPC npc : npcs) {
                    npc.update(foodPositions, waterPositions, dt);
                    
                    // Logic to "consume" resources
                    handleResourceConsumption(npc);
                }
            }
        });

        System.out.println("Graph Simulation updated: Hunger/Thirst enabled. NPCs: " + populationSize);
    }

    private void spawnNPC(float x, float y, float z) {
        GameObject npcSkin = new GameObject(new CubeGeometry(), npcTex);
        npcSkin.move(x, y, z);
        npcSkin.scale = new float[] { 1, 1, 1 };
        
        GraphNPC npc = new GraphNPC(npcSkin);
        npcs.add(npc);
        scene.addGameObject(npcSkin);
    }

    private void spawnFoodItems() {
        for (int i = 0; i < foodCount; i++) {
            GameObject food = new GameObject(foodGeometry, foodTex);
            float fx = rnd.nextFloat() * 90 - 45;
            float fz = rnd.nextFloat() * 90 - 45;
            food.move(fx, 1, fz);
            foodItems.add(food);
            scene.addGameObject(food);
        }
    }

    private void spawnWaterItems() {
        for (int i = 0; i < waterCount; i++) {
            GameObject water = new GameObject(waterGeometry, waterTex);
            float wx = rnd.nextFloat() * 90 - 45;
            float wz = rnd.nextFloat() * 90 - 45;
            water.move(wx, 1, wz);
            waterItems.add(water);
            scene.addGameObject(water);
        }
    }

    private void handleResourceConsumption(GraphNPC npc) {
        // Handle Food
        for (GameObject food : foodItems) {
            if (npc.position.distance(food.position) < 1.0f) {
                npc.hunger = 0; // Reset hunger
                relocateResource(food);
                break;
            }
        }

        // Handle Water
        for (GameObject water : waterItems) {
            if (npc.position.distance(water.position) < 1.0f) {
                npc.thirst = 0; // Reset thirst
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
