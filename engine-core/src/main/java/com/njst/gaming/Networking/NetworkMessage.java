package com.njst.gaming.Networking;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class NetworkMessage {
    private final String type;
    private final byte[] payload;
    private final long createdAtMillis;

    public NetworkMessage(String type, byte[] payload) {
        this(type, payload, System.currentTimeMillis());
    }

    public NetworkMessage(String type, byte[] payload, long createdAtMillis) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Message type must not be empty");
        }
        this.type = type;
        this.payload = payload == null ? new byte[0] : Arrays.copyOf(payload, payload.length);
        this.createdAtMillis = createdAtMillis;
    }

    public static NetworkMessage text(String type, String text) {
        return text(type, text, StandardCharsets.UTF_8);
    }

    public static NetworkMessage text(String type, String text, Charset charset) {
        Charset safeCharset = charset == null ? StandardCharsets.UTF_8 : charset;
        return new NetworkMessage(type, text == null ? new byte[0] : text.getBytes(safeCharset));
    }

    public String getType() {
        return type;
    }

    public byte[] getPayload() {
        return Arrays.copyOf(payload, payload.length);
    }

    public int getPayloadSize() {
        return payload.length;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public String getPayloadAsText() {
        return getPayloadAsText(StandardCharsets.UTF_8);
    }

    public String getPayloadAsText(Charset charset) {
        Charset safeCharset = charset == null ? StandardCharsets.UTF_8 : charset;
        return new String(payload, safeCharset);
    }
}
