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
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class BattleArenaGpuSkeletonAssetExporter {
    private static final String DEFAULT_CHARACTER_DEFINITION =
            "battle_arena/defeated.character.json";
    private static final String DEFAULT_OUTPUT =
            "battle_arena/defeated.gpu_skeleton.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private BattleArenaGpuSkeletonAssetExporter() {
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
                + " bones=" + exportRoot.boneCount
                + " maxDepth=" + exportRoot.maxDepth
                + " clips=" + exportRoot.clips.size()
                + " quaternions=" + (exportRoot.rotations.length / 4));
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
        for (Bone bone : bones) {
            bone.calculate_bind_matrix();
        }

        ExportRoot exportRoot = new ExportRoot();
        exportRoot.character = definitionPath;
        exportRoot.space = "local_rotations_with_bind_pose";
        exportRoot.boneCount = bones.size();
        exportRoot.maxBones = bones.size();
        exportRoot.boneNames = createBoneNames(bones);
        exportRoot.parentIndices = createParentIndices(bones);
        exportRoot.depths = createDepths(exportRoot.parentIndices);
        exportRoot.maxDepth = findMax(exportRoot.depths);
        exportRoot.localRestPositions = createLocalRestPositions(bones);
        exportRoot.localRestScales = createLocalRestScales(bones);
        exportRoot.inverseBindMatrices = createInverseBindMatrices(bones);
        exportRoot.clips = new ArrayList<ClipExport>();

        ArrayList<Float> rotations = new ArrayList<Float>();
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
            ClipExport clip = sampleClip(entry.getKey(), animationDefinition, animations, bones, rotations);
            exportRoot.clips.add(clip);
        }
        exportRoot.rotations = toFloatArray(rotations);
        validateExport(definition, exportRoot);
        return exportRoot;
    }

    private static ClipExport sampleClip(String name,
                                         BattleArenaCharacterDefinition.AnimationDefinition animationDefinition,
                                         Map<String, KeyframeAnimation> animations,
                                         ArrayList<Bone> bones,
                                         ArrayList<Float> rotations) {
        ClipExport clip = new ClipExport();
        clip.name = name;
        clip.asset = animationDefinition.assetPath();
        clip.framesPerSecond = animationDefinition.resolvedFramesPerSecond();
        clip.durationFrames = resolveDurationFrames(animations);
        clip.frameCount = Math.max(1, (int) Math.ceil(clip.durationFrames) + 1);
        clip.boneCount = bones.size();
        clip.rotationOffset = rotations.size() / 4;

        Map<Bone, KeyframeAnimation> animationByBone = createAnimationByBone(animations);
        Quaternion[] previousByBone = new Quaternion[bones.size()];
        for (int frame = 0; frame < clip.frameCount; frame++) {
            for (int boneIndex = 0; boneIndex < bones.size(); boneIndex++) {
                Bone bone = bones.get(boneIndex);
                Quaternion rotation = sampleQuaternion(animationByBone.get(bone), bone, frame);
                enforceContinuity(rotation, previousByBone[boneIndex]);
                previousByBone[boneIndex] = new Quaternion(rotation.x, rotation.y, rotation.z, rotation.w);
                rotations.add(rotation.x);
                rotations.add(rotation.y);
                rotations.add(rotation.z);
                rotations.add(rotation.w);
            }
        }
        return clip;
    }

    private static Map<Bone, KeyframeAnimation> createAnimationByBone(Map<String, KeyframeAnimation> animations) {
        Map<Bone, KeyframeAnimation> animationByBone = new IdentityHashMap<Bone, KeyframeAnimation>();
        for (KeyframeAnimation animation : animations.values()) {
            if (animation != null && animation.bone != null) {
                animationByBone.put(animation.bone, animation);
            }
        }
        return animationByBone;
    }

    private static Quaternion sampleQuaternion(KeyframeAnimation animation, Bone bone, float frame) {
        if (animation == null || animation.keyframes == null || animation.keyframes.isEmpty()) {
            return Quaternion.fromEuler(bone.rotation.x, bone.rotation.y, bone.rotation.z).normalize();
        }
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
            return Quaternion.fromEuler(bone.rotation.x, bone.rotation.y, bone.rotation.z).normalize();
        }
        if (previous == null) {
            return fromKeyframe(next);
        }
        if (next == null || next.time <= previous.time) {
            return fromKeyframe(previous);
        }
        float t = (frame - previous.time) / (next.time - previous.time);
        return Quaternion.slerp(fromKeyframe(previous), fromKeyframe(next), t).normalize();
    }

    private static Quaternion fromKeyframe(KeyframeAnimation.Keyframe keyframe) {
        if (keyframe == null || keyframe.rotation == null) {
            return new Quaternion();
        }
        return Quaternion.fromEuler(keyframe.rotation.x, keyframe.rotation.y, keyframe.rotation.z).normalize();
    }

    private static void enforceContinuity(Quaternion rotation, Quaternion previous) {
        if (previous == null) {
            return;
        }
        float dot = previous.x * rotation.x
                + previous.y * rotation.y
                + previous.z * rotation.z
                + previous.w * rotation.w;
        if (dot < 0f) {
            rotation.x = -rotation.x;
            rotation.y = -rotation.y;
            rotation.z = -rotation.z;
            rotation.w = -rotation.w;
        }
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

    private static String[] createBoneNames(ArrayList<Bone> bones) {
        String[] names = new String[bones.size()];
        for (int i = 0; i < bones.size(); i++) {
            names[i] = bones.get(i).name;
        }
        return names;
    }

    private static int[] createParentIndices(ArrayList<Bone> bones) {
        IdentityHashMap<Bone, Integer> indexByBone = new IdentityHashMap<Bone, Integer>();
        for (int i = 0; i < bones.size(); i++) {
            indexByBone.put(bones.get(i), i);
        }
        int[] parentIndices = new int[bones.size()];
        Arrays.fill(parentIndices, -1);
        for (int parentIndex = 0; parentIndex < bones.size(); parentIndex++) {
            Bone parent = bones.get(parentIndex);
            for (Bone child : parent.Children) {
                Integer childIndex = indexByBone.get(child);
                if (childIndex != null) {
                    parentIndices[childIndex.intValue()] = parentIndex;
                }
            }
        }
        return parentIndices;
    }

    private static int[] createDepths(int[] parentIndices) {
        int[] depths = new int[parentIndices.length];
        Arrays.fill(depths, -1);
        for (int i = 0; i < parentIndices.length; i++) {
            depths[i] = resolveDepth(i, parentIndices, depths);
        }
        return depths;
    }

    private static int resolveDepth(int boneIndex, int[] parentIndices, int[] depths) {
        if (depths[boneIndex] >= 0) {
            return depths[boneIndex];
        }
        int parentIndex = parentIndices[boneIndex];
        if (parentIndex < 0) {
            depths[boneIndex] = 0;
            return 0;
        }
        depths[boneIndex] = resolveDepth(parentIndex, parentIndices, depths) + 1;
        return depths[boneIndex];
    }

    private static int findMax(int[] values) {
        int max = 0;
        for (int value : values) {
            max = Math.max(max, value);
        }
        return max;
    }

    private static float[] createLocalRestPositions(ArrayList<Bone> bones) {
        float[] positions = new float[bones.size() * 4];
        for (int i = 0; i < bones.size(); i++) {
            Vector3 position = bones.get(i).position_to_parent;
            int offset = i * 4;
            positions[offset] = position.x;
            positions[offset + 1] = position.y;
            positions[offset + 2] = position.z;
            positions[offset + 3] = 0f;
        }
        return positions;
    }

    private static float[] createLocalRestScales(ArrayList<Bone> bones) {
        float[] scales = new float[bones.size() * 4];
        for (int i = 0; i < bones.size(); i++) {
            Vector3 scale = bones.get(i).scale;
            int offset = i * 4;
            scales[offset] = scale.x;
            scales[offset + 1] = scale.y;
            scales[offset + 2] = scale.z;
            scales[offset + 3] = 0f;
        }
        return scales;
    }

    private static float[] createInverseBindMatrices(ArrayList<Bone> bones) {
        float[] matrices = new float[bones.size() * 16];
        for (int i = 0; i < bones.size(); i++) {
            float[] matrix = bones.get(i).copyInverseBindPose();
            System.arraycopy(matrix, 0, matrices, i * 16, Math.min(16, matrix.length));
        }
        return matrices;
    }

    private static void validateExport(BattleArenaCharacterDefinition definition, ExportRoot exportRoot) {
        if (exportRoot.clips.size() != definition.animations.size()) {
            throw new IllegalStateException("Clip count mismatch clips=" + exportRoot.clips.size()
                    + " definitions=" + definition.animations.size());
        }
        int expectedOffset = 0;
        for (ClipExport clip : exportRoot.clips) {
            if (clip.rotationOffset != expectedOffset) {
                throw new IllegalStateException("Unexpected rotation offset for " + clip.name
                        + " offset=" + clip.rotationOffset + " expected=" + expectedOffset);
            }
            expectedOffset += clip.frameCount * clip.boneCount;
        }
        int totalQuaternions = exportRoot.rotations.length / 4;
        if (totalQuaternions != expectedOffset || exportRoot.rotations.length % 4 != 0) {
            throw new IllegalStateException("Rotation count mismatch quaternions=" + totalQuaternions
                    + " expected=" + expectedOffset
                    + " floats=" + exportRoot.rotations.length);
        }
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
            String[] names = GSON.fromJson(reader, String[].class);
            return Arrays.asList(names);
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

    private static float[] toFloatArray(ArrayList<Float> values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i).floatValue();
        }
        return result;
    }

    private static void ensureParentDirectory(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create directory: " + parent.getPath());
        }
    }

    private static void log(String message) {
        System.out.println("[BattleArenaGpuSkeletonExport] " + message);
    }

    static final class ExportRoot {
        String character;
        String space;
        int boneCount;
        int maxBones;
        int maxDepth;
        String[] boneNames;
        int[] parentIndices;
        int[] depths;
        float[] localRestPositions;
        float[] localRestScales;
        float[] inverseBindMatrices;
        ArrayList<ClipExport> clips;
        float[] rotations;
    }

    static final class ClipExport {
        String name;
        String asset;
        float framesPerSecond;
        float durationFrames;
        int frameCount;
        int boneCount;
        int rotationOffset;
    }
}
