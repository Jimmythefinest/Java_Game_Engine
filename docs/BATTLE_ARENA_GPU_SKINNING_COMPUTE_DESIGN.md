# Battle Arena GPU Bone Compute Design

## Goal

Move Battle Arena bone pose evaluation off the CPU and onto the GPU:

- Upload all animation rotation keyframes into persistent SSBOs.
- Use one compute dispatch per simulation tick to calculate all active character bone matrices.
- Use one workgroup per character.
- Evaluate skeleton transforms depth by depth with workgroup barriers.
- Reuse the existing skinned vertex shader path by writing final bone skinning matrices into the current `BoneData` SSBO shape.

The target is to remove per-tick Java allocations and CPU bone traversal from the combat hot path while keeping rendering code simple.

## Current State

Runtime today does this on the CPU:

1. `BattleArenaDemoLoader` fixed tick calls `ParallelKeyframeAnimator.animateSkeletons(...)`.
2. Each active `KeyframeAnimation` interpolates rotation and mutates a `Bone`.
3. `BattleArenaCharacterRuntime.syncRig()` updates the root transform.
4. `Scene.uploadSkeletonBuffer(...)` packs every `bone.getAnimationMatrix()` into a new `float[]`.
5. `vert111.glsl` reads `layout(std430, binding = 2) buffer BoneData { mat4 bone[]; }`.

This creates significant tick-time garbage:

- animation job/result objects,
- quaternion/vector temporaries,
- recursive `Bone.update()` objects,
- a freshly packed CPU bone matrix array.

The GPU design replaces steps 1-4 for Battle Arena characters.

## High-Level Architecture

Add a Battle Arena GPU skeleton system owned near the Battle Arena character runtime layer:

- `BattleArenaGpuSkeletonAsset`
  Immutable packed skeleton and animation data shared by all instances of the same character asset.

- `BattleArenaGpuSkeletonInstance`
  Per-character state: current animation, frame, root position, heading, output bone base index.

- `BattleArenaGpuSkeletonSystem`
  Owns compute shader, SSBOs, instance registry, uploads per-tick instance state, dispatches compute, and binds output bones to binding `2`.

- `battle_arena_bone_compute.glsl`
  Compute shader that writes all final skinning matrices.

The render vertex shader should not need a major redesign. It can continue reading `bone[boneStartIndex + bones[i]]`.

## Buffer Layout

Use std430-compatible layouts and avoid `bool` fields. Prefer `vec4` and `ivec4` alignment.

### Binding 2: Output Bone Matrices

Existing shader-facing output:

```glsl
layout(std430, binding = 2) buffer BoneData {
    mat4 bone[];
};
```

One final skinning matrix per character bone:

```text
globalBoneIndex = instance.outputBoneOffset + localBoneIndex
bone[globalBoneIndex] = globalModelMatrix * inverseBindPose
```

This should be persistent GPU storage. We should stop rebuilding it from CPU bone objects each frame for GPU-driven characters.

### Binding 3: Bone Metadata

One record per bone in the shared skeleton asset:

```glsl
struct BoneMeta {
    int parentIndex;
    int depth;
    int firstChild;
    int childCount;
    vec4 localRestPosition;  // xyz used
    vec4 inverseBind0;
    vec4 inverseBind1;
    vec4 inverseBind2;
    vec4 inverseBind3;
};
```

`parentIndex = -1` for root.

Depth is precomputed offline/on load. The compute shader uses depth passes so parent globals exist before children read them.

### Binding 4: Animation Clip Metadata

One record per animation clip:

```glsl
struct ClipMeta {
    int rotationOffset;      // offset into Binding 5, in vec4 quaternions
    int frameCount;
    int boneCount;
    float framesPerSecond;
};
```

Rotation storage is clip-major:

```text
rotationIndex = clip.rotationOffset + frame * clip.boneCount + boneIndex
```

Every clip should contain one rotation quaternion per bone per sampled frame. If an imported clip has no curve for a bone, bake/rest-fill that bone during export.

### Binding 5: Animation Rotations

All animation frames, rotations only:

```glsl
layout(std430, binding = 5) buffer AnimationRotations {
    vec4 rotationQuat[]; // xyzw
};
```

Use normalized quaternions. The exporter should convert existing Euler keyframes into sampled quaternions at the clip sample rate.

Why sampled rotations rather than sparse keyframes:

- The shader becomes simple and branch-light.
- We avoid variable-length curve lookup per bone.
- The current game already runs fixed 60 Hz ticks, and Battle Arena clips are modest.

Later optimization: store sparse keys plus per-bone curve ranges if memory becomes a problem.

### Binding 6: Character Instance State

One record per active character:

```glsl
struct CharacterPoseState {
    int clipIndex;
    int outputBoneOffset;
    int boneMetaOffset;
    int boneCount;
    float frame;
    float headingRadians;
    vec2 _pad0;
    vec4 rootPosition; // xyz used
};
```

`frame` can be fractional. The shader samples `floor(frame)` and `floor(frame)+1`, then slerps/nlerps.

### Binding 7: Scratch Global Bone Matrices

Global transform per character bone, before inverse bind:

```glsl
layout(std430, binding = 7) buffer GlobalBoneMatrices {
    mat4 globalBone[];
};
```

This can be the same size as Binding 2. It exists because children need parent global matrices while final output needs `global * inverseBind`.

### Binding 8: Dispatch Constants

Small config buffer or uniforms:

```glsl
uniform int maxDepth;
uniform int maxBonesPerCharacter;
```

If our compute wrapper does not support uniforms yet, use a tiny SSBO:

```glsl
struct DispatchConfig {
    int maxDepth;
    int maxBonesPerCharacter;
    int characterCount;
    int _pad;
};
```

## Compute Shader Execution Model

Dispatch shape:

```text
glDispatchCompute(characterCount, 1, 1)
```

Workgroup:

```glsl
layout(local_size_x = MAX_BONES_PER_CHARACTER) in;
```

One workgroup = one character.

One invocation = one local bone slot. If `localBoneIndex >= boneCount`, it idles but still participates in barriers.

### Depth-by-Depth Algorithm

Inside each workgroup:

```glsl
uint characterIndex = gl_WorkGroupID.x;
uint localBoneIndex = gl_LocalInvocationID.x;

for (int depth = 0; depth <= maxDepth; depth++) {
    if (localBoneIndex < boneCount && boneMeta.depth == depth) {
        local = TRS(localRestPosition, sampledRotation, vec3(1));

        if (parentIndex < 0) {
            global = rootTransform * local;
        } else {
            global = globalBone[outputOffset + parentIndex] * local;
        }

        globalBone[outputOffset + localBoneIndex] = global;
        bone[outputOffset + localBoneIndex] = global * inverseBindPose;
    }

    barrier();
    memoryBarrierShared();
    memoryBarrierBuffer();
}
```

Important: `barrier()` only synchronizes invocations inside one workgroup. That is exactly why one workgroup owns one character.

Use `memoryBarrierBuffer()` before child depth reads parent matrices from SSBO. If using `shared` memory for global matrices, use `memoryBarrierShared()` and write final matrices after all depths.

### Shared Memory Variant

For better performance:

```glsl
shared mat4 sharedGlobal[MAX_BONES_PER_CHARACTER];
```

Then:

- write parent/child globals into `sharedGlobal`,
- use `barrier()` between depths,
- write final `bone[]` once the local bone is computed.

This avoids round-tripping parent matrices through SSBO during the depth loop.

Constraint: `MAX_BONES_PER_CHARACTER * sizeof(mat4)` must fit device shared memory limits. With 128 bones, that is 8 KB, usually fine.

## Animation Sampling

The CPU controller remains authoritative for gameplay state:

- current animation key,
- current animation frame,
- root position,
- heading.

Each fixed tick updates `CharacterPoseState` only. Bone interpolation happens in the compute shader.

Sampling approach:

```glsl
int f0 = clamp(int(floor(frame)), 0, frameCount - 1);
int f1 = min(f0 + 1, frameCount - 1);
float t = fract(frame);
quat q = normalize(nlerp(q0, q1, t));
```

Use nlerp initially. It is cheaper than slerp and usually acceptable at dense sampled frames. If twisting artifacts appear, switch to slerp.

The Java exporter should produce quaternions with sign continuity:

```text
if dot(previousQuat, currentQuat) < 0, currentQuat = -currentQuat
```

This prevents interpolation taking the long path.

## Asset Bake

Add a new asset exporter step, separate from the hitbox track exporter:

```bash
gradle :battle-arena-core:exportBattleArenaGpuSkeletonAsset
```

Output options:

- JSON for easy inspection at first:
  `battle_arena/defeated.gpu_skeleton.json`

- Later binary for startup speed:
  `battle_arena/defeated.gpu_skeleton.bin`

The asset should include:

- bone names,
- parent index per bone,
- depth per bone,
- rest local position,
- inverse bind matrices,
- clip list,
- sampled rotation quaternions per clip/frame/bone,
- max depth,
- max bones.

The existing hitbox track exporter already samples animation pose offline. It can share code with the GPU skeleton exporter.

## Engine/API Changes Needed

The current `ComputeBackend` is too neural-network-specific. It creates buffers internally and only exposes `dispatch` with an automatic storage barrier.

Add lower-level graphics/compute APIs:

```java
interface GraphicsDevice {
    ComputeProgramHandle createComputeProgram(String shaderSource);
    void dispatchCompute(ComputeProgramHandle program, int x, int y, int z);
    void memoryBarrier(int barrierBits);
}
```

Or extend `ComputeBackend`:

```java
void bindExistingBuffer(int bindingIndex, BufferHandle buffer);
void use();
void dispatch(int x, int y, int z, int memoryBarrierBits);
void setUniformInt(String name, int value);
```

Also improve `BufferHandle`:

```java
void updateData(int[] data);
void updateData(float[] data, int elementOffset);
void setData(byte[] data, int usage); // optional for packed structs
```

Desktop implementation maps to OpenGL 4.3:

- `glCreateShader(GL_COMPUTE_SHADER)`
- `glDispatchCompute`
- `glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT | GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT)`

Android implementation maps to GLES 3.1:

- `GLES31.GL_COMPUTE_SHADER`
- `GLES31.glDispatchCompute`
- `GLES31.glMemoryBarrier(...)`

## Integration Plan

## Build Sub-Goals and Checkpoints

This section is the working checklist for total integration. Each sub-goal should be landed as a small commit with its validation evidence in the commit message or PR notes.

### Sub-Goal 0: Baseline and Safety Nets

Status: partially done.

Deliverables:

- Desktop compute smoke test task:
  `./gradlew :engine-platform-desktop:runComputeShaderSmokeTest`
- CPU bone benchmark task:
  `./gradlew :engine-platform-desktop:runBoneCpuBenchmark`
- Android instrumentation benchmark:
  `./gradlew :engine-platform-android:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.njst.gaming.android.BoneCpuBenchmarkInstrumentedTest`
- Feature flag policy:
  GPU bones must be opt-in until visual parity is proven.

Exit criteria:

- Desktop compute shader SSBO dispatch/readback passes.
- Desktop and Android CPU bone benchmarks run and produce recorded numbers.
- `gradle build` passes.

Current reference numbers:

- Desktop, 96 bones after CPU optimization:
  `updateOnly = 16.340 us`, `updateAndPack = 48.309 us`.
- Android SM-M145F, 96 bones after CPU optimization:
  `updateOnly = 381.585 us`, `updateAndPack = 1602.187 us`.

### Sub-Goal 1: GPU Skeleton Asset Exporter

Purpose: create the static data the compute shader will consume.

Status: complete.

Deliverables:

- `BattleArenaGpuSkeletonAssetExporter`.
- Gradle task:
  `gradle :battle-arena-core:exportBattleArenaGpuSkeletonAsset`
- Generated asset:
  `battle-arena-core/src/main/resources/battle_arena/defeated.gpu_skeleton.json`

Asset must contain:

- bone names in runtime order,
- parent index for each bone,
- depth for each bone,
- max depth,
- max bones,
- local rest position per bone,
- local rest scale per bone,
- inverse bind pose matrix per bone,
- clip metadata,
- sampled quaternion rotations per clip/frame/bone.

Validation:

- Exporter logs bone count, max depth, clip count, and total quaternion count.
- All configured animations in `defeated.character.json` are present.
- Every clip has `frameCount * boneCount` quaternion records.
- Quaternion sign continuity is applied per bone/clip.

Stop/go:

- Complete: exporter produced 86 bones, max depth 14, 11 clips, and 123668 sampled quaternions.

### Sub-Goal 2: Compute Shader API Completion

Purpose: make the compute layer able to support persistent skeletal SSBOs rather than neural-network-only buffer ownership.

Deliverables:

- `ComputeBackend` supports:
  - `bindBuffer(int, float[])`,
  - `bindBuffer(int, int[])`,
  - `updateBuffer(int, float[])`,
  - `updateBuffer(int, int[])`,
  - future or immediate support for existing `BufferHandle` binding if needed.
- Desktop and Android implementations stay API-compatible.
- Existing compute smoke test still passes.

Validation:

- `./gradlew :engine-platform-desktop:runComputeShaderSmokeTest`
- `gradle build`

Stop/go:

- If Android compute APIs compile but are not runtime-tested, keep Android GPU bones disabled by default with a clear log.

### Sub-Goal 3: CPU Reference Matrix Capture

Purpose: create a reliable reference for GPU matrix comparison.

Status: complete for the headless Battle Arena comparison path.

Deliverables:

- A diagnostic-only CPU method that packs final bone matrices for one Battle Arena character without changing gameplay.
- A comparison utility:
  - accepts CPU matrices,
  - accepts GPU readback matrices,
  - reports max absolute difference and first mismatching bone.

Validation:

- With CPU path active, reference matrix capture equals current `Scene.uploadSkeletonBuffer(...)` output for the character.
- Implemented as `BattleArenaGpuBoneCompare`, which loads the serialized CPU bones and animation map, computes CPU matrices, runs the compute shader in a headless desktop context, reads back GPU matrices, and reports max absolute difference.

Stop/go:

- Complete for compute proofing: idle frame 0 compared at max difference `8.34465E-7`.

### Sub-Goal 4: First Compute Shader Prototype

Purpose: prove one character, one clip, one frame.

Status: complete for one character in headless desktop mode.

Deliverables:

- `battle_arena_bone_compute.glsl`.
- Headless comparison runner:
  `gradle :battle-arena-desktop:runBattleArenaGpuBoneCompare`
- Persistent buffers:
  - bone metadata,
  - baked rotation data,
  - local rest positions,
  - local rest scales,
  - inverse bind matrices,
  - rotations,
  - output matrices.
- Debug readback method.

Scope:

- One character.
- Idle clip only is acceptable.
- One frame or frozen frame is acceptable.
- CPU rendering can still be active; this step only proves compute output.

Validation:

- GPU output matrix count equals bone count.
- Root and first child matrices are close to CPU reference.
- Max matrix difference is logged.

Stop/go:

- Complete: transform conventions were matched to CPU `Bone` behavior. Parent hierarchy propagates translation and rotation; local scale is applied only when packing each final bone matrix.

### Sub-Goal 5: Depth-by-Depth Full Skeleton Compute

Purpose: calculate the whole hierarchy in one workgroup per character.

Status: complete for one character; multi-character dispatch remains Sub-Goal 6.

Deliverables:

- Depth loop in compute shader.
- Workgroup barriers between depth passes.
- Shared memory path preferred:
  `shared mat4 sharedGlobal[MAX_BONES_PER_CHARACTER]`.
- SSBO fallback path only if shared memory causes platform issues.

Validation:

- Compare all bones against CPU reference for:
  - idle frame 0,
  - idle mid-frame,
  - punch mid-frame,
  - kick mid-frame,
  - walk mid-frame.
- Log max difference per clip.

Stop/go:

- Current headless checks pass with max differences under `0.01`:
  idle frame 0 `8.34465E-7`, idle frame 90 `8.34465E-7`, punch frame 30 `9.536743E-7`, kick frame 48 `1.4305115E-6`, walk frame 30 `7.1525574E-7`.

### Sub-Goal 6: Multi-Character Dispatch

Purpose: dispatch all active Battle Arena characters in one compute call.

Deliverables:

- One workgroup per active character.
- Instance state buffer updated once per tick.
- Output bone offsets assigned per character.

Validation:

- Two connected characters get different output bone ranges.
- Both characters render/compare correctly when assigned different animations.
- No CPU `Bone.update()` required for GPU-driven characters during the tick.

Stop/go:

- Verify character 1 and character 2 do not overwrite each other’s bone ranges.

### Sub-Goal 7: Render Integration Behind Feature Flag

Purpose: let the actual game render from GPU-computed matrices.

Deliverables:

- Feature flag:
  `-DbattleArena.gpuBones=true`
- `Scene.uploadSkeletonBuffer(...)` must skip CPU bone upload when the GPU skeleton provider owns binding `2`.
- `Weighted_GameObject.boneBufferStartIndex` points into GPU output matrix ranges.
- CPU fallback remains the default.

Validation:

- CPU path still works with flag off.
- GPU path renders one local character with flag on.
- Shadow pass also uses the same GPU-computed matrices.

Stop/go:

- If shadow pass breaks, keep GPU path disabled for shadows or route shadow shader through same binding fix before continuing.

### Sub-Goal 8: Runtime Animation State Integration

Purpose: make gameplay animation state drive the GPU instance buffer.

Deliverables:

- Controller exposes:
  - current animation key,
  - current frame,
  - root position,
  - heading.
- GPU system maps animation key to clip index.
- Per tick, update instance state only; do not traverse bones on CPU.

Validation:

- Idle, walk, run, jump, punch, kick, cast, side-step, and take-hit all select expected clips.
- Animation frame progression matches CPU path timing.

Stop/go:

- If event animations finish at different times than CPU path, fix frame source before tuning visuals.

### Sub-Goal 9: Android Runtime Trial

Purpose: prove the Android GLES 3.1 path can run the shader and improve the current bottleneck.

Deliverables:

- Android shader asset synced.
- Android compute backend can create/update all required SSBOs.
- Device-side smoke command documented.

Validation:

- Run battle arena with GPU bones flag on-device.
- Compare FPS/profiler before and after on the same scene.
- Check logcat for GPU fallback/errors.

Stop/go:

- If Android compute shader compiles but render output is wrong, keep desktop GPU path available and Android fallback active while debugging.

### Sub-Goal 10: Cleanup and Removal of CPU Hot Path

Purpose: remove the old CPU costs for Battle Arena after GPU parity is proven.

Deliverables:

- Battle Arena GPU characters do not register into CPU `Scene.skeletons`, or CPU upload ignores them.
- No `ParallelKeyframeAnimator.animateSkeletons(...)` call for GPU-driven characters.
- CPU `Bone` objects remain only for:
  - export tools,
  - CPU fallback,
  - debug comparison.

Validation:

- Allocation profiling shows no per-tick Java bone traversal/matrix packing for GPU-driven Battle Arena.
- CPU fallback still passes build and runs.

Stop/go:

- Do not delete CPU fallback until Android and desktop GPU paths have both been played through normal combat interactions.

### Phase 1: Data Bake

- Create GPU skeleton exporter.
- Generate bone metadata and sampled rotation tracks.
- Verify CPU and baked sampled poses produce similar root-relative hitbox positions.

### Phase 2: GPU Buffer Ownership

- Add `BattleArenaGpuSkeletonSystem`.
- Allocate persistent SSBOs:
  - bone metadata,
  - clip metadata,
  - rotation data,
  - instance state,
  - global scratch,
  - output bone matrices.
- Bind output matrix buffer to binding `2`.

### Phase 3: Compute Shader

- Add `battle_arena_bone_compute.glsl`.
- Start with one character and one clip.
- Read back output matrices in debug mode and compare against CPU matrices.

### Phase 4: Runtime Switch

- Add feature flag:

```text
battleArena.gpuBones=true
```

- If disabled or unsupported, keep CPU path.
- If enabled, skip `ParallelKeyframeAnimator.animateSkeletons(...)` and CPU `syncRig()` bone recursion for GPU-driven characters.
- Still keep CPU root position/heading for gameplay and camera.

### Phase 5: Render Path

- Keep `vert111.glsl` reading binding `2`.
- Ensure `boneStartIndex` points into GPU output buffer.
- `Scene.uploadSkeletonBuffer(...)` must not overwrite binding `2` after the compute system writes it.

Likely solution:

- Add a `SkeletonBufferProvider` concept.
- CPU provider builds current `float[]`.
- Battle Arena GPU provider owns binding `2`.

### Phase 6: Cleanup

- Remove CPU active skeleton collection for GPU characters.
- Avoid registering GPU-driven skeletons into the CPU `Scene.skeletons` list.
- Keep CPU bones only for editor/export/debug fallback.

## Synchronization and Barriers

There are two barrier layers:

1. Inside compute shader:
   - `barrier()` between skeleton depths.
   - `memoryBarrierShared()` if using shared memory.
   - `memoryBarrierBuffer()` if reading parent globals from SSBO.

2. After dispatch:
   - Java/OpenGL call to `glMemoryBarrier`.
   - Use at least `GL_SHADER_STORAGE_BARRIER_BIT`.
   - If vertex shader immediately reads the SSBO, include `GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT` conservatively on desktop or the GLES equivalent where available.

The dispatch must happen before draw calls for skinned objects.

## Constraints and Risks

- Workgroup local size must be a compile-time constant.
  Pick an initial `MAX_BONES_PER_CHARACTER`, such as `128`.

- If a character exceeds max bones, fallback to CPU or compile/load another shader variant.

- OpenGL/GLES shared memory limits vary.
  Query limits later; document initial assumption in logs.

- Existing animation data is Euler-based.
  Exporter should convert to quaternions once, not on GPU every tick.

- Existing CPU hitbox tracks are already baked.
  Runtime collision does not need live GPU bone readback.

- Debug readbacks are expensive.
  Use only behind a diagnostic flag.

## Acceptance Criteria

- One compute dispatch updates all active Battle Arena character bone matrices per tick.
- Vertex shader consumes computed matrices through existing binding `2`.
- CPU path remains available and buildable.
- No per-tick Java bone traversal for GPU-driven characters.
- No per-tick Java allocation for bone matrices.
- Visual pose matches CPU path within acceptable tolerance for idle, walk, run, punch, kick, jump, and hit reactions.
- Android and desktop both compile or cleanly fallback with a clear log message.

## Open Questions

- Do we want one shared skeleton asset per character definition, or one global mega-buffer for all character types?
- Should root motion stay fully CPU-authored, as now, or should animation root translation eventually feed gameplay?
- Should hitbox tracks remain CPU-baked JSON, or be packed into the same GPU skeleton asset for one animation asset pipeline?
- Do we want to retire `Bone` objects for runtime Battle Arena characters entirely after the GPU path is stable?
