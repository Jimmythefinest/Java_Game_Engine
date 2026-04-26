package com.njst.gaming.Networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class UdpNetworkServer extends AbstractNetworkPeer {
    private final NetworkSettings settings;
    private final Map<SocketAddress, UdpConnection> connections =
            Collections.synchronizedMap(new LinkedHashMap<SocketAddress, UdpConnection>());
    private final AtomicInteger nextConnectionId = new AtomicInteger(1);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private DatagramSocket socket;
    private Thread receiveThread;

    public UdpNetworkServer() {
        this(new NetworkSettings());
    }

    public UdpNetworkServer(NetworkSettings settings) {
        this.settings = settings == null ? new NetworkSettings() : settings;
    }

    public void start(int port) throws IOException {
        if (running.get()) {
            throw new IOException("Server is already running");
        }
        socket = new DatagramSocket(port);
        running.set(true);
        receiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                receiveLoop();
            }
        }, "njst-udp-server-" + port);
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    public boolean isRunning() {
        return running.get();
    }

    public List<NetworkConnection> getConnections() {
        List<NetworkConnection> snapshot = new ArrayList<NetworkConnection>();
        synchronized (connections) {
            snapshot.addAll(connections.values());
        }
        return Collections.unmodifiableList(snapshot);
    }

    public void broadcast(NetworkMessage message) throws IOException {
        IOException failure = null;
        synchronized (connections) {
            for (UdpConnection connection : connections.values()) {
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
    public void close() {
        running.set(false);
        if (socket != null) {
            socket.close();
        }
        List<UdpConnection> snapshot;
        synchronized (connections) {
            snapshot = new ArrayList<UdpConnection>(connections.values());
            connections.clear();
        }
        for (UdpConnection connection : snapshot) {
            connection.close();
            queueEvent(NetworkEvent.disconnected(connection));
        }
    }

    private void receiveLoop() {
        byte[] buffer = new byte[settings.getMaxMessageBytes()];
        while (running.get()) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                UdpConnection connection = connectionFor(packet.getSocketAddress());
                byte[] frame = copyPacketData(packet);
                NetworkMessage message = MessageFramer.decode(frame, settings.getMaxMessageBytes());
                queueEvent(NetworkEvent.message(connection, message));
            } catch (IOException e) {
                if (running.get()) {
                    queueEvent(NetworkEvent.error(null, e));
                }
            }
        }
    }

    private UdpConnection connectionFor(SocketAddress remoteAddress) {
        synchronized (connections) {
            UdpConnection connection = connections.get(remoteAddress);
            if (connection == null) {
                connection = new UdpConnection(nextConnectionId.getAndIncrement(), socket, remoteAddress,
                        settings.getMaxMessageBytes());
                connections.put(remoteAddress, connection);
                queueEvent(NetworkEvent.connected(connection));
            }
            return connection;
        }
    }

    private byte[] copyPacketData(DatagramPacket packet) {
        byte[] frame = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), packet.getOffset(), frame, 0, packet.getLength());
        return frame;
    }
}
