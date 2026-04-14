package com.njst.gaming.graphics;

import com.njst.gaming.Renderer;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.collision.SphericalHeightmapShape;
import com.njst.gaming.objects.GameObject;

public interface GraphicsDevice {
    ShaderHandle createShaderProgram(String vertexShaderSource, String fragmentShaderSource);

    BufferHandle createShaderStorageBuffer();

    String loadShaderSource(String filePath);

    String loadTextResource(String filePath);

    byte[] loadBinaryResource(String filePath);

    int loadTexture(String texturePath);

    int createTextureRGBA(int width, int height, byte[] rgbaPixels);

    ShadowMapHandle createShadowMap(int width, int height);

    ImposterBakeResult bakeImposter(Renderer renderer, GameObject object, int width, int height);

    SphericalHeightmapShape bakeSphericalHeightmap(Renderer renderer, GameObject object, int width, int height,
            Vector3 localCenter);

    void releaseTexture(int textureId);

    int createVertexArray();

    int[] createBuffers(int count);

    void bindVertexArray(int vaoId);

    void uploadArrayBufferFloat(int bufferId, float[] data);

    void uploadArrayBufferInt(int bufferId, int[] data);

    void uploadElementArrayBufferInt(int bufferId, int[] data);

    void setVertexAttribPointer(int bufferId, int location, int size);

    void setVertexAttribIPointer(int bufferId, int location, int size);

    void updateArrayBufferFloat(int bufferId, float[] data);

    void drawElementsTriangles(int indexCount);

    void drawElementsLines(int indexCount);

    void deleteBuffers(int[] buffers);

    void deleteVertexArrays(int[] vaos);

    void enableBlendAndDepth();

    void clearColorAndDepth();

    void clearDepth();

    void bindShadowMap(ShadowMapHandle shadowMap);

    void bindDefaultFramebuffer();

    void dumpShadowMap(ShadowMapHandle shadowMap, String outputPath);

    void viewport(int width, int height);

    int dynamicDrawUsage();
}
