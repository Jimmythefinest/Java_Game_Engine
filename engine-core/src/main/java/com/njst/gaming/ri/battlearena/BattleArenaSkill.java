package com.njst.gaming.ri.battlearena;

import com.njst.gaming.collision.CollisionEvent;

interface BattleArenaSkill {
    String id();

    boolean run(BattleArenaControlledCharacter user,
                BattleArenaSkillContext context,
                BattleArenaCharacterRuntime target);

    default void update(BattleArenaSkillContext context, float deltaSeconds) {
    }

    default void onCollision(BattleArenaSkillContext context, CollisionEvent event) {
    }
}
