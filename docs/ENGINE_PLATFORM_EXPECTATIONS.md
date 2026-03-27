# Engine Platform Expectations

This document defines what a platform module (desktop, Android, future Vulkan, etc.) is expected to provide so it can run `engine-core` correctly.

## What is an Engine Platform?

A platform module is the runtime adapter between OS/window/input/graphics APIs and the `engine-core` module.

Examples:

- `engine-platform-desktop`
- `android/engine-platform-android`
- future modules such as `engine-platform-vulkan`

## Required Responsibilities

A platform implementation is expected to provide all of the following.

### 1) Engine Lifecycle Bootstrapping

Platform code must:

1. Create and own the native app/window/context lifecycle.
2. Construct a `Scene` and a `Renderer`.
3. Inject a concrete `GraphicsDevice` into `Renderer`.
4. Wire `renderer.scene` and `scene.renderer`.
5. Call lifecycle methods in order:
   - `renderer.onSurfaceCreated()` once after graphics context is ready.
   - `renderer.onSurfaceChanged(width, height)` when size changes.
   - `renderer.onDrawFrame()` every frame.

### 2) Graphics Backend Contract

Platform code must implement `com.njst.gaming.graphics` interfaces:

- `GraphicsDevice`
- `ShaderHandle`
- `BufferHandle`
- (optional but recommended when needed) `ComputeBackend`

At minimum, the backend must support:

- shader loading/creation
- texture allocation/release
- vertex/index buffer upload and draw submission
- viewport and frame clear behavior
- depth/blend enable behavior consistent with engine expectations

## Shader Expectations

Current core rendering paths expect compatible shader bindings/conventions:

- model matrix uniform: `uMMatrix`
- camera position uniform: `eyepos1`
- material properties uniform: `properties`

If a backend uses a non-OpenGL binding model (for example Vulkan descriptors/push constants), it should map these expectations through backend glue/adapters.

## Input Expectations

Platform code is responsible for translating native input events into `engine-core` input state:

- key/button events -> `InputSystem` button states
- pointer/mouse movement -> pointer position and activity
- input mapping -> `InputBindings` lookup and dispatch

The platform loop should call `input.beginFrame()` once per frame after processing updates/render for consistent edge transitions.

## Timing and Frame Loop Expectations

A platform loop should:

1. Poll/process native events.
2. Run game update callbacks.
3. Render via `renderer.onDrawFrame()`.
4. Present/swap buffers.

Order can vary by platform, but it must be deterministic and consistent.

## Error Handling Expectations

Platform code should:

- fail fast when graphics context creation fails,
- avoid continuing render loop when renderer enters fatal error state (`renderer.hasError`),
- release native resources and backend handles on shutdown.

## Compatibility Expectations for New Backends

For backends such as Vulkan:

- Preserve behavior required by `Renderer` and `GameObject` even if internal implementation differs.
- Provide translation for draw-call and uniform-style APIs exposed by current interfaces.
- Keep backend-specific details inside the platform module; avoid leaking platform classes into `engine-core`.

## Minimal Platform Compliance Checklist

- [ ] Creates graphics context/device before renderer initialization.
- [ ] Injects non-null `GraphicsDevice` into `Renderer`.
- [ ] Wires `Scene` <-> `Renderer` references.
- [ ] Calls `onSurfaceCreated`, `onSurfaceChanged`, and `onDrawFrame` at correct times.
- [ ] Forwards input into `InputSystem` via `InputBindings`.
- [ ] Handles resize and updates viewport.
- [ ] Exits cleanly and releases platform resources.

## Related Docs

- `docs/ENGINE_CORE.md`
- `docs/ENGINE_AND_INPUT.md`
- `docs/RENDERER.md`
- `docs/BUILD_AND_RUN.md`
