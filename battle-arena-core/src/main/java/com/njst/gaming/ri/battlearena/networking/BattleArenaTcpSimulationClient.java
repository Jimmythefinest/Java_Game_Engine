package com.njst.gaming.ri.battlearena.networking;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.njst.gaming.Networking.NetworkEvent;
import com.njst.gaming.Networking.NetworkEventType;
import com.njst.gaming.Networking.NetworkMessage;
import com.njst.gaming.Networking.TcpNetworkClient;
import com.njst.gaming.ri.battlearena.BattleArenaPlayerInput;
import com.njst.gaming.ri.battlearena.BattleArenaSimulationSnapshot;

import java.io.IOException;
import java.util.List;

public final class BattleArenaTcpSimulationClient {
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 7788;

    private static final int CONNECT_TIMEOUT_MILLIS = 150;
    private static final float RECONNECT_INTERVAL_SECONDS = 2f;
    private static final String LOG_PREFIX = "[BattleArenaSimClient] ";

    private final Gson gson = new Gson();
    private final TcpNetworkClient client = new TcpNetworkClient();
    private final String host;
    private final int port;

    private BattleArenaSimulationSnapshot latestSnapshot;
    private String assignedPlayer;
    private float reconnectTimerSeconds;
    private boolean waitingLogged;

    public BattleArenaTcpSimulationClient(String host, int port) {
        this.host = host == null || host.trim().isEmpty() ? DEFAULT_HOST : host.trim();
        this.port = port > 0 ? port : DEFAULT_PORT;
    }

    public void update(float deltaSeconds) {
        connectIfNeeded(deltaSeconds);
        drainNetworkEvents();
    }

    public String assignedPlayer() {
        return assignedPlayer;
    }

    public BattleArenaSimulationSnapshot latestSnapshot() {
        return latestSnapshot;
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    public void sendInput(int tick, BattleArenaPlayerInput input) {
        if (!client.isConnected() || assignedPlayer == null) {
            return;
        }
        BattleArenaNetworkInputMessage inputMessage =
                BattleArenaNetworkInputMessage.fromInput(assignedPlayer, tick, input);
        try {
            client.send(NetworkMessage.text(
                    BattleArenaSimulationNetworkProtocol.INPUT_MESSAGE_TYPE,
                    gson.toJson(inputMessage)));
        } catch (IOException e) {
            log("input send failed: " + e.getMessage());
            client.close();
        }
    }

    public void close() {
        client.close();
    }

    private void connectIfNeeded(float deltaSeconds) {
        if (client.isConnected()) {
            reconnectTimerSeconds = 0f;
            return;
        }
        reconnectTimerSeconds -= Math.max(0f, deltaSeconds);
        if (reconnectTimerSeconds > 0f) {
            return;
        }
        reconnectTimerSeconds = RECONNECT_INTERVAL_SECONDS;
        try {
            client.connect(host, port, CONNECT_TIMEOUT_MILLIS);
        } catch (IOException e) {
            if (!waitingLogged) {
                waitingLogged = true;
                log("waiting for simulation server at " + host + ":" + port + " (" + e.getMessage() + ")");
            }
        }
    }

    private void drainNetworkEvents() {
        List<NetworkEvent> events = client.drainEvents();
        for (NetworkEvent event : events) {
            if (event.getType() == NetworkEventType.CONNECTED) {
                waitingLogged = false;
                log("connected to simulation server " + event.getConnection().getRemoteAddress());
                continue;
            }
            if (event.getType() == NetworkEventType.DISCONNECTED) {
                log("simulation server disconnected");
                assignedPlayer = null;
                latestSnapshot = null;
                continue;
            }
            if (event.getType() == NetworkEventType.ERROR) {
                log("network error: " + (event.getError() != null ? event.getError().getMessage() : "unknown"));
                continue;
            }
            if (event.getType() == NetworkEventType.MESSAGE) {
                applyMessage(event.getMessage());
            }
        }
    }

    private void applyMessage(NetworkMessage message) {
        if (message == null) {
            return;
        }
        if (BattleArenaSimulationNetworkProtocol.SESSION_MESSAGE_TYPE.equals(message.getType())) {
            applySession(message);
            return;
        }
        if (!BattleArenaSimulationNetworkProtocol.SNAPSHOT_MESSAGE_TYPE.equals(message.getType())) {
            return;
        }
        try {
            BattleArenaNetworkSnapshotMessage snapshotMessage =
                    gson.fromJson(message.getPayloadAsText(), BattleArenaNetworkSnapshotMessage.class);
            if (snapshotMessage != null) {
                latestSnapshot = snapshotMessage.toSnapshot();
            }
        } catch (JsonSyntaxException ignored) {
        }
    }

    private void applySession(NetworkMessage message) {
        try {
            BattleArenaSimulationSessionMessage session =
                    gson.fromJson(message.getPayloadAsText(), BattleArenaSimulationSessionMessage.class);
            if (session == null) {
                return;
            }
            if (BattleArenaSimulationSessionMessage.EVENT_ASSIGN.equals(session.event)) {
                assignedPlayer = session.player;
                log("assigned player " + assignedPlayer);
            }
        } catch (JsonSyntaxException ignored) {
        }
    }

    private static void log(String message) {
        System.out.println(LOG_PREFIX + message);
    }
}
