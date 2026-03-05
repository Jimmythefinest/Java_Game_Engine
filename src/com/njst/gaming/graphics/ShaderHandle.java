package com.njst.gaming.graphics;

import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Vector3;

public interface ShaderHandle {
    void use();

    int getUniformLocation(String name);

    void setUniformVector3(String name, float[] vector3f);

    void setUniformVector3(String name, Vector3 vector3f);

    void setUniformMatrix4fv(String name, float[] matrix);

    void setUniformMatrix4fv(String name, Matrix4 matrix);

    void activateTexture(int location, int textureID);

    boolean compiled();

    void cleanup();

    Object rawHandle();
}
