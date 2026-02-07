package com.njst.gaming.simulation.entities;

import com.njst.gaming.objects.GameObject;
import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Math.Vector3;

public class Resource {
    public GameObject visual;
    public Vector3 position;
    private String type; // "food" or "water"
    private float amount;
    private boolean collected = false;
    
    public Resource(String type, Vector3 position, int texture) {
        this.type = type;
        this.position = position;
        this.amount = type.equals("food") ? 40f : 50f;
        
        this.visual = new GameObject(
            new CubeGeometry(),
            texture
        );
        this.visual.position = position;
        this.visual.scale = new float[]{0.3f, 0.3f, 0.3f}; // Smaller cubes for resources
        this.visual.updateModelMatrix();
    }
    
    public boolean isNear(Vector3 agentPos, float radius) {
        float dx = position.x - agentPos.x;
        float dy = position.y - agentPos.y;
        float dz = position.z - agentPos.z;
        float distSquared = dx*dx + dy*dy + dz*dz;
        return distSquared < radius * radius;
    }
    
    public void collect() {
        collected = true;
    }
    
    public boolean isCollected() {
        return collected;
    }
    
    public String getType() {
        return type;
    }
    
    public float getAmount() {
        return amount;
    }
}
