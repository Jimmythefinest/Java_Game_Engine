package com.njst.game1;

import com.njst.gaming.objects.GameObject;
import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.*;
import com.njst.gaming.ai.NeuralNetwork;

public class agent {
    GameObject skin;
    NeuralNetwork brain;
    float[] pressures=new float[10];
    Vector3 position;
    
    // Survival system
    float energy;
    float food;
    float water;
    float maxEnergy = 100f;
    float maxFood = 100f;
    float maxWater = 100f;
    float initialEnergy;
    float survivalTime = 0f;
    boolean alive = true;
    
    // Consumption rates
    float energyConsumptionPerMove = 0.1f;
    float foodConsumptionRate = 0.05f;  // per update
    float waterConsumptionRate = 0.08f; // per update
    float foodToEnergyConversion = 0.5f;
    float waterToEnergyConversion = 0.3f;
    
    public agent() {
        this.skin = new GameObject(
                new CubeGeometry(),
                ShaderProgram.loadTexture("/jimmy/desertstorm.jpg"));
        position=new Vector3(0,0,0);
        this.skin.position=position;
        this.brain = new NeuralNetwork(new int[]{10,10,3},0.1f,false);
        
        // Initialize survival stats
        this.energy = 50f;
        this.food = 75f;
        this.water = 75f;
        this.initialEnergy = this.energy;
    }

    public void update() {
        if (!alive) return;
        
        float[] output=brain.feedForward(pressures);
        
        // Calculate movement
        float movement = Math.abs(output[0]) + Math.abs(output[2]);
        position.x+=output[0];
        position.y+=0;
        position.z+=output[2];
        this.skin.position=position;
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
    
    public agent reproduce() {
        // Create offspring with similar brain (can add mutation later)
        agent offspring = new agent();
        offspring.position = new Vector3(
            position.x + (float)(Math.random() * 4 - 2),
            position.y,
            position.z + (float)(Math.random() * 4 - 2)
        );
        offspring.skin.position = offspring.position;
        offspring.skin.updateModelMatrix();
        
        // Reset parent's reproduction capability
        survivalTime = 0f;
        energy -= 30f; // Cost of reproduction
        
        return offspring;
    }
     
}