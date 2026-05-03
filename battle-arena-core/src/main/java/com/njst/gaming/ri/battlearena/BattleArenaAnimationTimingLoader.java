package com.njst.gaming.ri.battlearena;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BattleArenaAnimationTimingLoader {
    private static final int BINARY_MAGIC = 0x42414753;
    private static final int BINARY_VERSION = 1;
    private static final String DEFAULT_GPU_SKELETON_ASSET = "battle_arena/defeated.gpu_skeleton.bin";

    private BattleArenaAnimationTimingLoader() {
    }

    public static Map<String, BattleArenaAnimationTiming> loadDefault(float tickSeconds) {
        return loadFromResource(DEFAULT_GPU_SKELETON_ASSET, tickSeconds);
    }

    public static Map<String, BattleArenaAnimationTiming> loadFromResource(String resourcePath,
                                                                           float tickSeconds) {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            stream = BattleArenaAnimationTimingLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        }
        if (stream == null) {
            return new LinkedHashMap<String, BattleArenaAnimationTiming>();
        }
        try {
            return loadFromBinary(readAll(stream), tickSeconds);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read Battle Arena animation timing resource: "
                    + resourcePath, e);
        }
    }

    public static Map<String, BattleArenaAnimationTiming> loadFromBinary(byte[] bytes,
                                                                         float tickSeconds) {
        LinkedHashMap<String, BattleArenaAnimationTiming> timings =
                new LinkedHashMap<String, BattleArenaAnimationTiming>();
        if (bytes == null || bytes.length == 0) {
            return timings;
        }
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(bytes)))) {
            int magic = input.readInt();
            int version = input.readInt();
            if (magic != BINARY_MAGIC || version != BINARY_VERSION) {
                throw new IllegalStateException("Unsupported GPU skeleton binary asset magic="
                        + magic + " version=" + version);
            }
            input.readInt();
            input.readInt();
            input.readInt();
            skipStringArray(input);
            skipIntArray(input);
            skipIntArray(input);
            skipFloatArray(input);
            skipFloatArray(input);
            skipFloatArray(input);
            int clipCount = input.readInt();
            float safeTickSeconds = Math.max(0.0001f, tickSeconds);
            for (int i = 0; i < clipCount; i++) {
                String name = input.readUTF();
                input.readUTF();
                float framesPerSecond = input.readFloat();
                float durationFrames = input.readFloat();
                int frameCount = input.readInt();
                input.readInt();
                input.readInt();
                float safeFramesPerSecond = framesPerSecond > 0f ? framesPerSecond : 60f;
                float safeDurationFrames = durationFrames > 0f
                        ? durationFrames
                        : Math.max(1, frameCount - 1);
                int lockTicks = Math.max(1,
                        (int) Math.ceil((safeDurationFrames / safeFramesPerSecond) / safeTickSeconds));
                timings.put(name, new BattleArenaAnimationTiming(
                        name,
                        safeFramesPerSecond,
                        safeDurationFrames,
                        frameCount,
                        lockTicks));
            }
            return timings;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read Battle Arena GPU skeleton binary asset", e);
        }
    }

    private static void skipStringArray(DataInputStream input) throws IOException {
        int count = input.readInt();
        for (int i = 0; i < count; i++) {
            input.readUTF();
        }
    }

    private static void skipIntArray(DataInputStream input) throws IOException {
        int count = input.readInt();
        for (int i = 0; i < count; i++) {
            input.readInt();
        }
    }

    private static void skipFloatArray(DataInputStream input) throws IOException {
        int count = input.readInt();
        for (int i = 0; i < count; i++) {
            input.readFloat();
        }
    }

    private static byte[] readAll(InputStream stream) throws IOException {
        try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }
}
