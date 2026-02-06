# ComputeShader

The `ComputeShader` class facilitates the execution of General-Purpose GPU (GPGPU) tasks using OpenGL Compute Shaders.

## Methods

### `ComputeShader(String shaderCode)`
Constructor. Compiles and links a compute shader from the provided GLSL source code.

### `void bindBufferToShader(int bindingIndex, float[] data)`
- **Purpose**: Creates a new SSBO with the provided data and binds it to the specified index.
- **Internal**: Tracks the buffer and its size for automatic retrieval.

### `void bindBufferToShader(int bindingIndex, int[] data)`
- **Purpose**: Same as above, but for integer data.

### `void updateBuffer(int bindingIndex, float[] data)`
- **Purpose**: Updates the data in an already bound buffer.

### `void dispatch(int x, int y, int z)`
- **Purpose**: Executes the compute shader with the specified number of workgroups.
- **Barrier**: Automatically calls `glMemoryBarrier` to ensure data consistency.

### `float[] getBufferData(int bindingIndex)`
- **Purpose**: Retrieves the current content of a buffer after computation.

### `void recompile(String shaderCode)`
- **Purpose**: Releases existing program resources and recompiles with new source code.

### `void release()`
- **Purpose**: Cleans up all programs and associated SSBOs.

---

## Usage Example

```java
String shaderCode = Utils.readFile("/shaders/my_compute.glsl");
ComputeShader cs = new ComputeShader(shaderCode);

float[] inputData = new float[1024];
// ... fill inputData

// Bind buffer to index 0
cs.bindBufferToShader(0, inputData);

// Run compute shader (e.g., 1024/64 workgroups)
cs.dispatch(16, 1, 1);

// Get results back
float[] results = cs.getBufferData(0);

// Cleanup
cs.release();
```
