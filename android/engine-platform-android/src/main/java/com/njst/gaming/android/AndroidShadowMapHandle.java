package com.njst.gaming.android;

import android.opengl.GLES31;

import com.njst.gaming.graphics.ShadowMapHandle;

final class AndroidShadowMapHandle implements ShadowMapHandle {
    private final int framebufferId;
    private final int textureId;
    private final int width;
    private final int height;

    AndroidShadowMapHandle(int width, int height) {
        this.width = width;
        this.height = height;

        int[] textures = new int[1];
        GLES31.glGenTextures(1, textures, 0);
        textureId = textures[0];
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureId);
        GLES31.glTexImage2D(
                GLES31.GL_TEXTURE_2D,
                0,
                GLES31.GL_DEPTH_COMPONENT24,
                width,
                height,
                0,
                GLES31.GL_DEPTH_COMPONENT,
                GLES31.GL_UNSIGNED_INT,
                null);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_NEAREST);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_NEAREST);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_COMPARE_MODE, GLES31.GL_NONE);

        int[] framebuffers = new int[1];
        GLES31.glGenFramebuffers(1, framebuffers, 0);
        framebufferId = framebuffers[0];
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, framebufferId);
        GLES31.glFramebufferTexture2D(
                GLES31.GL_FRAMEBUFFER,
                GLES31.GL_DEPTH_ATTACHMENT,
                GLES31.GL_TEXTURE_2D,
                textureId,
                0);

        int status = GLES31.glCheckFramebufferStatus(GLES31.GL_FRAMEBUFFER);
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0);
        if (status != GLES31.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Failed to create Android shadow map framebuffer. Status=" + status);
        }
    }

    int getFramebufferId() {
        return framebufferId;
    }

    @Override
    public int getTextureId() {
        return textureId;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}
