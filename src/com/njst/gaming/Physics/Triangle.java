package com.njst.gaming.Physics;

import com.njst.gaming.Math.Vector3;

public class Triangle {
    Vector3 v1,v2,v3;
    public Triangle(Vector3 v1,Vector3 v2,Vector3 v3){
        this.v1=v1;
        this.v2=v2;
        this.v3=v3;
    }
    public boolean intersects_line(Vector3 origin,Vector3 direction){
        return false;
    }
    public Vector3 midpoint(){
        return v1.add(v2,new Vector3()).add(v3,new Vector3()).mul(1f/3f);
    }
    public Vector3 normal(){
        return midpoint().sub(v3);
    }
    public static void main(String[] args) {
        Triangle test=new Triangle(
            new Vector3(2,1,0), 
            new Vector3(0,5,1), 
            new Vector3(1,0,2));
        System.out.println(test.normal().length());
        
    }
}
