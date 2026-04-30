package com.njst.gaming.ri.battlearena;

import com.google.gson.Gson;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.graphics.GraphicsDevice;

import java.util.List;
import java.util.Map;

final class BattleArenaHitboxTracks {
    private static final Gson GSON = new Gson();

    String character;
    String space;
    List<BoxDefinitionTrack> boxes;
    Map<String, AnimationTrack> animations;

    static BattleArenaHitboxTracks load(GraphicsDevice graphicsDevice, String path) {
        if (graphicsDevice == null || path == null || path.trim().isEmpty()) {
            return null;
        }
        String json = graphicsDevice.loadTextResource(path);
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        return GSON.fromJson(json, BattleArenaHitboxTracks.class);
    }

    Vector3 sampleCenter(String animationKey, float frame, String boxName) {
        AnimationTrack animation = animation(animationKey);
        if (animation == null || animation.frames == null || animation.frames.isEmpty()) {
            return null;
        }
        float clampedFrame = clamp(frame, 0f, Math.max(0f, animation.frames.size() - 1f));
        int firstIndex = Math.max(0, Math.min(animation.frames.size() - 1, (int) Math.floor(clampedFrame)));
        int secondIndex = Math.max(0, Math.min(animation.frames.size() - 1, firstIndex + 1));
        BoxFrameTrack first = boxFrame(animation.frames.get(firstIndex), boxName);
        BoxFrameTrack second = boxFrame(animation.frames.get(secondIndex), boxName);
        if (first == null && second == null) {
            return null;
        }
        if (first == null || second == null || first.center == null || second.center == null) {
            return toVector(first != null ? first.center : second.center);
        }
        float alpha = clampedFrame - firstIndex;
        return lerp(toVector(first.center), toVector(second.center), alpha);
    }

    boolean isActive(String animationKey, float frame, String boxName, boolean fallbackActive) {
        AnimationTrack animation = animation(animationKey);
        if (animation == null || animation.frames == null || animation.frames.isEmpty()) {
            return fallbackActive;
        }
        int index = Math.max(0, Math.min(animation.frames.size() - 1, Math.round(frame)));
        BoxFrameTrack box = boxFrame(animation.frames.get(index), boxName);
        return box != null ? box.active : fallbackActive;
    }

    private AnimationTrack animation(String animationKey) {
        if (animations == null || animations.isEmpty()) {
            return null;
        }
        AnimationTrack animation = animations.get(animationKey);
        if (animation != null) {
            return animation;
        }
        return animations.get(BattleArenaCharacterController.ANIM_IDLE);
    }

    private BoxFrameTrack boxFrame(FrameTrack frame, String boxName) {
        if (frame == null || frame.boxes == null) {
            return null;
        }
        return frame.boxes.get(boxName);
    }

    private Vector3 toVector(float[] values) {
        if (values == null || values.length < 3) {
            return null;
        }
        return new Vector3(values[0], values[1], values[2]);
    }

    private Vector3 lerp(Vector3 first, Vector3 second, float alpha) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        float clamped = clamp(alpha, 0f, 1f);
        return new Vector3(
                first.x + ((second.x - first.x) * clamped),
                first.y + ((second.y - first.y) * clamped),
                first.z + ((second.z - first.z) * clamped));
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    static final class BoxDefinitionTrack {
        String name;
        String kind;
        String followsBone;
        float[] halfExtents;
        String activeWhen;
        String onHitAnimation;
    }

    static final class AnimationTrack {
        String asset;
        float framesPerSecond;
        float durationFrames;
        List<FrameTrack> frames;
    }

    static final class FrameTrack {
        int frame;
        float timeSeconds;
        Map<String, BoxFrameTrack> boxes;
    }

    static final class BoxFrameTrack {
        boolean active;
        float[] center;
    }
}
