package com.njst.gaming.graphics;

import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Vector3;

/**
 * Represents a compiled shader program on the platform graphics backend.
 * Provides functions to bind the program, upload uniforms, and activate textures.
 */
public interface ShaderHandle {
    /**
     * Binds the shader program to the current rendering state.
     */
    void use();

    /**
     * Gets the GPU location of a named uniform variable.
     * @param name the uniform variable name
     * @return the location ID
     */
    int getUniformLocation(String name);

    /**
     * Sets a 3-component float vector uniform.
     * @param name the uniform variable name
     * @param vector3f the float array containing xyz
     */
    void setUniformVector3(String name, float[] vector3f);

    /**
     * Sets a 3-component float vector uniform.
     * @param name the uniform variable name
     * @param vector3f the Vector3 object
     */
    void setUniformVector3(String name, Vector3 vector3f);

    /**
     * Sets an integer uniform variable.
     * @param name the uniform variable name
     * @param value the integer value
     */
    void setUniformInt(String name, int value);

    /**
     * Sets a 4x4 matrix uniform variable.
     * @param name the uniform variable name
     * @param matrix the float array containing the 16 matrix elements
     */
    void setUniformMatrix4fv(String name, float[] matrix);

    /**
     * Sets a 4x4 matrix uniform variable.
     * @param name the uniform variable name
     * @param matrix the Matrix4 object
     */
    void setUniformMatrix4fv(String name, Matrix4 matrix);

    /**
     * Activates a texture and binds it to a specific unit.
     * @param location the shader uniform location
     * @param textureID the OpenGL texture ID
     */
    void activateTexture(int location, int textureID);

    /**
     * Activates a texture, binds it to a specific unit, and links it to a named uniform.
     * @param uniformName the uniform variable name
     * @param unit the texture unit (e.g. 0, 1, 2)
     * @param textureID the OpenGL texture ID
     */
    void activateTexture(String uniformName, int unit, int textureID);

    /**
     * @return true if the shader was successfully compiled and linked.
     */
    boolean compiled();

    /**
     * Cleans up the shader program and releases its GPU resources.
     */
    void cleanup();

    /**
     * @return the raw underlying platform shader handle (e.g. Integer for OpenGL ID).
     */
    Object rawHandle();
}
