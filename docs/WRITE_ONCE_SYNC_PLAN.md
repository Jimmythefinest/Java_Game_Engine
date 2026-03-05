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

## Progress Snapshot (as of 2026-03-05)
## Implemented
1. Multi-module split for desktop/main:
- `:engine-core` created.
- `:engine-platform-desktop` created.
- Root Gradle converted to aggregator + `runDemo` forwarding task.

2. Rendering abstraction layer introduced:
- `GraphicsDevice` interface added.
- `ShaderHandle` interface added.
- `BufferHandle` interface added.
- `DesktopGraphicsDevice` adapter implemented.

3. Renderer/native integration started:
- `Renderer` now depends on `GraphicsDevice`, `ShaderHandle`, `BufferHandle`.
- `ShaderProgram` implements `ShaderHandle`.
- `SSBO` implements `BufferHandle`.
- `ComputeShader` internally uses `BufferHandle`.

4. Object rendering API migration:
- `GameObject.render(...)` now takes `ShaderHandle`.
- Subclasses migrated (`Bone_object`, `Weighted_GameObject`, `CollisionBoxGameObject`, `instancedGameObject`, `LODGameObject`).

5. GL utility decoupling in object pipeline:
- `objects/*` buffer and draw calls now route through `GraphicsDevice`.
- Static `GlUtils` dependency removed from active object rendering path.

6. Core-boundary expansion:
- High-confidence shared set moved into `engine-core` boundaries (Scene/Renderer/Geometries/simulation/object core classes).
- `engine-core` now depends only on `gson` + `joml` jars.
- `NullGraphicsDevice` added to keep core classes platform-neutral by default.

## Measured Drift (latest checker run, 2026-03-05)
- Code drift:
  - `only-in-main = 78`
  - `only-in-android = 37`
  - `shared-paths = 30`
  - `content-drift = 27`
- Shader drift:
  - unchanged from baseline in this phase.

Note:
- `only-in-main` increased from `73` to `78` because new abstraction/support files were added on main side during migration (`graphics/*`, `DesktopGraphicsDevice`, `NullGraphicsDevice`, etc.). This is expected during staged migration.
- Platform-neutral Java files still outside `engine-core` boundaries: `48` (tracked from migration scan).

## Target vs Current Gap
1. `:engine-core`
- Status: `in_progress`
- Done: module exists; rendering interfaces added; Scene/Renderer/geometries/simulation/object core classes now included in core boundaries.
- Missing:
  - remaining platform-neutral files not yet included (48-file backlog, mostly loaders, legacy ai package, utility/test/demo files).
  - physical source move to `engine-core/src/main/java` (currently boundary uses include/exclude on shared legacy `src` tree).

2. `:engine-platform-desktop`
- Status: `in_progress`
- Done: module exists and compiles; desktop renderer path uses graphics interfaces; object rendering path uses `GraphicsDevice`.
- Missing:
  - keep all LWJGL implementations contained to desktop module packages only (no cross-import leaks).
  - split app launcher concerns from platform adapter concerns cleanly.

3. `:engine-platform-android`
- Status: `not_started`
- Missing: module creation and adapter implementation mirroring desktop abstractions.

4. `:app-desktop`
- Status: `partial`
- Done: runnable desktop task wired through `:engine-platform-desktop:runDemo`.
- Missing: dedicated launcher module split from platform module (currently combined).

5. `:app-android`
- Status: `not_started` (for new architecture)
- Missing: Android launcher rewired to consume `:engine-core` + `:engine-platform-android`.

## Next Execution Steps
1. Finish remaining `48` platform-neutral file migrations into core boundaries (priority: `Loaders/*` that are core-safe, `Evolution*`, remaining AI/util classes).
2. Move `engine-core` files physically into `engine-core/src/main/java` and remove boundary include/exclude coupling.
3. Create `:engine-platform-android` and implement Android-side `GraphicsDevice`/buffer/shader adapters.
4. Unify shader source-of-truth and add desktop/Android packaging tasks from one shared shader location.
5. Add CI checks:
   - core must not import LWJGL/Android packages.
   - sync checker and module compile checks must pass.

## Perfect-Split Checklist
For a "perfect" `engine-core` + `engine-platform-desktop` split:

1. `engine-core` checklist
- No `org.lwjgl.*`, `android.*`, or desktop-native imports.
- No dependency on platform adapter implementations (only interfaces).
- Sources live under module-local path (`engine-core/src/main/java`) with no include/exclude trick.
- Dependencies limited to cross-platform libs (`gson`, `joml`) plus JDK.

2. `engine-platform-desktop` checklist
- Contains all LWJGL/OpenGL/desktop glue implementations.
- Implements all rendering/window/input/asset interfaces consumed by core.
- Desktop launcher uses only core APIs + desktop adapters.
- No core business logic duplication.

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
