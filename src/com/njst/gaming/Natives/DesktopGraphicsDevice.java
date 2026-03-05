package com.njst.gaming.Natives;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import com.njst.gaming.graphics.BufferHandle;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.graphics.ShaderHandle;

public class DesktopGraphicsDevice implements GraphicsDevice {
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
