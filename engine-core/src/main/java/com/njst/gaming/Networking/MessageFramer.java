package com.njst.gaming.Networking;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

final class MessageFramer {
    private MessageFramer() {
    }

    static NetworkMessage read(DataInputStream input, int maxMessageBytes) throws IOException {
        int frameSize;
        try {
            frameSize = input.readInt();
        } catch (EOFException e) {
            return null;
        }
        if (frameSize <= 0 || frameSize > maxMessageBytes) {
            throw new IOException("Invalid network frame size: " + frameSize);
        }

        byte[] frame = new byte[frameSize];
        input.readFully(frame);
        return decode(frame, maxMessageBytes);
    }

    static NetworkMessage decode(byte[] frame, int maxMessageBytes) throws IOException {
        if (frame == null || frame.length <= 0 || frame.length > maxMessageBytes) {
            throw new IOException("Invalid network frame size: " + (frame == null ? 0 : frame.length));
        }
        DataInputStream frameInput = new DataInputStream(new ByteArrayInputStream(frame));
        String type = frameInput.readUTF();
        long createdAtMillis = frameInput.readLong();
        int payloadSize = frameInput.readInt();
        if (payloadSize < 0 || payloadSize > maxMessageBytes) {
            throw new IOException("Invalid network payload size: " + payloadSize);
        }
        byte[] payload = new byte[payloadSize];
        frameInput.readFully(payload);
        return new NetworkMessage(type, payload, createdAtMillis);
    }

    static void write(DataOutputStream output, NetworkMessage message, int maxMessageBytes) throws IOException {
        byte[] frame = encode(message, maxMessageBytes);
        output.writeInt(frame.length);
        output.write(frame);
        output.flush();
    }

    static byte[] encode(NetworkMessage message, int maxMessageBytes) throws IOException {
        ByteArrayOutputStream frameBytes = new ByteArrayOutputStream();
        DataOutputStream frameOutput = new DataOutputStream(frameBytes);
        frameOutput.writeUTF(message.getType());
        frameOutput.writeLong(message.getCreatedAtMillis());
        byte[] payload = message.getPayload();
        frameOutput.writeInt(payload.length);
        frameOutput.write(payload);
        frameOutput.flush();

        byte[] frame = frameBytes.toByteArray();
        if (frame.length > maxMessageBytes) {
            throw new IOException("Network frame exceeds max message size: " + frame.length);
        }
        return frame;
    }
}
