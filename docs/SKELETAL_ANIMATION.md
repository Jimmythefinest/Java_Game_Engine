# Skeletal Animation System

The skeletal animation system enables complex character animations by deforming meshes based on a bone hierarchy.

## Core Components

### 1. [Skeleton](../src/com/njst/gaming/skeleton/Skeleton.java)
- **Role**: Manages the hierarchy of `Bone` objects. 
- **Storage**: Holds a flat list of bones for easy indexing and a tree structure for hierarchical transformations.

### 2. [SkeletalAnimationController](../src/com/njst/gaming/Animations/SkeletalAnimationController.java)
- **Role**: Bridges the CPU-side skeleton data with the GPU.
- **SSBO Sync**: Every frame (via `update()`), it calculates the final transformation matrix for each bone and uploads the entire array to a Shader Storage Buffer Object (SSBO).
- **Binding**: Binds the buffer to a specific binding point (e.g., binding point 2) in the vertex shader.

### 3. [SkeletalModelLoader](../src/com/njst/gaming/Loaders/SkeletalModelLoader.java)
- **Role**: A factory for setting up skeletal models and animations from FBX files.
- **Initialization**: Automatically handles skeleton extraction, weighted mesh loading, and controller attachment.

### 4. [Skeletal_Animation](../src/com/njst/gaming/Animations/Skeletal_Animation.java)
- **Role**: Represents a single animation (e.g., "Idle", "Run") extracted from a source file.
- **Mapping**: Animations are mapped to specific bones in a `Skeleton` based on bone names.

---

## Technical Workflow

### 1. Loading a Character
Characters are loaded using the `SkeletalModelLoader`. This returns a `Weighted_GameObject` which contains the necessary GPU buffers and a linked `SkeletalAnimationController`.

```java
// Load model with texture and scale, and bind bones to SSBO index 2
Weighted_GameObject character = SkeletalModelLoader.load("/models/character.fbx", texture, 1.0f, 2);
Skeleton skeleton = character.getAnimationController().getSkeleton();
```

### 2. Loading and Playing Animations
Animations are loaded separately and mapped to the existing skeleton.

```java
// Load the "idle" animation (index 0 in FBX)
Skeletal_Animation idle = SkeletalModelLoader.loadAnimation("/models/character.fbx", "idle", 0, 1.0f, skeleton);
idle.start();
```

### 3. Rendering and Updates
The `Weighted_GameObject` automatically calls `animationController.bind()` during its `render()` call. However, the controller must be updated every frame to sync bone transformations to the GPU.

```java
// In your engine loop / onUpdate()
character.getAnimationController().update();
```

---

## Shader Integration
The vertex shader expects the bone matrices in an SSBO at the specified binding point.

```glsl
layout(std430, binding = 2) buffer BoneMatrices {
    mat4 bones[];
};

// In vertex shader calculation:
mat4 boneTransform = bones[boneIndex0] * weight0;
boneTransform     += bones[boneIndex1] * weight1;
// ... apply to position
```
