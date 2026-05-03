package com.njst.gaming.ri.battlearena.networking;

import com.njst.gaming.ri.battlearena.BattleArenaPlayerState;
import com.njst.gaming.ri.battlearena.BattleArenaSimulationSnapshot;
import com.njst.gaming.ri.battlearena.BattleArenaGuObjectState;

import java.util.ArrayList;
import java.util.List;

public final class BattleArenaNetworkSnapshotMessage {
    public int tick;
    public float tickSeconds;
    public List<BattleArenaNetworkPlayerStateMessage> players =
            new ArrayList<BattleArenaNetworkPlayerStateMessage>();
    public List<BattleArenaNetworkGuObjectMessage> guObjects =
            new ArrayList<BattleArenaNetworkGuObjectMessage>();

    public static BattleArenaNetworkSnapshotMessage fromSnapshot(BattleArenaSimulationSnapshot snapshot) {
        BattleArenaNetworkSnapshotMessage message = new BattleArenaNetworkSnapshotMessage();
        if (snapshot == null) {
            return message;
        }
        message.tick = snapshot.tick;
        message.tickSeconds = snapshot.tickSeconds;
        for (BattleArenaPlayerState player : snapshot.players) {
            message.players.add(BattleArenaNetworkPlayerStateMessage.fromState(player));
        }
        for (BattleArenaGuObjectState guObject : snapshot.guObjects) {
            message.guObjects.add(BattleArenaNetworkGuObjectMessage.fromState(guObject));
        }
        return message;
    }

    public BattleArenaSimulationSnapshot toSnapshot() {
        ArrayList<BattleArenaPlayerState> states = new ArrayList<BattleArenaPlayerState>();
        if (players != null) {
            for (BattleArenaNetworkPlayerStateMessage player : players) {
                if (player != null) {
                    states.add(player.toState());
                }
            }
        }
        ArrayList<BattleArenaGuObjectState> guObjectStates = new ArrayList<BattleArenaGuObjectState>();
        if (guObjects != null) {
            for (BattleArenaNetworkGuObjectMessage guObject : guObjects) {
                if (guObject != null) {
                    guObjectStates.add(guObject.toState());
                }
            }
        }
        return new BattleArenaSimulationSnapshot(tick, tickSeconds, states, guObjectStates);
    }
}
