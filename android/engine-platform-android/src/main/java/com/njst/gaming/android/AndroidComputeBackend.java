package com.njst.gaming.android;

import android.content.Context;

import com.njst.gaming.graphics.ComputeBackend;

public class AndroidComputeBackend implements ComputeBackend {
    private final AndroidComputeShader computeShader;

    public AndroidComputeBackend(Context context) {
        this(AndroidAssetLoader.readText(context.getAssets(), "resources/shaders/nn_compute_shader.glsl"));
    }

    public AndroidComputeBackend(String shaderSource) {
        this.computeShader = new AndroidComputeShader(shaderSource);
    }

    @Override
    public boolean hasError() {
        return computeShader.err != null && !computeShader.err.isEmpty();
    }

    @Override
    public String getError() {
        return computeShader.err;
    }

    @Override
    public boolean hasBuffer(int bindingIndex) {
        return computeShader.hasBuffer(bindingIndex);
    }

    @Override
    public int getBufferSize(int bindingIndex) {
        return computeShader.getBufferSize(bindingIndex);
    }

    @Override
    public void bindBuffer(int bindingIndex, float[] data) {
        computeShader.bindBufferToShader(bindingIndex, data);
    }

    @Override
    public void bindBuffer(int bindingIndex, int[] data) {
        computeShader.bindBufferToShader(bindingIndex, data);
    }

    @Override
    public void updateBuffer(int bindingIndex, float[] data) {
        computeShader.updateBuffer(bindingIndex, data);
    }

    @Override
    public void updateBuffer(int bindingIndex, int[] data) {
        computeShader.updateBuffer(bindingIndex, data);
    }

    @Override
    public void dispatch(int x, int y, int z) {
        computeShader.dispatch(x, y, z);
    }

    @Override
    public float[] readBuffer(int bindingIndex) {
        return computeShader.getBufferData(bindingIndex);
    }

    @Override
    public void bindBufferToShaderBinding(int sourceBindingIndex, int targetBindingIndex) {
        computeShader.bindBufferToShaderBinding(sourceBindingIndex, targetBindingIndex);
    }

    @Override
    public void release() {
        computeShader.release();
    }
}
