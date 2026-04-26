package com.njst.gaming.Networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class UdpNetworkClient extends AbstractNetworkPeer {
    private final NetworkSettings settings;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private DatagramSocket socket;
    private UdpConnection connection;
    private Thread receiveThread;

    public UdpNetworkClient() {
        this(new NetworkSettings());
    }

    public UdpNetworkClient(NetworkSettings settings) {
        this.settings = settings == null ? new NetworkSettings() : settings;
    }

    public void connect(String host, int port) throws IOException {
        connect(new InetSocketAddress(host, port));
    }

    public void connect(SocketAddress remoteAddress) throws IOException {
        if (running.get()) {
            throw new IOException("Client is already connected");
        }
        socket = new DatagramSocket();
        connection = new UdpConnection(1, socket, remoteAddress, settings.getMaxMessageBytes());
        running.set(true);
        queueEvent(NetworkEvent.connected(connection));
        startReceiver("njst-udp-client");
    }

    public boolean isConnected() {
        return connection != null && connection.isOpen() && running.get();
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
        running.set(false);
        if (connection != null) {
            connection.close();
        }
        if (socket != null) {
            socket.close();
        }
        if (connection != null) {
            queueEvent(NetworkEvent.disconnected(connection));
        }
    }

    private void startReceiver(String threadName) {
        receiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                receiveLoop();
            }
        }, threadName);
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    private void receiveLoop() {
        byte[] buffer = new byte[settings.getMaxMessageBytes()];
        while (running.get()) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                if (!packet.getSocketAddress().equals(connection.getRemoteAddress())) {
                    continue;
                }
                byte[] frame = copyPacketData(packet);
                NetworkMessage message = MessageFramer.decode(frame, settings.getMaxMessageBytes());
                queueEvent(NetworkEvent.message(connection, message));
            } catch (IOException e) {
                if (running.get()) {
                    queueEvent(NetworkEvent.error(connection, e));
                }
            }
        }
    }

    private byte[] copyPacketData(DatagramPacket packet) {
        byte[] frame = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), packet.getOffset(), frame, 0, packet.getLength());
        return frame;
    }
}
