package com.njst.gaming.ri.battlearena;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.njst.gaming.graphics.GraphicsDevice;

final class BattleArenaCharacterDefinitionLoader {
    private static final Gson GSON = new GsonBuilder().create();

    BattleArenaCharacterDefinition load(GraphicsDevice graphicsDevice, String definitionFile) {
        String json = graphicsDevice.loadTextResource(definitionFile);
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalStateException("Character definition not found or empty: " + definitionFile);
        }

        BattleArenaCharacterDefinition definition = GSON.fromJson(json, BattleArenaCharacterDefinition.class);
        if (definition == null) {
            throw new IllegalStateException("Unable to parse character definition: " + definitionFile);
        }
        populateAnimationDefinitions(definition, json, definitionFile);
        validate(definition, definitionFile);
        return definition;
    }

    private void validate(BattleArenaCharacterDefinition definition, String definitionFile) {
        if (definition.model == null) {
            throw new IllegalStateException("Missing model block in character definition: " + definitionFile);
        }
        require(definition.model.mesh, "model.mesh", definitionFile);
        require(definition.model.bones, "model.bones", definitionFile);
        require(definition.model.boneNames, "model.boneNames", definitionFile);
        require(definition.model.texture, "model.texture", definitionFile);

        require(animation(definition, BattleArenaCharacterController.ANIM_IDLE), "animations.idle", definitionFile);
        require(animation(definition, BattleArenaCharacterController.ANIM_WALK), "animations.walk", definitionFile);
        require(animation(definition, BattleArenaCharacterController.ANIM_WALK_BACKWARD), "animations.walk_backward", definitionFile);
        require(animation(definition, BattleArenaCharacterController.ANIM_RUN), "animations.run", definitionFile);
        if (definition.hitboxes == null || definition.hitboxes.isEmpty()) {
            throw new IllegalStateException("Missing hitboxes in character definition: " + definitionFile);
        }
    }

    String animation(BattleArenaCharacterDefinition definition, String animationKey) {
        BattleArenaCharacterDefinition.AnimationDefinition animation = animationDefinition(definition, animationKey);
        return animation != null ? animation.assetPath() : null;
    }

    BattleArenaCharacterDefinition.AnimationDefinition animationDefinition(BattleArenaCharacterDefinition definition,
                                                                           String animationKey) {
        if (definition == null || definition.animations == null) {
            return null;
        }
        return definition.animations.get(animationKey);
    }

    private void populateAnimationDefinitions(BattleArenaCharacterDefinition definition,
                                              String json,
                                              String definitionFile) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject animationsObject = root.getAsJsonObject("animations");
        if (animationsObject == null) {
            definition.animations = new java.util.LinkedHashMap<String, BattleArenaCharacterDefinition.AnimationDefinition>();
            return;
        }

        java.util.LinkedHashMap<String, BattleArenaCharacterDefinition.AnimationDefinition> animations =
                new java.util.LinkedHashMap<String, BattleArenaCharacterDefinition.AnimationDefinition>();
        for (java.util.Map.Entry<String, JsonElement> entry : animationsObject.entrySet()) {
            JsonElement value = entry.getValue();
            BattleArenaCharacterDefinition.AnimationDefinition animation =
                    parseAnimationDefinition(value, entry.getKey(), definitionFile);
            animations.put(entry.getKey(), animation);
        }
        definition.animations = animations;
    }

    private BattleArenaCharacterDefinition.AnimationDefinition parseAnimationDefinition(JsonElement value,
                                                                                        String animationKey,
                                                                                        String definitionFile) {
        BattleArenaCharacterDefinition.AnimationDefinition animation =
                new BattleArenaCharacterDefinition.AnimationDefinition();
        if (value == null || value.isJsonNull()) {
            return animation;
        }
        if (value.isJsonPrimitive()) {
            animation.asset = value.getAsString();
            animation.framesPerSecond = BattleArenaCharacterDefinition.DEFAULT_ANIMATION_FPS;
            return animation;
        }
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            JsonElement asset = object.get("asset");
            if (asset != null && !asset.isJsonNull()) {
                animation.asset = asset.getAsString();
            }
            JsonElement framesPerSecond = object.get("framesPerSecond");
            if (framesPerSecond != null && !framesPerSecond.isJsonNull()) {
                animation.framesPerSecond = framesPerSecond.getAsFloat();
            }
            return animation;
        }
        throw new IllegalStateException("Unsupported animation definition for animations."
                + animationKey + " in " + definitionFile);
    }

    private void require(String value, String fieldName, String definitionFile) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing " + fieldName + " in character definition: " + definitionFile);
        }
    }
}
