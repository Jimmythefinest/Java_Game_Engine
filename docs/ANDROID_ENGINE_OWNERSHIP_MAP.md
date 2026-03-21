# Android Engine Ownership Map

## Purpose
Classify Android engine-side files into:
- core-owned: should come from `engine-core`
- platform-android-owned: Android/OpenGL ES adapters that should live in `engine-platform-android`
- app-owned: Android app entry points and UI glue that should remain in `android/app`

This is the working migration checklist for the Android modularization effort.

## Core-Owned Duplicates In Android
These already exist in `engine-core` or clearly belong there. Android should stop owning local copies.

- `android/app/src/main/java/com/njst/gaming/Animations/Animation.java`
- `android/app/src/main/java/com/njst/gaming/Animations/KeyFrameParser.java`
- `android/app/src/main/java/com/njst/gaming/Animations/KeyframeAnimation.java`
- `android/app/src/main/java/com/njst/gaming/Animations/walkingAnimation.java`
- `android/app/src/main/java/com/njst/gaming/Camera.java`
- `android/app/src/main/java/com/njst/gaming/Geometries/CubeGeometry.java`
- `android/app/src/main/java/com/njst/gaming/Geometries/Geometry.java`
- `android/app/src/main/java/com/njst/gaming/Geometries/SphereGeometry.java`
- `android/app/src/main/java/com/njst/gaming/Geometries/TerrainGeometry.java`
- `android/app/src/main/java/com/njst/gaming/Geometries/TorusGeometry.java`
- `android/app/src/main/java/com/njst/gaming/Geometries/WeightedGeometry.java`
- `android/app/src/main/java/com/njst/gaming/Math/Matrix4.java`
- `android/app/src/main/java/com/njst/gaming/Math/Quaternion.java`
- `android/app/src/main/java/com/njst/gaming/Math/Vector3.java`
- `android/app/src/main/java/com/njst/gaming/Math/Vector3f.java`
- `android/app/src/main/java/com/njst/gaming/Physics/PhysicsEngine.java`
- `android/app/src/main/java/com/njst/gaming/Renderer.java`
- `android/app/src/main/java/com/njst/gaming/RootLogger.java`
- `android/app/src/main/java/com/njst/gaming/Scene.java`
- `android/app/src/main/java/com/njst/gaming/ai/Character.java`
- `android/app/src/main/java/com/njst/gaming/ai/NeuralNetwork.java`
- `android/app/src/main/java/com/njst/gaming/black.java`
- `android/app/src/main/java/com/njst/gaming/data.java`
- `android/app/src/main/java/com/njst/gaming/objects/Bone_object.java`
- `android/app/src/main/java/com/njst/gaming/objects/GameObject.java`
- `android/app/src/main/java/com/njst/gaming/objects/Weighted_GameObject.java`

## Core Candidates Missing From Core
These are Android-only today, but appear to be shared engine concepts rather than Android-specific glue. They need review before moving.

- `android/app/src/main/java/com/njst/gaming/Entity.java`
- `android/app/src/main/java/com/njst/gaming/Geometries/Bone.java`
- `android/app/src/main/java/com/njst/gaming/Geometries/BoneParser.java`
- `android/app/src/main/java/com/njst/gaming/Geometries/QuadGeometry.java`
- `android/app/src/main/java/com/njst/gaming/InstancedGameObject.java`
- `android/app/src/main/java/com/njst/gaming/JsonLoader.java`
- `android/app/src/main/java/com/njst/gaming/LineGeometry.java`
- `android/app/src/main/java/com/njst/gaming/Loaders/Ant.java`
- `android/app/src/main/java/com/njst/gaming/Loaders/AntSimulation.java`
- `android/app/src/main/java/com/njst/gaming/Loaders/DefaultLoader.java`
- `android/app/src/main/java/com/njst/gaming/Loaders/EvolutionSimulation.java`
- `android/app/src/main/java/com/njst/gaming/Loaders/NeuralNetworktest.java`
- `android/app/src/main/java/com/njst/gaming/Loaders/TetraLoader.java`
- `android/app/src/main/java/com/njst/gaming/Loaders/simulationLoader.java`
- `android/app/src/main/java/com/njst/gaming/Tester.java`
- `android/app/src/main/java/com/njst/gaming/ai/GpuNeuralNetwork.java`
- `android/app/src/main/java/com/njst/gaming/ai/NeuralNetworkVisualiser.java`
- `android/app/src/main/java/com/njst/gaming/objects/LineGameObject.java`
- `android/app/src/main/java/com/njst/gaming/utils/DistanceComparator.java`

## Platform-Android-Owned
These should be implemented in `engine-platform-android`, not `engine-core`.

- `android/app/src/main/java/com/njst/gaming/Natives/ComputeShader.java`
- `android/app/src/main/java/com/njst/gaming/Natives/Framebuffer.java`
- `android/app/src/main/java/com/njst/gaming/Natives/GlUtils.java`
- `android/app/src/main/java/com/njst/gaming/Natives/SSBO.java`
- `android/app/src/main/java/com/njst/gaming/Natives/ShaderProgram.java`
- `android/app/src/main/java/com/njst/gaming/FullScreenQuad.java`
- `android/app/src/main/java/com/njst/gaming/MyRenderer.java`

## App-Owned
These should stay in `android/app` as entry points or Android UI glue.

- `android/app/src/main/java/com/njst/gaming/MainActivity.java`
- `android/app/src/main/java/com/njst/gaming/CustomErrorLogger.java`
- `android/app/src/main/java/com/njst/gaming/objects/Game.java`
- `android/app/src/main/java/com/njst/gaming/objects/TextGameObject.java`

## Obvious Cleanup / Legacy Leftovers
These should not drive architecture decisions.

- `android/app/src/main/java/com/njst/gaming/lwjgl/test.java`
- `android/app/src/main/java/com/njst/gaming/BoneTest.java`
- `android/app/src/main/java/com/njst/gaming/LifeSim.java`
- `android/app/src/main/java/com/njst/gaming/Math/Matrix4f.java`
- `android/app/src/main/java/com/njst/gaming/Antsim/Creature.java`
- `android/app/src/main/java/com/njst/gaming/Antsim/Food.java`

## Next Steps
1. Create `android/engine-platform-android`.
2. Move platform-android-owned files out of `android/app`.
3. Wire `android/app` to depend on `engine-platform-android`.
4. Start deleting Android duplicates in the core-owned section by importing `engine-core` instead.
