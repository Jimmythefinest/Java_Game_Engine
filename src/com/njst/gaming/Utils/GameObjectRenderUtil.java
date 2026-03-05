package com.njst.gaming.Utils;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Renderer;
import com.njst.gaming.objects.GameObject;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.*;

public class GameObjectRenderUtil {
    private static final int CROP_PADDING_PX = 0;

    private GameObjectRenderUtil() {}

    public static BufferedImage renderToBitmap(Renderer renderer, GameObject object, int width, int height) {
        if (renderer == null || object == null || width <= 0 || height <= 0) {
            return null;
        }

        int[] crop = computeScreenBounds(renderer, object, width, height);
        int readX = 0;
        int readY = 0;
        int readW = width;
        int readH = height;
        if (crop != null && crop[2] > 0 && crop[3] > 0) {
            readX = crop[0];
            readY = height - (crop[1] + crop[3]);
            readW = crop[2];
            readH = crop[3];
        }

        int fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glDrawBuffer(GL_COLOR_ATTACHMENT0);

        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);

        int rbo = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rbo);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glDeleteFramebuffers(fbo);
            glDeleteRenderbuffers(rbo);
            glDeleteTextures(texture);
            return null;
        }

        int prevWidth = renderer.width;
        int prevHeight = renderer.height;
        glViewport(0, 0, width, height);
        glClearColor(0, 0, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        renderer.renderObjectLikeMainPass(object);

        ByteBuffer pixels = BufferUtils.createByteBuffer(readW * readH * 4);
        glReadPixels(readX, readY, readW, readH, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

        BufferedImage image = new BufferedImage(readW, readH, BufferedImage.TYPE_INT_ARGB);
        int stride = readW * 4;
        for (int y = 0; y < readH; y++) {
            int srcRow = (readH - 1 - y) * stride;
            for (int x = 0; x < readW; x++) {
                int i = srcRow + x * 4;
                int r = pixels.get(i) & 0xFF;
                int g = pixels.get(i + 1) & 0xFF;
                int b = pixels.get(i + 2) & 0xFF;
                int a = pixels.get(i + 3) & 0xFF;
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                image.setRGB(x, y, argb);
            }
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteFramebuffers(fbo);
        glDeleteRenderbuffers(rbo);
        glDeleteTextures(texture);
        glViewport(0, 0, prevWidth, prevHeight);

        return image;
    }

    public static int uploadImageAsTexture(BufferedImage image) {
        if (image == null) {
            return 0;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) {
            return 0;
        }

        int[] argb = new int[width * height];
        image.getRGB(0, 0, width, height, argb, 0, width);
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        // BufferedImage rows are top-to-bottom, while OpenGL expects bottom-to-top.
        for (int y = height - 1; y >= 0; y--) {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                int pixel = argb[row + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
        }
        buffer.flip();

        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);
        return texture;
    }

    private static int[] computeScreenBounds(Renderer renderer, GameObject object, int width, int height) {
        // Use LOCAL-space bounds + model matrix (full MVP) to match computeImposterWorldSize exactly.
        // Using the world-space AABB (min/max) without the model matrix gives an inflated crop
        // for rotated objects, leaving blank borders on the baked texture.
        Vector3 mn = object.localMin;
        Vector3 mx = object.localMax;
        if (mn == null || mx == null) {
            return null;
        }

        Vector3[] corners = new Vector3[] {
                new Vector3(mn.x, mn.y, mn.z),
                new Vector3(mn.x, mn.y, mx.z),
                new Vector3(mn.x, mx.y, mn.z),
                new Vector3(mn.x, mx.y, mx.z),
                new Vector3(mx.x, mn.y, mn.z),
                new Vector3(mx.x, mn.y, mx.z),
                new Vector3(mx.x, mx.y, mn.z),
                new Vector3(mx.x, mx.y, mx.z)
        };

        Matrix4 view = renderer.camera.getViewMatrix();
        Matrix4 proj = renderer.camera.getProjectionMatrix();
        // Full MVP: proj * view * model — same as computeImposterWorldSize
        Matrix4 viewProj = new Matrix4().set(proj.r).multiply(view).multiply(object.modelMatrix);

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        for (Vector3 c : corners) {
            Vector3 ndc = viewProj.multiply(c);
            float sx = (ndc.x * 0.5f + 0.5f) * width;
            float sy = (ndc.y * 0.5f + 0.5f) * height;
            float imgY = height - sy;

            if (sx < minX) minX = sx;
            if (sx > maxX) maxX = sx;
            if (imgY < minY) minY = imgY;
            if (imgY > maxY) maxY = imgY;
        }

        int x0 = clamp((int) Math.floor(minX) - CROP_PADDING_PX, 0, width - 1);
        int y0 = clamp((int) Math.floor(minY) - CROP_PADDING_PX, 0, height - 1);
        int x1 = clamp((int) Math.ceil(maxX) + CROP_PADDING_PX, 0, width);
        int y1 = clamp((int) Math.ceil(maxY) + CROP_PADDING_PX, 0, height);

        int w = x1 - x0;
        int h = y1 - y0;
        if (w <= 0 || h <= 0) {
            return null;
        }
        return new int[] { x0, y0, w, h };
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
