package com.njst.gaming.Math;

public class Vector3f {
    public float x,y,z;
    public Vector3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public Vector3f(Vector3f v){
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }
    public Vector3f() {
        this(0,0,0);
    }
    public Vector3f add(Vector3f v) {
        return new Vector3f(x+v.x, y+v.y, z+v.z);
    }
    public Vector3f sub(Vector3f v) {
        this.x-=v.x;
        this.y-=v.y;
        this.z-=v.z;
        return this;
        }
    public Vector3f cross(Vector3f v){
        return new Vector3f(y*v.z-z*v.y, z*v.x-x*v.z,x*v.y-y*v.x);
    }
    public Vector3f normalize(){
        float length = (float)Math.sqrt((x*x)+(y*y)+(z*z));
        x = x/length;
        y = y/length;
        z = z/length;
        return this;
    }
    public Vector3f rotateY(float angle){
        float cos = (float)Math.cos(angle);
        float sin = (float)Math.sin(angle);
        float newx = x*cos-y*sin;
        float newy = x*sin+y*cos;
        x = newx;
        y = newy;
        return this;
    }
    public Vector3f rotateX(float angle){
        float cos = (float)Math.cos(angle);
        float sin = (float)Math.sin(angle);
        float newx = x*cos+z*sin;
        float newz = -x*sin+z*cos;
        x = newx;
        z = newz;
        return this;
    }
    public Vector3f set(Vector3f v){
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        return this;
    }
    public Vector3f mul(float scalar){
        x*=scalar;
        y*=scalar;
        z*=scalar;
        return this;
    }
    public float distance(Vector3f v){
        return  (float)Math.sqrt(((x-v.x)*(x-v.x))+((y-v.y)*(y-v.y))+((z-v.z)*(z-v.z)));
    }
}
