# Renderer

The `Renderer` handles the OpenGL drawing process, managing cameras, shaders, and frame buffers.

## Key Fields

- `Camera camera`: The main perspective camera.
- `Scene scene`: The scene to be rendered.
- `ShaderProgram shaderProgram`: The active GLSL program for object rendering.
- `SSBO ssbo`: A persistent storage buffer for passing global constants (projection, view, camera pos) to shaders.

## Methods

### `void onSurfaceCreated()`
- **Purpose**: Initializes OpenGL resources.
- **Effect**: Compiles the main shader program, triggers the `SceneLoader`, and generates buffers for all objects.

### `void onSurfaceChanged(int w, int h)`
- **Purpose**: Updates the viewport and camera projection matrix when the window is resized.

### `void onDrawFrame()`
- **Purpose**: The main rendering loop method.
- **Step-by-Step**:
    1. Updates the global SSBO with view and projection matrices.
    2. Clears depth and color buffers.
    3. Triggers `scene.onDrawFrame()` for animation updates.
    4. Iterates through all `GameObject` instances and calls their `render()` method using the main shader.

### `long getlast()`
- **Purpose**: Returns the timestamp of the last frame for timing calculations.

---

## Usage Example

```java
Renderer renderer = new Renderer();
renderer.scene = myScene;

// Window/Surface triggers
renderer.onSurfaceCreated();
renderer.onSurfaceChanged(800, 600);

// Main loop
while (running) {
    renderer.onDrawFrame();
    // Swap buffers...
}
```
