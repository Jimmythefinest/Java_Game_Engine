package com.njst.gaming.ri.battlearena.networking;

public final class BattleArenaSimulationSessionMessage {
    public static final String MESSAGE_TYPE = "battle_arena.sim.session";
    public static final String EVENT_ASSIGN = "assign";

    public String event;
    public String player;

    public static BattleArenaSimulationSessionMessage assign(String player) {
        BattleArenaSimulationSessionMessage message = new BattleArenaSimulationSessionMessage();
        message.event = EVENT_ASSIGN;
        message.player = player;
        return message;
    }
}
