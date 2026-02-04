package com.njst.gaming.Geometries;

import com.njst.gaming.Math.Vector3;

public  class Geometry {
    public Vector3 max=new Vector3(-0.5f,-0.5f,-0.5f),min=new Vector3(0.5f,0.5f,0.5f);;

    public  float[] getVertices(){
        return null;
    }
    public  float[] getTextureCoordinates(){
        return null;
    }
    public  float[] getNormals(){
        return null;
    }
    public  int[] getIndices(){
        return null;
    }
    public Vector3[] getCollisionBoxes(){
        return new Vector3[]{min,max};
    }
}
