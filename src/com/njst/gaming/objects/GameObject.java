package com.njst.gaming.objects;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

import java.util.ArrayList;

import com.njst.gaming.Natives.*;
import com.njst.gaming.Utils.GeneralUtil;
import com.njst.gaming.Utils.Matrice_Math;

import com.njst.gaming.Geometries.*;
import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Vector3;

// import static com.njst.gaming.data.*;

public class GameObject {
    // Properties
    public String name = "Default";
    public float shininess = 32, ambientlight_multiplier = 1, mass = 10;
    public int texture;

    public Vector3 position = new Vector3(0, 0, 0); // Initial position (x, y, z)
    public float[] scale = new float[] { 1, 1, 1 }; // Initial size (sx, sy, sz)
    private Vector3 rotation = new Vector3(); // Initial size (sx, sy, sz)
    public Matrix4 modelMatrix = new Matrix4();

    public float[] velocity = new float[] { 0, 0, 0 };
    private boolean buffers_generated = false;

    public ArrayList<Animation> animations;

    // collision Box data
    public float[] collisionBox, collisionBounds, none_axis_aligned_CollisionBox;
    public Vector3 max = new Vector3(-0.5f, -0.5f, -0.5f), min = new Vector3(0.5f, 0.5f, 0.5f);;

    // Geometry data

    public int[] vaoIds = new int[1];
    public int[] vboIds = new int[5], vao = new int[1];
    // float[] vertices, normals, texture_coordinates, colors;
    // int[] indices;
    public Geometry geometry;

    public GameObject(Geometry geometry, int texture) {
        // vertices = geometry.getVertices();
        // normals = geometry.getNormals();
        // texture_coordinates = geometry.getTextureCoordinates();
        // indices = geometry.getIndices();
        this.texture = texture;
        this.geometry = geometry;

        // Initialize model matrix to identity

        modelMatrix.identity();
        animations = new ArrayList<Animation>();
        collisionBox = new float[] {
                -0.5f, -0.5f, -0.5f, 1,
                0.5f, -0.5f, -0.5f, 1,
                0.5f, 0.5f, -0.5f, 1,
                -0.5f, 0.5f, -0.5f, 1,

                -0.5f, -0.5f, 0.5f, 1,
                0.5f, -0.5f, 0.5f, 1,
                0.5f, 0.5f, 0.5f, 1,
                -0.5f, 0.5f, 0.5f, 1
        };
        collisionBounds = new float[] {
                -0.5f, -0.5f, -0.5f,
                0.5f, 0.5f, 0.5f
        };
    }

    public void addAnimation(Animation a) {
        animations.add(a);
    }

    public int getIndexCount() {
        return geometry.getIndices().length;
    }

    public void move(float x, float y, float z) {
        position.add(new Vector3(x, y, z));
        updateModelMatrix();
    }

    public void setPosition(float x, float y, float z) {
        position.x = x;
        position.y = y;
        position.z = z;
        updateModelMatrix();
    }

    public void rotate(float x, float y, float z) {
        rotation.x += x;
        rotation.y += y;
        rotation.z += z;

        updateModelMatrix();
    }

    public void setRotation(float x, float y, float z) {
        rotation.x = x;
        rotation.y = y;
        rotation.z = z;
        updateModelMatrix();
    }

    public void translate(Vector3 position) {
        this.position.add(position);
        updateModelMatrix();
    }

    public void resize(float sx, float sy, float sz) {
        scale[0] = sx;
        scale[1] = sy;
        scale[2] = sz;

        updateModelMatrix();
    }

    public void setScale(float sx, float sy, float sz) {
        scale[0] = sx;
        scale[1] = sy;
        scale[2] = sz;
        updateModelMatrix();
    }

    public void updateModelMatrix() {
        // Matrix.setIdentityM(modelMatrix,0);
        modelMatrix.identity();
        modelMatrix.translate(position);
        modelMatrix.rotate(rotation.x, new Vector3(1f, 0, 0));
        modelMatrix.rotate(rotation.y, new Vector3(0f, 1, 0));
        modelMatrix.rotate(rotation.z, new Vector3(0f, 0, 1));
        modelMatrix.scale(new Vector3(scale[0], scale[1], scale[2]));
        updateCollisionBox();

    }

    public void updateCollisionBox() {
        float[] collision1Box = GeneralUtil.from4to3(8,
                Matrice_Math.MatrixMultiplication(collisionBox, modelMatrix.get(new float[16]), 4));
        float[] x = new float[8], y = new float[8], z = new float[8];
        for (int i = 0; i < 8; i++) {
            x[i] = collision1Box[(i * 3)];
            y[i] = collision1Box[(i * 3) + 1];
            z[i] = collision1Box[(i * 3) + 2];
        }
        collisionBounds = new float[] {
                GeneralUtil.getMinValue(x),
                GeneralUtil.getMinValue(y),
                GeneralUtil.getMinValue(z),
                GeneralUtil.getMaxValue(x),
                GeneralUtil.getMaxValue(y),
                GeneralUtil.getMaxValue(z)
        };
        none_axis_aligned_CollisionBox = collision1Box;
        min = new Vector3(
                GeneralUtil.getMinValue(x),
                GeneralUtil.getMinValue(y),
                GeneralUtil.getMinValue(z));
        max = new Vector3(
                GeneralUtil.getMaxValue(x),
                GeneralUtil.getMaxValue(y),
                GeneralUtil.getMaxValue(z));
    }

    public float[] getModelMatrix() {
        return modelMatrix.get(new float[16]);
    }

    public void animate() {
        for (Animation a : animations) {
            a.animate(this);
        }
    }

    public void generateBuffers() {
        int vaoId = GlUtils.generateVAO(new int[1], 0, 0)[0];
        vboIds = GlUtils.generateVBOs(new int[6], 0, 0);
        GlUtils.bind_vertex_array(vaoId);
        int vboId = vboIds[0];
        int vboId1 = vboIds[1];
        GlUtils.set_VBO_Float(vboId, geometry.getVertices());
        GlUtils.set_VBO_Float(vboId1, geometry.getNormals());
        GlUtils.set_VBO_Float(vboIds[2], geometry.getTextureCoordinates());

        GlUtils.set_VBO_attrib_pointer(vboId1, 1, 3);
        GlUtils.set_VBO_attrib_pointer(vboId, 0, 3);
        GlUtils.set_VBO_attrib_pointer(vboIds[2], 2, 2);

        int eboId = vboIds[5];
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, geometry.getIndices(), GL_STATIC_DRAW);

        glBindVertexArray(0);
        vaoIds[0] = vaoId;
        buffers_generated = true;
    }

    public void cleanup() {
        if (buffers_generated) {
            GlUtils.DeleteBuffers(vboIds);
            GlUtils.DeleteVAO(vaoIds);
            buffers_generated = false;
        }
    }

    public ShaderProgram shaderprogram;

    public void render(ShaderProgram shader, int textureHandle) {
        if (!buffers_generated) {
            generateBuffers();
        }

        if (this.shaderprogram == null) {
            shaderprogram = shader;
        }
        // shaderprogram.use();
        shaderprogram.setUniformVector3("properties", new Vector3(shininess, ambientlight_multiplier, 0));
        // Bind the VAO
        shaderprogram.setUniformMatrix4fv("uMMatrix", modelMatrix);
        shaderprogram.ActivateTexture(textureHandle, texture);

        GlUtils.bind_vertex_array(vaoIds[0]); // Bind the VAO
        GlUtils.DrawElements(GL_TRIANGLES, geometry.getIndices().length, GL_UNSIGNED_INT, 0);
        GlUtils.bind_vertex_array(0); // Unbind the VAO
    }

    public interface Animation {
        public void animate(GameObject object);
    }
}
