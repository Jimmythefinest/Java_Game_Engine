package com.njst.gaming.ri.battlearena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BattleArenaSimulationSnapshot {
    public final int tick;
    public final float tickSeconds;
    public final List<BattleArenaPlayerState> players;

    public BattleArenaSimulationSnapshot(int tick,
                                         float tickSeconds,
                                         List<BattleArenaPlayerState> players) {
        this.tick = tick;
        this.tickSeconds = tickSeconds;
        this.players = players != null
                ? Collections.unmodifiableList(new ArrayList<BattleArenaPlayerState>(players))
                : Collections.<BattleArenaPlayerState>emptyList();
    }

    public BattleArenaPlayerState stateForPlayer(String playerId) {
        if (playerId == null) {
            return null;
        }
        for (BattleArenaPlayerState player : players) {
            if (player != null && playerId.equals(player.playerId)) {
                return player;
            }
        }
        return null;
    }
}
