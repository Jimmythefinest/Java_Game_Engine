package com.njst.gaming.objects;

import com.njst.gaming.Geometries.Geometry;
import com.njst.gaming.Natives.GlUtils;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Renderer;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Utility to bake a GameObject's 3D mesh into a 2D texture.
 */
public class ImposterGenerator {

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
        float height = max.y - min.y;
        float width = Math.max(max.x - min.x, max.z - min.z);
        float scale = Math.max(width, height);
        if (outScale != null && outScale.length > 0) outScale[0] = scale;

        // 1. Create Framebuffer
        int fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

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

        // Simple Orthographic projection for the bake
        Matrix4 proj = new Matrix4();
        // Shift ortho to start from 0 for Y so the base of the plant is at the bottom of the texture
        proj.ortho(-scale/2, scale/2, 0, scale, 0.1f, 100f);
        
        Matrix4 view = new Matrix4();
        view.lookAt(new Vector3(0, scale/2, scale * 2), new Vector3(0, scale/2, 0), new Vector3(0, 1, 0));

        ShaderProgram shader = renderer.shaderProgram;
        shader.use();
        
        // We need to set the projection/view matrices for this temporary render.
        // The project uses SSBO block 0 for Camera.
        // We'll temporarily update the SSBO to our baking matrices.
        // Actually, GameObject.render might use whatever is in the SSBO.
        // Let's assume we can push these matrices.
        
        // Find the SSBO in Renderer and update it.
        float[] consts = new float[39];
        System.arraycopy(proj.r, 0, consts, 0, 16);
        System.arraycopy(view.r, 0, consts, 16, 16);
        System.arraycopy(new Vector3(0,0,0).toArray(), 0, consts, 32, 3); // dummy eyepos
        renderer.ssbo.setData(consts, GL_DYNAMIC_DRAW);
        renderer.ssbo.bindToShader(0);

        object.setPosition(0, 0, 0);
        object.updateModelMatrix();
        object.render(shader, 0); 

        // 5. Cleanup
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteFramebuffers(fbo);
        glDeleteRenderbuffers(rbo);
        
        glViewport(0, 0, renderer.width, renderer.height);

        return texture;
    }
}
