package com.njst.gaming.ri.battlearena.controls;

import com.njst.gaming.ri.battlearena.BattleArenaCharacterRuntime;

public interface BattleArenaCharacterBrain {
    void update(BattleArenaCharacterRuntime self,
                BattleArenaCharacterRuntime opponent,
                BattleArenaCharacterControlState controls,
                float deltaSeconds);
}
