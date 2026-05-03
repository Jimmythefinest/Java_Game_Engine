package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Math.Vector3;

public interface BattleArenaPlayerStatusSource {
    float healthRatio(String playerId);

    Vector3 positionForPlayer(String playerId);
}
