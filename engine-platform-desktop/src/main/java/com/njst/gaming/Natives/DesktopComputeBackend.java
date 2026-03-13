package com.njst.gaming.Natives;

import com.njst.gaming.graphics.ComputeBackend;

public class DesktopComputeBackend implements ComputeBackend {
    private final ComputeShader computeShader;

    public DesktopComputeBackend() {
        this(ShaderProgram.loadShader("resources/shaders/nn_compute_shader.glsl"));
    }

    public DesktopComputeBackend(String shaderSource) {
        this.computeShader = new ComputeShader(shaderSource);
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
    public void dispatch(int x, int y, int z) {
        computeShader.dispatch(x, y, z);
    }

    @Override
    public float[] readBuffer(int bindingIndex) {
        return computeShader.getBufferData(bindingIndex);
    }

    @Override
    public void release() {
        computeShader.release();
    }
}
