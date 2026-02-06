# SSBO (Shader Storage Buffer Object)

The `SSBO` class provides a high-level wrapper for OpenGL Shader Storage Buffer Objects, used for passing large amounts of data to shaders or retrieving data from compute shaders.

## Methods

### `SSBO()`
Constructor. Generates a new buffer ID on the GPU.

### `void bind()`
Binds the buffer to the `GL_SHADER_STORAGE_BUFFER` target.

### `void unbind()`
Unbinds the buffer.

### `void setData(float[] data, int usage)`
- **Purpose**: Uploads an array of floats to the buffer.
- **Usage**: `GL43.GL_STATIC_DRAW`, `GL43.GL_DYNAMIC_DRAW`, etc.
- **Effect**: Allocates storage and sets the data.

### `void setData(int[] data, int usage)`
- **Purpose**: Uploads an array of integers to the buffer.
- **Usage**: Same as above.

### `void updateData(float[] data)`
- **Purpose**: Updates existing buffer data using `glBufferSubData`.
- **Note**: Does not reallocate storage; expects the buffer to already have sufficient size.

### `void bindToShader(int bindingPoint)`
- **Purpose**: Connects the buffer to a specific binding point in the shader.
- **Usage**: Matches `layout(std430, binding = X)` in GLSL.

### `float[] getData(int numElements)`
- **Purpose**: Reads data back from the GPU to the CPU.
- **Effect**: Maps the buffer for reading and copies it into a float array.

### `void delete()`
- **Purpose**: Deletes the buffer and frees GPU memory.

---

## Usage Example

### CPU Side (Java)
```java
SSBO ssbo = new SSBO();
float[] data = { 1.0f, 2.0f, 3.0f, 4.0f };

// Initial upload
ssbo.setData(data, GL43.GL_DYNAMIC_DRAW);

// Bind to binding point 0
ssbo.bindToShader(0);

// Later, update data
ssbo.updateData(new float[] { 5.0f, 6.0f, 7.0f, 8.0f });
```

### GPU Side (GLSL)
```glsl
layout(std430, binding = 0) buffer MyBuffer {
    float values[];
};
```
