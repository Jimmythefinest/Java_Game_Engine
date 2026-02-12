package com.njst.gaming.simulation.entities;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.objects.GameObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GraphNPC {

    public enum State {
        IDLE,
        WANDER,
        SEEK_FOOD,
        SEEK_WATER
    }

    // ===== Tunables =====
    private static final float MAX_SPEED = 5.0f;
    private static final float WANDER_RADIUS = 20.0f;
    private static final float SENSE_DISTANCE = 30.0f; // Increased for better survival
    private static final float ARRIVE_DISTANCE = 1.0f;
    
    private static final float HUNGER_RATE = 0.05f; // per second
    private static final float THIRST_RATE = 0.08f; // per second
    private static final float CRITICAL_THRESHOLD = 0.7f;

    // ===== State =====
    public final GameObject skin;
    public final Vector3 position;
    private final Vector3 velocity = new Vector3();
    
    private State currentState = State.IDLE;
    private float stateTimer = 0;
    public final Vector3 personalTarget = new Vector3();
    private Random rnd = new Random();

    // Survival Meters
    public float hunger = 0.0f; // 0 = full, 1 = starving
    public float thirst = 0.0f; // 0 = hydrated, 1 = parched

    // ===== Constructor =====
    public GraphNPC(GameObject skin) {
        this.skin = skin;
        this.position = skin.position;
        this.personalTarget.set(position);
    }

    // ===== Graph Logic (FSM) =====
    public void update(List<Vector3> foodPositions, List<Vector3> waterPositions, float dt) {
        stateTimer += dt;
        
        // Update meters
        hunger = Math.min(1.0f, hunger + HUNGER_RATE * dt);
        thirst = Math.min(1.0f, thirst + THIRST_RATE * dt);

        switch (currentState) {
            case IDLE:
                updateIdle(foodPositions, waterPositions);
                break;
            case WANDER:
                updateWander(foodPositions, waterPositions);
                break;
            case SEEK_FOOD:
                updateSeekFood(foodPositions, waterPositions);
                break;
            case SEEK_WATER:
                updateSeekWater(foodPositions, waterPositions);
                break;
        }

        moveToTarget(dt);
        skin.updateModelMatrix();
    }

    private void updateIdle(List<Vector3> foodPositions, List<Vector3> waterPositions) {
        // Edge: CRITICAL_NEED -> Transition to SEEK_FOOD or SEEK_WATER
        if (checkSurvivalNeeds(foodPositions, waterPositions)) return;

        // Edge: FINISH_WAITING -> Transition to WANDER
        if (stateTimer > 2.0f) {
            Vector3 wanderTarget = new Vector3(
                position.x + (rnd.nextFloat() * 2 - 1) * WANDER_RADIUS,
                position.y,
                position.z + (rnd.nextFloat() * 2 - 1) * WANDER_RADIUS
            );
            transitionTo(State.WANDER, wanderTarget);
        }
    }

    private void updateWander(List<Vector3> foodPositions, List<Vector3> waterPositions) {
        // Edge: CRITICAL_NEED -> Transition to SEEK_FOOD or SEEK_WATER
        if (checkSurvivalNeeds(foodPositions, waterPositions)) return;

        // Edge: ARRIVE_AT_TARGET or TIMEOUT -> Transition to IDLE
        if (position.distance(personalTarget) < ARRIVE_DISTANCE || stateTimer > 5.0f) {
            transitionTo(State.IDLE, position.clone());
        }
    }

    private void updateSeekFood(List<Vector3> foodPositions, List<Vector3> waterPositions) {
        // High priority: Check if thirsty is more critical now
        if (thirst > CRITICAL_THRESHOLD && thirst > hunger + 0.1f) {
            if (checkSurvivalNeeds(foodPositions, waterPositions)) return;
        }

        Vector3 nearestFood = findNearest(foodPositions);
        
        // Edge: FOOD_GONE -> Transition to IDLE
        if (nearestFood == null) {
            transitionTo(State.IDLE, position.clone());
            return;
        }

        personalTarget.set(nearestFood);

        // Edge: ARRIVE_AT_FOOD -> Transition to IDLE (Eat - hunger reset in loader)
        if (position.distance(personalTarget) < ARRIVE_DISTANCE) {
            transitionTo(State.IDLE, position.clone());
        }
    }

    private void updateSeekWater(List<Vector3> foodPositions, List<Vector3> waterPositions) {
        // High priority: Check if hunger is more critical now
        if (hunger > CRITICAL_THRESHOLD && hunger > thirst + 0.1f) {
            if (checkSurvivalNeeds(foodPositions, waterPositions)) return;
        }

        Vector3 nearestWater = findNearest(waterPositions);
        
        // Edge: WATER_GONE -> Transition to IDLE
        if (nearestWater == null) {
            transitionTo(State.IDLE, position.clone());
            return;
        }

        personalTarget.set(nearestWater);

        // Edge: ARRIVE_AT_WATER -> Transition to IDLE (Drink - thirst reset in loader)
        if (position.distance(personalTarget) < ARRIVE_DISTANCE) {
            transitionTo(State.IDLE, position.clone());
        }
    }

    private boolean checkSurvivalNeeds(List<Vector3> foodPositions, List<Vector3> waterPositions) {
        if (thirst > CRITICAL_THRESHOLD && thirst >= hunger) {
            Vector3 nearestWater = findNearest(waterPositions);
            if (nearestWater != null && position.distance(nearestWater) < SENSE_DISTANCE) {
                transitionTo(State.SEEK_WATER, nearestWater);
                return true;
            }
        }
        
        if (hunger > CRITICAL_THRESHOLD) {
            Vector3 nearestFood = findNearest(foodPositions);
            if (nearestFood != null && position.distance(nearestFood) < SENSE_DISTANCE) {
                transitionTo(State.SEEK_FOOD, nearestFood);
                return true;
            }
        }
        return false;
    }

    private void transitionTo(State newState, Vector3 target) {
        currentState = newState;
        stateTimer = 0;
        personalTarget.set(target);
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

    private Vector3 findNearest(List<Vector3> positions) {
        Vector3 nearest = null;
        float minDist = Float.MAX_VALUE;
        for (Vector3 pos : positions) {
            float d = position.distance(pos);
            if (d < minDist) {
                minDist = d;
                nearest = pos;
            }
        }
        return nearest;
    }

    public State getCurrentState() {
        return currentState;
    }
}
