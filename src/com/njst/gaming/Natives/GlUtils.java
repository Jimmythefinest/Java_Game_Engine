package com.njst.gaming.Natives;

import org.lwjgl.opengl.GL30;

import org.lwjgl.PointerBuffer;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;
//import static android.opengl.GL30.*;

public class GlUtils {
    public static final int GL_LINES = GL30.GL_LINES;
    public static final int GL_UNSIGNED_SHORT = GL30.GL_UNSIGNED_SHORT;
    public static final int GL_POINTS = GL30.GL_POINTS;
    public static final int GL_TRIANGLES = GL30.GL_TRIANGLES;
    public static final int GL_UNSIGNED_INT = GL30.GL_UNSIGNED_INT;
    public static final int GL_ARRAY_BUFFER = GL30.GL_ARRAY_BUFFER;
    public static final int GL_ELEMENT_ARRAY_BUFFER = GL30.GL_ELEMENT_ARRAY_BUFFER;
    public static int GL_LEQUAL = GL30.GL_LEQUAL;
    public static int GL_DEPTH_TEST = GL30.GL_DEPTH_TEST;

    public static void set_VBO_Float(int vbo, float[] data, int length) {
        // set_VBO_Float(vbo, Utils.Array_to_Buffer(data), data.length);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        // Position attribute (location = 0 in shader)
    }

    public static void set_VBO_Float(int vbo, float[] data) {
        set_VBO_Float(vbo, data, 0);
    }
    // public static void set_VBO_Short(int vbo,int[] data,int length){
    // set_VBO_Short(vbo, Utils.Array_to_Buffer(data), data.length);
    // }

    public static void set_VBO_Short(int vbo, short[] data, int length) {
        // set_VBO_Short(vbo, Utils.Array_to_Buffer(data), data.length);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);

    }

    public static void set_VBO_Int(int vbo, int[] data, int length) {
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
    }

    public static void set_VBO_Int(int vbo, int[] data) {
        set_VBO_Int(vbo, data, 0);
    }

    public static void set_VBO_attrib_pointer(int vbo, int location, int size) {
        glBindBuffer(GL_ARRAY_BUFFER, vbo); // For vertices
        glVertexAttribPointer(location, size, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(location);
    }

    public static void bind_vertex_array(int vbo) {
        glBindVertexArray(vbo);
    }

    public static void bind_array(int name, int location) {
        glBindBuffer(name, location);
    }

    public static int[] generateVAO(int[] storage, int number, int start) {
        glGenVertexArrays(storage);

        return storage;
    }

    public static int[] generateVBOs(int[] storage, int number, int start) {
        // glGenBuffers(number, storage, start);
        glGenBuffers(storage);
        return storage;
    }

    public static void DeleteBuffers(int[] buffers) {
        glDeleteBuffers(buffers);
    }

    public static void DeleteVAO(int[] vaos) {
        glDeleteVertexArrays(vaos);
    }

    public static void DrawElements(int mode, int length, int data_type, int indices) {

        glDrawElements(mode, length, data_type, indices);
    }

    public static void DrawElementsInstanced(int mode, int length, int data_type, int indices, int instance_number) {

        glDrawElements(mode, length, data_type, indices);
        glDrawRangeElements(0, 0, 0, ByteBuffer.allocate(2));
        glMultiDrawElements(1, new int[1], 1, PointerBuffer.allocateDirect(1));
    }

    public static void Enable(int i) {
        glEnable(i);
    }

    public static void DepthFunc(int i) {
        glDepthFunc(i);
    }

    public static int generateTexture() {
        // Create an array to hold the texture ID
        int[] textureHandle = new int[1];

        // Generate the texture ID
        GL30.glGenTextures(textureHandle);

        // Return the generated texture ID
        return textureHandle[0];
    }

    public static int generateFramebuffer() {
        // Generate a framebuffer object
        int[] frameBuffer = new int[1];
        GL30.glGenFramebuffers(frameBuffer);

        // Bind the framebuffer
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBuffer[0]);

        // Create a texture to attach to the framebuffer

        // Return the framebuffer ID
        return frameBuffer[0];
    }

}
