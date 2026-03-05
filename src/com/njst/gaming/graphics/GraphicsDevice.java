package com.njst.gaming.graphics;

public interface GraphicsDevice {
    ShaderHandle createShaderProgram(String vertexShaderSource, String fragmentShaderSource);

    BufferHandle createShaderStorageBuffer();

    String loadShaderSource(String filePath);

    void enableBlendAndDepth();

    void clearColorAndDepth();

    void viewport(int width, int height);

    int dynamicDrawUsage();
}
