package com.njst.gaming.ri.battlearena;

import java.util.LinkedHashMap;
import java.util.Map;

final class BattleArenaCharacterDefinition {
    static final String HITBOX_KIND_HURTBOX = "hurtbox";
    static final String HITBOX_KIND_HITBOX = "hitbox";

    ModelDefinition model = new ModelDefinition();
    Map<String, String> animations = new LinkedHashMap<String, String>();
    Map<String, EventDefinition> events = new LinkedHashMap<String, EventDefinition>();
    Map<String, HitboxDefinition> hitboxes = new LinkedHashMap<String, HitboxDefinition>();

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

    static final class HitboxDefinition {
        String kind;
        float[] center;
        float[] halfExtents;
        String activeWhen;
        String followsBone;
        String onHitAnimation;
    }
}
