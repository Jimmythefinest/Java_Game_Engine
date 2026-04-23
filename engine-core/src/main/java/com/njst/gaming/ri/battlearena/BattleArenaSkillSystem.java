package com.njst.gaming.ri.battlearena;

import com.njst.gaming.collision.CollisionEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class BattleArenaSkillSystem {
    private final BattleArenaFireballSkill fireballSkill = new BattleArenaFireballSkill();
    private final BattleArenaMudWallSkill mudWallSkill = new BattleArenaMudWallSkill();
    private final List<BattleArenaSkill> skills;

    BattleArenaSkillSystem() {
        ArrayList<BattleArenaSkill> allSkills = new ArrayList<>();
        allSkills.add(fireballSkill);
        allSkills.add(mudWallSkill);
        this.skills = Collections.unmodifiableList(allSkills);
    }

    List<BattleArenaSkill> skills() {
        return skills;
    }

    void update(BattleArenaSkillContext context, float deltaSeconds) {
        fireballSkill.update(context, deltaSeconds, mudWallSkill);
        mudWallSkill.update(context, deltaSeconds);
    }

    void onCollision(BattleArenaSkillContext context, CollisionEvent event) {
        mudWallSkill.onCollision(context, event);
    }

    boolean hasIncomingFireballThreat(BattleArenaCharacterRuntime self, BattleArenaCharacterRuntime opponent) {
        return fireballSkill.hasIncomingThreat(self, opponent, mudWallSkill);
    }

    void setDebugVisible(boolean debugVisible) {
        mudWallSkill.setDebugVisible(debugVisible);
    }
}
