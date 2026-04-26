package com.njst.gaming.Networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

final class UdpConnection implements NetworkConnection {
    private final int id;
    private final DatagramSocket socket;
    private final SocketAddress remoteAddress;
    private final int maxMessageBytes;
    private final AtomicBoolean open = new AtomicBoolean(true);

    UdpConnection(int id, DatagramSocket socket, SocketAddress remoteAddress, int maxMessageBytes) {
        this.id = id;
        this.socket = socket;
        this.remoteAddress = remoteAddress;
        this.maxMessageBytes = maxMessageBytes;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public boolean isOpen() {
        return open.get() && !socket.isClosed();
    }

    @Override
    public void send(NetworkMessage message) throws IOException {
        if (message == null) {
            throw new IllegalArgumentException("Message must not be null");
        }
        if (!isOpen()) {
            throw new IOException("Connection is closed");
        }
        byte[] frame = MessageFramer.encode(message, maxMessageBytes);
        DatagramPacket packet = new DatagramPacket(frame, frame.length, remoteAddress);
        socket.send(packet);
    }

    @Override
    public void close() {
        open.set(false);
    }
}
