package com.njst.gaming.Networking;

public final class NetworkEvent {
    private final NetworkEventType type;
    private final NetworkConnection connection;
    private final NetworkMessage message;
    private final Exception error;

    private NetworkEvent(NetworkEventType type, NetworkConnection connection, NetworkMessage message, Exception error) {
        this.type = type;
        this.connection = connection;
        this.message = message;
        this.error = error;
    }

    public static NetworkEvent connected(NetworkConnection connection) {
        return new NetworkEvent(NetworkEventType.CONNECTED, connection, null, null);
    }

    public static NetworkEvent message(NetworkConnection connection, NetworkMessage message) {
        return new NetworkEvent(NetworkEventType.MESSAGE, connection, message, null);
    }

    public static NetworkEvent disconnected(NetworkConnection connection) {
        return new NetworkEvent(NetworkEventType.DISCONNECTED, connection, null, null);
    }

    public static NetworkEvent error(NetworkConnection connection, Exception error) {
        return new NetworkEvent(NetworkEventType.ERROR, connection, null, error);
    }

    public NetworkEventType getType() {
        return type;
    }

    public NetworkConnection getConnection() {
        return connection;
    }

    public NetworkMessage getMessage() {
        return message;
    }

    public Exception getError() {
        return error;
    }
}
