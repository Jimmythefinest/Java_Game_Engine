package com.njst.gaming.objects;

import java.util.ArrayList;

import com.njst.gaming.Geometries.Geometry;
import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.graphics.ShaderHandle;

public class instancedGameObject extends GameObject{
    public ArrayList<Matrix4> matrices;
    int instanceBuffer;
    public instancedGameObject(Geometry geo,int texture){
        super(geo, texture);
        matrices=new ArrayList<>();
    }
    @Override
    public void  render(ShaderHandle shader, int textureHandle){
        for (Matrix4 mat : matrices) {
            this.modelMatrix=mat;
            super.render(shader, textureHandle);
            
        }

    }
    public void generateBuffers(){
        super.generateBuffers();
        instanceBuffer = graphicsDevice.createBuffers(1)[0];
        graphicsDevice.uploadArrayBufferFloat(instanceBuffer, getModelMatrices());
        graphicsDevice.setVertexAttribPointer(instanceBuffer,6,16);



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
