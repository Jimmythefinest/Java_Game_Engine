package com.njst.gaming.Networking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

abstract class AbstractNetworkPeer implements NetworkPeer, TcpConnection.EventSink {
    private final Queue<NetworkEvent> pendingEvents = new ConcurrentLinkedQueue<NetworkEvent>();
    private final List<NetworkListener> listeners = Collections.synchronizedList(new ArrayList<NetworkListener>());

    @Override
    public void update() {
        List<NetworkEvent> events = drainEvents();
        for (NetworkEvent event : events) {
            notifyListeners(event);
        }
    }

    @Override
    public List<NetworkEvent> drainEvents() {
        List<NetworkEvent> events = new ArrayList<NetworkEvent>();
        NetworkEvent event;
        while ((event = pendingEvents.poll()) != null) {
            events.add(event);
        }
        return events;
    }

    @Override
    public void addListener(NetworkListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(NetworkListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onConnectionEvent(NetworkEvent event) {
        pendingEvents.add(event);
        onQueuedEvent(event);
    }

    protected void queueEvent(NetworkEvent event) {
        pendingEvents.add(event);
        onQueuedEvent(event);
    }

    protected void onQueuedEvent(NetworkEvent event) {
    }

    private void notifyListeners(NetworkEvent event) {
        NetworkListener[] snapshot;
        synchronized (listeners) {
            snapshot = listeners.toArray(new NetworkListener[listeners.size()]);
        }
        for (NetworkListener listener : snapshot) {
            listener.onNetworkEvent(event);
        }
    }
}
