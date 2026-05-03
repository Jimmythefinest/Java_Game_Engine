package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Math.Vector3;

public final class BattleArenaSnapshotPlayerStatusSource implements BattleArenaPlayerStatusSource {
    private BattleArenaSimulationSnapshot snapshot;

    public void update(BattleArenaSimulationSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public float healthRatio(String playerId) {
        BattleArenaPlayerState state = stateForPlayer(playerId);
        return state != null ? state.healthRatio() : 0f;
    }

    @Override
    public Vector3 positionForPlayer(String playerId) {
        BattleArenaPlayerState state = stateForPlayer(playerId);
        if (state == null) {
            return new Vector3();
        }
        return new Vector3(state.x, state.y, state.z);
    }

    private BattleArenaPlayerState stateForPlayer(String playerId) {
        return snapshot != null ? snapshot.stateForPlayer(playerId) : null;
    }
}
