package com.njst.gaming.android;

import android.opengl.GLES31;

import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.graphics.ShaderHandle;

public class AndroidShaderProgram implements ShaderHandle {
    private final int programId;

    public AndroidShaderProgram(String vertexShaderSource, String fragmentShaderSource) {
        int vertexShaderId = compileShader(GLES31.GL_VERTEX_SHADER, vertexShaderSource);
        int fragmentShaderId = compileShader(GLES31.GL_FRAGMENT_SHADER, fragmentShaderSource);

        programId = GLES31.glCreateProgram();
        GLES31.glAttachShader(programId, vertexShaderId);
        GLES31.glAttachShader(programId, fragmentShaderId);
        GLES31.glLinkProgram(programId);

        int[] status = new int[1];
        GLES31.glGetProgramiv(programId, GLES31.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            String log = GLES31.glGetProgramInfoLog(programId);
            GLES31.glDeleteShader(vertexShaderId);
            GLES31.glDeleteShader(fragmentShaderId);
            GLES31.glDeleteProgram(programId);
            throw new IllegalStateException("Failed to link Android shader program: " + log);
        }

        GLES31.glDeleteShader(vertexShaderId);
        GLES31.glDeleteShader(fragmentShaderId);
    }

    private int compileShader(int type, String source) {
        int shaderId = GLES31.glCreateShader(type);
        GLES31.glShaderSource(shaderId, source);
        GLES31.glCompileShader(shaderId);

        int[] status = new int[1];
        GLES31.glGetShaderiv(shaderId, GLES31.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            String log = GLES31.glGetShaderInfoLog(shaderId);
            GLES31.glDeleteShader(shaderId);
            throw new IllegalStateException("Failed to compile Android shader: " + log);
        }
        return shaderId;
    }

    @Override
    public void use() {
        GLES31.glUseProgram(programId);
    }

    @Override
    public int getUniformLocation(String name) {
        return GLES31.glGetUniformLocation(programId, name);
    }

    @Override
    public void setUniformVector3(String name, float[] vector3f) {
        GLES31.glUniform3fv(getUniformLocation(name), 1, vector3f, 0);
    }

    @Override
    public void setUniformVector3(String name, Vector3 vector3f) {
        GLES31.glUniform3f(getUniformLocation(name), vector3f.x, vector3f.y, vector3f.z);
    }

    @Override
    public void setUniformMatrix4fv(String name, float[] matrix) {
        GLES31.glUniformMatrix4fv(getUniformLocation(name), 1, false, matrix, 0);
    }

    @Override
    public void setUniformMatrix4fv(String name, Matrix4 matrix) {
        GLES31.glUniformMatrix4fv(getUniformLocation(name), 1, false, matrix.get(new float[16]), 0);
    }

    @Override
    public void activateTexture(int location, int textureID) {
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureID);
        GLES31.glUniform1i(location, 0);
    }

    @Override
    public void activateTexture(String uniformName, int unit, int textureID) {
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + unit);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureID);
        GLES31.glUniform1i(getUniformLocation(uniformName), unit);
    }

    @Override
    public boolean compiled() {
        int[] status = new int[1];
        GLES31.glGetProgramiv(programId, GLES31.GL_LINK_STATUS, status, 0);
        return status[0] != 0;
    }

    @Override
    public void cleanup() {
        GLES31.glDeleteProgram(programId);
    }

    @Override
    public Object rawHandle() {
        return programId;
    }
}
