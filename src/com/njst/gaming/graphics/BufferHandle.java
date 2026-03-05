package com.njst.gaming.graphics;

public interface BufferHandle {
    void bind();

    void unbind();

    void setData(float[] data, int usage);

    void setData(int[] data, int usage);

    void updateData(float[] data);

    void bindToShader(int bindingPoint);

    float[] getData(int numElements);

    void delete();

    int getId();
}
