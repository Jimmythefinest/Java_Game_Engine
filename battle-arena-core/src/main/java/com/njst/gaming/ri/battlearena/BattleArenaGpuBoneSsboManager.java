package com.njst.gaming.ri.battlearena;

import com.google.gson.Gson;
import com.njst.gaming.graphics.ComputeBackend;
import com.njst.gaming.graphics.GraphicsDevice;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class BattleArenaGpuBoneSsboManager {
    public static final int METADATA_BINDING = 6;
    public static final int LOCAL_REST_POSITION_BINDING = 7;
    public static final int LOCAL_ROTATION_BINDING = 8;
    public static final int INVERSE_BIND_MATRIX_BINDING = 9;
    public static final int OUTPUT_MATRIX_BINDING = 2;
    public static final int LOCAL_REST_SCALE_BINDING = 11;
    public static final int INSTANCE_STATE_BINDING = 12;
    public static final int RENDER_BONE_MATRIX_BINDING = 2;
    private static final int OUTPUT_BUFFER_BINDING_START = 13;
    private static final int OUTPUT_BUFFER_COUNT = 3;
    private static final String GPU_SKELETON_ASSET = "battle_arena/defeated.gpu_skeleton.json";
    private static final String GPU_SKELETON_BINARY_ASSET = "battle_arena/defeated.gpu_skeleton.bin";
    private static final String COMPUTE_SHADER = "resources/shaders/battle_arena_bone_compute.glsl";
    private static final boolean RUN_DISPATCH_DIAGNOSTIC_SWEEP = false;
    private static final String[] DIAGNOSTIC_COMPUTE_SHADERS = {
            "resources/shaders/battle_arena_bone_compute_noop.glsl",
            "resources/shaders/battle_arena_bone_compute_identity_output.glsl",
            "resources/shaders/battle_arena_bone_compute_globals_only.glsl"
    };
    private static final int INSTANCE_STATE_STRIDE = 4;
    private static final int BINARY_MAGIC = 0x42414753;
    private static final int BINARY_VERSION = 1;
    private static final Gson GSON = new Gson();

    private final GpuSkeletonAsset asset;
    private final Map<String, Integer> clipIndexByName = new HashMap<String, Integer>();
    private final ArrayList<InstanceState> instances = new ArrayList<InstanceState>();
    private final IdentityHashMap<BattleArenaGpuSkeletonPoseSource, Integer> instanceBySource =
            new IdentityHashMap<BattleArenaGpuSkeletonPoseSource, Integer>();
    private final ArrayList<DiagnosticBackend> diagnosticBackends = new ArrayList<DiagnosticBackend>();
    private ComputeBackend computeBackend;
    private boolean computeBuffersBound;
    private int outputBoneCapacity;
    private int outputBufferIndex;

    public BattleArenaGpuBoneSsboManager(GraphicsDevice graphicsDevice) {
        this.asset = loadAsset(graphicsDevice);
        validateParentBeforeChildOrder(asset);
        for (int i = 0; i < asset.clips.size(); i++) {
            Clip clip = asset.clips.get(i);
            if (clip != null && clip.name != null) {
                clipIndexByName.put(clip.name, i);
            }
        }
    }

    public static boolean isSupported(GraphicsDevice graphicsDevice) {
        if (graphicsDevice == null) {
            return false;
        }
        ComputeBackend probe = null;
        try {
            String shaderSource = graphicsDevice.loadShaderSource(COMPUTE_SHADER);
            probe = graphicsDevice.createComputeBackend(shaderSource);
            boolean supported = probe != null && !probe.hasError();
            if (!supported && probe != null) {
                System.err.println("[BattleArena] GPU bone compute probe failed: " + probe.getError());
            }
            return supported;
        } catch (RuntimeException e) {
            System.err.println("[BattleArena] GPU bone compute probe failed: " + e.getMessage());
            return false;
        } finally {
            if (probe != null) {
                probe.release();
            }
        }
    }

    public int registerSkeleton(BattleArenaGpuSkeletonPoseSource source) {
        if (source == null) {
            return -1;
        }
        Integer existing = instanceBySource.get(source);
        if (existing != null) {
            return existing.intValue();
        }
        InstanceState instance = new InstanceState();
        instance.boneOffset = source.gpuBoneBufferStartIndex();
        instance.boneCount = source.gpuBoneCount();
        applyPose(instance, source.currentGpuAnimationKey(), source.currentGpuAnimationFrame());
        instances.add(instance);
        int instanceId = instances.size() - 1;
        instanceBySource.put(source, instanceId);
        return instanceId;
    }

    public void syncPose(BattleArenaGpuSkeletonPoseSource source) {
        int instanceId = registerSkeleton(source);
        if (instanceId >= 0) {
            setPose(instanceId, source.currentGpuAnimationKey(), source.currentGpuAnimationFrame());
        }
    }

    public void syncPoses(List<? extends BattleArenaGpuSkeletonPoseSource> sources) {
        if (sources == null) {
            return;
        }
        for (BattleArenaGpuSkeletonPoseSource source : sources) {
            syncPose(source);
        }
    }

    public void setPose(int instanceId, String animationKey, float animationFrame) {
        if (instanceId < 0 || instanceId >= instances.size()) {
            return;
        }
        applyPose(instances.get(instanceId), animationKey, animationFrame);
    }

    public void bindOrUpdateInstanceState(ComputeBackend computeBackend) {
        if (computeBackend == null) {
            return;
        }
        this.computeBackend = computeBackend;
        bindOrUpdateInstanceStateOnly(computeBackend, createInstanceStateBuffer());
    }

    public void dispatchSingle(GraphicsDevice graphicsDevice, BattleArenaGpuSkeletonPoseSource source) {
        int instanceId = registerSkeleton(source);
        if (instanceId < 0) {
            return;
        }
        syncPose(source);
        dispatchAll(graphicsDevice);
    }

    public void dispatchAll(GraphicsDevice graphicsDevice) {
        if (instances.isEmpty()) {
            return;
        }
        ensureComputeBuffers(graphicsDevice);
        int[] state = createInstanceStateBuffer();
        if (RUN_DISPATCH_DIAGNOSTIC_SWEEP) {
            ensureDiagnosticBackends(graphicsDevice);
            for (DiagnosticBackend diagnosticBackend : diagnosticBackends) {
                bindOrUpdateInstanceStateOnly(diagnosticBackend.backend, state);
                diagnosticBackend.backend.dispatch(instances.size(), 1, 1);
            }
        }
        bindOrUpdateInstanceStateOnly(computeBackend, state);
        advanceOutputBuffer(computeBackend);
        computeBackend.dispatch(instances.size(), 1, 1);
    }

    public void bindOutputForRendering() {
        if (computeBackend != null) {
            computeBackend.bindBufferToShaderBinding(currentOutputBufferBinding(), RENDER_BONE_MATRIX_BINDING);
        }
    }

    public int[] createInstanceStateBuffer() {
        int[] state = new int[instances.size() * INSTANCE_STATE_STRIDE];
        for (int i = 0; i < instances.size(); i++) {
            InstanceState instance = instances.get(i);
            int offset = i * INSTANCE_STATE_STRIDE;
            Clip clip = asset.clips.get(instance.clipIndex);
            state[offset] = clip != null ? clip.rotationOffset : 0;
            state[offset + 1] = instance.frameIndex;
            state[offset + 2] = instance.boneOffset;
            state[offset + 3] = instance.flags;
        }
        return state;
    }

    public int instanceCount() {
        return instances.size();
    }

    public int boneCount() {
        return asset.boneCount;
    }

    public int clipIndex(String animationKey) {
        Integer clipIndex = clipIndexByName.get(animationKey);
        if (clipIndex != null) {
            return clipIndex.intValue();
        }
        clipIndex = clipIndexByName.get(BattleArenaCharacterController.ANIM_IDLE);
        return clipIndex != null ? clipIndex.intValue() : 0;
    }

    public int frameIndex(String animationKey, float animationFrame) {
        Clip clip = asset.clips.get(clipIndex(animationKey));
        int frameCount = clip != null ? clip.frameCount : 1;
        if (frameCount <= 0) {
            return 0;
        }
        int frame = (int) Math.floor(Math.max(0f, animationFrame));
        return frame % frameCount;
    }

    public Map<String, BattleArenaAnimationTiming> createAnimationTimings(float tickSeconds) {
        LinkedHashMap<String, BattleArenaAnimationTiming> timings =
                new LinkedHashMap<String, BattleArenaAnimationTiming>();
        float safeTickSeconds = Math.max(0.0001f, tickSeconds);
        for (Clip clip : asset.clips) {
            if (clip == null || clip.name == null || clip.name.trim().isEmpty()) {
                continue;
            }
            float framesPerSecond = clip.framesPerSecond > 0f ? clip.framesPerSecond : 60f;
            float durationFrames = clip.durationFrames > 0f
                    ? clip.durationFrames
                    : Math.max(1, clip.frameCount - 1);
            float durationSeconds = durationFrames / framesPerSecond;
            int lockTicks = Math.max(1, (int) Math.ceil(durationSeconds / safeTickSeconds));
            timings.put(clip.name, new BattleArenaAnimationTiming(
                    clip.name,
                    framesPerSecond,
                    durationFrames,
                    clip.frameCount,
                    lockTicks));
        }
        return timings;
    }

    public int boneOffset(int instanceId) {
        if (instanceId < 0 || instanceId >= instances.size()) {
            return 0;
        }
        return instances.get(instanceId).boneOffset;
    }

    public ComputeBackend computeBackend() {
        return computeBackend;
    }

    private void ensureComputeBuffers(GraphicsDevice graphicsDevice) {
        if (computeBackend == null) {
            String shaderSource = graphicsDevice.loadShaderSource(COMPUTE_SHADER);
            computeBackend = graphicsDevice.createComputeBackend(shaderSource);
            if (computeBackend == null || computeBackend.hasError()) {
                throw new IllegalStateException("Battle Arena GPU bone compute unavailable: "
                        + (computeBackend != null ? computeBackend.getError() : "null backend"));
            }
        }
        if (computeBuffersBound) {
            ensureOutputCapacity();
            return;
        }
        bindStaticBuffers(computeBackend);
        computeBuffersBound = true;
    }

    private void ensureDiagnosticBackends(GraphicsDevice graphicsDevice) {
        if (diagnosticBackends.isEmpty()) {
            for (String shaderPath : DIAGNOSTIC_COMPUTE_SHADERS) {
                String shaderSource = graphicsDevice.loadShaderSource(shaderPath);
                ComputeBackend backend = graphicsDevice.createComputeBackend(shaderSource);
                if (backend == null || backend.hasError()) {
                    throw new IllegalStateException("Battle Arena GPU diagnostic compute unavailable: "
                            + shaderPath + " "
                            + (backend != null ? backend.getError() : "null backend"));
                }
                bindStaticBuffers(backend);
                diagnosticBackends.add(new DiagnosticBackend(backend));
            }
            return;
        }
        for (DiagnosticBackend diagnosticBackend : diagnosticBackends) {
            ensureOutputCapacity(diagnosticBackend.backend);
        }
    }

    private void bindStaticBuffers(ComputeBackend backend) {
        backend.bindBuffer(METADATA_BINDING, createSkeletonMetadata());
        backend.bindBuffer(LOCAL_REST_POSITION_BINDING, asset.localRestPositions);
        backend.bindBuffer(LOCAL_ROTATION_BINDING, asset.rotations);
        backend.bindBuffer(INVERSE_BIND_MATRIX_BINDING, asset.inverseBindMatrices);
        bindOutputBuffers(backend);
        backend.bindBuffer(LOCAL_REST_SCALE_BINDING, asset.localRestScales);
    }

    private void bindOrUpdateInstanceStateOnly(ComputeBackend backend, int[] state) {
        if (!backend.hasBuffer(INSTANCE_STATE_BINDING)) {
            backend.bindBuffer(INSTANCE_STATE_BINDING, state);
        } else {
            backend.updateBuffer(INSTANCE_STATE_BINDING, state);
        }
    }

    private void ensureOutputCapacity() {
        ensureOutputCapacity(computeBackend);
    }

    private void ensureOutputCapacity(ComputeBackend backend) {
        int requiredBones = Math.max(asset.boneCount, totalOutputBoneCount());
        if (requiredBones > outputBoneCapacity) {
            outputBoneCapacity = requiredBones;
        }
        int requiredFloats = outputBoneCapacity * 16;
        if (backend == null) {
            return;
        }
        for (int i = 0; i < OUTPUT_BUFFER_COUNT; i++) {
            int binding = outputBufferBinding(i);
            if (backend.getBufferSize(binding) < requiredFloats) {
                backend.bindBuffer(binding, new float[requiredFloats]);
            }
        }
    }

    private void bindOutputBuffers(ComputeBackend backend) {
        outputBoneCapacity = Math.max(asset.boneCount, totalOutputBoneCount());
        float[] initialData = new float[outputBoneCapacity * 16];
        for (int i = 0; i < OUTPUT_BUFFER_COUNT; i++) {
            backend.bindBuffer(outputBufferBinding(i), initialData);
        }
        backend.bindBufferToShaderBinding(currentOutputBufferBinding(), OUTPUT_MATRIX_BINDING);
    }

    private void advanceOutputBuffer(ComputeBackend backend) {
        outputBufferIndex = (outputBufferIndex + 1) % OUTPUT_BUFFER_COUNT;
        backend.bindBufferToShaderBinding(currentOutputBufferBinding(), OUTPUT_MATRIX_BINDING);
    }

    private int currentOutputBufferBinding() {
        return outputBufferBinding(outputBufferIndex);
    }

    private int outputBufferBinding(int index) {
        return OUTPUT_BUFFER_BINDING_START + index;
    }

    private int totalOutputBoneCount() {
        int total = asset.boneCount;
        for (InstanceState instance : instances) {
            total = Math.max(total, instance.boneOffset + instance.boneCount);
        }
        return total;
    }

    private int[] createSkeletonMetadata() {
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

    private void applyPose(InstanceState instance, String animationKey, float animationFrame) {
        instance.animationKey = animationKey != null ? animationKey : BattleArenaCharacterController.ANIM_IDLE;
        instance.clipIndex = clipIndex(instance.animationKey);
        instance.frameIndex = frameIndex(instance.animationKey, animationFrame);
    }

    private static GpuSkeletonAsset loadAsset(GraphicsDevice graphicsDevice) {
        if (graphicsDevice == null) {
            throw new IllegalArgumentException("graphicsDevice is required");
        }
        try {
            byte[] binaryAsset = graphicsDevice.loadBinaryResource(GPU_SKELETON_BINARY_ASSET);
            if (binaryAsset != null && binaryAsset.length > 0) {
                return readBinaryAsset(binaryAsset);
            }
        } catch (RuntimeException e) {
            System.err.println("[BattleArena] GPU skeleton binary asset unavailable, falling back to JSON: "
                    + e.getMessage());
        }
        GpuSkeletonAsset asset = GSON.fromJson(
                graphicsDevice.loadTextResource(GPU_SKELETON_ASSET),
                GpuSkeletonAsset.class);
        if (asset == null || asset.clips == null || asset.clips.isEmpty()) {
            throw new IllegalStateException("Unable to load GPU skeleton asset: " + GPU_SKELETON_ASSET);
        }
        return asset;
    }

    private static void validateParentBeforeChildOrder(GpuSkeletonAsset asset) {
        if (asset == null || asset.parentIndices == null) {
            throw new IllegalStateException("GPU skeleton asset is missing parent indices");
        }
        for (int i = 0; i < asset.parentIndices.length; i++) {
            int parentIndex = asset.parentIndices[i];
            if (parentIndex >= i) {
                String boneName = asset.boneNames != null && i < asset.boneNames.length
                        ? asset.boneNames[i]
                        : String.valueOf(i);
                throw new IllegalStateException("GPU skeleton asset must be parent-before-child for sequential compute: "
                        + boneName + " parentIndex=" + parentIndex + " boneIndex=" + i);
            }
        }
    }

    private static GpuSkeletonAsset readBinaryAsset(byte[] bytes) {
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(bytes)))) {
            int magic = input.readInt();
            int version = input.readInt();
            if (magic != BINARY_MAGIC || version != BINARY_VERSION) {
                throw new IllegalStateException("Unsupported GPU skeleton binary asset magic="
                        + magic + " version=" + version);
            }
            GpuSkeletonAsset asset = new GpuSkeletonAsset();
            asset.boneCount = input.readInt();
            asset.maxBones = input.readInt();
            asset.maxDepth = input.readInt();
            asset.boneNames = readStringArray(input);
            asset.parentIndices = readIntArray(input);
            asset.depths = readIntArray(input);
            asset.localRestPositions = readFloatArray(input);
            asset.localRestScales = readFloatArray(input);
            asset.inverseBindMatrices = readFloatArray(input);
            int clipCount = input.readInt();
            asset.clips = new ArrayList<Clip>(clipCount);
            for (int i = 0; i < clipCount; i++) {
                Clip clip = new Clip();
                clip.name = input.readUTF();
                clip.asset = input.readUTF();
                clip.framesPerSecond = input.readFloat();
                clip.durationFrames = input.readFloat();
                clip.frameCount = input.readInt();
                clip.boneCount = input.readInt();
                clip.rotationOffset = input.readInt();
                asset.clips.add(clip);
            }
            asset.rotations = readFloatArray(input);
            if (asset.clips.isEmpty()) {
                throw new IllegalStateException("GPU skeleton binary asset has no clips");
            }
            return asset;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read GPU skeleton binary asset", e);
        }
    }

    private static String[] readStringArray(DataInputStream input) throws IOException {
        String[] values = new String[input.readInt()];
        for (int i = 0; i < values.length; i++) {
            values[i] = input.readUTF();
        }
        return values;
    }

    private static int[] readIntArray(DataInputStream input) throws IOException {
        int[] values = new int[input.readInt()];
        for (int i = 0; i < values.length; i++) {
            values[i] = input.readInt();
        }
        return values;
    }

    private static float[] readFloatArray(DataInputStream input) throws IOException {
        float[] values = new float[input.readInt()];
        for (int i = 0; i < values.length; i++) {
            values[i] = input.readFloat();
        }
        return values;
    }

    private static final class InstanceState {
        String animationKey = BattleArenaCharacterController.ANIM_IDLE;
        int clipIndex;
        int frameIndex;
        int boneOffset;
        int boneCount;
        int flags;
    }

    private static final class DiagnosticBackend {
        final ComputeBackend backend;

        DiagnosticBackend(ComputeBackend backend) {
            this.backend = backend;
        }
    }

    private static final class GpuSkeletonAsset {
        int boneCount;
        int maxBones;
        int maxDepth;
        String[] boneNames;
        int[] parentIndices;
        int[] depths;
        float[] localRestPositions;
        float[] localRestScales;
        float[] inverseBindMatrices;
        float[] rotations;
        ArrayList<Clip> clips = new ArrayList<Clip>();
    }

    private static final class Clip {
        String name;
        String asset;
        float framesPerSecond;
        float durationFrames;
        int frameCount;
        int boneCount;
        int rotationOffset;
    }
}
