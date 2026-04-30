package com.njst.gaming.ri.battlearena.skills;

import com.njst.gaming.collision.CollisionEvent;
import com.njst.gaming.ri.battlearena.BattleArenaCharacterRuntime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BattleArenaSkillSystem {
    private final BattleArenaFireballSkill fireballSkill = new BattleArenaFireballSkill();
    private final BattleArenaMudWallSkill mudWallSkill = new BattleArenaMudWallSkill();
    private final List<BattleArenaSkill> skills;

    public BattleArenaSkillSystem() {
        ArrayList<BattleArenaSkill> allSkills = new ArrayList<>();
        allSkills.add(fireballSkill);
        allSkills.add(mudWallSkill);
        this.skills = Collections.unmodifiableList(allSkills);
    }

    public List<BattleArenaSkill> skills() {
        return skills;
    }

    public void update(BattleArenaSkillContext context, float deltaSeconds) {
        fireballSkill.update(context, deltaSeconds, mudWallSkill);
        mudWallSkill.update(context, deltaSeconds);
    }

    public void onCollision(BattleArenaSkillContext context, CollisionEvent event) {
        mudWallSkill.onCollision(context, event);
    }

    public boolean hasIncomingFireballThreat(BattleArenaCharacterRuntime self, BattleArenaCharacterRuntime opponent) {
        return fireballSkill.hasIncomingThreat(self, opponent, mudWallSkill);
    }

    public void setDebugVisible(boolean debugVisible) {
        mudWallSkill.setDebugVisible(debugVisible);
    }
}
