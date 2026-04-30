package com.njst.gaming.ri.battlearena;

import java.util.LinkedHashMap;
import java.util.Map;

final class BattleArenaCharacterDefinition {
    static final String HITBOX_KIND_HURTBOX = "hurtbox";
    static final String HITBOX_KIND_HITBOX = "hitbox";
    static final float DEFAULT_ANIMATION_FPS = 60f;

    ModelDefinition model = new ModelDefinition();
    Map<String, AnimationDefinition> animations = new LinkedHashMap<String, AnimationDefinition>();
    Map<String, EventDefinition> events = new LinkedHashMap<String, EventDefinition>();
    Map<String, HitboxDefinition> hitboxes = new LinkedHashMap<String, HitboxDefinition>();
    String hitboxTracks;

    static final class ModelDefinition {
        String mesh;
        String bones;
        String boneNames;
        String texture;
    }

    static final class AnimationDefinition {
        String asset;
        Float framesPerSecond;

        String assetPath() {
            return asset;
        }

        float resolvedFramesPerSecond() {
            if (framesPerSecond == null || framesPerSecond.floatValue() <= 0f) {
                return DEFAULT_ANIMATION_FPS;
            }
            return framesPerSecond.floatValue();
        }
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
