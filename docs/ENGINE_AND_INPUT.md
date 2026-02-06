# Engine and Input Systems

This document explains the usage of the abstracted `Engine` and the advanced `Input` management system.

## 1. The Engine Class ([Engine.java](../src/com/njst/gaming/Engine.java))

The `Engine` class is an abstract base class that manages the GLFW window lifecycle, OpenGL context initialization, and the main render loop. It is designed to remove boilerplate code from your application.

### Key Features:
- **Automatic Initialization**: Handles `glfwInit`, window creation, and OpenGL capability loading.
- **Main Loop**: Includes a standardized render loop with FPS tracking in the window title.
- **Console Debugging**: Starts a background daemon thread that listens for terminal commands (e.g., `tp x y z`).
- **Standardized Hooks**:
    - `onInit()`: Called once when the engine starts. Use this for loading models and setting up the scene.
    - `onUpdate()`: Called every frame before rendering. Use this for game logic and input polling.
    - `onKey(int key, int action)`: Event-driven hook for discrete input events.

### Usage Example:
```java
public class MyGame extends Engine {
    @Override
    protected void onInit() {
        // Load your assets here
        scene.loader = new DefaultLoader();
    }

    @Override
    protected void onUpdate() {
        // Continuous game logic
    }

    @Override
    protected void onKey(int key, int action) {
        // Event-based keys
    }

    public static void main(String[] args) {
        new MyGame().run();
    }
}
```

## 2. The Input System ([Input.java](../src/com/njst/gaming/Input.java))

The `Input` class manages keyboard and mouse states, providing both polling-based and event-based interaction.

### Capabilities:
- **Key Polling**: Check if a key is currently held down.
- **Edge Detection**: Detect the exact frame a key was pressed or released.
- **Mouse Tracking**: Get cursor coordinates and scroll wheel offsets.

### Common Methods:
- `isKeyDown(int key)`: `true` if the key is currently held.
- `isKeyPressed(int key)`: `true` only on the frame the key was first pressed.
- `isKeyReleased(int key)`: `true` only on the frame the key was released.
- `getMouseX() / getMouseY()`: Get current cursor coordinates.
- `getScrollY()`: Get vertical scroll amount.

### Example in `onUpdate()`:
```java
@Override
protected void onUpdate() {
    // Smoother movement via polling
    if (input.isKeyDown(GLFW_KEY_W)) {
        player.moveForward();
    }

    // Toggle interaction via edge detection
    if (input.isKeyPressed(GLFW_KEY_E)) {
        player.interact();
    }
}
```
