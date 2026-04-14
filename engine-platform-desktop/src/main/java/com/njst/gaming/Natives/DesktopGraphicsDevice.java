package com.njst.gaming.Natives;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDrawElements;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.njst.gaming.Renderer;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.collision.SphericalHeightmapShape;
import com.njst.gaming.graphics.BufferHandle;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.graphics.ImposterBakeResult;
import com.njst.gaming.graphics.ShaderHandle;
import com.njst.gaming.objects.GameObject;

public class DesktopGraphicsDevice implements GraphicsDevice {
    private final DesktopImposterBaker imposterBaker = new DesktopImposterBaker();
    private int whiteTextureId;

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
    public String loadTextResource(String filePath) {
        return ShaderProgram.loadTextResource(filePath);
    }

    @Override
    public byte[] loadBinaryResource(String filePath) {
        return ShaderProgram.loadBinaryResource(filePath);
    }

    @Override
    public int loadTexture(String texturePath) {
        if (texturePath == null || texturePath.isEmpty() || texturePath.startsWith("generated:")) {
            return getWhiteTexture();
        }
        return ShaderProgram.loadTexture(texturePath);
    }

    @Override
    public int createTextureRGBA(int width, int height, byte[] rgbaPixels) {
        return ShaderProgram.createTextureRGBA(width, height, rgbaPixels);
    }

    @Override
    public com.njst.gaming.graphics.ShadowMapHandle createShadowMap(int width, int height) {
        return new DesktopShadowMapHandle(width, height);
    }

    @Override
    public ImposterBakeResult bakeImposter(Renderer renderer, GameObject object, int width, int height) {
        return imposterBaker.bake(renderer, object, width, height);
    }

    @Override
    public SphericalHeightmapShape bakeSphericalHeightmap(Renderer renderer, GameObject object, int width, int height,
            Vector3 localCenter) {
        return com.njst.gaming.Utils.GameObjectRenderUtil.bakeSphericalHeightmap(renderer, object, width, height,
                localCenter);
    }

    @Override
    public void releaseTexture(int textureId) {
        imposterBaker.releaseTexture(textureId);
        if (textureId == whiteTextureId) {
            whiteTextureId = 0;
        }
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
    public void setVertexAttribIPointer(int bufferId, int location, int size) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bufferId);
        GL30.glVertexAttribIPointer(location, size, GL_UNSIGNED_INT, 0, 0);
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
    public void clearDepth() {
        glClear(GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void bindShadowMap(com.njst.gaming.graphics.ShadowMapHandle shadowMap) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, ((DesktopShadowMapHandle) shadowMap).getFramebufferId());
    }

    @Override
    public void bindDefaultFramebuffer() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void dumpShadowMap(com.njst.gaming.graphics.ShadowMapHandle shadowMap, String outputPath) {
        if (!(shadowMap instanceof DesktopShadowMapHandle) || outputPath == null || outputPath.isEmpty()) {
            return;
        }
        DesktopShadowMapHandle desktopShadowMap = (DesktopShadowMapHandle) shadowMap;
        FloatBuffer pixels = BufferUtils.createFloatBuffer(desktopShadowMap.getWidth() * desktopShadowMap.getHeight());
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, desktopShadowMap.getTextureId());
        GL30.glGetTexImage(GL30.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT, GL_FLOAT, pixels);
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, 0);

        float minDepth = 1f;
        float maxDepth = 0f;
        int writtenPixelCount = 0;
        for (int i = 0; i < pixels.capacity(); i++) {
            float depth = pixels.get(i);
            if (depth < 0f || depth > 1f) {
                continue;
            }
            minDepth = Math.min(minDepth, depth);
            maxDepth = Math.max(maxDepth, depth);
            if (depth < 0.999999f) {
                writtenPixelCount++;
            }
        }

        BufferedImage image = new BufferedImage(
                desktopShadowMap.getWidth(),
                desktopShadowMap.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        BufferedImage occupancyImage = new BufferedImage(
                desktopShadowMap.getWidth(),
                desktopShadowMap.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        float range = Math.max(maxDepth - minDepth, 0.000001f);
        for (int y = 0; y < desktopShadowMap.getHeight(); y++) {
            for (int x = 0; x < desktopShadowMap.getWidth(); x++) {
                float depth = pixels.get(x + (y * desktopShadowMap.getWidth()));
                float normalized = (depth - minDepth) / range;
                int grayscale = Math.max(0, Math.min(255, Math.round(normalized * 255f)));
                int argb = 0xFF000000 | (grayscale << 16) | (grayscale << 8) | grayscale;
                image.setRGB(x, desktopShadowMap.getHeight() - y - 1, argb);
                int occupancy = depth < 0.999999f ? 255 : 0;
                int occupancyArgb = 0xFF000000 | (occupancy << 16) | (occupancy << 8) | occupancy;
                occupancyImage.setRGB(x, desktopShadowMap.getHeight() - y - 1, occupancyArgb);
            }
        }

        File output = new File(outputPath);
        File parent = output.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try {
            ImageIO.write(image, "png", output);
            File occupancyOutput = new File(withSuffix(outputPath, "_occupancy"));
            ImageIO.write(occupancyImage, "png", occupancyOutput);
            System.out.println("[ShadowMap] Saved debug depth map to: " + output.getAbsolutePath());
            System.out.println("[ShadowMap] Saved occupancy map to: " + occupancyOutput.getAbsolutePath());
            System.out.println("[ShadowMap] Depth stats min=" + minDepth
                    + " max=" + maxDepth
                    + " touchedPixels=" + writtenPixelCount
                    + "/" + pixels.capacity());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write shadow map debug image: " + outputPath, e);
        }
    }

    private String withSuffix(String outputPath, String suffix) {
        int extensionIndex = outputPath.lastIndexOf('.');
        if (extensionIndex <= 0) {
            return outputPath + suffix;
        }
        return outputPath.substring(0, extensionIndex) + suffix + outputPath.substring(extensionIndex);
    }

    @Override
    public void viewport(int width, int height) {
        GL30.glViewport(0, 0, width, height);
    }

    @Override
    public int dynamicDrawUsage() {
        return GL15.GL_DYNAMIC_DRAW;
    }

    private int getWhiteTexture() {
        if (whiteTextureId != 0) {
            return whiteTextureId;
        }
        whiteTextureId = ShaderProgram.createTextureRGBA(1, 1, new byte[] {
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
        });
        return whiteTextureId;
    }
}
