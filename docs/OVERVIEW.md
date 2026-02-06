# NJST Game Engine Project Overview

This documentation provides a technical reference for the NJST Game Engine.

## Project Structure

- `src/com/njst/gaming/`: Core engine source code.
  - `Animations/`: Keyframe and procedural animation logic.
  - `ai/`: Neural networks and reinforcement learning systems.
  - `Math/`: 3D math utilities (Vectors, Matrices, Quaternions).
  - `objects/`: 3D object representations and mesh management.
  - `Physics/`: Collision detection and rigid body dynamics.
- `src/com/rebuild/`: Sample implementations and main entry points (e.g., `RotatingCube`).
- `bin/`: Compiled class files.

## High-Level Execution Flow

1. **Initialization**: The application starts in a class containing a `main` method (e.g., [RotatingCube.java](../src/com/rebuild/RotatingCube.java)). It initializes GLFW, creates an OpenGL context, and sets up a `Scene` and `Renderer`.
2. **Main Loop**: The loop runs while the window is open, calling `renderer.onDrawFrame()`.
3. **Render Call**: The `Renderer` prepares the GPU (clearing buffers, updating camera matrices) and then iterates through the `Scene`'s game objects, invoking their `render()` methods.
4. **Update Call**: During the render frame, `scene.onDrawFrame()` is called to update animations and physics.

## Documentation Index

- [Core Engine Components](CORE_ENGINE.md): Detailed breakdown of Scene, Renderer, and GameObject.
- [Engine & Input System](ENGINE_AND_INPUT.md): Guide to the abstracted Engine lifecycle and Input management.
- [Skeletal Animation](SKELETAL_ANIMATION.md): Reference for character skinning and animation logic.
- [SSBO Management](SSBO.md): Comprehensive guide to Shader Storage Buffer Objects.
- [Compute Shader Guide](COMPUTE_SHADER.md): How to perform massive visual and physical computations on the GPU.
- [Scene Management](SCENE.md): Methods and structure of the world container.
- [Renderer Reference](RENDERER.md): Detailed look at the drawing pipeline and matrices.
- [AI & Simulations](AI_AND_SIMULATION.md): Guide to the neural network and reinforcement learning packages.
- [Build & Run](BUILD_AND_RUN.md): Instructions for compiling and executing the project.
