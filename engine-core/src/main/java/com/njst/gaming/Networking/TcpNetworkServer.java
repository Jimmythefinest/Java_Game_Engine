package com.njst.gaming.Networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TcpNetworkServer extends AbstractNetworkPeer {
    private final NetworkSettings settings;
    private final List<TcpConnection> connections = Collections.synchronizedList(new ArrayList<TcpConnection>());
    private final AtomicInteger nextConnectionId = new AtomicInteger(1);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public TcpNetworkServer() {
        this(new NetworkSettings());
    }

    public TcpNetworkServer(NetworkSettings settings) {
        this.settings = settings == null ? new NetworkSettings() : settings;
    }

    public void start(int port) throws IOException {
        if (running.get()) {
            throw new IOException("Server is already running");
        }
        serverSocket = new ServerSocket(port);
        running.set(true);
        acceptThread = new Thread(new Runnable() {
            @Override
            public void run() {
                acceptLoop();
            }
        }, "njst-network-server-accept-" + port);
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public boolean isRunning() {
        return running.get();
    }

    public List<NetworkConnection> getConnections() {
        List<NetworkConnection> snapshot = new ArrayList<NetworkConnection>();
        synchronized (connections) {
            snapshot.addAll(connections);
        }
        return Collections.unmodifiableList(snapshot);
    }

    public void broadcast(NetworkMessage message) throws IOException {
        IOException failure = null;
        synchronized (connections) {
            for (TcpConnection connection : connections) {
                if (!connection.isOpen()) {
                    continue;
                }
                try {
                    connection.send(message);
                } catch (IOException e) {
                    failure = e;
                    queueEvent(NetworkEvent.error(connection, e));
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    @Override
    protected void onQueuedEvent(NetworkEvent event) {
        if (event.getType() == NetworkEventType.DISCONNECTED && event.getConnection() instanceof TcpConnection) {
            connections.remove(event.getConnection());
        }
    }

    @Override
    public void close() {
        running.set(false);
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        List<TcpConnection> snapshot;
        synchronized (connections) {
            snapshot = new ArrayList<TcpConnection>(connections);
        }
        for (TcpConnection connection : snapshot) {
            connection.close();
        }
    }

    private void acceptLoop() {
        try {
            while (running.get()) {
                Socket socket = serverSocket.accept();
                TcpConnection connection = new TcpConnection(nextConnectionId.getAndIncrement(), socket, settings, this);
                connections.add(connection);
                queueEvent(NetworkEvent.connected(connection));
                connection.startReader("njst-network-server-client-" + connection.getId());
            }
        } catch (IOException e) {
            if (running.get()) {
                queueEvent(NetworkEvent.error(null, e));
            }
        } finally {
            running.set(false);
        }
    }
}
