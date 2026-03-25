package com.njst.gaming.android;

import android.opengl.GLES31;

import java.util.HashMap;
import java.util.Map;

public class AndroidComputeShader {
    private final Map<Integer, AndroidShaderStorageBuffer> buffers = new HashMap<>();
    private final Map<Integer, Integer> bufferSizes = new HashMap<>();
    private int program;
    public String err = "";

    public AndroidComputeShader(String shaderCode) {
        createShaderProgram(shaderCode);
    }

    private void createShaderProgram(String shaderCode) {
        int shader = GLES31.glCreateShader(GLES31.GL_COMPUTE_SHADER);
        GLES31.glShaderSource(shader, shaderCode);
        GLES31.glCompileShader(shader);

        int[] status = new int[1];
        GLES31.glGetShaderiv(shader, GLES31.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            err = "Android compute shader compile error:\n" + GLES31.glGetShaderInfoLog(shader);
            GLES31.glDeleteShader(shader);
            return;
        }

        program = GLES31.glCreateProgram();
        GLES31.glAttachShader(program, shader);
        GLES31.glLinkProgram(program);
        GLES31.glDeleteShader(shader);

        GLES31.glGetProgramiv(program, GLES31.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            err = "Android compute shader link error:\n" + GLES31.glGetProgramInfoLog(program);
            GLES31.glDeleteProgram(program);
            program = 0;
        }
    }

    public void bindBufferToShader(int bindingIndex, float[] data) {
        AndroidShaderStorageBuffer buffer = new AndroidShaderStorageBuffer();
        buffer.setData(data, GLES31.GL_DYNAMIC_COPY);
        buffer.bindToShader(bindingIndex);
        buffers.put(bindingIndex, buffer);
        bufferSizes.put(bindingIndex, data.length);
    }

    public void bindBufferToShader(int bindingIndex, int[] data) {
        AndroidShaderStorageBuffer buffer = new AndroidShaderStorageBuffer();
        buffer.setData(data, GLES31.GL_DYNAMIC_COPY);
        buffer.bindToShader(bindingIndex);
        buffers.put(bindingIndex, buffer);
        bufferSizes.put(bindingIndex, data.length);
    }

    public void updateBuffer(int bindingIndex, float[] data) {
        AndroidShaderStorageBuffer buffer = buffers.get(bindingIndex);
        if (buffer != null) {
            bufferSizes.put(bindingIndex, data.length);
            buffer.updateData(data);
            buffer.bindToShader(bindingIndex);
        }
    }

    public void dispatch(int x, int y, int z) {
        if (program == 0) {
            return;
        }
        GLES31.glUseProgram(program);
        GLES31.glDispatchCompute(x, y, z);
        GLES31.glMemoryBarrier(GLES31.GL_SHADER_STORAGE_BARRIER_BIT | GLES31.GL_BUFFER_UPDATE_BARRIER_BIT);
    }

    public float[] getBufferData(int bindingIndex) {
        AndroidShaderStorageBuffer buffer = buffers.get(bindingIndex);
        Integer size = bufferSizes.get(bindingIndex);
        return buffer != null && size != null ? buffer.getData(size) : null;
    }

    public void release() {
        if (program != 0) {
            GLES31.glDeleteProgram(program);
            program = 0;
        }
        for (AndroidShaderStorageBuffer buffer : buffers.values()) {
            buffer.delete();
        }
        buffers.clear();
        bufferSizes.clear();
    }

    public boolean hasBuffer(int bindingIndex) {
        return buffers.containsKey(bindingIndex);
    }

    public int getBufferSize(int bindingIndex) {
        Integer size = bufferSizes.get(bindingIndex);
        return size != null ? size : 0;
    }
}
