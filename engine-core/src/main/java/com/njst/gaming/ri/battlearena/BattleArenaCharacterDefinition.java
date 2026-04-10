package com.njst.gaming.ri.battlearena;

import java.util.LinkedHashMap;
import java.util.Map;

final class BattleArenaCharacterDefinition {
    ModelDefinition model = new ModelDefinition();
    Map<String, String> animations = new LinkedHashMap<String, String>();
    Map<String, EventDefinition> events = new LinkedHashMap<String, EventDefinition>();
    HitboxSetDefinition hitboxes = new HitboxSetDefinition();

    static final class ModelDefinition {
        String mesh;
        String bones;
        String boneNames;
        String texture;
    }

    static final class EventDefinition {
        String play;
        String then;
        Boolean interrupts;
    }

    static final class HitboxSetDefinition {
        HitboxDefinition body;
        HitboxDefinition punch;
    }

    static final class HitboxDefinition {
        float[] center;
        float[] halfExtents;
        String activeWhen;
    }
}
