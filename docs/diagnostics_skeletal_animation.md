# Diagnostics: Skeletal Animation System Regression

This report outlines the technical discrepancies between the old (working) logic and the new (broken) skeletal animation system.

## 1. Missing Transformation Propagation
**Issue:** The new `SkeletalAnimationController.update()` method only handles GPU synchronization but fails to trigger the CPU-side bone transformation hierarchy update.

- **Old Logic:** The old `DefaultLoader` implementation implicitly triggered updates or relied on direct bone manipulation that cascaded.
- **New Logic:** `SkeletalAnimationController.update()` calls `updateGPU()`, but it **never** calls `skeleton.root_bone.update()`. Without this call, bone rotations applied by keyframe animations never propagate down the hierarchy, resulting in static or incorrectly placed bones.

## 2. Redundant Shader Compilation
**Issue:** `Weighted_GameObject` compiles a new instance of `vert111.glsl` and `frag111.glsl` for every object instance in `generateBuffers()`.

- **Impact:** This is highly inefficient and results in each object having its own private shader program.
- **Uniform Mismatch:** The `Renderer` only sets uniforms (like `eyepos1`) on its main `shaderProgram`. `Weighted_GameObject.render()` does not synchronize these uniforms with its own `program1`.

## 3. Shader Standard Mismatch
**Issue:** `vert111.glsl` uses `uMMatrix` (model matrix) on top of bone transformations.

- **Risk:** If the bone matrices already include the world-space transformation (which they do if `global_position` is used in `Bone.getAnimationMatrix()`), then multiplying by `uMMatrix` in the shader applies the object's transform twice.

## 4. Animation Mapping
- **Comparison:** The old `DefaultLoader` had much more aggressive mapping logic, including stripping underscores and colons from bone names to ensure matches. The new `SkeletalModelLoader` is stricter and might fail to map animations if naming conventions differ slightly between the FBX and the code.

## Recommendations
1.  **Update Hierarchy:** Modify `SkeletalAnimationController.update()` to call `skeleton.root_bone.update()` before `updateGPU()`.
2.  **Shared Shaders:** Move shader management out of `Weighted_GameObject` and into a shared location or the `SkeletalModelLoader`.
3.  **Uniform Sync:** Ensure `Weighted_GameObject` receives camera and lighting data from the main `Renderer`.
