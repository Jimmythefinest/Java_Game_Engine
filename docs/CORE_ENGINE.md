# Core Engine Components

The functionality of the engine is centered around three primary classes in `com.njst.gaming`.

## 1. Scene ([Scene.java](../src/com/njst/gaming/Scene.java))

The `Scene` class acts as the world container. It manages:
- **Game Objects**: Stored in `ArrayList<GameObject> objects;`.
- **Animations**: Handles `ArrayList<Animation> animations;` and keyframe sequence management.
- **Physics**: Contains a reference to the `PhysicsEngine`.
- **Input Forwarding**: Provides hooks like `cursorMoved()` to handle user interaction.

**Key Method**: `onDrawFrame()` - Called every frame to advance animations and simulations.

## 2. Renderer ([Renderer.java](../src/com/njst/gaming/Renderer.java))

The `Renderer` handles the OpenGL state and the rendering loop.
- **Shader Management**: Uses `ShaderProgram` to load and bind GLSL shaders.
- **SSBO**: Implements efficient camera data transfer using `SSBO` (Shader Storage Buffer Objects).
- **Camera**: Manages the view and projection matrices via the `Camera` class.

**Key Method**: `onDrawFrame()` - Clears the screen, binds shader uniforms, and renders each object in the scene.

## 3. GameObject ([GameObject.java](../src/com/njst/gaming/objects/GameObject.java))

A `GameObject` is the base entity for anything visible in the scene.
- **Geometry**: Stores mesh data in VBOs (Vertex Buffer Objects).
- **Transforms**: Manages position, rotation, and scaling.
- **Rendering**: Contains the `render()` method which binds the appropriate buffers and issues draw calls.

**Usage Pattern**:
```java
// Create a GameObject
GameObject obj = new GameObject(geometry, texture);
// Generate GPU buffers
obj.generateBuffers();
// Add to scene
scene.addGameObject(obj);
```
