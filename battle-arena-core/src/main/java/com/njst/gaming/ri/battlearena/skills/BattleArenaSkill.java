package com.njst.gaming.ri.battlearena.skills;

import com.njst.gaming.collision.CollisionEvent;
import com.njst.gaming.ri.battlearena.BattleArenaCharacterRuntime;
import com.njst.gaming.ri.battlearena.BattleArenaControlledCharacter;

public interface BattleArenaSkill {
    String id();

    boolean run(BattleArenaControlledCharacter user,
                BattleArenaSkillContext context,
                BattleArenaCharacterRuntime target);

    default void update(BattleArenaSkillContext context, float deltaSeconds) {
    }

    default void onCollision(BattleArenaSkillContext context, CollisionEvent event) {
    }
}
