package com.njst.gaming.simulation.brain;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.simulation.entities.GraphNPC;
import java.util.List;

public class RuleBasedBrain implements NPCBrain {

    private static final float ARRIVE_DISTANCE = 1.0f;
    private static final float THIRST_THRESHOLD = 0.7f;
    private static final float HUNGER_THRESHOLD = 0.5f;

    @Override
    public void update(GraphNPC npc, List<Vector3> foodPositions, List<Vector3> waterPositions, List<Vector3> npcPositions, float dt) {
        
        // Priority 1: Thirst
        if (npc.thirst > THIRST_THRESHOLD) {
            npc.transitionTo(GraphNPC.State.SEEK_WATER);
            Vector3 nearestWater = findNearest(npc.position, waterPositions);
            if (nearestWater != null) npc.getPersonalTarget().set(nearestWater);
        } 
        // Priority 2: Hunger
        else if (npc.hunger > HUNGER_THRESHOLD) {
            npc.transitionTo(GraphNPC.State.SEEK_FOOD);
            Vector3 nearestFood = findNearest(npc.position, foodPositions);
            if (nearestFood != null) npc.getPersonalTarget().set(nearestFood);
        }
        // Priority 3: Exploration / Idle
        else if (npc.getCurrentState() == GraphNPC.State.IDLE || 
                 npc.position.distance(npc.getPersonalTarget()) < ARRIVE_DISTANCE) {
            npc.transitionTo(GraphNPC.State.WANDER);
            npc.setNewWanderTarget();
        }
    }

    private Vector3 findNearest(Vector3 npcPos, List<Vector3> positions) {
        Vector3 nearest = null;
        float minDist = Float.MAX_VALUE;
        for (Vector3 pos : positions) {
            float d = npcPos.distance(pos);
            if (d < minDist) {
                minDist = d;
                nearest = pos;
            }
        }
        return nearest;
    }

    @Override
    public NPCBrain reproduce() {
        // Rules are static, so just return a new instance
        return new RuleBasedBrain();
    }
}
