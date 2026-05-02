package com.njst.gaming.android;

import android.opengl.GLES31;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class AndroidComputeShader {
    private static final String TAG = "NJST";
    private static final int DISPATCH_LOG_INTERVAL = 120;

    private final Map<Integer, AndroidShaderStorageBuffer> buffers = new HashMap<>();
    private final Map<Integer, Integer> bufferSizes = new HashMap<>();
    private final String label;
    private int program;
    private long dispatchNanos;
    private int dispatchSamples;
    public String err = "";

    public AndroidComputeShader(String shaderCode) {
        this.label = parseLabel(shaderCode);
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

    public void updateBuffer(int bindingIndex, int[] data) {
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
        long startNanos = System.nanoTime();
        GLES31.glUseProgram(program);
        GLES31.glDispatchCompute(x, y, z);
        GLES31.glMemoryBarrier(GLES31.GL_SHADER_STORAGE_BARRIER_BIT);
        recordDispatchTime(System.nanoTime() - startNanos, x, y, z);
    }

    private void recordDispatchTime(long elapsedNanos, int x, int y, int z) {
        dispatchNanos += elapsedNanos;
        dispatchSamples++;
        if (dispatchSamples < DISPATCH_LOG_INTERVAL) {
            return;
        }
        float averageMs = dispatchNanos / (1_000_000f * dispatchSamples);
        Log.i(TAG, "AndroidComputeShader.dispatchOnly label=" + label
                + " avgMs=" + averageMs
                + " samples=" + dispatchSamples
                + " groups=" + x + "," + y + "," + z);
        dispatchNanos = 0L;
        dispatchSamples = 0;
    }

    private String parseLabel(String shaderCode) {
        if (shaderCode == null) {
            return "compute";
        }
        String marker = "DISPATCH_LABEL:";
        int index = shaderCode.indexOf(marker);
        if (index < 0) {
            return "compute";
        }
        int start = index + marker.length();
        int end = shaderCode.indexOf('\n', start);
        if (end < 0) {
            end = shaderCode.length();
        }
        String value = shaderCode.substring(start, end).trim();
        return value.isEmpty() ? "compute" : value;
    }

    public float[] getBufferData(int bindingIndex) {
        AndroidShaderStorageBuffer buffer = buffers.get(bindingIndex);
        Integer size = bufferSizes.get(bindingIndex);
        return buffer != null && size != null ? buffer.getData(size) : null;
    }

    public void bindBufferToShaderBinding(int sourceBindingIndex, int targetBindingIndex) {
        AndroidShaderStorageBuffer buffer = buffers.get(sourceBindingIndex);
        if (buffer != null) {
            buffer.bindToShader(targetBindingIndex);
        }
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
