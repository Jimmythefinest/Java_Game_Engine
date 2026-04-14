package com.njst.gaming.android;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLES31;
import android.opengl.GLUtils;
import android.util.Log;

import com.njst.gaming.Renderer;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.collision.SphericalHeightmapShape;
import com.njst.gaming.graphics.BufferHandle;
import com.njst.gaming.graphics.GraphicsDevice;
import com.njst.gaming.graphics.ImposterBakeResult;
import com.njst.gaming.graphics.ShaderHandle;
import com.njst.gaming.graphics.ShadowMapHandle;
import com.njst.gaming.objects.GameObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class AndroidGraphicsDevice implements GraphicsDevice {
    private static final String TAG = "NJST";
    private final AssetManager assetManager;
    private int whiteTextureId;

    public AndroidGraphicsDevice(Context context) {
        this.assetManager = context.getAssets();
    }

    @Override
    public ShaderHandle createShaderProgram(String vertexShaderSource, String fragmentShaderSource) {
        return new AndroidShaderProgram(vertexShaderSource, fragmentShaderSource);
    }

    @Override
    public BufferHandle createShaderStorageBuffer() {
        return new AndroidShaderStorageBuffer();
    }

    @Override
    public String loadShaderSource(String filePath) {
        String normalized = filePath.replace('\\', '/');
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("resources/")) {
            normalized = normalized.substring("resources/".length());
        }

        Log.i(TAG, "Loading shader asset: " + normalized);
        try (InputStream inputStream = assetManager.open(normalized);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder shader = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                shader.append(line).append('\n');
            }
            String shaderSource = normalizeShaderSource(shader.toString(), normalized);
            Log.i(TAG, "Loaded shader asset: " + normalized + " (" + shaderSource.length() + " chars)");
            return shaderSource;
        } catch (IOException e) {
            Log.e(TAG, "Failed to load shader asset: " + normalized, e);
            throw new IllegalStateException("Unable to load Android shader asset: " + normalized, e);
        }
    }

    private String normalizeShaderSource(String shaderSource, String assetPath) {
        if (shaderSource == null || shaderSource.isEmpty()) {
            return shaderSource;
        }

        if (shaderSource.startsWith("#version 310 es")) {
            return shaderSource;
        }

        if (shaderSource.startsWith("#version 450 core")
                || shaderSource.startsWith("#version 430 core")
                || shaderSource.startsWith("#version 330 core")) {
            Log.i(TAG, "Rewriting desktop GLSL version for Android asset: " + assetPath);
            int newlineIndex = shaderSource.indexOf('\n');
            if (newlineIndex < 0) {
                return "#version 310 es\n";
            }
            return "#version 310 es\n" + shaderSource.substring(newlineIndex + 1);
        }

        return shaderSource;
    }

    @Override
    public String loadTextResource(String filePath) {
        return AndroidAssetLoader.readText(assetManager, filePath);
    }

    @Override
    public byte[] loadBinaryResource(String filePath) {
        return AndroidAssetLoader.readBytes(assetManager, filePath);
    }

    @Override
    public int loadTexture(String texturePath) {
        if (texturePath == null || texturePath.isEmpty() || texturePath.startsWith("generated:")) {
            Log.i(TAG, "Using generated white texture for request: " + texturePath);
            return getWhiteTexture();
        }

        String normalized = texturePath.replace('\\', '/');
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("resources/")) {
            normalized = normalized.substring("resources/".length());
        }

        Log.i(TAG, "Loading texture asset: " + normalized);
        try (InputStream externalStream = AndroidAssetLoader.openExternalStream(texturePath)) {
            if (externalStream != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(externalStream);
                if (bitmap == null) {
                    Log.e(TAG, "Bitmap decode returned null for external texture asset: " + normalized);
                    return getWhiteTexture();
                }
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int textureId = createTextureFromBitmap(bitmap);
                Log.i(TAG, "Loaded external texture asset: " + normalized + " -> id=" + textureId
                        + " size=" + width + "x" + height);
                bitmap.recycle();
                return textureId;
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to load external texture asset: " + normalized, e);
        }
        try (InputStream inputStream = assetManager.open(normalized)) {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                Log.e(TAG, "Bitmap decode returned null for texture asset: " + normalized);
                return getWhiteTexture();
            }
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int textureId = createTextureFromBitmap(bitmap);
            Log.i(TAG, "Loaded texture asset: " + normalized + " -> id=" + textureId + " size=" + width + "x" + height);
            bitmap.recycle();
            return textureId;
        } catch (IOException e) {
            Log.e(TAG, "Failed to load texture asset, falling back to white: " + normalized, e);
            return getWhiteTexture();
        }
    }

    @Override
    public int createTextureRGBA(int width, int height, byte[] rgbaPixels) {
        if (rgbaPixels == null || rgbaPixels.length != width * height * 4) {
            throw new IllegalArgumentException("RGBA texture data does not match the requested size.");
        }
        int[] textures = new int[1];
        GLES31.glGenTextures(1, textures, 0);
        int textureId = textures[0];
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureId);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE);
        ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(rgbaPixels.length);
        pixelBuffer.put(rgbaPixels).position(0);
        GLES31.glTexImage2D(
                GLES31.GL_TEXTURE_2D,
                0,
                GLES31.GL_RGBA,
                width,
                height,
                0,
                GLES31.GL_RGBA,
                GLES31.GL_UNSIGNED_BYTE,
                pixelBuffer);
        Log.i(TAG, "Created RGBA texture id=" + textureId + " size=" + width + "x" + height);
        return textureId;
    }

    @Override
    public ShadowMapHandle createShadowMap(int width, int height) {
        return new AndroidShadowMapHandle(width, height);
    }

    private int getWhiteTexture() {
        if (whiteTextureId != 0) {
            Log.i(TAG, "Reusing fallback white texture id=" + whiteTextureId);
            return whiteTextureId;
        }
        int[] textures = new int[1];
        GLES31.glGenTextures(1, textures, 0);
        whiteTextureId = textures[0];
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, whiteTextureId);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE);
        ByteBuffer pixel = ByteBuffer.allocateDirect(4);
        pixel.put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).position(0);
        GLES31.glTexImage2D(
                GLES31.GL_TEXTURE_2D,
                0,
                GLES31.GL_RGBA,
                1,
                1,
                0,
                GLES31.GL_RGBA,
                GLES31.GL_UNSIGNED_BYTE,
                pixel);
        Log.i(TAG, "Created fallback white texture id=" + whiteTextureId);
        return whiteTextureId;
    }

    private int createTextureFromBitmap(Bitmap bitmap) {
        int[] textures = new int[1];
        GLES31.glGenTextures(1, textures, 0);
        int textureId = textures[0];
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureId);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0);
        return textureId;
    }

    @Override
    public ImposterBakeResult bakeImposter(Renderer renderer, GameObject object, int width, int height) {
        return new ImposterBakeResult(0, width, height);
    }

    @Override
    public SphericalHeightmapShape bakeSphericalHeightmap(Renderer renderer, GameObject object, int width, int height,
            Vector3 localCenter) {
        Log.w(TAG, "Spherical heightmap baking is not implemented on Android yet.");
        return null;
    }

    @Override
    public void releaseTexture(int textureId) {
        if (textureId == 0) {
            return;
        }
        int[] textures = new int[] { textureId };
        GLES31.glDeleteTextures(1, textures, 0);
        if (textureId == whiteTextureId) {
            whiteTextureId = 0;
        }
    }

    @Override
    public int createVertexArray() {
        int[] vertexArrays = new int[1];
        GLES30.glGenVertexArrays(1, vertexArrays, 0);
        return vertexArrays[0];
    }

    @Override
    public int[] createBuffers(int count) {
        int[] buffers = new int[count];
        GLES31.glGenBuffers(count, buffers, 0);
        return buffers;
    }

    @Override
    public void bindVertexArray(int vaoId) {
        GLES30.glBindVertexArray(vaoId);
    }

    @Override
    public void uploadArrayBufferFloat(int bufferId, float[] data) {
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, bufferId);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length * Float.BYTES).order(java.nio.ByteOrder.nativeOrder());
        byteBuffer.asFloatBuffer().put(data).position(0);
        GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER, data.length * Float.BYTES, byteBuffer, GLES31.GL_STATIC_DRAW);
    }

    @Override
    public void uploadArrayBufferInt(int bufferId, int[] data) {
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, bufferId);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length * Integer.BYTES).order(java.nio.ByteOrder.nativeOrder());
        byteBuffer.asIntBuffer().put(data).position(0);
        GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER, data.length * Integer.BYTES, byteBuffer, GLES31.GL_STATIC_DRAW);
    }

    @Override
    public void uploadElementArrayBufferInt(int bufferId, int[] data) {
        GLES31.glBindBuffer(GLES31.GL_ELEMENT_ARRAY_BUFFER, bufferId);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length * Integer.BYTES).order(java.nio.ByteOrder.nativeOrder());
        byteBuffer.asIntBuffer().put(data).position(0);
        GLES31.glBufferData(GLES31.GL_ELEMENT_ARRAY_BUFFER, data.length * Integer.BYTES, byteBuffer, GLES31.GL_STATIC_DRAW);
    }

    @Override
    public void setVertexAttribPointer(int bufferId, int location, int size) {
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, bufferId);
        GLES31.glVertexAttribPointer(location, size, GLES31.GL_FLOAT, false, 0, 0);
        GLES31.glEnableVertexAttribArray(location);
    }

    @Override
    public void setVertexAttribIPointer(int bufferId, int location, int size) {
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, bufferId);
        GLES30.glVertexAttribIPointer(location, size, GLES31.GL_INT, 0, 0);
        GLES31.glEnableVertexAttribArray(location);
    }

    @Override
    public void updateArrayBufferFloat(int bufferId, float[] data) {
        uploadArrayBufferFloat(bufferId, data);
    }

    @Override
    public void drawElementsTriangles(int indexCount) {
        GLES31.glDrawElements(GLES31.GL_TRIANGLES, indexCount, GLES31.GL_UNSIGNED_INT, 0);
    }

    @Override
    public void drawElementsLines(int indexCount) {
        GLES31.glDrawElements(GLES31.GL_LINES, indexCount, GLES31.GL_UNSIGNED_INT, 0);
    }

    @Override
    public void deleteBuffers(int[] buffers) {
        GLES31.glDeleteBuffers(buffers.length, buffers, 0);
    }

    @Override
    public void deleteVertexArrays(int[] vaos) {
        GLES30.glDeleteVertexArrays(vaos.length, vaos, 0);
    }

    @Override
    public void enableBlendAndDepth() {
        GLES31.glEnable(GLES31.GL_BLEND);
        GLES31.glBlendFunc(GLES31.GL_SRC_ALPHA, GLES31.GL_ONE_MINUS_SRC_ALPHA);
        GLES31.glEnable(GLES31.GL_DEPTH_TEST);
    }

    @Override
    public void clearColorAndDepth() {
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT | GLES31.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void clearDepth() {
        GLES31.glClear(GLES31.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void bindShadowMap(ShadowMapHandle shadowMap) {
        if (!(shadowMap instanceof AndroidShadowMapHandle)) {
            throw new IllegalArgumentException("Unexpected shadow map handle type: " + shadowMap);
        }
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, ((AndroidShadowMapHandle) shadowMap).getFramebufferId());
    }

    @Override
    public void bindDefaultFramebuffer() {
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void dumpShadowMap(ShadowMapHandle shadowMap, String outputPath) {
        Log.w(TAG, "Shadow map dumping is not implemented on Android yet. Requested output: " + outputPath);
    }

    @Override
    public void viewport(int width, int height) {
        GLES31.glViewport(0, 0, width, height);
    }

    @Override
    public int dynamicDrawUsage() {
        return GLES31.GL_DYNAMIC_DRAW;
    }
}
