package com.njst.gaming.ri.battlearena;

import com.google.gson.Gson;
import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Bone;
import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Quaternion;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.DesktopComputeBackend;
import com.njst.gaming.Natives.HeadlessContext;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.skeleton.Skeleton;
import com.njst.gaming.skeleton.Skeleton.Skeletal_Animation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class BattleArenaGpuBoneCompare {
    private static final String CHARACTER_DEFINITION = "battle_arena/defeated.character.json";
    private static final String GPU_SKELETON_ASSET = "battle_arena/defeated.gpu_skeleton.json";
    private static final String COMPUTE_SHADER = "resources/shaders/battle_arena_bone_compute.glsl";
    private static final Gson GSON = new Gson();
    private static final float DEFAULT_TOLERANCE = 0.01f;

    private BattleArenaGpuBoneCompare() {
    }

    public static void main(String[] args) throws Exception {
        File resourceRoot = new File(args != null && args.length > 0
                ? args[0]
                : "battle-arena-core/src/main/resources");
        String clipName = args != null && args.length > 1 ? args[1] : "idle";
        int frameIndex = args != null && args.length > 2 ? Integer.parseInt(args[2]) : 0;

        GpuSkeletonAsset gpuAsset = loadGpuAsset(resourceRoot);
        Clip clip = findClip(gpuAsset, clipName);
        frameIndex = clamp(frameIndex, 0, Math.max(0, clip.frameCount - 1));
        float[] cpuMatrices = calculateCpuMatrices(resourceRoot, clipName, frameIndex);
        float[] gpuMatrices = calculateGpuMatrices(gpuAsset, clip, frameIndex);
        ComparisonResult result = compare(cpuMatrices, gpuMatrices, gpuAsset.boneNames, DEFAULT_TOLERANCE);

        System.out.println("[BattleArenaGpuBoneCompare] clip=" + clipName
                + " frame=" + frameIndex
                + " bones=" + gpuAsset.boneCount
                + " maxAbsDiff=" + result.maxAbsDiff
                + " worstBone=" + result.worstBoneName
                + " worstComponent=" + result.worstComponent);
        if (!result.passed) {
            printBoneDiffSummary(cpuMatrices, gpuMatrices, gpuAsset.boneNames, 12);
            throw new IllegalStateException("GPU bone compute mismatch exceeds " + DEFAULT_TOLERANCE
                    + ": " + result);
        }
        System.out.println("[BattleArenaGpuBoneCompare] SUCCESS: GPU matrices match CPU path within "
                + DEFAULT_TOLERANCE);
    }

    private static float[] calculateGpuMatrices(GpuSkeletonAsset asset, Clip clip, int frameIndex) {
        HeadlessContext context = new HeadlessContext(1, 1, "Battle Arena GPU Bone Compare");
        DesktopComputeBackend compute = null;
        try {
            context.init();

            String shaderSource = ShaderProgram.loadShader(COMPUTE_SHADER);
            compute = new DesktopComputeBackend(shaderSource);
            if (compute.hasError()) {
                throw new IllegalStateException(compute.getError());
            }

            compute.bindBuffer(0, createMetadata(asset, clip, frameIndex));
            compute.bindBuffer(1, asset.localRestPositions);
            compute.bindBuffer(2, asset.rotations);
            compute.bindBuffer(3, asset.inverseBindMatrices);
            compute.bindBuffer(4, new float[asset.boneCount * 16]);
            compute.bindBuffer(5, asset.localRestScales);
            compute.dispatch(1, 1, 1);
            return compute.readBuffer(4);
        } finally {
            if (compute != null) {
                compute.release();
            }
            context.destroy();
        }
    }

    private static int[] createMetadata(GpuSkeletonAsset asset, Clip clip, int frameIndex) {
        int[] metadata = new int[5 + asset.boneCount * 2];
        metadata[0] = asset.boneCount;
        metadata[1] = asset.maxDepth;
        metadata[2] = clip.rotationOffset;
        metadata[3] = frameIndex;
        metadata[4] = 0;
        for (int i = 0; i < asset.boneCount; i++) {
            int offset = 5 + i * 2;
            metadata[offset] = asset.parentIndices[i];
            metadata[offset + 1] = asset.depths[i];
        }
        return metadata;
    }

    private static float[] calculateCpuMatrices(File resourceRoot, String clipName, int frameIndex)
            throws IOException, ClassNotFoundException {
        CharacterDefinition definition = loadCharacterDefinition(resourceRoot);
        AnimationDefinition animationDefinition = definition.animations.get(clipName);
        if (animationDefinition == null) {
            throw new IllegalArgumentException("Unknown animation clip: " + clipName);
        }
        ArrayList<Bone> bones = loadBones(resourceRoot, definition.model.bones);
        applyBoneNames(bones, loadBoneNames(resourceRoot, definition.model.boneNames));
        Bone rootBone = findRootBone(bones);
        if (rootBone == null) {
            throw new IllegalStateException("Unable to find root bone");
        }
        rootBone.set_Parent_position(new Vector3(0f, 0f, 0f));
        rootBone.set_Parent_rotation(new Vector3(0f, 0f, 0f));
        rootBone.update();
        for (Bone bone : bones) {
            bone.calculate_bind_matrix();
        }

        Map<String, KeyframeAnimation> animations = loadAnimationMap(resourceRoot, animationDefinition.asset);
        bindAnimations(rootBone, animations);
        Map<Bone, KeyframeAnimation> animationByBone = createAnimationByBone(animations);
        for (Bone bone : bones) {
            KeyframeAnimation animation = animationByBone.get(bone);
            if (animation != null) {
                bone.rotation.set(sampleRotation(animation, frameIndex));
            }
        }
        rootBone.update();

        float[] matrices = new float[bones.size() * 16];
        for (int i = 0; i < bones.size(); i++) {
            Matrix4 matrix = bones.get(i).getAnimationMatrix();
            System.arraycopy(matrix.r, 0, matrices, i * 16, 16);
        }
        return matrices;
    }

    private static Vector3 sampleRotation(KeyframeAnimation animation, float frame) {
        if (animation == null || animation.keyframes == null || animation.keyframes.isEmpty()) {
            return new Vector3(0f, 0f, 0f);
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

    private static ComparisonResult compare(float[] cpu, float[] gpu, String[] boneNames, float tolerance) {
        if (cpu == null || gpu == null || cpu.length != gpu.length) {
            throw new IllegalStateException("Matrix length mismatch cpu="
                    + (cpu == null ? -1 : cpu.length)
                    + " gpu=" + (gpu == null ? -1 : gpu.length));
        }
        ComparisonResult result = new ComparisonResult();
        result.passed = true;
        result.worstBoneIndex = -1;
        result.worstComponent = -1;
        for (int i = 0; i < cpu.length; i++) {
            float diff = Math.abs(cpu[i] - gpu[i]);
            if (diff > result.maxAbsDiff) {
                result.maxAbsDiff = diff;
                result.worstBoneIndex = i / 16;
                result.worstComponent = i % 16;
                result.cpuValue = cpu[i];
                result.gpuValue = gpu[i];
            }
        }
        result.passed = result.maxAbsDiff <= tolerance;
        result.worstBoneName = result.worstBoneIndex >= 0 && result.worstBoneIndex < boneNames.length
                ? boneNames[result.worstBoneIndex]
                : "<none>";
        return result;
    }

    private static void printBoneDiffSummary(float[] cpu, float[] gpu, String[] boneNames, int limit) {
        System.out.println("[BattleArenaGpuBoneCompare] first bone diffs:");
        int boneCount = Math.min(limit, boneNames.length);
        for (int bone = 0; bone < boneCount; bone++) {
            float maxDiff = 0f;
            int maxComponent = 0;
            for (int component = 0; component < 16; component++) {
                int index = bone * 16 + component;
                float diff = Math.abs(cpu[index] - gpu[index]);
                if (diff > maxDiff) {
                    maxDiff = diff;
                    maxComponent = component;
                }
            }
            int translationOffset = bone * 16 + 12;
            System.out.println("  " + bone + " " + boneNames[bone]
                    + " maxDiff=" + maxDiff
                    + " component=" + maxComponent
                    + " cpuT=(" + cpu[translationOffset]
                    + ", " + cpu[translationOffset + 1]
                    + ", " + cpu[translationOffset + 2]
                    + ") gpuT=(" + gpu[translationOffset]
                    + ", " + gpu[translationOffset + 1]
                    + ", " + gpu[translationOffset + 2]
                    + ")");
        }
    }

    private static CharacterDefinition loadCharacterDefinition(File resourceRoot) throws IOException {
        try (Reader reader = new FileReader(new File(resourceRoot, CHARACTER_DEFINITION))) {
            CharacterDefinition definition = GSON.fromJson(reader, CharacterDefinition.class);
            if (definition == null) {
                throw new IllegalStateException("Unable to parse " + CHARACTER_DEFINITION);
            }
            return definition;
        }
    }

    private static GpuSkeletonAsset loadGpuAsset(File resourceRoot) throws IOException {
        try (Reader reader = new FileReader(new File(resourceRoot, GPU_SKELETON_ASSET))) {
            GpuSkeletonAsset asset = GSON.fromJson(reader, GpuSkeletonAsset.class);
            if (asset == null) {
                throw new IllegalStateException("Unable to parse " + GPU_SKELETON_ASSET);
            }
            return asset;
        }
    }

    private static Clip findClip(GpuSkeletonAsset asset, String clipName) {
        for (Clip clip : asset.clips) {
            if (clipName.equals(clip.name)) {
                return clip;
            }
        }
        throw new IllegalArgumentException("Unknown GPU clip: " + clipName);
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

    private static Map<String, KeyframeAnimation> loadAnimationMap(File resourceRoot, String path)
            throws IOException, ClassNotFoundException {
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

    private static Map<Bone, KeyframeAnimation> createAnimationByBone(Map<String, KeyframeAnimation> animations) {
        Map<Bone, KeyframeAnimation> animationByBone = new IdentityHashMap<Bone, KeyframeAnimation>();
        for (KeyframeAnimation animation : animations.values()) {
            if (animation != null && animation.bone != null) {
                animationByBone.put(animation.bone, animation);
            }
        }
        return animationByBone;
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class CharacterDefinition {
        ModelDefinition model = new ModelDefinition();
        Map<String, AnimationDefinition> animations = new HashMap<String, AnimationDefinition>();
    }

    private static final class ModelDefinition {
        String bones;
        String boneNames;
    }

    private static final class AnimationDefinition {
        String asset;
    }

    private static final class GpuSkeletonAsset {
        int boneCount;
        int maxDepth;
        String[] boneNames;
        int[] parentIndices;
        int[] depths;
        float[] localRestPositions;
        float[] localRestScales;
        float[] inverseBindMatrices;
        ArrayList<Clip> clips;
        float[] rotations;
    }

    private static final class Clip {
        String name;
        int frameCount;
        int rotationOffset;
    }

    private static final class ComparisonResult {
        boolean passed;
        float maxAbsDiff;
        int worstBoneIndex;
        String worstBoneName;
        int worstComponent;
        float cpuValue;
        float gpuValue;

        @Override
        public String toString() {
            return "ComparisonResult{"
                    + "maxAbsDiff=" + maxAbsDiff
                    + ", worstBoneIndex=" + worstBoneIndex
                    + ", worstBoneName='" + worstBoneName + '\''
                    + ", worstComponent=" + worstComponent
                    + ", cpuValue=" + cpuValue
                    + ", gpuValue=" + gpuValue
                    + '}';
        }
    }
}
