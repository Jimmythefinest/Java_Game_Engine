# Engine Core Documentation

This document describes the `engine-core` module, its responsibilities, and the main extension points used by platform backends (`engine-platform-desktop`, Android platform, future Vulkan backend, etc.).

## Module Purpose

`engine-core` contains platform-agnostic game engine logic:

- Scene graph and world orchestration (`Scene`, loaders, simulation hooks)
- Rendering flow orchestration (`Renderer`) through abstract graphics interfaces
- Entity representation (`GameObject` and geometry types)
- Core math (`Vector3`, `Matrix4`, `Quaternion`)
- Input state model (`input/*`)
- Physics and animation systems
- AI/simulation helpers

Build configuration for the module lives in `engine-core/build.gradle` and defines Java 8 compatibility with core dependencies (`gson`, `joml`).

## Directory Structure (engine-core)

- `com.njst.gaming`
  - `Renderer`, `Scene`, `Camera`, terrain/world managers, loaders
- `com.njst.gaming.graphics`
  - Graphics abstraction interfaces (`GraphicsDevice`, `ShaderHandle`, `BufferHandle`, `ComputeBackend`)
- `com.njst.gaming.objects`
  - `GameObject`, LOD and collision variants
- `com.njst.gaming.Geometries`
  - Built-in primitive and procedural mesh sources
- `com.njst.gaming.Animations`
  - Keyframe + procedural animation systems
- `com.njst.gaming.Physics`
  - Physics and collision helpers
- `com.njst.gaming.input`
  - Input states, codes, and binding map
- `com.njst.gaming.Math`
  - Math primitives used across engine systems
- `com.njst.gaming.ai` and `com.njst.gaming.simulation`
  - AI/agent systems used by simulation loaders

## Core Runtime Flow

1. A platform module creates `Renderer` with a concrete `GraphicsDevice` implementation.
2. `Renderer.onSurfaceCreated()` loads shaders, asks the active scene loader to populate objects, and initializes object buffers.
3. Every frame, `Renderer.onDrawFrame()`:
   - updates camera constants buffer (`ssbo`),
   - clears frame state through `GraphicsDevice`,
   - updates scene logic (`scene.onDrawFrame()`),
   - renders skybox and then scene objects.
4. `Renderer.onSurfaceChanged()` updates viewport and projection ratio.

## Graphics Abstraction Layer

The `com.njst.gaming.graphics` package is the main seam between core and platform code.

### `GraphicsDevice`

Platform rendering backend contract for:

- shader creation/loading
- texture allocation and release
- vertex/index buffer creation and upload
- draw submission (`drawElementsTriangles`, `drawElementsLines`)
- frame state operations (viewport, clear, blend/depth enable)
- utility values (`dynamicDrawUsage`)

### `ShaderHandle`

Backend shader program contract for:

- binding (`use`)
- uniform updates (vector/matrix)
- texture binding activation
- lifecycle (`compiled`, `cleanup`)

### `BufferHandle`

Generic buffer contract used for SSBO-style data transfer from `Renderer`.

### `ComputeBackend`

Abstraction for compute workflows used by GPU neural-network logic.

## Scene and Object Model

### `Scene`

Owns world objects, delegates loading through scene loaders, and runs per-frame simulation hooks.

### `GameObject`

Base renderable entity with:

- transform state (position/rotation/scale)
- geometry reference
- texture ID and material-like parameters (`shininess`, `ambientlight_multiplier`)
- GPU buffer lifecycle (`generateBuffers`, `cleanup`)
- animation callback list
- collision bounds generation from geometry

`GameObject` uses `GraphicsDevice` exclusively for buffer upload/draw calls, allowing backend substitution without changing scene code.

## Input Model

`InputSystem` stores per-frame button and pointer states. `InputBindings` maps platform key/mouse events to engine input codes. Platform modules are responsible for translating native events into this model.

## Extending engine-core for a New Platform Backend

To integrate a new platform renderer (for example, Vulkan):

1. Implement `GraphicsDevice` for the platform.
2. Implement `ShaderHandle` and `BufferHandle` used by that `GraphicsDevice`.
3. Provide `ComputeBackend` if compute features are needed by `GPUNeuralNetwork`.
4. Create a platform engine bootstrap that constructs `Renderer(new YourGraphicsDevice())`, then wires `Scene` and input callbacks.

## Notes and Current Constraints

- The abstraction is backend pluggable, but API shape is currently draw-call/attribute oriented.
- Some shader naming conventions are currently expected by `Renderer`/`GameObject` (for example, `uMMatrix`, `eyepos1`, `properties`), so backend shaders must provide compatible bindings.
- Asset and shader loading paths are currently established by platform implementations and loader conventions.

## Related Documentation

- `docs/RENDERER.md`
- `docs/SCENE.md`
- `docs/ENGINE_AND_INPUT.md`
- `docs/ENGINE_PLATFORM_EXPECTATIONS.md`
- `docs/AI_AND_SIMULATION.md`
- `docs/BUILD_AND_RUN.md`
