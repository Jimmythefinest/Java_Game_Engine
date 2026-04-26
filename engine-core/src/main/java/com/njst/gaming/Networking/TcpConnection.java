package com.njst.gaming.Networking;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

final class TcpConnection implements NetworkConnection {
    interface EventSink {
        void onConnectionEvent(NetworkEvent event);
    }

    private final int id;
    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;
    private final int maxMessageBytes;
    private final EventSink eventSink;
    private final AtomicBoolean open = new AtomicBoolean(true);
    private final AtomicBoolean disconnectedEmitted = new AtomicBoolean(false);
    private Thread readerThread;

    TcpConnection(int id, Socket socket, NetworkSettings settings, EventSink eventSink) throws IOException {
        this.id = id;
        this.socket = socket;
        this.maxMessageBytes = settings.getMaxMessageBytes();
        this.eventSink = eventSink;
        this.socket.setTcpNoDelay(settings.isTcpNoDelay());
        this.input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    void startReader(String threadName) {
        readerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                readLoop();
            }
        }, threadName);
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readLoop() {
        try {
            while (open.get()) {
                NetworkMessage message = MessageFramer.read(input, maxMessageBytes);
                if (message == null) {
                    break;
                }
                eventSink.onConnectionEvent(NetworkEvent.message(this, message));
            }
        } catch (IOException e) {
            if (open.get()) {
                eventSink.onConnectionEvent(NetworkEvent.error(this, e));
            }
        } finally {
            close();
        }
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return socket.getRemoteSocketAddress();
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
        synchronized (output) {
            MessageFramer.write(output, message, maxMessageBytes);
        }
    }

    @Override
    public void close() {
        if (open.getAndSet(false)) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
        emitDisconnected();
    }

    private void emitDisconnected() {
        if (disconnectedEmitted.compareAndSet(false, true)) {
            eventSink.onConnectionEvent(NetworkEvent.disconnected(this));
        }
    }
}
