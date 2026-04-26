package com.njst.gaming.ri.battlearena;

final class BattleArenaTcpSessionMessage {
    static final String MESSAGE_TYPE = "battle_arena.session";
    static final String EVENT_ASSIGN = "assign";
    static final String EVENT_SPAWN = "spawn";
    static final String EVENT_DESPAWN = "despawn";

    String event;
    String player;

    static BattleArenaTcpSessionMessage assign(String player) {
        return create(EVENT_ASSIGN, player);
    }

    static BattleArenaTcpSessionMessage spawn(String player) {
        return create(EVENT_SPAWN, player);
    }

    static BattleArenaTcpSessionMessage despawn(String player) {
        return create(EVENT_DESPAWN, player);
    }

    private static BattleArenaTcpSessionMessage create(String event, String player) {
        BattleArenaTcpSessionMessage message = new BattleArenaTcpSessionMessage();
        message.event = event;
        message.player = player;
        return message;
    }
}
