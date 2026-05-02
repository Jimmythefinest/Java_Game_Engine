package com.njst.gaming.android;

import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES31;
import android.os.Bundle;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;
import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Bone;
import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Quaternion;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.skeleton.Skeleton;
import com.njst.gaming.skeleton.Skeleton.Skeletal_Animation;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class BattleArenaGpuVsCpuBenchmarkInstrumentedTest {
    private static final String TAG = "BattleArenaGpuVsCpu";
    private static final String CHARACTER_DEFINITION = "battle_arena/defeated.character.json";
    private static final String GPU_SKELETON_ASSET = "battle_arena/defeated.gpu_skeleton.json";
    private static final String COMPUTE_SHADER = "resources/shaders/battle_arena_bone_compute.glsl";
    private static final float DEFAULT_TOLERANCE = 0.01f;
    private static final int EGL_OPENGL_ES3_BIT_KHR = 0x00000040;
    private static final int METADATA_BINDING = 6;
    private static final int LOCAL_REST_POSITION_BINDING = 7;
    private static final int LOCAL_ROTATION_BINDING = 8;
    private static final int INVERSE_BIND_MATRIX_BINDING = 9;
    private static final int OUTPUT_MATRIX_BINDING = 2;
    private static final int LOCAL_REST_SCALE_BINDING = 11;
    private static final int INSTANCE_STATE_BINDING = 12;
    private static final Gson GSON = new Gson();

    @Test
    public void benchmarkGpuVsCpuBones() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AssetManager assets = context.getAssets();
        String clipName = getArgument("clip", "idle");
        int iterations = Integer.parseInt(getArgument("iterations", "100"));

        GpuSkeletonAsset gpuAsset = loadGpuAsset(assets);
        Clip clip = findClip(gpuAsset, clipName);
        CharacterDefinition definition = loadCharacterDefinition(assets);
        AnimationDefinition animationDefinition = definition.animations.get(clipName);
        if (animationDefinition == null) {
            throw new IllegalArgumentException("Unknown animation clip: " + clipName);
        }

        ArrayList<Bone> bones = loadBones(assets, definition.model.bones);
        applyBoneNames(bones, loadBoneNames(assets, definition.model.boneNames));
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
        Map<String, KeyframeAnimation> animations = loadAnimationMap(assets, animationDefinition.asset);
        bindAnimations(rootBone, animations);

        BenchmarkResults cpuResults = new BenchmarkResults();
        for (int iter = 0; iter < iterations; iter++) {
            int frameIndex = iter % clip.frameCount;
            long start = System.nanoTime();
            float[] cpuMatrices = calculateCpuMatrices(bones, rootBone, animations, frameIndex);
            long elapsed = System.nanoTime() - start;
            cpuResults.addTime(elapsed / 1_000_000.0);
            if (iter == 0) {
                cpuResults.sampleMatrices = cpuMatrices;
            }
        }

        BenchmarkResults gpuResults = new BenchmarkResults();
        HeadlessGlesContext glesContext = new HeadlessGlesContext();
        AndroidComputeBackend compute = null;
        String glVersion = "<unknown>";
        String renderer = "<unknown>";
        try {
            glesContext.init();
            glVersion = String.valueOf(GLES31.glGetString(GLES31.GL_VERSION));
            renderer = String.valueOf(GLES31.glGetString(GLES31.GL_RENDERER));
            String shaderSource = toGlesComputeShader(AndroidAssetLoader.readText(assets, COMPUTE_SHADER));
            compute = new AndroidComputeBackend(shaderSource);
            if (compute.hasError()) {
                throw new IllegalStateException(compute.getError());
            }

            compute.bindBuffer(METADATA_BINDING, createMetadata(gpuAsset));
            compute.bindBuffer(LOCAL_REST_POSITION_BINDING, gpuAsset.localRestPositions);
            compute.bindBuffer(LOCAL_ROTATION_BINDING, gpuAsset.rotations);
            compute.bindBuffer(INVERSE_BIND_MATRIX_BINDING, gpuAsset.inverseBindMatrices);
            compute.bindBuffer(OUTPUT_MATRIX_BINDING, new float[gpuAsset.boneCount * 16]);
            compute.bindBuffer(LOCAL_REST_SCALE_BINDING, gpuAsset.localRestScales);
            compute.bindBuffer(INSTANCE_STATE_BINDING, createInstanceState(clip, 0));

            calculateGpuMatrices(compute, gpuAsset, clip, 0);
            for (int iter = 0; iter < iterations; iter++) {
                int frameIndex = iter % clip.frameCount;
                long start = System.nanoTime();
                float[] gpuMatrices = calculateGpuMatrices(compute, gpuAsset, clip, frameIndex);
                long elapsed = System.nanoTime() - start;
                gpuResults.addTime(elapsed / 1_000_000.0);
                if (iter == 0) {
                    gpuResults.sampleMatrices = gpuMatrices;
                }
            }
        } finally {
            if (compute != null) {
                compute.release();
            }
            glesContext.destroy();
        }

        ComparisonResult validation = compare(cpuResults.sampleMatrices, gpuResults.sampleMatrices,
                gpuAsset.boneNames, DEFAULT_TOLERANCE);
        cpuResults.computeStats();
        gpuResults.computeStats();
        String result = "Battle Arena GPU vs CPU benchmark"
                + " clip=" + clipName
                + " iterations=" + iterations
                + " bones=" + gpuAsset.boneCount
                + " validation=" + (validation.passed ? "PASSED" : "FAILED")
                + " maxAbsDiff=" + validation.maxAbsDiff
                + " worstBone=" + validation.worstBoneName
                + " worstComponent=" + validation.worstComponent
                + " cpuAvgMs=" + cpuResults.avgTime
                + " gpuAvgMs=" + gpuResults.avgTime
                + " speedup=" + (cpuResults.avgTime / gpuResults.avgTime)
                + " gpuIncludesReadback=true"
                + " glVersion=" + glVersion
                + " renderer=" + renderer;
        Log.i(TAG, result);
        System.out.println(result);
        if (!validation.passed) {
            printBoneDiffSummary(cpuResults.sampleMatrices, gpuResults.sampleMatrices, gpuAsset.boneNames, 12);
            throw new IllegalStateException("GPU and CPU results differ beyond tolerance: " + validation);
        }
    }

    private static String getArgument(String name, String fallback) {
        Bundle arguments = InstrumentationRegistry.getArguments();
        String value = arguments != null ? arguments.getString(name) : null;
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static String toGlesComputeShader(String shaderSource) {
        return shaderSource.replaceFirst("#version 430 core",
                "#version 310 es\nprecision highp float;\nprecision highp int;");
    }

    private static float[] calculateGpuMatrices(AndroidComputeBackend compute,
                                                GpuSkeletonAsset asset,
        Clip clip,
                                                int frameIndex) {
        compute.updateBuffer(INSTANCE_STATE_BINDING, createInstanceState(clip, frameIndex));
        compute.dispatch(1, 1, 1);
        return compute.readBuffer(OUTPUT_MATRIX_BINDING);
    }

    private static int[] createMetadata(GpuSkeletonAsset asset) {
        int[] metadata = new int[2 + asset.boneCount * 2];
        metadata[0] = asset.boneCount;
        metadata[1] = asset.maxDepth;
        for (int i = 0; i < asset.boneCount; i++) {
            int offset = 2 + i * 2;
            metadata[offset] = asset.parentIndices[i];
            metadata[offset + 1] = asset.depths[i];
        }
        return metadata;
    }

    private static int[] createInstanceState(Clip clip, int frameIndex) {
        return new int[] {clip.rotationOffset, frameIndex, 0, 0};
    }

    private static float[] calculateCpuMatrices(ArrayList<Bone> bones,
                                                Bone rootBone,
                                                Map<String, KeyframeAnimation> animations,
                                                int frameIndex) {
        rootBone.set_Parent_position(new Vector3(0f, 0f, 0f));
        rootBone.set_Parent_rotation(new Vector3(0f, 0f, 0f));
        rootBone.update();

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

    private static CharacterDefinition loadCharacterDefinition(AssetManager assets) {
        return GSON.fromJson(AndroidAssetLoader.readText(assets, CHARACTER_DEFINITION),
                CharacterDefinition.class);
    }

    private static GpuSkeletonAsset loadGpuAsset(AssetManager assets) {
        return GSON.fromJson(AndroidAssetLoader.readText(assets, GPU_SKELETON_ASSET),
                GpuSkeletonAsset.class);
    }

    private static Clip findClip(GpuSkeletonAsset asset, String clipName) {
        for (Clip clip : asset.clips) {
            if (clipName.equals(clip.name)) {
                return clip;
            }
        }
        throw new IllegalArgumentException("Unknown GPU clip: " + clipName);
    }

    private static ArrayList<Bone> loadBones(AssetManager assets, String path)
            throws IOException, ClassNotFoundException {
        Object value = readObject(AndroidAssetLoader.readBytes(assets, path));
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

    private static Map<String, KeyframeAnimation> loadAnimationMap(AssetManager assets, String path)
            throws IOException, ClassNotFoundException {
        Object value = readObject(AndroidAssetLoader.readBytes(assets, path));
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

    private static Object readObject(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return input.readObject();
        }
    }

    private static List<String> loadBoneNames(AssetManager assets, String path) {
        String[] names = GSON.fromJson(AndroidAssetLoader.readText(assets, path), String[].class);
        return Arrays.asList(names);
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
            Log.i(TAG, bone + " " + boneNames[bone]
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

    private static final class HeadlessGlesContext {
        private EGLDisplay display = EGL14.EGL_NO_DISPLAY;
        private EGLContext context = EGL14.EGL_NO_CONTEXT;
        private EGLSurface surface = EGL14.EGL_NO_SURFACE;

        void init() {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            if (display == EGL14.EGL_NO_DISPLAY) {
                throw new IllegalStateException("Unable to get EGL display");
            }
            int[] version = new int[2];
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
                throw new IllegalStateException("Unable to initialize EGL");
            }
            int[] configAttributes = new int[] {
                    EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT_KHR,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] configCount = new int[1];
            if (!EGL14.eglChooseConfig(display, configAttributes, 0, configs, 0, configs.length,
                    configCount, 0) || configCount[0] <= 0) {
                throw new IllegalStateException("Unable to choose EGL config");
            }
            int[] contextAttributes = new int[] {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                    EGL14.EGL_NONE
            };
            context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT,
                    contextAttributes, 0);
            if (context == EGL14.EGL_NO_CONTEXT) {
                throw new IllegalStateException("Unable to create EGL context");
            }
            int[] surfaceAttributes = new int[] {
                    EGL14.EGL_WIDTH, 1,
                    EGL14.EGL_HEIGHT, 1,
                    EGL14.EGL_NONE
            };
            surface = EGL14.eglCreatePbufferSurface(display, configs[0], surfaceAttributes, 0);
            if (surface == EGL14.EGL_NO_SURFACE) {
                throw new IllegalStateException("Unable to create EGL pbuffer surface");
            }
            if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
                throw new IllegalStateException("Unable to make EGL context current");
            }
        }

        void destroy() {
            if (display != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_CONTEXT);
                if (surface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(display, surface);
                    surface = EGL14.EGL_NO_SURFACE;
                }
                if (context != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(display, context);
                    context = EGL14.EGL_NO_CONTEXT;
                }
                EGL14.eglTerminate(display);
                display = EGL14.EGL_NO_DISPLAY;
            }
        }
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

    private static final class BenchmarkResults {
        final ArrayList<Double> times = new ArrayList<Double>();
        float[] sampleMatrices;
        double minTime = Double.MAX_VALUE;
        double maxTime = 0.0;
        double avgTime = 0.0;
        double totalTime = 0.0;

        void addTime(double timeMs) {
            times.add(timeMs);
            minTime = Math.min(minTime, timeMs);
            maxTime = Math.max(maxTime, timeMs);
            totalTime += timeMs;
        }

        void computeStats() {
            avgTime = times.isEmpty() ? 0.0 : totalTime / times.size();
        }
    }
}
