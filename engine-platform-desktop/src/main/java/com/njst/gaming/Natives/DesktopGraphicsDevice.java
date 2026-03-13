package com.njst.gaming.Natives;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDrawElements;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.njst.gaming.graphics.BufferHandle;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.graphics.ImposterBakeResult;
import com.njst.gaming.graphics.ShaderHandle;
import com.njst.gaming.Renderer;
import com.njst.gaming.objects.GameObject;

public class DesktopGraphicsDevice implements GraphicsDevice {
    private final DesktopImposterBaker imposterBaker = new DesktopImposterBaker();

    @Override
    public ShaderHandle createShaderProgram(String vertexShaderSource, String fragmentShaderSource) {
        return new ShaderProgram(vertexShaderSource, fragmentShaderSource);
    }

    @Override
    public BufferHandle createShaderStorageBuffer() {
        return new SSBO();
    }

    @Override
    public String loadShaderSource(String filePath) {
        return ShaderProgram.loadShader(filePath);
    }

    @Override
    public int loadTexture(String texturePath) {
        return ShaderProgram.loadTexture(texturePath);
    }

    @Override
    public ImposterBakeResult bakeImposter(Renderer renderer, GameObject object, int width, int height) {
        return imposterBaker.bake(renderer, object, width, height);
    }

    @Override
    public void releaseTexture(int textureId) {
        imposterBaker.releaseTexture(textureId);
    }

    @Override
    public int createVertexArray() {
        return GL30.glGenVertexArrays();
    }

    @Override
    public int[] createBuffers(int count) {
        int[] buffers = new int[count];
        GL15.glGenBuffers(buffers);
        return buffers;
    }

    @Override
    public void bindVertexArray(int vaoId) {
        GL30.glBindVertexArray(vaoId);
    }

    @Override
    public void uploadArrayBufferFloat(int bufferId, float[] data) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bufferId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
    }

    @Override
    public void uploadArrayBufferInt(int bufferId, int[] data) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bufferId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
    }

    @Override
    public void uploadElementArrayBufferInt(int bufferId, int[] data) {
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, bufferId);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
    }

    @Override
    public void setVertexAttribPointer(int bufferId, int location, int size) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bufferId);
        GL20.glVertexAttribPointer(location, size, GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(location);
    }

    @Override
    public void updateArrayBufferFloat(int bufferId, float[] data) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bufferId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
    }

    @Override
    public void drawElementsTriangles(int indexCount) {
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
    }

    @Override
    public void drawElementsLines(int indexCount) {
        glDrawElements(GL_LINES, indexCount, GL_UNSIGNED_INT, 0);
    }

    @Override
    public void deleteBuffers(int[] buffers) {
        GL15.glDeleteBuffers(buffers);
    }

    @Override
    public void deleteVertexArrays(int[] vaos) {
        GL30.glDeleteVertexArrays(vaos);
    }

    @Override
    public void enableBlendAndDepth() {
        GL30.glEnable(GL30.GL_BLEND);
        GL30.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);
        GL30.glEnable(GL30.GL_DEPTH_TEST);
    }

    @Override
    public void clearColorAndDepth() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void viewport(int width, int height) {
        GL30.glViewport(0, 0, width, height);
    }

    @Override
    public int dynamicDrawUsage() {
        return GL15.GL_DYNAMIC_DRAW;
    }
}
