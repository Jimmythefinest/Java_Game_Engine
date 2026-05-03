package com.njst.gaming.ri.battlearena;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Bone;
import com.njst.gaming.Math.Quaternion;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.skeleton.Skeleton;
import com.njst.gaming.skeleton.Skeleton.Skeletal_Animation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BattleArenaHitboxTrackExporter {
    private static final String DEFAULT_CHARACTER_DEFINITION =
            "battle_arena/defeated.character.json";
    private static final String DEFAULT_OUTPUT =
            "battle_arena/defeated.hitbox_tracks.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private BattleArenaHitboxTrackExporter() {
    }

    public static void main(String[] args) throws Exception {
        File resourceRoot = new File(args != null && args.length > 0
                ? args[0]
                : "battle-arena-core/src/main/resources");
        String definitionPath = args != null && args.length > 1
                ? args[1]
                : DEFAULT_CHARACTER_DEFINITION;
        String outputPath = args != null && args.length > 2
                ? args[2]
                : DEFAULT_OUTPUT;

        ExportRoot exportRoot = export(resourceRoot, definitionPath);
        File outputFile = new File(resourceRoot, outputPath);
        ensureParentDirectory(outputFile);
        try (Writer writer = new FileWriter(outputFile)) {
            GSON.toJson(exportRoot, writer);
        }
        log("wrote " + outputFile.getPath()
                + " animations=" + exportRoot.animations.size()
                + " boxes=" + exportRoot.boxes.size());
    }

    static ExportRoot export(File resourceRoot, String definitionPath) throws IOException, ClassNotFoundException {
        BattleArenaCharacterDefinition definition = loadDefinition(resourceRoot, definitionPath);
        ArrayList<Bone> bones = loadBones(resourceRoot, definition.model.bones);
        applyBoneNames(bones, loadBoneNames(resourceRoot, definition.model.boneNames));
        Bone rootBone = findRootBone(bones);
        if (rootBone == null) {
            throw new IllegalStateException("Unable to find root bone for " + definitionPath);
        }
        rootBone.set_Parent_position(new Vector3(0f, 0f, 0f));
        rootBone.set_Parent_rotation(new Vector3(0f, 0f, 0f));
        rootBone.update();

        ArrayList<BoxDefinitionTrack> boxes = createBoxDefinitions(definition);
        ExportRoot exportRoot = new ExportRoot();
        exportRoot.character = definitionPath;
        exportRoot.space = "character_root_relative";
        exportRoot.boxes = boxes;
        exportRoot.animations = new LinkedHashMap<String, AnimationTrack>();

        Map<Bone, BoneRestPose> restPoseByBone = captureRestPose(bones);
        for (Map.Entry<String, BattleArenaCharacterDefinition.AnimationDefinition> entry
                : definition.animations.entrySet()) {
            BattleArenaCharacterDefinition.AnimationDefinition animationDefinition = entry.getValue();
            if (animationDefinition == null
                    || animationDefinition.assetPath() == null
                    || animationDefinition.assetPath().trim().isEmpty()) {
                continue;
            }
            Map<String, KeyframeAnimation> animations =
                    loadAnimationMap(resourceRoot, animationDefinition.assetPath());
            bindAnimations(rootBone, animations);
            exportRoot.animations.put(entry.getKey(), sampleAnimation(
                    entry.getKey(),
                    animationDefinition,
                    animations,
                    definition,
                    boxes,
                    bones,
                    rootBone,
                    restPoseByBone));
        }
        return exportRoot;
    }

    private static AnimationTrack sampleAnimation(String animationKey,
                                                  BattleArenaCharacterDefinition.AnimationDefinition animationDefinition,
                                                  Map<String, KeyframeAnimation> animations,
                                                  BattleArenaCharacterDefinition definition,
                                                  ArrayList<BoxDefinitionTrack> boxes,
                                                  ArrayList<Bone> bones,
                                                  Bone rootBone,
                                                  Map<Bone, BoneRestPose> restPoseByBone) {
        AnimationTrack track = new AnimationTrack();
        track.asset = animationDefinition.assetPath();
        track.framesPerSecond = animationDefinition.resolvedFramesPerSecond();
        track.durationFrames = resolveDurationFrames(animations);
        track.frames = new ArrayList<FrameTrack>();

        int sampleCount = Math.max(1, (int) Math.ceil(track.durationFrames) + 1);
        for (int frame = 0; frame < sampleCount; frame++) {
            restoreRestPose(restPoseByBone);
            applyAnimationPose(animations, frame);
            rootBone.update();

            FrameTrack frameTrack = new FrameTrack();
            frameTrack.frame = frame;
            frameTrack.timeSeconds = frame / track.framesPerSecond;
            frameTrack.boxes = new LinkedHashMap<String, BoxFrameTrack>();

            for (BoxDefinitionTrack box : boxes) {
                BattleArenaCharacterDefinition.HitboxDefinition sourceDefinition =
                        definition.hitboxes.get(box.name);
                Bone anchorBone = findBone(bones, sourceDefinition.followsBone);
                Vector3 anchorPosition = anchorBone != null
                        ? anchorBone.global_position
                        : rootBone.global_position;
                Vector3 center = new Vector3(anchorPosition).add(new Vector3(sourceDefinition.center));
                Vector3 rootRelativeCenter = center.sub(new Vector3(rootBone.global_position));

                BoxFrameTrack boxFrame = new BoxFrameTrack();
                boxFrame.center = toArray(rootRelativeCenter);
                boxFrame.active = isBoxActiveForAnimation(sourceDefinition, animationKey);
                frameTrack.boxes.put(box.name, boxFrame);
            }
            track.frames.add(frameTrack);
        }
        return track;
    }

    private static void applyAnimationPose(Map<String, KeyframeAnimation> animations, float frame) {
        for (KeyframeAnimation animation : animations.values()) {
            if (animation == null || animation.bone == null || animation.keyframes == null
                    || animation.keyframes.isEmpty()) {
                continue;
            }
            Vector3 rotation = sampleRotation(animation, frame);
            animation.bone.setRotation(rotation);
        }
    }

    private static Vector3 sampleRotation(KeyframeAnimation animation, float frame) {
        KeyframeAnimation.Keyframe previous = null;
        KeyframeAnimation.Keyframe next = null;
        for (KeyframeAnimation.Keyframe keyframe : animation.keyframes) {
            if (keyframe == null) {
                continue;
            }
            if (keyframe.time <= frame) {
                previous = keyframe;
                continue;
            }
            next = keyframe;
            break;
        }
        if (previous == null && next == null) {
            return new Vector3(0f, 0f, 0f);
        }
        if (previous == null) {
            return new Vector3(next.rotation);
        }
        if (next == null || next.time <= previous.time) {
            return new Vector3(previous.rotation);
        }
        float t = (frame - previous.time) / (next.time - previous.time);
        Quaternion start = Quaternion.fromEuler(previous.rotation.x, previous.rotation.y, previous.rotation.z);
        Quaternion end = Quaternion.fromEuler(next.rotation.x, next.rotation.y, next.rotation.z);
        return new Vector3(Quaternion.slerp(start, end, t).toEuler());
    }

    private static boolean isBoxActiveForAnimation(BattleArenaCharacterDefinition.HitboxDefinition definition,
                                                   String animationKey) {
        if (definition.activeWhen == null || definition.activeWhen.trim().isEmpty()) {
            return true;
        }
        return definition.activeWhen.equals(animationKey);
    }

    private static ArrayList<BoxDefinitionTrack> createBoxDefinitions(BattleArenaCharacterDefinition definition) {
        ArrayList<BoxDefinitionTrack> boxes = new ArrayList<BoxDefinitionTrack>();
        for (Map.Entry<String, BattleArenaCharacterDefinition.HitboxDefinition> entry
                : definition.hitboxes.entrySet()) {
            BattleArenaCharacterDefinition.HitboxDefinition hitbox = entry.getValue();
            if (hitbox == null || hitbox.halfExtents == null || hitbox.halfExtents.length < 3) {
                continue;
            }
            BoxDefinitionTrack box = new BoxDefinitionTrack();
            box.name = entry.getKey();
            box.kind = hitbox.kind;
            box.followsBone = hitbox.followsBone;
            box.halfExtents = toArray(new Vector3(hitbox.halfExtents));
            box.activeWhen = hitbox.activeWhen;
            box.onHitAnimation = hitbox.onHitAnimation;
            boxes.add(box);
        }
        return boxes;
    }

    private static BattleArenaCharacterDefinition loadDefinition(File resourceRoot,
                                                                 String definitionPath) throws IOException {
        File definitionFile = new File(resourceRoot, definitionPath);
        try (Reader reader = new FileReader(definitionFile)) {
            BattleArenaCharacterDefinition definition = GSON.fromJson(reader, BattleArenaCharacterDefinition.class);
            if (definition == null) {
                throw new IllegalStateException("Unable to parse " + definitionFile.getPath());
            }
            return definition;
        }
    }

    private static ArrayList<Bone> loadBones(File resourceRoot, String path) throws IOException, ClassNotFoundException {
        Object value = readObject(new File(resourceRoot, path));
        if (!(value instanceof ArrayList<?>)) {
            throw new IllegalStateException("Expected bone list in " + path);
        }
        ArrayList<Bone> bones = new ArrayList<Bone>();
        for (Object entry : (ArrayList<?>) value) {
            if (!(entry instanceof Bone)) {
                throw new IllegalStateException("Expected Bone in " + path);
            }
            bones.add((Bone) entry);
        }
        return bones;
    }

    private static Map<String, KeyframeAnimation> loadAnimationMap(File resourceRoot,
                                                                   String path) throws IOException, ClassNotFoundException {
        Object value = readObject(new File(resourceRoot, path));
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalStateException("Expected animation map in " + path);
        }
        Map<String, KeyframeAnimation> animations = new HashMap<String, KeyframeAnimation>();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            if (entry.getKey() instanceof String && entry.getValue() instanceof KeyframeAnimation) {
                animations.put((String) entry.getKey(), (KeyframeAnimation) entry.getValue());
            }
        }
        return animations;
    }

    private static Object readObject(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(readAllBytes(file)))) {
            return input.readObject();
        }
    }

    private static byte[] readAllBytes(File file) throws IOException {
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            throw new IOException("File too large: " + file.getPath());
        }
        byte[] bytes = new byte[(int) length];
        try (FileInputStream input = new FileInputStream(file)) {
            int offset = 0;
            while (offset < bytes.length) {
                int read = input.read(bytes, offset, bytes.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
            return bytes;
        }
    }

    private static List<String> loadBoneNames(File resourceRoot, String path) throws IOException {
        try (Reader reader = new FileReader(new File(resourceRoot, path))) {
            BoneNameList names = GSON.fromJson(reader, BoneNameList.class);
            return names;
        }
    }

    private static void applyBoneNames(ArrayList<Bone> bones, List<String> names) {
        if (bones.size() != names.size()) {
            throw new IllegalStateException("Bone count mismatch names=" + names.size() + " bones=" + bones.size());
        }
        for (int i = 0; i < bones.size(); i++) {
            bones.get(i).name = names.get(i);
        }
    }

    private static void bindAnimations(Bone rootBone, Map<String, KeyframeAnimation> animations) {
        Skeleton skeleton = new Skeleton(rootBone);
        Skeletal_Animation skeletalAnimation = new Skeletal_Animation();
        skeletalAnimation.set_Animation_map(animations);
        skeleton.map(skeletalAnimation);
    }

    private static float resolveDurationFrames(Map<String, KeyframeAnimation> animations) {
        float duration = 0f;
        for (KeyframeAnimation animation : animations.values()) {
            if (animation == null || animation.keyframes == null) {
                continue;
            }
            for (KeyframeAnimation.Keyframe keyframe : animation.keyframes) {
                if (keyframe != null) {
                    duration = Math.max(duration, keyframe.time);
                }
            }
        }
        return duration;
    }

    private static Bone findRootBone(ArrayList<Bone> bones) {
        Map<Bone, Boolean> children = new IdentityHashMap<Bone, Boolean>();
        for (Bone bone : bones) {
            for (Bone child : bone.Children) {
                children.put(child, Boolean.TRUE);
            }
        }
        for (Bone bone : bones) {
            if (!children.containsKey(bone)) {
                return bone;
            }
        }
        return bones.isEmpty() ? null : bones.get(0);
    }

    private static Bone findBone(ArrayList<Bone> bones, String nameFragment) {
        if (nameFragment == null) {
            return null;
        }
        String needle = nameFragment.toLowerCase();
        for (Bone bone : bones) {
            if (bone.name != null && bone.name.toLowerCase().contains(needle)) {
                return bone;
            }
        }
        return null;
    }

    private static Map<Bone, BoneRestPose> captureRestPose(ArrayList<Bone> bones) {
        Map<Bone, BoneRestPose> restPoseByBone = new IdentityHashMap<Bone, BoneRestPose>();
        for (Bone bone : bones) {
            BoneRestPose restPose = new BoneRestPose();
            restPose.position = new Vector3(bone.position_to_parent);
            restPose.rotation = new Vector3(bone.rotation);
            restPose.parentPosition = new Vector3(bone.parentposition);
            restPose.parentRotation = new Vector3(bone.parent_rotation);
            restPoseByBone.put(bone, restPose);
        }
        return restPoseByBone;
    }

    private static void restoreRestPose(Map<Bone, BoneRestPose> restPoseByBone) {
        for (Map.Entry<Bone, BoneRestPose> entry : restPoseByBone.entrySet()) {
            Bone bone = entry.getKey();
            BoneRestPose restPose = entry.getValue();
            bone.position_to_parent.set(restPose.position);
            bone.rotation.set(restPose.rotation);
            bone.set_Parent_position(restPose.parentPosition);
            bone.set_Parent_rotation(restPose.parentRotation);
        }
    }

    private static float[] toArray(Vector3 vector) {
        return new float[] {vector.x, vector.y, vector.z};
    }

    private static void ensureParentDirectory(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create directory: " + parent.getPath());
        }
    }

    private static void log(String message) {
        System.out.println("[BattleArenaHitboxExport] " + message);
    }

    private static final class BoneNameList extends ArrayList<String> {
    }

    private static final class BoneRestPose {
        Vector3 position;
        Vector3 rotation;
        Vector3 parentPosition;
        Vector3 parentRotation;
    }

    static final class ExportRoot {
        String character;
        String space;
        ArrayList<BoxDefinitionTrack> boxes;
        Map<String, AnimationTrack> animations;
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
        ArrayList<FrameTrack> frames;
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
