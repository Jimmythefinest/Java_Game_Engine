package com.njst.gaming.ri.battlearena.networking;

public final class BattleArenaSimulationNetworkProtocol {
    public static final String INPUT_MESSAGE_TYPE = "battle_arena.sim.input";
    public static final String SNAPSHOT_MESSAGE_TYPE = "battle_arena.sim.snapshot";
    public static final String SESSION_MESSAGE_TYPE = BattleArenaSimulationSessionMessage.MESSAGE_TYPE;

    private BattleArenaSimulationNetworkProtocol() {
    }
}
