package com.njst.gaming.graphics;

public class NullGraphicsDevice implements GraphicsDevice {
    private static IllegalStateException unsupported() {
        return new IllegalStateException("GraphicsDevice is not configured.");
    }

    @Override
    public ShaderHandle createShaderProgram(String vertexShaderSource, String fragmentShaderSource) {
        throw unsupported();
    }

    @Override
    public BufferHandle createShaderStorageBuffer() {
        throw unsupported();
    }

    @Override
    public String loadShaderSource(String filePath) {
        throw unsupported();
    }

    @Override
    public int loadTexture(String texturePath) {
        throw unsupported();
    }

    @Override
    public int createVertexArray() {
        throw unsupported();
    }

    @Override
    public int[] createBuffers(int count) {
        throw unsupported();
    }

    @Override
    public void bindVertexArray(int vaoId) {
        throw unsupported();
    }

    @Override
    public void uploadArrayBufferFloat(int bufferId, float[] data) {
        throw unsupported();
    }

    @Override
    public void uploadArrayBufferInt(int bufferId, int[] data) {
        throw unsupported();
    }

    @Override
    public void uploadElementArrayBufferInt(int bufferId, int[] data) {
        throw unsupported();
    }

    @Override
    public void setVertexAttribPointer(int bufferId, int location, int size) {
        throw unsupported();
    }

    @Override
    public void updateArrayBufferFloat(int bufferId, float[] data) {
        throw unsupported();
    }

    @Override
    public void drawElementsTriangles(int indexCount) {
        throw unsupported();
    }

    @Override
    public void drawElementsLines(int indexCount) {
        throw unsupported();
    }

    @Override
    public void deleteBuffers(int[] buffers) {
        throw unsupported();
    }

    @Override
    public void deleteVertexArrays(int[] vaos) {
        throw unsupported();
    }

    @Override
    public void enableBlendAndDepth() {
        throw unsupported();
    }

    @Override
    public void clearColorAndDepth() {
        throw unsupported();
    }

    @Override
    public void viewport(int width, int height) {
        throw unsupported();
    }

    @Override
    public int dynamicDrawUsage() {
        throw unsupported();
    }
}
