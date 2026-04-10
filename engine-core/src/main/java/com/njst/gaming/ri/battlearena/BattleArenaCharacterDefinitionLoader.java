package com.njst.gaming.ri.battlearena;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
        if (definition == null || definition.animations == null) {
            return null;
        }
        return definition.animations.get(animationKey);
    }

    private void require(String value, String fieldName, String definitionFile) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing " + fieldName + " in character definition: " + definitionFile);
        }
    }
}
