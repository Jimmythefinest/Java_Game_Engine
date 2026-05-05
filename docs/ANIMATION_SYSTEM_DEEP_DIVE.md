# Animation System Deep Dive

This document details the architecture and workflows of the skeletal animation system within `engine-core`. The system is built for performance and parallelism, evaluating keyframes asynchronously and efficiently piping bone transforms to the GPU via Shader Storage Buffer Objects (SSBO).

## 1. Bone and Skeleton Hierarchy

### `Bone.java`
The foundational element of the skeletal system.
*   **Properties**: Maintains a transform hierarchy including `position_to_parent`, `rotation`, and `scale`. It tracks its hierarchical `global_position` and `global_rotation` through quaternions (`parent_orientation`, `local_orientation`).
*   **Bind Pose**: Stores an `inverse_bindpose` matrix, which is calculated initially and used to transform vertices from model space into bone space.
*   **Animation Matrix**: `getAnimationMatrix()` constructs the final skinning matrix by translating, rotating, and scaling by the *current* animated properties, and multiplying it by the `inverse_bindpose`.

### `Skeleton.java`
Manages a hierarchy of `Bone`s.
*   **Storage**: Retains a reference to the `root_bone` and maintains a flat `ArrayList<Bone> bones` for efficient linear iteration.
*   **Binding (`Skeletal_Animation`)**: Contains the `Skeletal_Animation` inner class which maps a `Map<String, Animation>` (e.g., loaded from an FBX) to the actual `Bone` objects based on their names. It includes aggressive string normalization to handle `Assimp` artifacts (e.g., stripping `$assimpfbx$rotation`) and standardizing Mixamo rigs.

## 2. Animation Logic

### `Animation.java` and `KeyframeAnimation.java`
*   **Base Class**: `Animation.java` provides the core API: `animate(deltaSeconds)`, `start()`, `stop()`, and an `onfinish` callback.
*   **Keyframes**: `KeyframeAnimation.java` holds a chronological list of `Keyframe` objects (time, position, rotation).
*   **Interpolation**: Determines the previous and next keyframes based on the current `time`. It calculates interpolation (`t`) and uses Spherical Linear Interpolation (`Quaternion.slerp()`) to compute smooth bone rotations.

## 3. Parallel Execution (`ParallelKeyframeAnimator.java`)

To avoid CPU bottlenecks when processing hundreds of bones, animation evaluation is offloaded to a worker thread pool.
*   **Thread Pool**: Creates daemon worker threads (up to the number of available CPU cores).
*   **Job Creation**: Batches active `KeyframeAnimation` instances into `AnimationJob`s. 
*   **Worker Evaluation**: Worker threads perform the heavy math (quaternion conversions and slerping). They return an `AnimationResult`.
*   **Thread Safety**: The `AnimationResult`s are gathered, and only the main thread applies the resulting rotations back to the `Bone` objects. This guarantees that the scene graph remains thread-safe while still gaining the performance benefits of multi-threaded math.

## 4. GPU Synchronization (`BoneSsboManager.java`)

Once all animations are applied and the bone graph is updated, the engine must send the new transforms to the graphics pipeline.
*   **Manager Role**: `BoneSsboManager` (implementing `BoneMatrixSsboManager`) is responsible for allocating and uploading bone data.
*   **Packing**: It iterates through all registered `Skeleton`s, calls `getAnimationMatrix()` on each `Bone`, and packs these $4\times4$ matrices sequentially into a single `float[]` array.
*   **SSBO Upload**: This array is uploaded to an SSBO (`BufferHandle`) using `GL_DYNAMIC_DRAW` and bound to **binding point 2**. 
*   **Shader Skinning**: Vertex shaders read this buffer using `layout(std430, binding = 2) buffer BoneMatrices { mat4 bones[]; };` to perform hardware skinning based on vertex bone indices and weights.
