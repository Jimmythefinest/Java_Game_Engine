package com.rebuild;

import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Natives.GlUtils;

public class Cube {
    public int vaoId;
    public Matrix4 modelMatrix;
    // private int vboId;
    private int eboId;
    public Cube(){
         // Set up vertex data and buffers and configure vertex attributes
         float[] vertices = {
            // Positions          // Colors
            -0.5f, -0.5f, -0.5f,   // Back face
             0.5f, -0.5f, -0.5f, 
             0.5f,  0.5f, -0.5f, 
            -0.5f,  0.5f, -0.5f, 
            -0.5f, -0.5f,  0.5f,   // Front face
             0.5f, -0.5f,  0.5f, 
             0.5f,  0.5f,  0.5f, 
            -0.5f,  0.5f,  0.5f, 
        };
        float[] colours={
             // Positions          // Colors
              1.0f, 0.0f, 0.0f,  // Back face
               0.0f, 1.0f, 0.0f,
               0.0f, 0.0f, 1.0f,
              1.0f, 1.0f, 0.0f,
             1.0f, 0.0f, 1.0f,  // Front face
             0.0f, 1.0f, 1.0f,
              1.0f, 1.0f, 1.0f,
             0.5f, 0.5f, 0.5f
        };

        int[] indices = {
            // Back face
            0, 1, 2,
            2, 3, 0,
            // Front face
            4, 5, 6,
            6, 7, 4,
            // Left face
            0, 3, 7,
            7, 4, 0,
            // Right face
            1, 5, 6,
            6, 2, 1,
            // Top face
            3, 2, 6,
            6, 7, 3,
            // Bottom face
            0, 1, 5,
            5, 4, 0
        };

        // Create VAO, VBO and EBO
        vaoId = GlUtils.generateVAO(new int[1],0,0)[0];
        int[] vbos=GlUtils.generateVBOs(new int[3],0,0);
        GlUtils.bind_vertex_array(vaoId);
        int vboId =  vbos[0];
        int vboId1 = vbos[1];
        GlUtils.set_VBO_Float(vboId, vertices, vertices.length);
        GlUtils.set_VBO_Float(vboId1, colours, 5);

        GlUtils.set_VBO_attrib_pointer(vboId1, 1, 3);
        GlUtils.set_VBO_attrib_pointer(vboId, 0, 3);


        eboId = vbos[2];
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glBindVertexArray(0); // Unbind VAO
        
        modelMatrix=new Matrix4().identity();

    }
    public Matrix4 getModelMatrix(){
        return modelMatrix;
    }
    
}
