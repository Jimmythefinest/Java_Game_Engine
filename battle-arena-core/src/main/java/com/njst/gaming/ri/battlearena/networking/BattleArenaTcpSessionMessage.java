package com.njst.gaming.ri.battlearena.networking;

public final class BattleArenaTcpSessionMessage {
    public static final String MESSAGE_TYPE = "battle_arena.session";
    public static final String EVENT_ASSIGN = "assign";
    public static final String EVENT_SPAWN = "spawn";
    public static final String EVENT_DESPAWN = "despawn";

    public String event;
    public String player;

    public static BattleArenaTcpSessionMessage assign(String player) {
        return create(EVENT_ASSIGN, player);
    }

    public static BattleArenaTcpSessionMessage spawn(String player) {
        return create(EVENT_SPAWN, player);
    }

    public static BattleArenaTcpSessionMessage despawn(String player) {
        return create(EVENT_DESPAWN, player);
    }

    private static BattleArenaTcpSessionMessage create(String event, String player) {
        BattleArenaTcpSessionMessage message = new BattleArenaTcpSessionMessage();
        message.event = event;
        message.player = player;
        return message;
    }
}
