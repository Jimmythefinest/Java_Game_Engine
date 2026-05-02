package com.njst.gaming.ri.battlearena;

import java.util.List;

public interface BattleArenaPlayerStateProvider {
    List<BattleArenaPlayerState> initialStates();

    int currentTick();

    float tickSeconds();

    void submitInput(String playerId, int tick, BattleArenaPlayerInput input);

    void tick();

    List<BattleArenaPlayerState> snapshotStates();

    BattleArenaPlayerState stateForPlayer(String playerId);
}
