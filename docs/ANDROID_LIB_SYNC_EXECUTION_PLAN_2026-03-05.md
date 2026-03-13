# Android Library Sync Execution Plan (2026-03-05)

## Objective
Make the Android engine code conform to `docs/WRITE_ONCE_SYNC_PLAN.md` by introducing a real Android platform library (`engine-platform-android`) that consumes `engine-core`, removing duplicated shared logic, and driving drift to zero for core-owned paths.

## Baseline (measured on 2026-03-05)
- Code drift from `tools/check_port_sync.sh`:
- `only-in-main = 83`
- `only-in-android = 37`
- `shared-paths = 30`
- `content-drift = 27`
- Shader drift:
- `only-in-main = 16`
- `only-in-android = 4`
- `shared-paths = 6`
- `content-drift = 4`

## Scope For This Execution Cycle
- In scope:
- Create Android platform library module and move Android engine classes out of `android/app`.
- Wire Android app to depend on platform library, not duplicated engine source files.
- Align package conventions (`Utils` vs `utils`, `GpuNeuralNetwork` vs `GPUNeuralNetwork`) on the Android side.
- Begin shared shader source-of-truth pipeline for Android packaging.
- Add guardrails and checks for no Android/LWJGL leakage into core.
- Out of scope:
- Full physical move of all root `src` shared classes to `engine-core/src/main/java` in this same cycle.
- Full gameplay parity for every loader/simulation entry point.

## Execution Plan

### Phase 1: Module Scaffolding And Wiring
- Create `android/engine-platform-android` as `com.android.library`.
- Move `android/app/src/main/java/com/njst/gaming/**` into `android/engine-platform-android/src/main/java/com/njst/gaming/**`.
- Keep Android entry points in app only (`MainActivity`, UI integration classes), with rendering adapter classes in the library.
- Update `android/settings.gradle.kts` to include `:engine-platform-android`.
- Update `android/app/build.gradle.kts`:
- remove direct ownership of engine source files.
- add `implementation(project(":engine-platform-android"))`.
- Exit criteria:
- `./gradlew :android:app:assembleDebug` (or Android-local equivalent) succeeds.

### Phase 2: Core Boundary Conformance
- In `engine-platform-android`, keep only adapter and wiring classes that depend on Android/OpenGL ES.
- Replace direct business logic duplication in Android with imports from shared core classes where available.
- Resolve naming/package drift:
- normalize to one utils package style (`com.njst.gaming.utils` or `com.njst.gaming.Utils`), then update all imports.
- choose one neural net class naming convention and apply consistently.
- Exit criteria:
- Android library compiles after package normalization.
- No duplicate classes for the same concept remain in Android module.

### Phase 3: Rendering Adapter Completion
- Implement Android counterparts for core rendering contracts used by desktop:
- `GraphicsDevice`
- `ShaderHandle`
- `BufferHandle`
- Add or complete Android implementations near existing classes (`ShaderProgram`, `SSBO`, `ComputeShader`, `GlUtils`) without leaking platform code into core.
- Ensure `Renderer` path uses interface contracts only.
- Exit criteria:
- No compile-time dependency from core to `android.*` packages.
- Android render path instantiates adapter implementations successfully.

### Phase 4: Shader Source Unification
- Define one shared shader source directory for engine-owned shaders.
- Add Android Gradle copy task to package shared shaders into `app/src/main/assets/shaders` or library assets at build time.
- Remove duplicate Android-local shader copies once load paths are validated.
- Exit criteria:
- Same shader files are consumed by desktop and Android for shared pipeline stages.
- Shader drift counters stop increasing; targeted shared shader drift becomes `0`.

### Phase 5: Guardrails And CI Checks
- Add automated checks:
- run `tools/check_port_sync.sh` in CI.
- fail build on forbidden imports in `engine-core` (`android.*`, `org.lwjgl.*`).
- module compile checks:
- `:engine-core:compileJava`
- `:engine-platform-desktop:compileJava`
- Android library compile task.
- Exit criteria:
- CI fails on drift regression or boundary violations.

## Task Breakdown For Today (2026-03-05)
1. Add `:engine-platform-android` module in `android/` and wire dependency from `:app`.
2. Move Android engine classes from app module to library module with no behavior changes.
3. Build-fix pass for package/import issues caused by module move.
4. Add initial adapter TODO map file listing each class that must implement core contracts.
5. Re-run drift checker and record post-move baseline in docs.

## Definition Of Done
1. Android app launches while consuming engine code from `:engine-platform-android`.
2. No shared engine business logic remains directly in `android/app`.
3. Core boundary checks are in place and green.
4. Drift trend is downward from 2026-03-05 baseline, with explicit counts recorded after each phase.

## Risk Register
- Build system split risk:
- Root project and `android/` project are currently separate Gradle universes. Keep migration incremental inside `android/` first, then unify root architecture.
- Package rename churn:
- Renaming `Utils/utils` and neural net class names may touch many files; do this in one dedicated commit.
- Shader loading regressions:
- Asset path changes can break runtime shader resolution; add a startup shader smoke test.

## Verification Commands
- `bash tools/check_port_sync.sh`
- `./gradlew :engine-core:compileJava :engine-platform-desktop:compileJava`
- `cd android && ./gradlew :engine-platform-android:assemble :app:assembleDebug`
