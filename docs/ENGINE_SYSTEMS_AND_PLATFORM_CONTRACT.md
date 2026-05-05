# Engine Systems and Platform Contract

This document provides a summary of the systems housed in `engine-core` and the contract required for platform-specific backends (such as Desktop/LWJGL or Android) to integrate with the engine.

## 1. Engine Core Systems

The `engine-core` module provides all platform-agnostic engine logic. It is responsible for orchestrating the game world, rendering logic, mathematics, and input state.

### `Scene` and `GameObject`
*   **`Scene`**: The world container. It owns the list of `GameObject`s, handles scene loading, and advances per-frame simulation hooks (like animations and physics).
*   **`GameObject`**: The base renderable entity. It maintains a transform (position, rotation, scale), geometry data, textures, and handles generating bounding volumes for collisions.

### `Renderer`
The `Renderer` orchestrates the rendering flow using the abstracted graphics interfaces.
*   It updates the `Camera` state and transfers it efficiently to the GPU (e.g., using SSBOs).
*   Every frame (`onDrawFrame`), it clears the screen, updates the scene, and walks through all visible `GameObject`s to issue draw calls via the active `GraphicsDevice`.

### `InputSystem`
The core engine reads input in a platform-agnostic way via `com.njst.gaming.input.InputSystem`.
*   Maintains `ButtonState` (for discrete keys/buttons) and `PointerState` (for mouse/touch coordinates).
*   Handles frame-by-frame edge detection (`beginFrame()`, `isKeyPressed()`, `isKeyDown()`).

### Physics & Animations
*   Built-in systems for collision detection, bounding volumes, and keyframe/procedural skeletal animations.

---

## 2. Engine-to-Platform Contract

To port the engine to a new platform (e.g., Desktop LWJGL, Android, or Vulkan), the platform backend must implement the abstraction layer defined in `com.njst.gaming.graphics` and translate native events.

### `GraphicsDevice`
This is the primary seam between the core engine and the platform backend. The backend must provide an implementation of `GraphicsDevice` that handles:
*   **Resource Creation**: Allocating textures, vertex arrays (VAO), and buffers (VBO/EBO).
*   **Shaders**: Compiling and loading vertex/fragment shaders and returning `ShaderHandle` instances.
*   **Drawing**: Submitting draw calls (e.g., `drawElementsTriangles()`, `drawElementsLines()`).
*   **Frame State**: Managing viewport size, depth testing, blending, and clearing frame buffers.
*   **Compute/Advanced**: Implementing `ComputeBackend` for GPU neural network evaluations or providing specialized bakers like `bakeImposter()`.

### Resource Handles
The backend implements opaque handles returned by `GraphicsDevice`:
*   **`ShaderHandle`**: Contract for binding a shader program and updating uniform variables (matrices, vectors) and textures.
*   **`BufferHandle`**: Represents an SSBO-style buffer for camera and light data transfer.
*   **`ShadowMapHandle`**: Encapsulates a framebuffer and depth texture for shadow rendering.

### Input Translation
Platform modules are responsible for capturing native events (like GLFW callbacks on Desktop or Android TouchEvents) and translating them into engine codes. These native events are fed into `InputSystem.button(code)` or `InputSystem.pointer(id)` to update the core state.

### Bootstrapping
A typical platform implementation does the following:
1. Initializes the native window and graphics context (e.g., GLFW + OpenGL).
2. Creates a concrete `GraphicsDevice` and instantiates the `Renderer` with it.
3. Wires up native input listeners to the `InputSystem`.
4. Enters a main loop where it repeatedly calls `renderer.onDrawFrame()`.
