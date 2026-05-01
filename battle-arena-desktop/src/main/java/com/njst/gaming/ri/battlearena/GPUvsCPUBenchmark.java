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

public final class GPUvsCPUBenchmark {
    private static final String CHARACTER_DEFINITION = "battle_arena/defeated.character.json";
    private static final String GPU_SKELETON_ASSET = "battle_arena/defeated.gpu_skeleton.json";
    private static final String COMPUTE_SHADER = "resources/shaders/battle_arena_bone_compute.glsl";
    private static final Gson GSON = new Gson();
    private static final float DEFAULT_TOLERANCE = 0.01f;

    private GPUvsCPUBenchmark() {
    }

    public static void main(String[] args) throws Exception {
        File resourceRoot = new File(args != null && args.length > 0
                ? args[0]
                : "battle-arena-core/src/main/resources");
        String clipName = args != null && args.length > 1 ? args[1] : "idle";
        int iterations = args != null && args.length > 2 ? Integer.parseInt(args[2]) : 1000;

        GpuSkeletonAsset gpuAsset = loadGpuAsset(resourceRoot);
        Clip clip = findClip(gpuAsset, clipName);
        
        System.out.println("=".repeat(80));
        System.out.println("[GPUvsCPUBenchmark] Starting GPU vs CPU Benchmark");
        System.out.println("=".repeat(80));
        System.out.println("Configuration:");
        System.out.println("  Clip: " + clipName);
        System.out.println("  Iterations: " + iterations);
        System.out.println("  Frame Count: " + clip.frameCount);
        System.out.println("  Bone Count: " + gpuAsset.boneCount);
        System.out.println();

        // Load CPU resources
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

        BenchmarkResults cpuResults = new BenchmarkResults();
        BenchmarkResults gpuResults = new BenchmarkResults();

        // CPU Benchmark
        System.out.println("Running CPU benchmark (" + iterations + " iterations)...");
        for (int iter = 0; iter < iterations; iter++) {
            int frameIndex = iter % clip.frameCount;
            long startTime = System.nanoTime();
            float[] cpuMatrices = calculateCpuMatrices(resourceRoot, bones, rootBone, animations, clipName, frameIndex);
            long endTime = System.nanoTime();
            
            cpuResults.addTime((endTime - startTime) / 1_000_000.0); // Convert to ms
            if (iter == 0) {
                cpuResults.sampleMatrices = cpuMatrices;
            }
        }
        System.out.println("CPU benchmark complete.\n");

        // GPU Benchmark - Initialize resources once
        System.out.println("Initializing GPU resources...");
        GpuComputeResources gpuResources = initializeGpuResources(gpuAsset);
        GpuComputeResources resources = gpuResources; // For lambda access
        GpuSkeletonAsset asset = gpuAsset; // For lambda access

        resources.compute.bindBuffer(BattleArenaGpuBoneSsboManager.METADATA_BINDING, createMetadata(asset));
        resources.compute.bindBuffer(BattleArenaGpuBoneSsboManager.LOCAL_REST_POSITION_BINDING, asset.localRestPositions);
        resources.compute.bindBuffer(BattleArenaGpuBoneSsboManager.LOCAL_ROTATION_BINDING, asset.rotations);
        resources.compute.bindBuffer(BattleArenaGpuBoneSsboManager.INVERSE_BIND_MATRIX_BINDING, asset.inverseBindMatrices);
        resources.compute.bindBuffer(BattleArenaGpuBoneSsboManager.OUTPUT_MATRIX_BINDING, new float[asset.boneCount * 16]);
        resources.compute.bindBuffer(BattleArenaGpuBoneSsboManager.LOCAL_REST_SCALE_BINDING, asset.localRestScales);
        resources.compute.bindBuffer(BattleArenaGpuBoneSsboManager.INSTANCE_STATE_BINDING, createInstanceState(clip, 0));
        System.out.println("GPU initialization complete.");
        System.out.println("Warming up GPU compute backend...");
        try {
            calculateGpuMatricesOptimized(gpuResources, gpuAsset, clip, 0);

            System.out.println("GPU warm-up complete.\n");
            System.out.println("Running GPU benchmark (" + iterations + " iterations)...");
            for (int iter = 0; iter < iterations; iter++) {
                int frameIndex = iter % clip.frameCount;
                long startTime = System.nanoTime();
                float[] gpuMatrices = calculateGpuMatricesOptimized(gpuResources, gpuAsset, clip, frameIndex);
                long endTime = System.nanoTime();
                
                gpuResults.addTime((endTime - startTime) / 1_000_000.0); // Convert to ms
                if (iter == 0) {
                    gpuResults.sampleMatrices = gpuMatrices;
                }
            }
        } finally {
            gpuResources.cleanup();
        }
        System.out.println("GPU benchmark complete.\n");

        // Validation
        System.out.println("=".repeat(80));
        System.out.println("VALIDATION: Comparing GPU vs CPU results (frame 0)");
        System.out.println("=".repeat(80));
        ComparisonResult validation = compare(cpuResults.sampleMatrices, gpuResults.sampleMatrices, 
                                             gpuAsset.boneNames, DEFAULT_TOLERANCE);
        System.out.println("Validation Status: " + (validation.passed ? "PASSED" : "FAILED"));
        System.out.println("  Max Absolute Difference: " + String.format("%.8f", validation.maxAbsDiff));
        System.out.println("  Worst Bone: " + validation.worstBoneName);
        System.out.println("  Worst Component: " + validation.worstComponent);
        System.out.println("  CPU Value: " + String.format("%.8f", validation.cpuValue));
        System.out.println("  GPU Value: " + String.format("%.8f", validation.gpuValue));
        System.out.println();

        // Performance Results
        System.out.println("=".repeat(80));
        System.out.println("PERFORMANCE RESULTS");
        System.out.println("=".repeat(80));
        
        cpuResults.print("CPU");
        System.out.println();
        gpuResults.print("GPU");
        System.out.println();

        double speedup = cpuResults.avgTime / gpuResults.avgTime;
        System.out.println("Performance Comparison:");
        System.out.println("  CPU Avg Time: " + String.format("%.4f ms", cpuResults.avgTime));
        System.out.println("  GPU Avg Time: " + String.format("%.4f ms", gpuResults.avgTime));
        System.out.println("  Speedup Factor: " + String.format("%.2fx", speedup));
        System.out.println("  Faster: " + (speedup > 1 ? "GPU (" + String.format("%.1f%%", (speedup - 1) * 100) + " faster)" : "CPU (" + String.format("%.1f%%", (1 - speedup) * 100) + " faster)"));
        System.out.println();

        if (!validation.passed) {
            System.out.println("[WARNING] Validation failed! GPU and CPU results differ beyond tolerance.");
            printBoneDiffSummary(cpuResults.sampleMatrices, gpuResults.sampleMatrices, gpuAsset.boneNames, 12);
        } else {
            System.out.println("[SUCCESS] GPU and CPU computations match within tolerance!");
        }
        
        System.out.println("=".repeat(80));
    }

    private static GpuComputeResources initializeGpuResources(GpuSkeletonAsset asset) {
        HeadlessContext context = new HeadlessContext(1, 1, "GPU vs CPU Benchmark");
        context.init();
        
        String shaderSource = ShaderProgram.loadShader(COMPUTE_SHADER);
        DesktopComputeBackend compute = new DesktopComputeBackend(shaderSource);
        if (compute.hasError()) {
            throw new IllegalStateException(compute.getError());
        }
        
        return new GpuComputeResources(context, compute, asset);
    }

    private static float[] calculateGpuMatricesOptimized(GpuComputeResources resources, GpuSkeletonAsset asset, Clip clip, int frameIndex) {
        resources.compute.updateBuffer(BattleArenaGpuBoneSsboManager.INSTANCE_STATE_BINDING,
                createInstanceState(clip, frameIndex));
        resources.compute.dispatch(1, 1, 1);
        return resources.compute.readBuffer(BattleArenaGpuBoneSsboManager.OUTPUT_MATRIX_BINDING);
    }

    private static float[] calculateGpuMatrices(GpuSkeletonAsset asset, Clip clip, int frameIndex) {
        HeadlessContext context = new HeadlessContext(1, 1, "GPU vs CPU Benchmark");
        DesktopComputeBackend compute = null;
        try {
            context.init();

            String shaderSource = ShaderProgram.loadShader(COMPUTE_SHADER);
            compute = new DesktopComputeBackend(shaderSource);
            if (compute.hasError()) {
                throw new IllegalStateException(compute.getError());
            }

            // compute.bindBuffer(BattleArenaGpuBoneSsboManager.METADATA_BINDING, createMetadata(asset));
            // compute.bindBuffer(BattleArenaGpuBoneSsboManager.LOCAL_REST_POSITION_BINDING, asset.localRestPositions);
            // compute.bindBuffer(BattleArenaGpuBoneSsboManager.LOCAL_ROTATION_BINDING, asset.rotations);
            // compute.bindBuffer(BattleArenaGpuBoneSsboManager.INVERSE_BIND_MATRIX_BINDING, asset.inverseBindMatrices);
            // compute.bindBuffer(BattleArenaGpuBoneSsboManager.OUTPUT_MATRIX_BINDING, new float[asset.boneCount * 16]);
            // compute.bindBuffer(BattleArenaGpuBoneSsboManager.LOCAL_REST_SCALE_BINDING, asset.localRestScales);
            compute.dispatch(1, 1, 1);
            return new float[86 * 16];
        } finally {
            if (compute != null) {
                compute.release();
            }
            context.destroy();
        }
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

    private static float[] calculateCpuMatrices(File resourceRoot, ArrayList<Bone> bones, Bone rootBone, 
                                                Map<String, KeyframeAnimation> animations, String clipName, int frameIndex)
            throws IOException, ClassNotFoundException {
        // Reset bone state
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
        System.out.println("[GPUvsCPUBenchmark] First bone diffs:");
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
                    + " maxDiff=" + String.format("%.8f", maxDiff)
                    + " component=" + maxComponent
                    + " cpuT=(" + String.format("%.4f", cpu[translationOffset])
                    + ", " + String.format("%.4f", cpu[translationOffset + 1])
                    + ", " + String.format("%.4f", cpu[translationOffset + 2])
                    + ") gpuT=(" + String.format("%.4f", gpu[translationOffset])
                    + ", " + String.format("%.4f", gpu[translationOffset + 1])
                    + ", " + String.format("%.4f", gpu[translationOffset + 2])
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
        private ArrayList<Double> times = new ArrayList<>();
        float[] sampleMatrices;
        
        double minTime = Double.MAX_VALUE;
        double maxTime = 0;
        double avgTime = 0;
        double totalTime = 0;

        void addTime(double timeMs) {
            times.add(timeMs);
            minTime = Math.min(minTime, timeMs);
            maxTime = Math.max(maxTime, timeMs);
            totalTime += timeMs;
        }

        void computeStats() {
            if (times.isEmpty()) return;
            avgTime = totalTime / times.size();
            
            // Calculate standard deviation
            double sumSquaredDiff = 0;
            for (double time : times) {
                double diff = time - avgTime;
                sumSquaredDiff += diff * diff;
            }
            double stdDev = Math.sqrt(sumSquaredDiff / times.size());
        }

        void print(String label) {
            computeStats();
            System.out.println(label + " Performance Metrics:");
            System.out.println("  Total Time: " + String.format("%.4f ms", totalTime));
            System.out.println("  Average Time: " + String.format("%.4f ms", avgTime));
            System.out.println("  Min Time: " + String.format("%.4f ms", minTime));
            System.out.println("  Max Time: " + String.format("%.4f ms", maxTime));
        }
    }

    private static final class GpuComputeResources {
        final HeadlessContext context;
        final DesktopComputeBackend compute;

        GpuComputeResources(HeadlessContext context, DesktopComputeBackend compute, GpuSkeletonAsset asset) {
            this.context = context;
            this.compute = compute;
        }

        void cleanup() {
            if (compute != null) {
                compute.release();
            }
            context.destroy();
        }
    }
}
