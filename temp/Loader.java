package com.njst.game1;

import com.njst.gaming.*;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.Geometries.*;
import com.njst.gaming.Natives.*;
import com.njst.gaming.Math.*;
import com.njst.gaming.Animations.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Iterator;
import java.util.List;

public class Loader implements Scene.SceneLoader {
	private Scene scene;
	private List<NPC> npcs = new ArrayList<>();
	private List<Projectile> projectiles = new ArrayList<>();
	private Random rnd = new Random();
	
	// Survival system
	private ArrayList<agent> agents = new ArrayList<>();
	private ArrayList<Resource> resources = new ArrayList<>();
	private int maxPopulation = 50;
	private int foodTexture;
	private int waterTexture;
	private float resourceCollectionRadius = 2.0f;
	private int updateCounter = 0;
	private int resourceSpawnInterval = 60; // Spawn resources every 60 updates

	public void load(Scene s) {
		this.scene = s;
		int skyboxTex = ShaderProgram.loadTexture("/jimmy/desertstorm.jpg");
		int groundTex = ShaderProgram.loadTexture("/jimmy/j.jpg");
		int npcTex = ShaderProgram.loadTexture("/jimmy/desertstorm.jpg");
		
		// Different textures for food and water
		foodTexture = ShaderProgram.loadTexture("/jimmy/j.jpg");
		waterTexture = ShaderProgram.loadTexture("/jimmy/desertstorm.jpg");

		GameObject skybox = new GameObject(
				new SphereGeometry(1, 20, 20),
				skyboxTex);
		skybox.scale = new float[] { 500, 500, 500 };
		skybox.ambientlight_multiplier = 5;
		s.addGameObject(skybox);
		s.objects.remove(skybox);
		GameObject ground = new GameObject(
				new TerrainGeometry(100, 100,new float[100][100]),
				groundTex);
		ground.move(0, -0.5f, 0);
		s.addGameObject(ground);

		// Initialize agents
		for(int i=0;i<10;i++){
			agent newAgent = new agent();
			// Randomize starting positions
			newAgent.position.x = (float)(Math.random() * 20 - 10);
			newAgent.position.z = (float)(Math.random() * 20 - 10);
			newAgent.skin.position = newAgent.position;
			newAgent.skin.updateModelMatrix();
			
			agents.add(newAgent);
			s.addGameObject(newAgent.skin);
		}
		
		// Spawn initial resources
		spawnResources(5, 5);
		
		s.animations.add(new Animation(){
			public void animate(){
				updateCounter++;
				
				// Update all agents
				for(int i=0;i<agents.size();i++){
					agents.get(i).update();
					
					// Check resource collection
					checkResourceCollection(agents.get(i));
				}
				
				// Remove dead agents
				removeDeadAgents();
				
				// Handle reproduction
				handleReproduction();
				
				// Spawn new resources periodically
				if (updateCounter % resourceSpawnInterval == 0) {
					spawnResources(2, 2);
				}
				
				// Remove collected resources
				cleanupResources();
			}
		});

	}
	
	private void spawnResources(int foodCount, int waterCount) {
		// Spawn food
		for (int i = 0; i < foodCount; i++) {
			Vector3 pos = new Vector3(
				(float)(Math.random() * 40 - 20),
				0f,
				(float)(Math.random() * 40 - 20)
			);
			Resource food = new Resource("food", pos, foodTexture);
			resources.add(food);
			scene.addGameObject(food.visual);
		}
		
		// Spawn water
		for (int i = 0; i < waterCount; i++) {
			Vector3 pos = new Vector3(
				(float)(Math.random() * 40 - 20),
				0f,
				(float)(Math.random() * 40 - 20)
			);
			Resource water = new Resource("water", pos, waterTexture);
			resources.add(water);
			scene.addGameObject(water.visual);
		}
	}
	
	private void checkResourceCollection(agent agent) {
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
		Iterator<agent> iterator = agents.iterator();
		while (iterator.hasNext()) {
			agent a = iterator.next();
			if (!a.isAlive()) {
				scene.objects.remove(a.skin);
				iterator.remove();
				System.out.println("Agent died. Population: " + agents.size());
			}
		}
	}
	
	private void handleReproduction() {
		// Check if we can add more agents
		if (agents.size() >= maxPopulation) return;
		
		ArrayList<agent> newAgents = new ArrayList<>();
		
		for (agent a : agents) {
			if (a.canReproduce() && agents.size() + newAgents.size() < maxPopulation) {
				agent offspring = a.reproduce();
				newAgents.add(offspring);
				scene.addGameObject(offspring.skin);
				System.out.println("Agent reproduced! Population: " + (agents.size() + newAgents.size()));
			}
		}
		
		agents.addAll(newAgents);
	}
	
	private void cleanupResources() {
		Iterator<Resource> iterator = resources.iterator();
		while (iterator.hasNext()) {
			Resource r = iterator.next();
			if (r.isCollected()) {
				// Move resource far away and make invisible
				r.visual.position = new Vector3(1000, -1000, 1000);
				r.visual.scale = new float[]{0.001f, 0.001f, 0.001f};
				r.visual.updateModelMatrix();
				iterator.remove();
			}
		}
	}

	public void spawnProjectile(Vector3 pos, Vector3 vel, int team) {
		
	}
}
