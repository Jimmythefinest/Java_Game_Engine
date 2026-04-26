package com.njst.gaming.Networking;

import java.util.List;

public interface NetworkPeer {
    void update();

    List<NetworkEvent> drainEvents();

    void addListener(NetworkListener listener);

    void removeListener(NetworkListener listener);

    void close();
}
