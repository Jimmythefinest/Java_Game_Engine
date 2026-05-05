package com.njst.gaming.graphics;

import com.njst.gaming.Renderer;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.collision.SphericalHeightmapShape;
import com.njst.gaming.objects.GameObject;

/**
 * The core abstraction for platform-specific rendering backends.
 * It provides the necessary functions to compile shaders, manage textures,
 * allocate buffers, and dispatch draw calls to the underlying graphics API.
 */
public interface GraphicsDevice {
    /**
     * Creates and compiles a shader program from vertex and fragment source code.
     * @param vertexShaderSource the GLSL vertex shader code
     * @param fragmentShaderSource the GLSL fragment shader code
     * @return a handle to the compiled shader program
     */
    ShaderHandle createShaderProgram(String vertexShaderSource, String fragmentShaderSource);

    /**
     * Creates a new Shader Storage Buffer Object (SSBO) style buffer handle.
     * @return a handle to the allocated buffer
     */
    BufferHandle createShaderStorageBuffer();

    /**
     * Creates a compute shader backend for parallel GPU calculations.
     * @param shaderSource the GLSL compute shader source code
     * @return a handle to the compute backend
     */
    ComputeBackend createComputeBackend(String shaderSource);

    /**
     * Loads the raw shader source string from a file path.
     * @param filePath the path to the shader file
     * @return the source string
     */
    String loadShaderSource(String filePath);

    /**
     * Loads a text file into a string.
     * @param filePath the path to the text file
     * @return the file contents
     */
    String loadTextResource(String filePath);

    /**
     * Loads a file into a binary byte array.
     * @param filePath the path to the binary file
     * @return the file contents as bytes
     */
    byte[] loadBinaryResource(String filePath);

    /**
     * Loads an image from the specified path and creates an OpenGL texture.
     * @param texturePath the path to the image file
     * @return the OpenGL texture ID
     */
    int loadTexture(String texturePath);

    /**
     * Creates a texture using an existing RGBA pixel byte array.
     * @param width the width of the texture
     * @param height the height of the texture
     * @param rgbaPixels the RGBA byte array
     * @return the OpenGL texture ID
     */
    int createTextureRGBA(int width, int height, byte[] rgbaPixels);

    /**
     * Creates a framebuffer tailored for shadow mapping with a depth attachment.
     * @param width the width of the shadow map
     * @param height the height of the shadow map
     * @return a handle to the created shadow map
     */
    ShadowMapHandle createShadowMap(int width, int height);

    /**
     * Bakes a 2D imposter texture for the given GameObject.
     * @param renderer the current renderer
     * @param object the GameObject to bake
     * @param width the width of the target imposter texture
     * @param height the height of the target imposter texture
     * @return the resulting imposter data
     */
    ImposterBakeResult bakeImposter(Renderer renderer, GameObject object, int width, int height);

    /**
     * Bakes a spherical heightmap from a specific object for collision logic.
     * @param renderer the current renderer
     * @param object the target GameObject
     * @param width the resolution width
     * @param height the resolution height
     * @param localCenter the local center vector to bake around
     * @return the baked spherical heightmap shape
     */
    SphericalHeightmapShape bakeSphericalHeightmap(Renderer renderer, GameObject object, int width, int height,
            Vector3 localCenter);

    /**
     * Releases an allocated texture by ID.
     * @param textureId the texture ID to release
     */
    void releaseTexture(int textureId);

    /**
     * Generates a new Vertex Array Object (VAO).
     * @return the generated VAO ID
     */
    int createVertexArray();

    /**
     * Generates multiple Vertex Buffer Objects (VBO/EBO).
     * @param count the number of buffers to generate
     * @return an array of generated buffer IDs
     */
    int[] createBuffers(int count);

    /**
     * Binds a Vertex Array Object.
     * @param vaoId the VAO ID to bind
     */
    void bindVertexArray(int vaoId);

    /**
     * Uploads a float array to an Array Buffer.
     * @param bufferId the destination buffer ID
     * @param data the float array data
     */
    void uploadArrayBufferFloat(int bufferId, float[] data);

    /**
     * Uploads an integer array to an Array Buffer.
     * @param bufferId the destination buffer ID
     * @param data the integer array data
     */
    void uploadArrayBufferInt(int bufferId, int[] data);

    /**
     * Uploads an integer array to an Element Array Buffer (EBO).
     * @param bufferId the destination buffer ID
     * @param data the integer array data
     */
    void uploadElementArrayBufferInt(int bufferId, int[] data);

    /**
     * Configures a float vertex attribute pointer.
     * @param bufferId the target buffer ID
     * @param location the shader location index
     * @param size the number of components per vertex attribute
     */
    void setVertexAttribPointer(int bufferId, int location, int size);

    /**
     * Configures an integer vertex attribute pointer.
     * @param bufferId the target buffer ID
     * @param location the shader location index
     * @param size the number of components per vertex attribute
     */
    void setVertexAttribIPointer(int bufferId, int location, int size);

    /**
     * Updates an existing Array Buffer with new float data.
     * @param bufferId the destination buffer ID
     * @param data the float array data
     */
    void updateArrayBufferFloat(int bufferId, float[] data);

    /**
     * Issues a draw call using element indices (triangles).
     * @param indexCount the number of indices to draw
     */
    void drawElementsTriangles(int indexCount);

    /**
     * Issues a draw call using element indices (lines).
     * @param indexCount the number of indices to draw
     */
    void drawElementsLines(int indexCount);

    /**
     * Deletes multiple buffers.
     * @param buffers an array of buffer IDs to delete
     */
    void deleteBuffers(int[] buffers);

    /**
     * Deletes multiple Vertex Array Objects.
     * @param vaos an array of VAO IDs to delete
     */
    void deleteVertexArrays(int[] vaos);

    /**
     * Enables GL blending and depth testing for standard rendering.
     */
    void enableBlendAndDepth();

    /**
     * Clears both the color and depth framebuffers.
     */
    void clearColorAndDepth();

    /**
     * Clears only the depth framebuffer.
     */
    void clearDepth();

    /**
     * Binds a ShadowMap framebuffer for depth-only rendering.
     * @param shadowMap the ShadowMapHandle to bind
     */
    void bindShadowMap(ShadowMapHandle shadowMap);

    /**
     * Reverts rendering to the default window framebuffer.
     */
    void bindDefaultFramebuffer();

    /**
     * Utility method to dump a shadow map to a disk image for debugging.
     * @param shadowMap the ShadowMapHandle to dump
     * @param outputPath the file output path
     */
    void dumpShadowMap(ShadowMapHandle shadowMap, String outputPath);

    /**
     * Sets the rendering viewport dimensions.
     * @param width the viewport width
     * @param height the viewport height
     */
    void viewport(int width, int height);

    /**
     * @return the platform-specific flag for dynamic draw buffer usage.
     */
    int dynamicDrawUsage();
}
