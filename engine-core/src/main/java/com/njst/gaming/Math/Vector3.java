package com.njst.gaming.Math;

import org.joml.Vector3f;
import org.joml.Vector3fc;

public class Vector3 {
    public float x,y,z;
    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public Vector3(Vector3 v){
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }
    public Vector3() {
        this(0,0,0);
    }
    public Vector3(float x) {
        this(x,x,x);
    }
    public Vector3(float[] data) {
        this.x = data[0];
        this.y = data[1];
        this.z = data[2];
    }
    public Vector3 add(Vector3 v) {
        this.x+=v.x;
        this.y+=v.y;
        this.z+=v.z;
        return this;
    }
    public Vector3 add(Vector3 v,Vector3 dist) {
        dist.set(this);
        dist.add(v);
        return dist;
    }
    public Vector3 sub(Vector3 v) {
        this.x-=v.x;
        this.y-=v.y;
        this.z-=v.z;
        return this;
        }
    public Vector3 cross(Vector3 v){
        return new Vector3(y*v.z-z*v.y, z*v.x-x*v.z,x*v.y-y*v.x);
    }
    public Vector3 normalize(){
        float length = (float)Math.sqrt((x*x)+(y*y)+(z*z));
        x = x/length;
        y = y/length;
        z = z/length;
        return this;
    }
    public Vector3 rotateY(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        return new Vector3(
            cos * x + sin * z,
            y,
            -sin * x + cos * z
        );
    }

    public Vector3 rotateX(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        return new Vector3(
            x,
            cos * y - sin * z,
            sin * y + cos * z
        );
    }
    public Vector3 rotateZ(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        return new Vector3(
            cos * x - sin * y,
            sin * x + cos * y,
            z // Z remains unchanged
        );
    }

    public Vector3 set(Vector3 v){
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        return this;
    }
    public Vector3 mul(float scalar){
        x*=scalar;
        y*=scalar;
        z*=scalar;
        return this;
    }
    public float distance(Vector3 v){
        return  (float)Math.sqrt(((x-v.x)*(x-v.x))+((y-v.y)*(y-v.y))+((z-v.z)*(z-v.z)));
    }
    public Vector3 set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }
    public Vector3 sub(Vector3 v0, Vector3 vector3) {
        return vector3.set(this.x-v0.x,
        this.y-v0.y,
        this.z-v0.z);
        
    }
    public float dot(Vector3 sub) {
        return (this.x*sub.x)+(this.y*sub.y)+(this.z*sub.z);
    }
    public Vector3 clone() {
        return new Vector3(this);
    }
    public Vector3 lerp(Vector3 target, float t) {
    // Clamp t to the range [0, 1]
    t = Math.max(0, Math.min(1, t));
    return new Vector3(
        this.x + (target.x - this.x) * t,
        this.y + (target.y - this.y) * t,
        this.z + (target.z - this.z) * t
    );
}
    public Vector3fc asVector3f() {
       return new Vector3f(x,y,z);
    }
    public String toString(){
        return "x:"+x+" y:"+y+" z:"+z;
    }
    public float length() {
        return distance(new Vector3());
        
    }
    public float[] toArray() {
       return new float[]{x,y,z};
    }

}
