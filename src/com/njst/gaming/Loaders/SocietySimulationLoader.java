package com.njst.gaming.Loaders;

import com.njst.gaming.*;
import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Geometries.TerrainGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.simulation.entities.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SocietySimulationLoader implements Scene.SceneLoader, NPC.ProjectileSpawner {
    private Scene scene;
    private List<NPC> npcs = new ArrayList<>();
    private List<Projectile> projectiles = new ArrayList<>();
    private List<Agent> agents = new ArrayList<>();
    private List<Resource> resources = new ArrayList<>();
    
    private int maxPopulation = 50;
    private int maxResources = 100;
    private int foodTexture;
    private int waterTexture;
    private float resourceCollectionRadius = 2.0f;
    private int updateCounter = 0;
    private int resourceSpawnInterval = 60;
    private Random rnd = new Random();

    @Override
    public void load(Scene s) {
        this.scene = s;
        
        // Load textures
        int skyboxTex = ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg");
        int groundTex = ShaderProgram.loadTexture(data.rootDirectory + "/j.jpg");
        int agentTex = ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg");
        foodTexture = ShaderProgram.loadTexture(data.rootDirectory + "/j.jpg");
        waterTexture = ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg");

        // Skybox
        GameObject skybox = new GameObject(new SphereGeometry(1, 20, 20), skyboxTex);
        skybox.scale = new float[] { 500, 500, 500 };
        skybox.ambientlight_multiplier = 5;
        s.addGameObject(skybox);
        // Note: In temp/Loader.java there was a line s.objects.remove(skybox); which seems odd. 
        // I'll keep it as a comment for now or ignore it if it breaks anything.
        
        // Ground
        GameObject ground = new GameObject(new TerrainGeometry(100, 100, new float[100][100]), groundTex);
        ground.move(0, -0.5f, 0);
        s.addGameObject(ground);

        // Initialize agents
        for (int i = 0; i < 10; i++) {
            Agent newAgent = new Agent(agentTex);
            newAgent.position.x = (float)(Math.random() * 20 - 10);
            newAgent.position.z = (float)(Math.random() * 20 - 10);
            newAgent.skin.position = newAgent.position;
            newAgent.skin.updateModelMatrix();
            
            agents.add(newAgent);
            s.addGameObject(newAgent.skin);
        }

        // Initialize NPCs
        for (int i = 0; i < 4; i++) {
            GameObject npcObj = new GameObject(new com.njst.gaming.Geometries.CubeGeometry(), agentTex);
            npcObj.position = new Vector3((float)(Math.random() * 30 - 15), 0, (float)(Math.random() * 30 - 15));
            npcObj.updateModelMatrix();
            NPC npc = new NPC(npcObj, i % 2); // Two teams
            npcs.add(npc);
            s.addGameObject(npcObj);
        }
        
        // Initial resources
        spawnResources(5, 5);
        
        // Simulation animation loop
        s.animations.add(new Animation() {
            @Override
            public void animate() {
                float dt = 1.0f / 60.0f; // Assuming 60 FPS
                updateCounter++;
                
                // Update Agents
                for (int i = 0; i < agents.size(); i++) {
                    Agent a = agents.get(i);
                    a.update();
                    checkResourceCollection(a);
                }
                
                // Update NPCs
                for (NPC npc : npcs) {
                    npc.update(dt, npcs, SocietySimulationLoader.this);
                }
                
                // Update Projectiles
                Iterator<Projectile> projIter = projectiles.iterator();
                while (projIter.hasNext()) {
                    Projectile p = projIter.next();
                    p.update(dt);
                    if (p.isExpired()) {
                        scene.removeGameObject(p.obj);
                        projIter.remove();
                    }
                }
                
                // Population management
                removeDeadAgents();
                handleReproduction();
                
                // Periodic resource spawning
                if (updateCounter % resourceSpawnInterval == 0) {
                    spawnResources(2, 2);
                }
                
                // Resource cleanup
                cleanupResources();
            }
        });
    }

    private void spawnResources(int foodCount, int waterCount) {
        int currentCount = resources.size();
        if (currentCount >= maxResources) return;

        // Scale down desired spawns if near the cap
        int remainingSpace = maxResources - currentCount;
        int totalToSpawn = foodCount + waterCount;
        if (totalToSpawn > remainingSpace) {
            float ratio = (float)remainingSpace / totalToSpawn;
            foodCount = Math.max(0, (int)(foodCount * ratio));
            waterCount = Math.max(0, (int)(waterCount * ratio));
        }

        for (int i = 0; i < foodCount; i++) {
            Vector3 pos = new Vector3((float)(Math.random() * 40 - 20), 0f, (float)(Math.random() * 40 - 20));
            Resource food = new Resource("food", pos, foodTexture);
            resources.add(food);
            scene.addGameObject(food.visual);
        }
        for (int i = 0; i < waterCount; i++) {
            Vector3 pos = new Vector3((float)(Math.random() * 40 - 20), 0f, (float)(Math.random() * 40 - 20));
            Resource water = new Resource("water", pos, waterTexture);
            resources.add(water);
            scene.addGameObject(water.visual);
        }
    }

    private void checkResourceCollection(Agent agent) {
        if (!agent.isAlive()) return;
        for (Resource resource : resources) {
            if (!resource.isCollected() && resource.isNear(agent.position, resourceCollectionRadius)) {
                if (resource.getType().equals("food")) {
                    agent.collectFood(resource.getAmount());
                } else if (resource.getType().equals("water")) {
                    agent.collectWater(resource.getAmount());
                }
                resource.collect();
            }
        }
    }

    private void removeDeadAgents() {
        Iterator<Agent> iterator = agents.iterator();
        while (iterator.hasNext()) {
            Agent a = iterator.next();
            if (!a.isAlive()) {
                scene.removeGameObject(a.skin);
                iterator.remove();
            }
        }
    }

    private void handleReproduction() {
        if (agents.size() >= maxPopulation) return;
        List<Agent> newAgents = new ArrayList<>();
        for (Agent a : agents) {
            if (a.canReproduce() && agents.size() + newAgents.size() < maxPopulation) {
                Agent offspring = a.reproduce();
                newAgents.add(offspring);
                scene.addGameObject(offspring.skin);
            }
        }
        agents.addAll(newAgents);
    }

    private void cleanupResources() {
        Iterator<Resource> iterator = resources.iterator();
        while (iterator.hasNext()) {
            Resource r = iterator.next();
            if (r.isCollected()) {
                scene.removeGameObject(r.visual);
                iterator.remove();
            }
        }
    }

    @Override
    public void spawnProjectile(Vector3 pos, Vector3 vel, int team) {
        GameObject projObj = new GameObject(new com.njst.gaming.Geometries.CubeGeometry(), foodTexture);
        projObj.position = pos;
        projObj.scale = new float[]{0.1f, 0.1f, 0.1f};
        projObj.updateModelMatrix();
        Projectile p = new Projectile(projObj, vel, team);
        projectiles.add(p);
        scene.addGameObject(projObj);
    }
}
