package com.njst.gaming.ri.battlearena;

import com.njst.gaming.input.ActionInput;
import com.njst.gaming.input.PointerState;

import java.util.ArrayList;
import java.util.List;

final class BattleArenaControlledCharacter {
    private static final float DEFAULT_MAX_HEALTH = 100f;

    final BattleArenaCharacterRuntime runtime;
    final BattleArenaCharacterController controller;
    final BattleArenaCharacterControlState controls = new BattleArenaCharacterControlState();
    final BattleArenaCharacterBrain brain;
    final boolean playerControlled;
    final List<BattleArenaSkill> skills = new ArrayList<>();
    boolean castLatched;
    private final float maxHealth;
    private float currentHealth;

    BattleArenaControlledCharacter(BattleArenaCharacterRuntime runtime,
                                   BattleArenaCharacterController controller,
                                   List<BattleArenaSkill> skills,
                                   BattleArenaCharacterBrain brain,
                                   boolean playerControlled) {
        this.runtime = runtime;
        this.controller = controller;
        this.brain = brain;
        this.playerControlled = playerControlled;
        this.castLatched = false;
        this.maxHealth = DEFAULT_MAX_HEALTH;
        this.currentHealth = DEFAULT_MAX_HEALTH;
        this.runtime.setCharacter(this);
        if (skills != null) {
            this.skills.addAll(skills);
        }
    }

    void reset() {
        controller.reset();
        controls.clear();
        castLatched = false;
        currentHealth = maxHealth;
    }

    void captureControls(ActionInput actions,
                         PointerState movementPointer,
                         BattleArenaCharacterRuntime opponent,
                         float deltaSeconds) {
        if (playerControlled) {
            controls.capturePlayerInput(actions, movementPointer);
            return;
        }
        controls.clear();
        if (brain != null) {
            brain.update(runtime, opponent, controls, deltaSeconds);
        }
    }

    void updateController(float sceneSpeed) {
        controller.update(controls, sceneSpeed);
    }

    float getCurrentHealth() {
        return currentHealth;
    }

    float getMaxHealth() {
        return maxHealth;
    }

    float getHealthRatio() {
        if (maxHealth <= 0f) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, currentHealth / maxHealth));
    }

    void applyDamage(float damage) {
        if (damage <= 0f) {
            return;
        }
        currentHealth = Math.max(0f, currentHealth - damage);
    }

    boolean runSkill(String skillId,
                     BattleArenaSkillContext context,
                     BattleArenaCharacterRuntime target) {
        if (skillId == null || skillId.trim().isEmpty()) {
            return false;
        }
        for (BattleArenaSkill skill : skills) {
            if (skill != null && skillId.equals(skill.id())) {
                return skill.run(this, context, target);
            }
        }
        return false;
    }
}
