package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Networking.NetworkConnection;
import com.njst.gaming.Networking.NetworkEvent;
import com.njst.gaming.Networking.NetworkEventType;
import com.njst.gaming.Networking.NetworkMessage;
import com.njst.gaming.Networking.TcpNetworkServer;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BattleArenaTcpControlServer {
    private static final int DEFAULT_PORT = 7777;
    private static final String CONTROL_MESSAGE_TYPE = "battle_arena.controls";
    private static final String LOG_PREFIX = "[BattleArenaTCPServer] ";
    private static final Map<NetworkConnection, String> playersByConnection =
            new LinkedHashMap<NetworkConnection, String>();

    private BattleArenaTcpControlServer() {
    }

    public static void main(String[] args) throws Exception {
        int port = readPort(args);
        TcpNetworkServer server = new TcpNetworkServer();
        server.start(port);
        log("listening on tcp port " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                server.close();
            }
        }, "battle-arena-tcp-control-server-shutdown"));

        while (server.isRunning()) {
            for (NetworkEvent event : server.drainEvents()) {
                handleEvent(server, event);
            }
            Thread.sleep(8L);
        }
    }

    private static void handleEvent(TcpNetworkServer server, NetworkEvent event) {
        if (event.getType() == NetworkEventType.CONNECTED) {
            registerPlayer(server, event.getConnection());
            return;
        }
        if (event.getType() == NetworkEventType.DISCONNECTED) {
            unregisterPlayer(server, event.getConnection());
            return;
        }
        if (event.getType() == NetworkEventType.ERROR) {
            log("error " + (event.getError() != null ? event.getError().getMessage() : "unknown"));
            return;
        }
        if (event.getType() != NetworkEventType.MESSAGE || event.getMessage() == null) {
            return;
        }
        if (!CONTROL_MESSAGE_TYPE.equals(event.getMessage().getType())) {
            return;
        }
        relayControlMessage(server, event.getConnection(), event.getMessage());
    }

    private static void registerPlayer(TcpNetworkServer server, NetworkConnection connection) {
        if (connection == null) {
            return;
        }
        String player = "player-" + connection.getId();
        playersByConnection.put(connection, player);
        log("connected " + describe(connection) + " assigned=" + player);
        sendSessionMessage(connection, BattleArenaTcpSessionMessage.assign(player));
        for (String activePlayer : playersByConnection.values()) {
            sendSessionMessage(connection, BattleArenaTcpSessionMessage.spawn(activePlayer));
        }
        broadcastSessionMessage(server, connection, BattleArenaTcpSessionMessage.spawn(player));
    }

    private static void unregisterPlayer(TcpNetworkServer server, NetworkConnection connection) {
        String player = playersByConnection.remove(connection);
        log("disconnected " + describe(connection) + (player != null ? " player=" + player : ""));
        if (player != null) {
            broadcastSessionMessage(server, connection, BattleArenaTcpSessionMessage.despawn(player));
        }
    }

    private static void relayControlMessage(TcpNetworkServer server,
                                            NetworkConnection sender,
                                            NetworkMessage message) {
        for (NetworkConnection connection : server.getConnections()) {
            if (connection == null || sender == connection || !connection.isOpen()) {
                continue;
            }
            try {
                connection.send(message);
            } catch (IOException e) {
                log("relay failed to " + describe(connection) + ": " + e.getMessage());
            }
        }
    }

    private static void broadcastSessionMessage(TcpNetworkServer server,
                                                NetworkConnection excluded,
                                                BattleArenaTcpSessionMessage sessionMessage) {
        NetworkMessage message = encodeSessionMessage(sessionMessage);
        for (NetworkConnection connection : server.getConnections()) {
            if (connection == null || connection == excluded || !connection.isOpen()) {
                continue;
            }
            try {
                connection.send(message);
            } catch (IOException e) {
                log("session relay failed to " + describe(connection) + ": " + e.getMessage());
            }
        }
    }

    private static void sendSessionMessage(NetworkConnection connection, BattleArenaTcpSessionMessage sessionMessage) {
        if (connection == null || !connection.isOpen()) {
            return;
        }
        try {
            connection.send(encodeSessionMessage(sessionMessage));
        } catch (IOException e) {
            log("session send failed to " + describe(connection) + ": " + e.getMessage());
        }
    }

    private static NetworkMessage encodeSessionMessage(BattleArenaTcpSessionMessage sessionMessage) {
        String payload = "{\"event\":\"" + escapeJson(sessionMessage.event)
                + "\",\"player\":\"" + escapeJson(sessionMessage.player) + "\"}";
        return NetworkMessage.text(BattleArenaTcpSessionMessage.MESSAGE_TYPE, payload);
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static int readPort(String[] args) {
        if (args != null && args.length > 0) {
            return parsePort(args[0], DEFAULT_PORT);
        }
        return parsePort(System.getProperty("battleArena.controlPort"), DEFAULT_PORT);
    }

    private static int parsePort(String value, int fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String describe(NetworkConnection connection) {
        return connection == null ? "unknown" : connection.getId() + " " + connection.getRemoteAddress();
    }

    private static void log(String message) {
        System.out.println(LOG_PREFIX + message);
    }
}
