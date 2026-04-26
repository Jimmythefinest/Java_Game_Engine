package com.njst.gaming.Networking;

public final class NetworkSettings {
    public static final int DEFAULT_MAX_MESSAGE_BYTES = 1024 * 1024;

    private final int maxMessageBytes;
    private final boolean tcpNoDelay;

    public NetworkSettings() {
        this(DEFAULT_MAX_MESSAGE_BYTES, true);
    }

    public NetworkSettings(int maxMessageBytes, boolean tcpNoDelay) {
        if (maxMessageBytes <= 0) {
            throw new IllegalArgumentException("Max message bytes must be greater than zero");
        }
        this.maxMessageBytes = maxMessageBytes;
        this.tcpNoDelay = tcpNoDelay;
    }

    public int getMaxMessageBytes() {
        return maxMessageBytes;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }
}
