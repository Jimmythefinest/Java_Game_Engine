package com.njst.gaming.simulation.brain;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.simulation.entities.GraphNPC;
import java.util.List;

public interface NPCBrain {
    void update(GraphNPC npc, List<Vector3> foodPositions, List<Vector3> waterPositions, List<Vector3> npcPositions, float dt);
    NPCBrain reproduce();
}
