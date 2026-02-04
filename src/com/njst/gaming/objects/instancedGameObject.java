package com.njst.gaming.objects;

import java.util.ArrayList;

import com.njst.gaming.Geometries.Geometry;
import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Natives.GlUtils;
import com.njst.gaming.Natives.ShaderProgram;

public class instancedGameObject extends GameObject{
    public ArrayList<Matrix4> matrices;
    int instanceBuffer;
    public instancedGameObject(Geometry geo,int texture){
        super(geo, texture);
        matrices=new ArrayList<>();
    }
    @Override
    public void  render(ShaderProgram shader, int textureHandle){
        for (Matrix4 mat : matrices) {
            this.modelMatrix=mat;
            super.render(shader, textureHandle);
            
        }

    }
    public void generateBuffers(){
        super.generateBuffers();
        instanceBuffer = GlUtils.generateVBOs(new int[1], 0, 0)[0];
        GlUtils.set_VBO_Float(instanceBuffer, getModelMatrices());
        GlUtils.set_VBO_attrib_pointer(instanceBuffer,6,16);



    }
    public float[] getModelMatrices(){
        float[] data=new float[matrices.size()*16];
        int pos=0;
        for (Matrix4 mat : matrices) {
            System.arraycopy(mat.get(new float[16]),0,data,pos,16);
            pos+=16;
            
        }
        return data;
    }

}