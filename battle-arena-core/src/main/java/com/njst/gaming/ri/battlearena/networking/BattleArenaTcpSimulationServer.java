package com.njst.gaming.ri.battlearena.networking;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.njst.gaming.Networking.NetworkConnection;
import com.njst.gaming.Networking.NetworkEvent;
import com.njst.gaming.Networking.NetworkEventType;
import com.njst.gaming.Networking.NetworkMessage;
import com.njst.gaming.Networking.TcpNetworkServer;
import com.njst.gaming.ri.battlearena.BattleArenaAnimationTimingLoader;
import com.njst.gaming.ri.battlearena.BattleArenaChaseNpcController;
import com.njst.gaming.ri.battlearena.BattleArenaLocalPlayerStateServer;
import com.njst.gaming.ri.battlearena.BattleArenaSimulationServer;
import com.njst.gaming.ri.battlearena.BattleArenaSimulationSnapshot;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BattleArenaTcpSimulationServer {
    public static final int DEFAULT_PORT = 7788;

    private static final String LOG_PREFIX = "[BattleArenaSimServer] ";
    private static final long TICK_NANOS = 1000000000L / BattleArenaLocalPlayerStateServer.TICK_RATE;
    private static final float[][] DEFAULT_SPAWNS = new float[][] {
            {-1.6f, 0f, 0f},
            {0f, 0f, 0f},
            {1.6f, 0f, 0f},
            {3.2f, 0f, 0f}
    };
    private static final String[] ASSIGNABLE_PLAYERS = {
            "player_0",
            "player_2",
            "player_3"
    };

    private final Gson gson = new Gson();
    private final TcpNetworkServer networkServer = new TcpNetworkServer();
    private final BattleArenaSimulationServer simulationServer;
    private final Map<NetworkConnection, String> playersByConnection =
            new LinkedHashMap<NetworkConnection, String>();
    private int nextPlayerIndex;

    public BattleArenaTcpSimulationServer() {
        this.simulationServer = new BattleArenaSimulationServer(
                DEFAULT_SPAWNS,
                BattleArenaAnimationTimingLoader.loadDefault(BattleArenaLocalPlayerStateServer.TICK_SECONDS));
        this.simulationServer.setNpcController("player_1", new BattleArenaChaseNpcController("player_0"));
    }

    public void start(int port) throws IOException {
        networkServer.start(port);
        log("listening on tcp port " + port);
    }

    public boolean isRunning() {
        return networkServer.isRunning();
    }

    public void close() {
        networkServer.close();
    }

    public void runLoop() throws InterruptedException {
        long nextTickNanos = System.nanoTime();
        while (isRunning()) {
            drainNetworkEvents();
            long now = System.nanoTime();
            if (now >= nextTickNanos) {
                simulationServer.tick();
                broadcastSnapshot(simulationServer.snapshot());
                nextTickNanos += TICK_NANOS;
                if (now - nextTickNanos > TICK_NANOS) {
                    nextTickNanos = now + TICK_NANOS;
                }
                continue;
            }
            long sleepNanos = Math.max(100000L, nextTickNanos - now);
            Thread.sleep(Math.min(2L, Math.max(1L, sleepNanos / 1000000L)));
        }
    }

    private void drainNetworkEvents() {
        for (NetworkEvent event : networkServer.drainEvents()) {
            if (event.getType() == NetworkEventType.CONNECTED) {
                assignPlayer(event.getConnection());
            } else if (event.getType() == NetworkEventType.DISCONNECTED) {
                unassignPlayer(event.getConnection());
            } else if (event.getType() == NetworkEventType.ERROR) {
                log("network error: " + (event.getError() != null ? event.getError().getMessage() : "unknown"));
            } else if (event.getType() == NetworkEventType.MESSAGE) {
                applyMessage(event.getConnection(), event.getMessage());
            }
        }
    }

    private void assignPlayer(NetworkConnection connection) {
        if (connection == null) {
            return;
        }
        String player = ASSIGNABLE_PLAYERS[nextPlayerIndex++ % ASSIGNABLE_PLAYERS.length];
        playersByConnection.put(connection, player);
        log("connected " + describe(connection) + " assigned=" + player);
        send(connection, BattleArenaSimulationNetworkProtocol.SESSION_MESSAGE_TYPE,
                gson.toJson(BattleArenaSimulationSessionMessage.assign(player)));
        sendSnapshot(connection, simulationServer.snapshot());
    }

    private void unassignPlayer(NetworkConnection connection) {
        String player = playersByConnection.remove(connection);
        log("disconnected " + describe(connection) + (player != null ? " player=" + player : ""));
    }

    private void applyMessage(NetworkConnection connection, NetworkMessage message) {
        if (connection == null || message == null
                || !BattleArenaSimulationNetworkProtocol.INPUT_MESSAGE_TYPE.equals(message.getType())) {
            return;
        }
        String assignedPlayer = playersByConnection.get(connection);
        if (assignedPlayer == null) {
            return;
        }
        try {
            BattleArenaNetworkInputMessage inputMessage =
                    gson.fromJson(message.getPayloadAsText(), BattleArenaNetworkInputMessage.class);
            if (inputMessage == null) {
                return;
            }
            int tick = Math.max(simulationServer.currentTick() + 1, inputMessage.tick);
            simulationServer.submitInput(assignedPlayer, tick, inputMessage.toInput());
        } catch (JsonSyntaxException ignored) {
        }
    }

    private void broadcastSnapshot(BattleArenaSimulationSnapshot snapshot) {
        NetworkMessage message = encodeSnapshot(snapshot);
        for (NetworkConnection connection : networkServer.getConnections()) {
            if (connection != null && connection.isOpen()) {
                send(connection, message);
            }
        }
    }

    private void sendSnapshot(NetworkConnection connection, BattleArenaSimulationSnapshot snapshot) {
        send(connection, encodeSnapshot(snapshot));
    }

    private NetworkMessage encodeSnapshot(BattleArenaSimulationSnapshot snapshot) {
        return NetworkMessage.text(
                BattleArenaSimulationNetworkProtocol.SNAPSHOT_MESSAGE_TYPE,
                gson.toJson(BattleArenaNetworkSnapshotMessage.fromSnapshot(snapshot)));
    }

    private void send(NetworkConnection connection, String type, String payload) {
        send(connection, NetworkMessage.text(type, payload));
    }

    private void send(NetworkConnection connection, NetworkMessage message) {
        if (connection == null || !connection.isOpen()) {
            return;
        }
        try {
            connection.send(message);
        } catch (IOException e) {
            log("send failed to " + describe(connection) + ": " + e.getMessage());
        }
    }

    private static String describe(NetworkConnection connection) {
        return connection == null ? "unknown" : connection.getId() + " " + connection.getRemoteAddress();
    }

    private static void log(String message) {
        System.out.println(LOG_PREFIX + message);
    }
}
