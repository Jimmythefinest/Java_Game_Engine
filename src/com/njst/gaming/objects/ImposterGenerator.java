package com.njst.gaming.objects;

import com.njst.gaming.Geometries.Geometry;
import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Renderer;

import java.io.File;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImageWrite;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Utility to bake a GameObject's 3D mesh into a 2D texture.
 */
public class ImposterGenerator {
    private static final String DEBUG_DUMP_PATH = "imposter_bake_debug.png";
    private static final float DEFAULT_BAKE_FOV_DEG = 45.0f;

    /**
     * Bakes the given GameObject into a texture and returns the scale used.
     * 
     * @param object The object to bake.
     * @param renderer The renderer (used for shaders and camera setup).
     * @param size Resolution of the baked texture (e.g., 256).
     * @param outScale Array of size 1 to receive the calculated scale.
     * @return OpenGL texture ID of the baked imposter.
     */
    public static int bake(GameObject object, Renderer renderer, int size, float[] outScale) {
        System.out.println("[ImposterGenerator] Baking object: " + object.name + " (" + size + "x" + size + ")");
        
        // Find bounds to fit the object in view
        Vector3 max = object.geometry.max;
        Vector3 min = object.geometry.min;
        float spanX = max.x - min.x;
        float spanY = max.y - min.y;
        float spanZ = max.z - min.z;
        float width = Math.max(spanX, spanZ);
        float height = spanY;
        float scale = Math.max(width, height);
        if (outScale != null && outScale.length > 0) outScale[0] = scale;

        // 1. Create Framebuffer
        int fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glDrawBuffer(GL_COLOR_ATTACHMENT0);

        // 2. Create Texture to render into
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size, size, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);

        // 3. Create Depth Buffer
        int rbo = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, size, size);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rbo);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Framebuffer incomplete!");
            return 0;
        }

        // 4. Render the object
        glViewport(0, 0, size, size);
        glClearColor(0, 0, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        renderer.renderObjectLikeMainPass(object);
        dumpCurrentFramebufferToPng(size, size, DEBUG_DUMP_PATH);

        // 5. Cleanup
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteFramebuffers(fbo);
        glDeleteRenderbuffers(rbo);
        
        glViewport(0, 0, renderer.width, renderer.height);

        return texture;
    }

    /**
     * Bakes a grid of views into a single atlas image and writes it to disk.
     *
     * @param object The object to bake.
     * @param renderer The renderer (used for shaders and camera setup).
     * @param tileSize Resolution per view (e.g., 256).
     * @param viewCountAzimuth Number of azimuth views (columns).
     * @param viewCountElevation Number of elevation views (rows).
     * @param outputPath PNG output path.
     * @return true if the atlas was written successfully.
     */
    public static boolean bakeAtlasToFile(GameObject object, Renderer renderer, int tileSize,
            int viewCountAzimuth, int viewCountElevation, String outputPath) {
        if (object == null || renderer == null) {
            System.err.println("[ImposterGenerator] bakeAtlasToFile: object/renderer is null");
            return false;
        }
        if (tileSize <= 0 || viewCountAzimuth <= 0 || viewCountElevation <= 0) {
            System.err.println("[ImposterGenerator] bakeAtlasToFile: invalid tile/view counts");
            return false;
        }
        int atlasWidth = tileSize * viewCountAzimuth;
        int atlasHeight = tileSize * viewCountElevation;
        System.out.println("[ImposterGenerator] Baking atlas: " + object.name + " " + atlasWidth + "x" + atlasHeight
                + " (" + viewCountAzimuth + "x" + viewCountElevation + ")");

        // Save and replace the renderer camera to avoid disturbing runtime settings.
        com.njst.gaming.Camera originalCamera = renderer.camera;
        renderer.camera = new com.njst.gaming.Camera(new Vector3(0f, 0f, -5f), new Vector3(0f, 0f, 0f),
                new Vector3(0f, 1f, 0f));
        renderer.camera.setPerspective(DEFAULT_BAKE_FOV_DEG, 1.0f, 0.1f, 1000.0f);

        // Compute bounds for camera distance.
        Vector3 max = object.geometry.max;
        Vector3 min = object.geometry.min;
        Vector3 center = new Vector3(
                (max.x + min.x) * 0.5f,
                (max.y + min.y) * 0.5f,
                (max.z + min.z) * 0.5f);
        float halfX = (max.x - min.x) * 0.5f;
        float halfY = (max.y - min.y) * 0.5f;
        float halfZ = (max.z - min.z) * 0.5f;
        float halfMax = Math.max(halfY, Math.max(halfX, halfZ));
        float fovRad = (float) Math.toRadians(DEFAULT_BAKE_FOV_DEG * 0.5f);
        float cameraDistance = (float) (halfMax / Math.tan(fovRad)) * 1.15f;
        float farPlane = cameraDistance + halfMax * 4.0f;
        renderer.camera.setPerspective(DEFAULT_BAKE_FOV_DEG, 1.0f, 0.1f, farPlane);

        int fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glDrawBuffer(GL_COLOR_ATTACHMENT0);

        int atlasTex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, atlasTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, atlasWidth, atlasHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, atlasTex, 0);

        int rbo = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, atlasWidth, atlasHeight);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rbo);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("[ImposterGenerator] Framebuffer incomplete during atlas bake");
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glDeleteFramebuffers(fbo);
            glDeleteRenderbuffers(rbo);
            renderer.camera = originalCamera;
            return false;
        }

        glViewport(0, 0, atlasWidth, atlasHeight);
        glDisable(GL_SCISSOR_TEST);
        glClearColor(0, 0, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glEnable(GL_SCISSOR_TEST);
        for (int row = 0; row < viewCountElevation; row++) {
            float elevationDeg = getElevationDeg(row, viewCountElevation);
            for (int col = 0; col < viewCountAzimuth; col++) {
                float azimuthDeg = getAzimuthDeg(col, viewCountAzimuth);

                Vector3 dir = directionFromAngles(azimuthDeg, elevationDeg);
                Vector3 camPos = new Vector3(center).add(new Vector3(dir).mul(cameraDistance));
                renderer.camera.lookAt(camPos, center, new Vector3(0f, 1f, 0f));

                int x = col * tileSize;
                int y = row * tileSize;
                glViewport(x, y, tileSize, tileSize);
                glScissor(x, y, tileSize, tileSize);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                renderer.renderObjectLikeMainPass(object);
            }
        }
        glDisable(GL_SCISSOR_TEST);

        boolean wrote = dumpCurrentFramebufferToPng(atlasWidth, atlasHeight, outputPath);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteFramebuffers(fbo);
        glDeleteRenderbuffers(rbo);
        glDeleteTextures(atlasTex);

        renderer.camera = originalCamera;
        glViewport(0, 0, renderer.width, renderer.height);

        return wrote;
    }

    /**
     * Bakes a single view into a 1x1 atlas at a fixed camera distance and direction.
     */
    public static boolean bakeSingleViewToFile(GameObject object, Renderer renderer, int tileSize,
            float cameraDistance, Vector3 direction, String outputPath) {
        if (object == null || renderer == null) {
            System.err.println("[ImposterGenerator] bakeSingleViewToFile: object/renderer is null");
            return false;
        }
        if (tileSize <= 0) {
            System.err.println("[ImposterGenerator] bakeSingleViewToFile: invalid tile size");
            return false;
        }
        Vector3 dir = new Vector3(direction).normalize();
        System.out.println("[ImposterGenerator] Baking single view: " + object.name + " (" + tileSize + "x" + tileSize
                + "), distance=" + cameraDistance);

        com.njst.gaming.Camera originalCamera = renderer.camera;
        renderer.camera = new com.njst.gaming.Camera(new Vector3(0f, 0f, -5f), new Vector3(0f, 0f, 0f),
                new Vector3(0f, 1f, 0f));
        renderer.camera.setPerspective(DEFAULT_BAKE_FOV_DEG, 1.0f, 0.1f, cameraDistance * 5.0f);

        Vector3 max = object.geometry.max;
        Vector3 min = object.geometry.min;
        Vector3 center = new Vector3(
                (max.x + min.x) * 0.5f,
                (max.y + min.y) * 0.5f,
                (max.z + min.z) * 0.5f);
        Vector3 camPos = new Vector3(center).add(new Vector3(dir).mul(cameraDistance));
        renderer.camera.lookAt(camPos, center, new Vector3(0f, 1f, 0f));

        int fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glDrawBuffer(GL_COLOR_ATTACHMENT0);

        int tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, tileSize, tileSize, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, tex, 0);

        int rbo = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, tileSize, tileSize);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rbo);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("[ImposterGenerator] Framebuffer incomplete during single-view bake");
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glDeleteFramebuffers(fbo);
            glDeleteRenderbuffers(rbo);
            renderer.camera = originalCamera;
            return false;
        }

        glViewport(0, 0, tileSize, tileSize);
        glClearColor(0, 0, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        renderer.renderObjectLikeMainPass(object);

        boolean wrote = dumpCurrentFramebufferToPng(tileSize, tileSize, outputPath);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteFramebuffers(fbo);
        glDeleteRenderbuffers(rbo);
        glDeleteTextures(tex);

        renderer.camera = originalCamera;
        glViewport(0, 0, renderer.width, renderer.height);
        return wrote;
    }

    /**
     * Computes a quad size that matches the projected bounds for the given camera view.
     */
    public static float[] computeImposterSizeForCamera(GameObject object, Renderer renderer,
            Vector3 cameraPos, Vector3 target, Vector3 up) {
        if (object == null || renderer == null) {
            return new float[] { 1f, 1f };
        }
        com.njst.gaming.Camera originalCamera = renderer.camera;
        Vector3 origPos = renderer.camera.cameraPosition.clone();
        Vector3 origTarget = renderer.camera.targetPosition.clone();
        Vector3 origUp = renderer.camera.upDirection.clone();
        renderer.camera.lookAt(cameraPos, target, up);

        Vector3 min = object.geometry.min;
        Vector3 max = object.geometry.max;
        Vector3 center = new Vector3(
                (min.x + max.x) * 0.5f,
                (min.y + max.y) * 0.5f,
                (min.z + max.z) * 0.5f);

        Matrix4 view = renderer.camera.getViewMatrix();
        Matrix4 proj = renderer.camera.getProjectionMatrix();
        Matrix4 viewProj = new Matrix4().set(proj.r).multiply(view);

        Vector3[] corners = new Vector3[] {
                new Vector3(min.x, min.y, min.z),
                new Vector3(min.x, min.y, max.z),
                new Vector3(min.x, max.y, min.z),
                new Vector3(min.x, max.y, max.z),
                new Vector3(max.x, min.y, min.z),
                new Vector3(max.x, min.y, max.z),
                new Vector3(max.x, max.y, min.z),
                new Vector3(max.x, max.y, max.z)
        };

        float maxAbsX = 0f;
        float maxAbsY = 0f;
        for (Vector3 c : corners) {
            Vector3 world = object.modelMatrix.multiply(c);
            Vector3 ndc = viewProj.multiply(world);
            maxAbsX = Math.max(maxAbsX, Math.abs(ndc.x));
            maxAbsY = Math.max(maxAbsY, Math.abs(ndc.y));
        }

        Vector3 centerWorld = object.modelMatrix.multiply(center);
        Vector3 centerView = view.multiply(centerWorld);
        float distance = -centerView.z;
        if (distance <= 0.0001f) {
            distance = 0.0001f;
        }

        float m00 = proj.r[0];
        float m11 = proj.r[5];
        float halfWidth = maxAbsX * distance / m00;
        float halfHeight = maxAbsY * distance / m11;

        renderer.camera.lookAt(origPos, origTarget, origUp);
        renderer.camera = originalCamera;
        return new float[] { halfWidth * 2.0f, halfHeight * 2.0f };
    }

    private static boolean dumpCurrentFramebufferToPng(int width, int height, String path) {
        ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        ByteBuffer flipped = BufferUtils.createByteBuffer(width * height * 4);
        int rowSize = width * 4;
        for (int y = 0; y < height; y++) {
            int srcRow = (height - 1 - y) * rowSize;
            int dstRow = y * rowSize;
            for (int x = 0; x < rowSize; x++) {
                flipped.put(dstRow + x, pixels.get(srcRow + x));
            }
        }
        int stride = width * 4;
        File outFile = new File(path);
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (!STBImageWrite.stbi_write_png(path, width, height, 4, flipped, stride)) {
            System.err.println("[ImposterGenerator] Failed to write PNG: " + path);
            return false;
        }
        System.out.println("[ImposterGenerator] PNG written: " + path);
        return true;
    }

    private static float getAzimuthDeg(int index, int count) {
        if (count == 1) return 0.0f;
        float step = 360.0f / count;
        return (index + 0.5f) * step;
    }

    private static float getElevationDeg(int index, int count) {
        if (count == 1) return 0.0f;
        float step = 180.0f / count;
        return -90.0f + (index + 0.5f) * step;
    }

    private static Vector3 directionFromAngles(float azimuthDeg, float elevationDeg) {
        float az = (float) Math.toRadians(azimuthDeg);
        float el = (float) Math.toRadians(elevationDeg);
        float cosEl = (float) Math.cos(el);
        float x = (float) (Math.cos(az) * cosEl);
        float y = (float) Math.sin(el);
        float z = (float) (Math.sin(az) * cosEl);
        return new Vector3(x, y, z).normalize();
    }
}
