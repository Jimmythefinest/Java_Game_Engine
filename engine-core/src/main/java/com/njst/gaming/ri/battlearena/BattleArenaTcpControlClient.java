package com.njst.gaming.ri.battlearena;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.njst.gaming.Networking.NetworkEvent;
import com.njst.gaming.Networking.NetworkEventType;
import com.njst.gaming.Networking.NetworkMessage;
import com.njst.gaming.Networking.TcpNetworkClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class BattleArenaTcpControlClient {
    static final String MESSAGE_TYPE = "battle_arena.controls";
    static final String ANDROID_PLAYER = "android";
    static final String DESKTOP_PLAYER = "desktop";
    static final String DEFAULT_HOST = "127.0.0.1";
    static final int DEFAULT_PORT = 7777;

    private static final int CONNECT_TIMEOUT_MILLIS = 150;
    private static final float RECONNECT_INTERVAL_SECONDS = 2f;
    private static final String LOG_PREFIX = "[BattleArenaTCP] ";

    private final Gson gson = new Gson();
    private final TcpNetworkClient client = new TcpNetworkClient();
    private final Map<String, BattleArenaTcpControlSnapshot> latestByPlayer =
            new HashMap<String, BattleArenaTcpControlSnapshot>();
    private final String host;
    private final int port;

    private float reconnectTimerSeconds = 0f;
    private boolean connectionLogged = false;

    BattleArenaTcpControlClient() {
        this(
                System.getProperty("battleArena.remoteHost", DEFAULT_HOST),
                readPortProperty("battleArena.remotePort", DEFAULT_PORT));
    }

    BattleArenaTcpControlClient(String host, int port) {
        this.host = host == null || host.trim().isEmpty() ? DEFAULT_HOST : host;
        this.port = port;
    }

    void update(float deltaSeconds) {
        connectIfNeeded(deltaSeconds);
        drainNetworkEvents();
    }

    boolean hasSnapshot(String player) {
        return latestByPlayer.containsKey(player);
    }

    void copyControls(String player, BattleArenaCharacterControlState controls) {
        BattleArenaTcpControlSnapshot snapshot = latestByPlayer.get(player);
        if (snapshot == null) {
            controls.clear();
            return;
        }
        snapshot.copyTo(controls);
    }

    void sendControls(String player, BattleArenaCharacterControlState controls) {
        if (!client.isConnected()) {
            return;
        }
        BattleArenaTcpControlSnapshot snapshot = BattleArenaTcpControlSnapshot.fromControls(player, controls);
        try {
            client.send(NetworkMessage.text(MESSAGE_TYPE, gson.toJson(snapshot)));
        } catch (IOException e) {
            log("send failed: " + e.getMessage());
            client.close();
        }
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
            if (connectionLogged) {
                return;
            }
            connectionLogged = true;
            log("waiting for control server at " + host + ":" + port + " (" + e.getMessage() + ")");
        }
    }

    private void drainNetworkEvents() {
        List<NetworkEvent> events = client.drainEvents();
        for (NetworkEvent event : events) {
            if (event.getType() == NetworkEventType.CONNECTED) {
                connectionLogged = false;
                log("connected to control server " + event.getConnection().getRemoteAddress());
                continue;
            }
            if (event.getType() == NetworkEventType.DISCONNECTED) {
                log("control server disconnected");
                continue;
            }
            if (event.getType() == NetworkEventType.ERROR) {
                log("network error: " + event.getError().getMessage());
                continue;
            }
            if (event.getType() == NetworkEventType.MESSAGE) {
                applyMessage(event.getMessage());
            }
        }
    }

    private void applyMessage(NetworkMessage message) {
        if (message == null || !MESSAGE_TYPE.equals(message.getType())) {
            return;
        }
        BattleArenaTcpControlSnapshot parsed = parseJson(message.getPayloadAsText());
        if (parsed == null) {
            parsed = parseKeyValuePayload(message.getPayloadAsText());
        }
        if (parsed == null || parsed.player == null || parsed.player.trim().isEmpty()) {
            return;
        }
        BattleArenaTcpControlSnapshot stored = latestByPlayer.get(parsed.player);
        if (stored == null) {
            stored = new BattleArenaTcpControlSnapshot();
            latestByPlayer.put(parsed.player, stored);
        }
        stored.copyFrom(parsed);
    }

    private BattleArenaTcpControlSnapshot parseJson(String payload) {
        try {
            return gson.fromJson(payload, BattleArenaTcpControlSnapshot.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    private BattleArenaTcpControlSnapshot parseKeyValuePayload(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return null;
        }
        BattleArenaTcpControlSnapshot snapshot = new BattleArenaTcpControlSnapshot();
        String[] parts = payload.split("[,;]");
        for (String part : parts) {
            String[] keyValue = part.split("=", 2);
            if (keyValue.length != 2) {
                continue;
            }
            snapshot.apply(keyValue[0].trim(), keyValue[1].trim());
        }
        return snapshot;
    }

    private static int readPortProperty(String name, int fallback) {
        String value = System.getProperty(name);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static void log(String message) {
        System.out.println(LOG_PREFIX + message);
    }
}
