# Write Once, Run Everywhere Sync Plan

## Goal
Create a single shared engine codebase used by both desktop and Android, with platform-specific code isolated behind adapters.

## Current State (as of 2026-03-05)
- `73` files exist only in main (`src/com/njst/gaming`).
- `37` files exist only in Android (`android/app/src/main/java/com/njst/gaming`).
- `30` paths overlap; `27` have content drift.
- Shader sets are split and drifted between:
  - `src/resources/shaders`
  - `android/app/src/main/java/com/*.glsl`

## Target Architecture
Use a single multi-module Gradle root:

1. `:engine-core`
- Pure Java/Kotlin shared logic.
- No direct LWJGL, Android SDK, or OpenGL ES calls.
- Owns: scene graph, simulation, math, AI, loaders that do not require platform APIs.

2. `:engine-platform-desktop`
- Desktop-specific GL + window/input bindings (LWJGL, headless context, desktop file I/O).
- Implements interfaces from `:engine-core`.

3. `:engine-platform-android`
- Android-specific GL + lifecycle + asset/input bindings.
- Implements interfaces from `:engine-core`.

4. `:app-desktop`
- Desktop launcher only.

5. `:app-android`
- Android launcher only (Activity/Renderer wiring).

## Non-Negotiable Rules
1. Shared engine behavior must be added in `:engine-core` first.
2. Platform modules may only contain adapters/wiring, not business logic.
3. Shaders have one source of truth in shared assets.
4. No duplicated class names in different packages for the same concept (`Utils` vs `utils`, `GpuNeuralNetwork` vs `GPUNeuralNetwork`, etc.).

## Migration Backlog (Priority Order)
1. Create shared package conventions
- Standardize on one package style:
  - `com.njst.gaming.utils` (lowercase folder)
  - consistent class naming (`GpuNeuralNetwork` or `GPUNeuralNetwork`, choose one)
- Rename and update imports in both codebases.

2. Extract core math + scene primitives
- Move these first into `:engine-core` and make both platforms consume them:
  - `Math/*`
  - `Scene.java`
  - `objects/GameObject.java`
  - animation classes (`Animations/*`)

3. Extract simulation + AI core
- Move shared logic:
  - `simulation/*`
  - `ai/*` logic that does not call platform APIs
- Keep rendering/debug UI adapters in platform modules.

4. Split rendering API from implementation
- Introduce core interfaces in `:engine-core`:
  - `GraphicsDevice`
  - `ShaderHandle`
  - `BufferHandle`
  - `TextureHandle`
- Rework `Renderer`, `ShaderProgram`, `ComputeShader`, `SSBO`, `GlUtils` to use adapter interface.
- Keep actual GL calls only in platform modules.

5. Unify loaders
- Promote shared loader contracts/models to core.
- Platform-specific file access injected via `AssetProvider` interface.

6. Unify shader pipeline
- Move all shared `.glsl` into one location in shared module resources.
- Create packaging tasks:
  - Desktop: load directly from resources.
  - Android: copy to `app/src/main/assets/shaders` during build.

## Sprint Plan
1. Sprint 1: Structure + conventions
- Create modules.
- Add core interfaces and package naming cleanup.
- Add compile-only desktop/android adapters.

2. Sprint 2: Core extraction
- Move math, scene, objects, animation, simulation/AI logic.
- Make both apps compile against `:engine-core`.

3. Sprint 3: Renderer abstraction
- Move rendering contracts to core.
- Implement desktop and Android render backends.

4. Sprint 4: Loaders + shaders + parity tests
- Single shader source.
- Cross-platform scene load smoke tests.

## Acceptance Criteria
1. New gameplay/simulation feature can be implemented entirely in `:engine-core` and run unchanged on desktop + Android.
2. Both launchers pass a common parity test suite.
3. Sync report shows:
- `only-main = 0` for shared domain paths.
- `only-android = 0` for shared domain paths.
- `drifted-shared-files = 0` for core-owned paths.

## Automation
Use:

`tools/check_port_sync.sh`

to continuously report drift while migration is in progress.
