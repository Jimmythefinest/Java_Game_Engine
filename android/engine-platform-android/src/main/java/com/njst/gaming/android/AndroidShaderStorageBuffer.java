package com.njst.gaming.android;

import android.opengl.GLES30;
import android.opengl.GLES31;

import com.njst.gaming.graphics.BufferHandle;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class AndroidShaderStorageBuffer implements BufferHandle {
    private final int bufferId;

    public AndroidShaderStorageBuffer() {
        int[] buffers = new int[1];
        GLES31.glGenBuffers(1, buffers, 0);
        bufferId = buffers[0];
    }

    @Override
    public void bind() {
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, bufferId);
    }

    @Override
    public void unbind() {
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, 0);
    }

    @Override
    public void setData(float[] data, int usage) {
        bind();
        FloatBuffer buffer = ByteBuffer.allocateDirect(data.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(data).position(0);
        GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, data.length * Float.BYTES, buffer, usage);
        unbind();
    }

    @Override
    public void setData(int[] data, int usage) {
        bind();
        IntBuffer buffer = ByteBuffer.allocateDirect(data.length * Integer.BYTES)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer();
        buffer.put(data).position(0);
        GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, data.length * Integer.BYTES, buffer, usage);
        unbind();
    }

    @Override
    public void updateData(float[] data) {
        bind();
        FloatBuffer buffer = ByteBuffer.allocateDirect(data.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(data).position(0);
        GLES31.glBufferSubData(GLES31.GL_SHADER_STORAGE_BUFFER, 0, data.length * Float.BYTES, buffer);
        unbind();
    }

    @Override
    public void updateData(int[] data) {
        bind();
        IntBuffer buffer = ByteBuffer.allocateDirect(data.length * Integer.BYTES)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer();
        buffer.put(data).position(0);
        GLES31.glBufferSubData(GLES31.GL_SHADER_STORAGE_BUFFER, 0, data.length * Integer.BYTES, buffer);
        unbind();
    }

    @Override
    public void bindToShader(int bindingPoint) {
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, bindingPoint, bufferId);
    }

    @Override
    public float[] getData(int numElements) {
        bind();
        Buffer mapped = GLES30.glMapBufferRange(
                GLES31.GL_SHADER_STORAGE_BUFFER,
                0,
                numElements * Float.BYTES,
                GLES30.GL_MAP_READ_BIT);

        float[] result = new float[numElements];
        if (mapped instanceof ByteBuffer) {
            FloatBuffer floatBuffer = ((ByteBuffer) mapped)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            floatBuffer.get(result);
        }

        GLES30.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER);
        unbind();
        return result;
    }

    @Override
    public void delete() {
        int[] buffers = new int[] { bufferId };
        GLES31.glDeleteBuffers(1, buffers, 0);
    }

    @Override
    public int getId() {
        return bufferId;
    }
}
