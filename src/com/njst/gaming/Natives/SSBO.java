package com.njst.gaming.Natives;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL43;

import com.njst.gaming.Utils.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class SSBO {
    private int ssboId;
    private FloatBuffer reusableBuffer;

    public SSBO() {
        ssboId = GL43.glGenBuffers();
    }

    public void bind() {
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, ssboId);
    }

    public void unbind() {
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
    }

    public void setData(float[] data, int usage) {
        bind();
        if (reusableBuffer == null || reusableBuffer.capacity() < data.length) {
            reusableBuffer = BufferUtils.createFloatBuffer(data.length);
        }
        reusableBuffer.clear();
        reusableBuffer.put(data).flip();

        GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, reusableBuffer, usage);
        unbind();
    }

    public void setData(int[] data, int usage) {
        bind();
        IntBuffer buffer = Utils.Array_to_Buffer(data);
        GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, buffer, usage);
        unbind();
    }

    public void updateData(float[] data) {
        bind();
        FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
        buffer.put(data).flip();

        GL43.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, buffer);
        unbind();
    }

    public void bindToShader(int bindingPoint) {
        GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, bindingPoint, ssboId);
    }

    public float[] getData(int numElements) {
        bind();

        ByteBuffer byteBuffer = GL43.glMapBufferRange(
                GL43.GL_SHADER_STORAGE_BUFFER,
                0,
                numElements * Float.BYTES,
                GL43.GL_MAP_READ_BIT);

        FloatBuffer floatBuffer = byteBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
        float[] data = new float[numElements];
        floatBuffer.get(data);

        GL43.glUnmapBuffer(GL43.GL_SHADER_STORAGE_BUFFER);
        unbind();
        return data;
    }

    public void delete() {
        GL43.glDeleteBuffers(ssboId);
        ssboId = 0;
    }

    public int getId() {
        return ssboId;
    }
}