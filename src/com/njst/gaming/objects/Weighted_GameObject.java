package com.njst.gaming.objects;

import com.njst.gaming.Geometries.WeightedGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.GlUtils;
import com.njst.gaming.Natives.ShaderProgram;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL30.*;
import com.njst.gaming.Animations.SkeletalAnimationController;
import com.njst.gaming.Animations.Animation;


public class Weighted_GameObject extends GameObject{
    public  WeightedGeometry geo;
    ShaderProgram program1;
    private SkeletalAnimationController animationController;

    public Weighted_GameObject(WeightedGeometry geo,int t){
        super(geo, t);
        this.geo=geo;
    }

    public void setAnimationController(SkeletalAnimationController controller) {
        this.animationController = controller;
    }

    public SkeletalAnimationController getAnimationController() {
        return animationController;
    }
    public void generateBuffers() {

        int vaoId = GlUtils.generateVAO(new int[1], 0, 0)[0];
        int[] vbos = GlUtils.generateVBOs(new int[6], 0, 0);
        GlUtils.bind_vertex_array(vaoId);
        int vboId = vbos[0];
        int vboId1 = vbos[1];
        GlUtils.set_VBO_Float(vboId, geo.getVertices());
        GlUtils.set_VBO_Float(vboId1, geo.getNormals(), 5);
        GlUtils.set_VBO_Float(vbos[2], geo.getTextureCoordinates());
        GlUtils.set_VBO_Float(vbos[3], geo.getWeightss(), geo.getWeightss().length);
        GlUtils.set_VBO_Int(vbos[4], geo.getBoness());
        System.out.println("Number of Weights"+geo.getWeightss().length/4);
        System.out.println("Number of Vertices"+(geo.getVertices().length/3));

        GlUtils.set_VBO_attrib_pointer(vboId, 0, 3);
        GlUtils.set_VBO_attrib_pointer(vboId1, 1, 3);
        GlUtils.set_VBO_attrib_pointer(vbos[2], 2, 2);
        GlUtils.set_VBO_attrib_pointer(vbos[3], 3, 4);
        GlUtils.set_VBO_attrib_pointer(vbos[4], 4, 4);

        int eboId = vbos[5];
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, geo.getIndices(), GL_STATIC_DRAW);

        glBindVertexArray(0);
        vaoIds[0] = vaoId;
        program1 = new ShaderProgram(
                ShaderProgram.loadShader("/jimmy/vert111.glsl"), ShaderProgram.loadShader("/jimmy/frag111.glsl"));
       
    }
     public void render(ShaderProgram shaderprogram, int textureHandle) {
        if (animationController != null) {
            animationController.bind();
        }
        program1.use();
        program1.setUniformVector3("properties", new Vector3(shininess, ambientlight_multiplier, 0));
        // Bind the VAO
        program1.setUniformMatrix4fv("uMMatrix", modelMatrix);
        program1.ActivateTexture(textureHandle, texture);

        GlUtils.bind_vertex_array(vaoIds[0]); // Bind the VAO
        GlUtils.DrawElements(GL_TRIANGLES, geo.getIndices().length, GL_UNSIGNED_INT, 0);
        GlUtils.bind_vertex_array(0); // Unbind the VAO
        shaderprogram.use();
    }
}
