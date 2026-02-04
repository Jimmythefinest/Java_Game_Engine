package com.njst.gaming.Natives;

import org.lwjgl.opengl.GL43;

import java.util.HashMap;
import java.util.Map;

public class ComputeShader {
    private int program = 0;
    public String err = "";

    private final Map<Integer, SSBO> buffers = new HashMap<>();
    private final Map<Integer, Integer> bufferSizes = new HashMap<>();

    public ComputeShader(String shaderCode) {
        createShaderProgram(shaderCode);
    }

    private void createShaderProgram(String shaderCode) {
        int shader = GL43.glCreateShader(GL43.GL_COMPUTE_SHADER);
        GL43.glShaderSource(shader, shaderCode);
        GL43.glCompileShader(shader);

        int status = GL43.glGetShaderi(shader, GL43.GL_COMPILE_STATUS);
        if (status == GL43.GL_FALSE) {
            err += "ComputeShader compile error:\n" + GL43.glGetShaderInfoLog(shader);
            GL43.glDeleteShader(shader);
            return;
        }

        program = GL43.glCreateProgram();
        GL43.glAttachShader(program, shader);
        GL43.glLinkProgram(program);
        GL43.glDeleteShader(shader);

        status = GL43.glGetProgrami(program, GL43.GL_LINK_STATUS);
        if (status == GL43.GL_FALSE) {
            err += "ComputeShader link error:\n" + GL43.glGetProgramInfoLog(program);
            GL43.glDeleteProgram(program);
            program = 0;
        }
    }

    public void bindBufferToShader(int bindingIndex, float[] data) {
        SSBO buffer = new SSBO();
        buffer.setData(data, GL43.GL_DYNAMIC_COPY);
        buffer.bindToShader(bindingIndex);
        buffers.put(bindingIndex, buffer);
        bufferSizes.put(bindingIndex, data.length);
    }

    public void bindBufferToShader(int bindingIndex, int[] data) {
        SSBO buffer = new SSBO();
        buffer.setData(data, GL43.GL_DYNAMIC_COPY);
        buffer.bindToShader(bindingIndex);
        buffers.put(bindingIndex, buffer);
        bufferSizes.put(bindingIndex, data.length);
    }

    public void updateBuffer(int bindingIndex, float[] data) {
        SSBO buffer = buffers.get(bindingIndex);
        bufferSizes.put(bindingIndex, data.length);
        if (buffer != null) {
            buffer.updateData(data);
        }
    }

    public void dispatch(int x, int y, int z) {
        if (program == 0) return;

        GL43.glUseProgram(program);
        GL43.glDispatchCompute(x, y, z);
        GL43.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
    }

    public float[] getBufferData(int bindingIndex) {
        SSBO buffer = buffers.get(bindingIndex);
        return buffer != null ? buffer.getData(bufferSizes.get(bindingIndex)) : null;
    }

    public void recompile(String shaderCode) {
        release();
        createShaderProgram(shaderCode);
    }

    public void release() {
        if (program != 0) {
            GL43.glDeleteProgram(program);
            program = 0;
        }

        for (SSBO buffer : buffers.values()) {
            buffer.delete();
        }
        buffers.clear();
        bufferSizes.clear();
    }

    public int getProgram() {
        return program;
    }
}
