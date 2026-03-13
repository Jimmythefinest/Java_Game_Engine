package com.njst.gaming.simulation.entities;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.simulation.brain.NPCBrain;
import com.njst.gaming.simulation.brain.SurvivalBrain;
import java.util.List;
import java.util.Random;

public class GraphNPC {

    public enum State {
        IDLE,
        WANDER,
        SEEK_FOOD,
        SEEK_WATER,
        GO_TO_BOX
    }

    // ===== Tunables =====
    private static final float MAX_SPEED = 6.0f;
    private static final float WANDER_RADIUS = 25.0f;
    
    public static float HUNGER_RATE = 0.08f;
    public static float THIRST_RATE = 0.12f;

    // ===== State =====
    public final GameObject skin;
    public final Vector3 position;
    private final Vector3 velocity = new Vector3();
    
    private State currentState = State.IDLE;
    public final Vector3 personalTarget = new Vector3();
    private Random rnd = new Random();

    // Survival Meters
    public float hunger = 0.0f; 
    public float thirst = 0.0f; 
    public float lifetime = 0.0f;
    public boolean isAlive = true;

    // Modular Brain
    private NPCBrain brain;

    // ===== Constructor =====
    public GraphNPC(GameObject skin) {
        this(skin, new SurvivalBrain());
    }

    public GraphNPC(GameObject skin, NPCBrain brain) {
        this.skin = skin;
        this.position = skin.position;
        this.personalTarget.set(position);
        this.brain = brain;
    }

    // ===== Graph Logic =====
    public void update(List<Vector3> foodPositions, List<Vector3> waterPositions, List<Vector3> npcPositions, float dt) {
        if (!isAlive) return;

        lifetime += dt;
        
        // Update meters
        hunger = Math.min(1.0f, hunger + HUNGER_RATE * dt);
        thirst = Math.min(1.0f, thirst + THIRST_RATE * dt);

        // Check for death
        if (hunger >= 1.0f || thirst >= 1.0f) {
            isAlive = false;
            // System.out.println("NPC Died at age: " + lifetime);
            return;
        }

        // Delegate brain logic
        brain.update(this, foodPositions, waterPositions, npcPositions, dt);

        moveToTarget(dt);
        skin.updateModelMatrix();
    }

    public void transitionTo(State newState) {
        currentState = newState;
        if (newState == State.WANDER) {
            setNewWanderTarget();
        } else if (newState == State.IDLE) {
            personalTarget.set(position);
        }
    }

    public void setNewWanderTarget() {
        personalTarget.set(
            position.x + (rnd.nextFloat() * 2 - 1) * WANDER_RADIUS,
            position.y,
            position.z + (rnd.nextFloat() * 2 - 1) * WANDER_RADIUS
        );
    }

    private void moveToTarget(float dt) {
        Vector3 toTarget = personalTarget.clone().sub(position);
        float distance = toTarget.length();

        if (distance > 0.1f) {
            toTarget.mul(1.0f / distance);
            velocity.x = toTarget.x * MAX_SPEED;
            velocity.z = toTarget.z * MAX_SPEED;
        } else {
            velocity.set(0, 0, 0);
        }

        position.x += velocity.x * dt;
        position.z += velocity.z * dt;
    }

    public GraphNPC reproduce(GameObject newSkin) {
        return new GraphNPC(newSkin, brain.reproduce());
    }

    // Getters for Brain
    public State getCurrentState() { return currentState; }
    public Vector3 getPersonalTarget() { return personalTarget; }
    public NPCBrain getBrain() { return brain; }
}
