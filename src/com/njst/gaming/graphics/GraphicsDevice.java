package com.njst.gaming.graphics;

import com.njst.gaming.Renderer;
import com.njst.gaming.objects.GameObject;

public interface GraphicsDevice {
    ShaderHandle createShaderProgram(String vertexShaderSource, String fragmentShaderSource);

    BufferHandle createShaderStorageBuffer();

    String loadShaderSource(String filePath);

    int loadTexture(String texturePath);

    ImposterBakeResult bakeImposter(Renderer renderer, GameObject object, int width, int height);

    void releaseTexture(int textureId);

    int createVertexArray();

    int[] createBuffers(int count);

    void bindVertexArray(int vaoId);

    void uploadArrayBufferFloat(int bufferId, float[] data);

    void uploadArrayBufferInt(int bufferId, int[] data);

    void uploadElementArrayBufferInt(int bufferId, int[] data);

    void setVertexAttribPointer(int bufferId, int location, int size);

    void updateArrayBufferFloat(int bufferId, float[] data);

    void drawElementsTriangles(int indexCount);

    void drawElementsLines(int indexCount);

    void deleteBuffers(int[] buffers);

    void deleteVertexArrays(int[] vaos);

    void enableBlendAndDepth();

    void clearColorAndDepth();

    void viewport(int width, int height);

    int dynamicDrawUsage();
}
