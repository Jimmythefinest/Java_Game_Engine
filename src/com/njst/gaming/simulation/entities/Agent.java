package com.njst.gaming.simulation.entities;

import com.njst.gaming.objects.GameObject;
import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.ai.NeuralNetwork;

public class Agent {
    public GameObject skin;
    public NeuralNetwork brain;
    public float[] pressures = new float[10];
    public Vector3 position;
    
    // Survival system
    public float energy;
    public float food;
    public float water;
    public float maxEnergy = 100f;
    public float maxFood = 100f;
    public float maxWater = 100f;
    public float initialEnergy;
    public float survivalTime = 0f;
    public boolean alive = true;
    
    // Consumption rates
    private final float energyConsumptionPerMove = 0.1f;
    private final float foodConsumptionRate = 0.05f;  // per update
    private final float waterConsumptionRate = 0.08f; // per update
    private final float foodToEnergyConversion = 0.5f;
    private final float waterToEnergyConversion = 0.3f;
    
    public Agent(int skinTexture) {
        this.skin = new GameObject(
                new CubeGeometry(),
                skinTexture);
        position = new Vector3(0, 0, 0);
        this.skin.position = position;
        this.brain = new NeuralNetwork(new int[]{10, 10, 3}, 0.1f, false);
        
        // Initialize survival stats
        this.energy = 50f;
        this.food = 75f;
        this.water = 75f;
        this.initialEnergy = this.energy;
    }

    public void update() {
        if (!alive) return;
        
        float[] output = brain.feedForward(pressures);
        
        // Calculate movement
        float movement = Math.abs(output[0]) + Math.abs(output[2]);
        position.x += output[0];
        position.y += 0;
        position.z += output[2];
        this.skin.position = position;
        this.skin.updateModelMatrix();
        
        // Consume energy for movement
        energy -= movement * energyConsumptionPerMove;
        
        // Consume food and water over time
        food -= foodConsumptionRate;
        water -= waterConsumptionRate;
        
        // Convert food and water to energy if available
        if (food > 0 && energy < maxEnergy) {
            float foodToEnergy = Math.min(food, foodConsumptionRate * 2) * foodToEnergyConversion;
            energy = Math.min(maxEnergy, energy + foodToEnergy);
        }
        
        if (water > 0 && energy < maxEnergy) {
            float waterToEnergy = Math.min(water, waterConsumptionRate * 2) * waterToEnergyConversion;
            energy = Math.min(maxEnergy, energy + waterToEnergy);
        }
        
        // Clamp values
        food = Math.max(0, food);
        water = Math.max(0, water);
        energy = Math.max(0, energy);
        
        // Check death conditions
        if (energy <= 0 || (food <= 0 && water <= 0)) {
            alive = false;
        }
        
        // Track survival time
        if (alive) {
            survivalTime += 1f;
        }
    }
    
    public void collectFood(float amount) {
        food = Math.min(maxFood, food + amount);
    }
    
    public void collectWater(float amount) {
        water = Math.min(maxWater, water + amount);
    }
    
    public boolean isAlive() {
        return alive;
    }
    
    public boolean canReproduce() {
        // Can reproduce if survived longer with more energy than initial
        return alive && energy > initialEnergy * 1.5f && survivalTime > 100f;
    }
    
    public Agent reproduce() {
        // Create offspring with similar brain
        Agent offspring = new Agent(this.skin.texture);
        offspring.position = new Vector3(
            position.x + (float)(Math.random() * 4 - 2),
            position.y,
            position.z + (float)(Math.random() * 4 - 2)
        );
        offspring.skin.position = offspring.position;
        offspring.skin.updateModelMatrix();
        
        // Apply mutation to offspring's brain
        offspring.brain = this.brain.copy();
        offspring.brain.mutate(0.1f, 0.05f);
        
        // Reset parent's reproduction capability
        survivalTime = 0f;
        energy -= 30f; // Cost of reproduction
        
        return offspring;
    }
}
