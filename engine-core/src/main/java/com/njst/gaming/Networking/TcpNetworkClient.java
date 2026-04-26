package com.njst.gaming.Networking;

import java.io.IOException;
import java.net.Socket;

public class TcpNetworkClient extends AbstractNetworkPeer {
    private final NetworkSettings settings;
    private TcpConnection connection;

    public TcpNetworkClient() {
        this(new NetworkSettings());
    }

    public TcpNetworkClient(NetworkSettings settings) {
        this.settings = settings == null ? new NetworkSettings() : settings;
    }

    public void connect(String host, int port) throws IOException {
        if (connection != null && connection.isOpen()) {
            throw new IOException("Client is already connected");
        }
        Socket socket = new Socket(host, port);
        connection = new TcpConnection(1, socket, settings, this);
        queueEvent(NetworkEvent.connected(connection));
        connection.startReader("njst-network-client-" + host + ":" + port);
    }

    public boolean isConnected() {
        return connection != null && connection.isOpen();
    }

    public NetworkConnection getConnection() {
        return connection;
    }

    public void send(NetworkMessage message) throws IOException {
        if (connection == null) {
            throw new IOException("Client is not connected");
        }
        connection.send(message);
    }

    @Override
    public void close() {
        if (connection != null) {
            connection.close();
        }
    }
}
