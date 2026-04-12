package com.njst.gaming.ri.battlearena;

interface BattleArenaCharacterBrain {
    void update(BattleArenaCharacterRuntime self,
                BattleArenaCharacterRuntime opponent,
                BattleArenaCharacterControlState controls,
                float deltaSeconds);
}
