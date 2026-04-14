# Repository Guidelines

## Project Structure & Module Organization
This repository is a Gradle multi-project build. `engine-core/src/main/java` contains shared engine code: rendering, math, collision, input, AI, and asset serializers under `com.njst.gaming`. `engine-platform-desktop/src/main/java` contains the LWJGL desktop runtime, demo entry points, native OpenGL bindings, and exporters. Desktop assets live in `engine-platform-desktop/src/main/resources`, especially `shaders/`, `battle_arena/`, `skinned/`, and `weighted_geometry/`. The `android/` tree is maintained separately for the Android port; only sync generated assets there when a task explicitly targets it. Reference material belongs in `docs/`, while `backups/` and `temp/` are not primary source locations.

## Build, Test, and Development Commands
Use Gradle from the repository root:

- `gradle build`: compiles all included modules.
- `gradle runDemo`: runs the default desktop app (`com.rebuild.Tetris3D`).
- `gradle :engine-platform-desktop:runBattleground`: launches the battle arena demo.
- `gradle :engine-platform-desktop:runCollisionDemo`: exercises collision runtime behavior.
- `gradle exportFbxAnimation -Pfbx=path/to/file.fbx -PanimationIndex=0`: exports animation assets into `src/main/resources/weighted_geometry/`.
- `gradle :engine-platform-desktop:syncDefeatedSkinnedAssetToAndroid`: rebuilds and copies the skinned asset into Android assets.

## Coding Style & Naming Conventions
Follow the existing Java style: 4-space indentation, braces on the same line, and one top-level public class per file. Packages stay lowercase (`com.njst.gaming...`); class names use PascalCase; methods and fields use camelCase. Keep new code inside the appropriate module instead of adding more root-level `src/` files. Resource names should remain descriptive and lowercase with underscores, for example `shadow_depth_vert.glsl`.

## Testing Guidelines
There is no dedicated `src/test/java` suite yet. Validate changes with `gradle build` plus the narrowest runnable task that covers the change, such as `runDemo`, `runBattleground`, or a specific exporter task. If you add automated tests, place them under each module’s `src/test/java` and name them `*Test.java`.

## Commit & Pull Request Guidelines
Recent commits use short imperative subjects such as `Fix FBX normals and UV loading` and `Update battle arena assets and scene setup`. Keep commit titles focused on one change. Pull requests should include a concise summary, affected module(s), validation commands run, and screenshots for rendering or asset-pipeline changes. Link any related issue or design note in `docs/` when relevant.
