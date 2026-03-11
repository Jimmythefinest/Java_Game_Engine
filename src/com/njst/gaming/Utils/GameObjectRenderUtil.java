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
        if (renderer == null || renderer.camera == null) {
            return null;
        }

        object.updateModelMatrix();

        Vector3 localCenter = getLocalBoundsCenter(object);
        float worldRadius = computeWorldBoundingSphereRadius(object);
        if (localCenter == null || worldRadius <= 0.0001f) {
            return null;
        }

        Vector3 worldCenter = object.modelMatrix.multiply(localCenter);
        Matrix4 view = renderer.camera.getViewMatrix();
        Matrix4 proj = renderer.camera.getProjectionMatrix();
        Matrix4 viewProj = new Matrix4().set(proj.r).multiply(view);

        Vector3 ndcCenter = viewProj.multiply(worldCenter);
        float centerDepth = -view.multiply(worldCenter).z;
        if (centerDepth <= 0.0001f) {
            return null;
        }

        float tanAlpha;
        if (centerDepth > worldRadius + 0.0001f) {
            tanAlpha = worldRadius / (float) Math.sqrt(centerDepth * centerDepth - worldRadius * worldRadius);
        } else {
            tanAlpha = Float.POSITIVE_INFINITY;
        }

        float tanFov = (float) Math.tan(Math.toRadians(renderer.camera.FOV * 0.5f));
        float aspect = renderer.camera.aspect;
        if (tanFov <= 0.0001f || aspect <= 0.0001f) {
            return null;
        }

        float ndcHalfY = tanAlpha / tanFov;
        float ndcHalfX = ndcHalfY / aspect;
        if (!Float.isFinite(ndcHalfX) || !Float.isFinite(ndcHalfY)) {
            ndcHalfX = 2f;
            ndcHalfY = 2f;
        }

        float minNdcX = ndcCenter.x - ndcHalfX;
        float maxNdcX = ndcCenter.x + ndcHalfX;
        float minNdcY = ndcCenter.y - ndcHalfY;
        float maxNdcY = ndcCenter.y + ndcHalfY;

        float minX = (minNdcX * 0.5f + 0.5f) * width;
        float maxX = (maxNdcX * 0.5f + 0.5f) * width;
        float maxY = height - ((minNdcY * 0.5f + 0.5f) * height);
        float minY = height - ((maxNdcY * 0.5f + 0.5f) * height);

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

    private static Vector3 getLocalBoundsCenter(GameObject object) {
        if (object.localMin != null && object.localMax != null) {
            return new Vector3(
                    (object.localMin.x + object.localMax.x) * 0.5f,
                    (object.localMin.y + object.localMax.y) * 0.5f,
                    (object.localMin.z + object.localMax.z) * 0.5f);
        }
        if (object.min != null && object.max != null) {
            return new Vector3(
                    (object.min.x + object.max.x) * 0.5f,
                    (object.min.y + object.max.y) * 0.5f,
                    (object.min.z + object.max.z) * 0.5f);
        }
        return new Vector3(0f, 0f, 0f);
    }

    private static float computeWorldBoundingSphereRadius(GameObject object) {
        if (object.localMin != null && object.localMax != null) {
            float ex = Math.abs(object.localMax.x - object.localMin.x) * 0.5f;
            float ey = Math.abs(object.localMax.y - object.localMin.y) * 0.5f;
            float ez = Math.abs(object.localMax.z - object.localMin.z) * 0.5f;
            float localRadius = (float) Math.sqrt(ex * ex + ey * ey + ez * ez);
            return Math.max(0.0001f, localRadius * getMaxModelAxisScale(object));
        }
        if (object.min != null && object.max != null) {
            float dx = object.max.x - object.min.x;
            float dy = object.max.y - object.min.y;
            float dz = object.max.z - object.min.z;
            float worldRadius = 0.5f * (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            return Math.max(0.0001f, worldRadius);
        }
        return 1f;
    }

    private static float getMaxModelAxisScale(GameObject object) {
        if (object.modelMatrix == null || object.modelMatrix.r == null || object.modelMatrix.r.length < 16) {
            return 1f;
        }
        float[] m = object.modelMatrix.r;
        float sx = (float) Math.sqrt(m[0] * m[0] + m[1] * m[1] + m[2] * m[2]);
        float sy = (float) Math.sqrt(m[4] * m[4] + m[5] * m[5] + m[6] * m[6]);
        float sz = (float) Math.sqrt(m[8] * m[8] + m[9] * m[9] + m[10] * m[10]);
        return Math.max(0.0001f, Math.max(sx, Math.max(sy, sz)));
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
