# Scene

The `Scene` class is the primary container for the engine's game state, acting as a manager for objects, animations, and the environment.

## Key Fields

- `ArrayList<GameObject> objects`: All physically present entities in the scene.
- `ArrayList<Animation> animations`: Procedural or procedural animations active in the scene.
- `SceneLoader loader`: A custom interface used to populate the scene upon initialization.
- `float[][] heightMap`: Terrain data used for physics and rendering.

## Methods

### `void addGameObject(GameObject r)`
- **Purpose**: Adds an object to the scene.
- **Automatic Sorting**: Attempts to sort objects based on their collision bounds (Z-sorting logic depends on specific implementation details).

### `boolean removeGameObject(GameObject obj)`
- **Purpose**: Removes a GameObject from the scene.
- **Returns**: `true` if the object was found and removed, `false` otherwise.

### `void onDrawFrame()`
- **Purpose**: Updates the scene state before rendering.
- **Logic**: Handles camera movement (if flags are set) and iterates through all active animations to call `animate()`.

### `void cursorMoved(double x, double y)`
- **Purpose**: Handles mouse movement events, typically rotation of the camera around its target.

### `void addTetra()`
- **Purpose**: A debug method that casts a ray from the camera and adds a tetrahedron object at the intersection point.

### `int wheretoaddgameobject(float a)`
- **Purpose**: Helper for binary searching the correct insertion index for an object based on its bounds.

---

## Usage Example

```java
Scene scene = new Scene();

// Set a custom loader
scene.loader = (s) -> {
    GameObject cube = new GameObject(new CubeGeometry(), texture);
    s.addGameObject(cube);
};

// Inside main loop
scene.onDrawFrame();
```
