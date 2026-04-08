package com.njst.gaming.Utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import com.njst.gaming.*;
import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;

import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Camera;
import com.njst.gaming.Renderer;
import com.njst.gaming.collision.SphericalHeightmapShape;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.objects.GameObject;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.*;

public class GameObjectRenderUtil {
    private static final int CROP_PADDING_PX = 0;
    private static final int FACE_POSITIVE_X = 0;
    private static final int FACE_NEGATIVE_X = 1;
    private static final int FACE_POSITIVE_Y = 2;
    private static final int FACE_NEGATIVE_Y = 3;
    private static final int FACE_POSITIVE_Z = 4;
    private static final int FACE_NEGATIVE_Z = 5;
    private static final String[] FACE_NAMES = { "posx", "negx", "posy", "negy", "posz", "negz" };

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
         File output = new File(data.rootDirectory, "flat_bake_face_" + temp++ + ".png");
            try {
                ImageIO.write(image, "png", output);
            } catch (Exception e) {
                throw new RuntimeException("Failed to write spherical bake face image: " + output.getAbsolutePath(), e);
            }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteFramebuffers(fbo);
        glDeleteRenderbuffers(rbo);
        glDeleteTextures(texture);
        glViewport(0, 0, prevWidth, prevHeight);

        return image;
    }
    static int temp=0;

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

    public static SphericalHeightmapShape bakeSphericalHeightmap(Renderer renderer, GameObject object, int width, int height,
            Vector3 localCenter) {
        if (renderer == null || object == null || width <= 0 || height <= 0) {
            return null;
        }

        object.setGraphicsDevice(renderer.getGraphicsDevice());
        if (object.vaoIds[0] == 0) {
            object.generateBuffers();
        }

        int faceSize = Math.max(16, Math.max(width / 4, height / 2));
        float[][] cubeFaces = renderRadialDistanceCubeFaces(object, faceSize, localCenter != null ? localCenter : new Vector3());
        float[][] heightSamples = new float[height][width];
        for (int row = 0; row < height; row++) {
            float v = (row + 0.5f) / (float) height;
            float polar = v * (float) Math.PI;
            float sinPolar = (float) Math.sin(polar);
            float y = (float) Math.cos(polar);
            for (int col = 0; col < width; col++) {
                float u = (col + 0.5f) / (float) width;
                float azimuth = (u - 0.5f) * (float) (Math.PI * 2.0);
                Vector3 direction = new Vector3(
                        (float) Math.cos(azimuth) * sinPolar,
                        y,
                        (float) Math.sin(azimuth) * sinPolar);
                heightSamples[row][col] = sampleCubemapDistance(cubeFaces, direction);
            }
        }

        return new SphericalHeightmapShape(heightSamples, 0f, localCenter != null ? localCenter : new Vector3());
    }

    public static void dumpSphericalHeightmapCubeFaces(Renderer renderer, GameObject object, int faceSize,
            Vector3 localCenter, File outputDirectory) {
        if (renderer == null || object == null || faceSize <= 0 || outputDirectory == null) {
            return;
        }

        object.setGraphicsDevice(renderer.getGraphicsDevice());
        if (object.vaoIds[0] == 0) {
            object.generateBuffers();
        }

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        float[][] cubeFaces = renderRadialDistanceCubeFaces(object, faceSize, localCenter != null ? localCenter : new Vector3());
        for (int i = 0; i < cubeFaces.length; i++) {
            float[] face = cubeFaces[i];
            float min = Float.POSITIVE_INFINITY;
            float max = Float.NEGATIVE_INFINITY;
            for (float sample : face) {
                if (sample < min) {
                    min = sample;
                }
                if (sample > max) {
                    max = sample;
                }
            }

            BufferedImage image = toGrayscaleImage(face, faceSize, faceSize, min, max);
            File output = new File(outputDirectory, "spherical_bake_face_" + FACE_NAMES[i] + ".png");
            try {
                ImageIO.write(image, "png", output);
            } catch (Exception e) {
                throw new RuntimeException("Failed to write spherical bake face image: " + output.getAbsolutePath(), e);
            }
        }
    }

    public static void dumpSphericalHeightmapCubeFaceColorRenders(Renderer renderer, GameObject object, int faceSize,
            Vector3 localCenter, File outputDirectory) {
        if (renderer == null || object == null || faceSize <= 0 || outputDirectory == null) {
            return;
        }

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        int fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glDrawBuffer(GL_COLOR_ATTACHMENT0);

        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, faceSize, faceSize, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);

        int rbo = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, faceSize, faceSize);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rbo);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glDeleteFramebuffers(fbo);
            glDeleteRenderbuffers(rbo);
            glDeleteTextures(texture);
            return;
        }

        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        boolean cullEnabled = glIsEnabled(GL_CULL_FACE);
        boolean depthEnabled = glIsEnabled(GL_DEPTH_TEST);
        int previousDepthFunc = glGetInteger(GL_DEPTH_FUNC);

        glViewport(0, 0, faceSize, faceSize);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glClearDepth(1.0);

        Vector3[] directions = new Vector3[] {
                new Vector3(1f, 0f, 0f),
                new Vector3(-1f, 0f, 0f),
                new Vector3(0f, 1f, 0f),
                new Vector3(0f, -1f, 0f),
                new Vector3(0f, 0f, 1f),
                new Vector3(0f, 0f, -1f)
        };
        Vector3[] upVectors = new Vector3[] {
                new Vector3(0f, -1f, 0f),
                new Vector3(0f, -1f, 0f),
                new Vector3(0f, 0f, 1f),
                new Vector3(0f, 0f, -1f),
                new Vector3(0f, -1f, 0f),
                new Vector3(0f, -1f, 0f)
        };

        Vector3 center = localCenter != null ? localCenter : new Vector3();
        for (int face = 0; face < 6; face++) {
            Camera bakeCamera = new Camera(
                    new Vector3(center),
                    new Vector3(center).add(directions[face]),
                    upVectors[face]);
            bakeCamera.setPerspective((float) Math.toRadians(90f), 1f, 0.001f, 1000f);

            glClearColor(0f, 0f, 0f, 0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            renderer.renderObjectWithCameraAndLight(object, bakeCamera, new Vector3(0f, 0f, 0f));

            ByteBuffer pixels = BufferUtils.createByteBuffer(faceSize * faceSize * 4);
            glReadPixels(0, 0, faceSize, faceSize, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            BufferedImage image = toArgbImage(pixels, faceSize, faceSize);
            File output = new File(outputDirectory, "spherical_render_face_" + FACE_NAMES[face] + ".png");
            try {
                ImageIO.write(image, "png", output);
            } catch (Exception e) {
                throw new RuntimeException("Failed to write spherical face render image: " + output.getAbsolutePath(), e);
            }
        }

        if (cullEnabled) {
            glEnable(GL_CULL_FACE);
        } else {
            glDisable(GL_CULL_FACE);
        }
        if (depthEnabled) {
            glEnable(GL_DEPTH_TEST);
        } else {
            glDisable(GL_DEPTH_TEST);
        }
        glDepthFunc(previousDepthFunc);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteFramebuffers(fbo);
        glDeleteRenderbuffers(rbo);
        glDeleteTextures(texture);
        glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
    }

    private static float[][] renderRadialDistanceCubeFaces(GameObject object, int faceSize, Vector3 localCenter) {
        ShaderProgram shader = new ShaderProgram(
                ShaderProgram.loadShader("resources/shaders/bake_spherical_heightmap.vert.glsl"),
                ShaderProgram.loadShader("resources/shaders/bake_spherical_heightmap.frag.glsl"));

        int fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glDrawBuffer(GL_COLOR_ATTACHMENT0);

        int colorTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R32F, faceSize, faceSize, 0, GL_RED, GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture, 0);

        int rbo = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, faceSize, faceSize);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rbo);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glDeleteFramebuffers(fbo);
            glDeleteRenderbuffers(rbo);
            glDeleteTextures(colorTexture);
            shader.cleanup();
            return new float[6][faceSize * faceSize];
        }

        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        boolean cullEnabled = glIsEnabled(GL_CULL_FACE);
        boolean depthEnabled = glIsEnabled(GL_DEPTH_TEST);
        int previousDepthFunc = glGetInteger(GL_DEPTH_FUNC);

        glViewport(0, 0, faceSize, faceSize);
        glDisable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_GREATER);
        glClearDepth(0.0);

        Matrix4 projection = new Matrix4().perspective((float) Math.toRadians(90f), 1f, 0.001f, 1000f);
        Matrix4 identityModel = new Matrix4().identity();
        shader.use();
        shader.setUniformMatrix4fv("uMMatrix", identityModel);
        shader.setUniformVector3("uLocalBakeCenter", localCenter);

        Vector3[] directions = new Vector3[] {
                new Vector3(1f, 0f, 0f),
                new Vector3(-1f, 0f, 0f),
                new Vector3(0f, 1f, 0f),
                new Vector3(0f, -1f, 0f),
                new Vector3(0f, 0f, 1f),
                new Vector3(0f, 0f, -1f)
        };
        Vector3[] upVectors = new Vector3[] {
                new Vector3(0f, -1f, 0f),
                new Vector3(0f, -1f, 0f),
                new Vector3(0f, 0f, 1f),
                new Vector3(0f, 0f, -1f),
                new Vector3(0f, -1f, 0f),
                new Vector3(0f, -1f, 0f)
        };

        float[][] faces = new float[6][faceSize * faceSize];
        glBindVertexArray(object.vaoIds[0]);
        for (int face = 0; face < 6; face++) {
            Matrix4 view = new Matrix4().lookAt(localCenter, new Vector3(localCenter).add(directions[face]), upVectors[face]);
            shader.setUniformMatrix4fv("uBakeView", view);
            shader.setUniformMatrix4fv("uBakeProj", projection);

            glClearColor(0f, 0f, 0f, 0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glDrawElements(GL_TRIANGLES, object.getIndexCount(), GL_UNSIGNED_INT, 0);

            FloatBuffer pixels = BufferUtils.createFloatBuffer(faceSize * faceSize);
            glReadPixels(0, 0, faceSize, faceSize, GL_RED, GL_FLOAT, pixels);
            pixels.get(faces[face]);
        }
        glBindVertexArray(0);

        if (cullEnabled) {
            glEnable(GL_CULL_FACE);
        } else {
            glDisable(GL_CULL_FACE);
        }
        if (depthEnabled) {
            glEnable(GL_DEPTH_TEST);
        } else {
            glDisable(GL_DEPTH_TEST);
        }
        glDepthFunc(previousDepthFunc);
        glClearDepth(1.0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteFramebuffers(fbo);
        glDeleteRenderbuffers(rbo);
        glDeleteTextures(colorTexture);
        shader.cleanup();
        glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);

        return faces;
    }

    private static float sampleCubemapDistance(float[][] cubeFaces, Vector3 direction) {
        float absX = Math.abs(direction.x);
        float absY = Math.abs(direction.y);
        float absZ = Math.abs(direction.z);

        int face;
        float u;
        float v;
        if (absX >= absY && absX >= absZ) {
            if (direction.x >= 0f) {
                face = FACE_POSITIVE_X;
                u = -direction.z / absX;
                v = -direction.y / absX;
            } else {
                face = FACE_NEGATIVE_X;
                u = direction.z / absX;
                v = -direction.y / absX;
            }
        } else if (absY >= absX && absY >= absZ) {
            if (direction.y >= 0f) {
                face = FACE_POSITIVE_Y;
                u = direction.x / absY;
                v = direction.z / absY;
            } else {
                face = FACE_NEGATIVE_Y;
                u = direction.x / absY;
                v = -direction.z / absY;
            }
        } else {
            if (direction.z >= 0f) {
                face = FACE_POSITIVE_Z;
                u = direction.x / absZ;
                v = -direction.y / absZ;
            } else {
                face = FACE_NEGATIVE_Z;
                u = -direction.x / absZ;
                v = -direction.y / absZ;
            }
        }

        float[] samples = cubeFaces[face];
        int faceSize = (int) Math.sqrt(samples.length);
        float px = clamp01((u * 0.5f) + 0.5f) * (faceSize - 1);
        float py = clamp01((v * 0.5f) + 0.5f) * (faceSize - 1);
        int x0 = (int) Math.floor(px);
        int y0 = (int) Math.floor(py);
        int x1 = Math.min(faceSize - 1, x0 + 1);
        int y1 = Math.min(faceSize - 1, y0 + 1);
        float tx = px - x0;
        float ty = py - y0;

        float s00 = samples[y0 * faceSize + x0];
        float s10 = samples[y0 * faceSize + x1];
        float s01 = samples[y1 * faceSize + x0];
        float s11 = samples[y1 * faceSize + x1];
        float top = s00 + ((s10 - s00) * tx);
        float bottom = s01 + ((s11 - s01) * tx);
        return top + ((bottom - top) * ty);
    }

    private static BufferedImage toGrayscaleImage(float[] samples, int width, int height, float min, float max) {
        float range = max - min;
        if (range <= 0.000001f) {
            range = 1f;
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float sample = samples[y * width + x];
                float normalized = (sample - min) / range;
                int gray = Math.max(0, Math.min(255, Math.round(normalized * 255f)));
                int argb = (255 << 24) | (gray << 16) | (gray << 8) | gray;
                image.setRGB(x, y, argb);
            }
        }
        return image;
    }

    private static BufferedImage toArgbImage(ByteBuffer pixels, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int stride = width * 4;
        for (int y = 0; y < height; y++) {
            int srcRow = (height - 1 - y) * stride;
            for (int x = 0; x < width; x++) {
                int i = srcRow + x * 4;
                int r = pixels.get(i) & 0xFF;
                int g = pixels.get(i + 1) & 0xFF;
                int b = pixels.get(i + 2) & 0xFF;
                int a = pixels.get(i + 3) & 0xFF;
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                image.setRGB(x, y, argb);
            }
        }
        return image;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
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
