package com.njst.gaming.Networking;

import java.io.IOException;
import java.net.SocketAddress;

public interface NetworkConnection {
    int getId();

    SocketAddress getRemoteAddress();

    boolean isOpen();

    void send(NetworkMessage message) throws IOException;

    void close();
}
