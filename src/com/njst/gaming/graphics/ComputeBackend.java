package com.njst.gaming.graphics;

public interface ComputeBackend {
    boolean hasError();

    String getError();

    boolean hasBuffer(int bindingIndex);

    int getBufferSize(int bindingIndex);

    void bindBuffer(int bindingIndex, float[] data);

    void bindBuffer(int bindingIndex, int[] data);

    void updateBuffer(int bindingIndex, float[] data);

    void dispatch(int x, int y, int z);

    float[] readBuffer(int bindingIndex);

    void release();
}
