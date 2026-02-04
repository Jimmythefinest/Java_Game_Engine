package com.njst.gaming.Math;


import java.nio.*;

import org.joml.*;

import com.njst.gaming.Utils.Utils;


public class Matrix4 {
    public float[] r;
    public Matrix4() {
        r=new float[16];
    }

    public Matrix4 identity(){
        r=new Matrix4f().identity().get(r);//.setIdentityM(r, 0);
     
        return this;
    }
    public Matrix4 lookAt(Vector3 camera_position,Vector3 target_position,Vector3 up){
        //r.setLookAt();
        lookAt(camera_position.x,camera_position.y,camera_position.z,
        target_position.x,target_position.y,target_position.z,
        up.x,up.y,up.z);
        return this;
    }
    
    // Helper vector functions
    private float[] normalize(float[] v) {
        float length = (float)java.lang.Math.sqrt(dot(v, v));
        return new float[]{ v[0] / length, v[1] / length, v[2] / length };
    }
    
    private float dot(float[] a, float[] b) {
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }
    public Matrix4 set(float[] r){
        this.r=r;
        return this;
    }
    private float[] cross(float[] a, float[] b) {
        return new float[]{
            a[1]*b[2] - a[2]*b[1],
            a[2]*b[0] - a[0]*b[2],
            a[0]*b[1] - a[1]*b[0]
        };
    }
    // Simple lookAt matrix (column-major order)
    public  Matrix4 lookAt(float eyeX, float eyeY, float eyeZ,
                                       float centerX, float centerY, float centerZ,
                                       float upX, float upY, float upZ) {
        float[] f = normalize(new float[]{centerX - eyeX, centerY - eyeY, centerZ - eyeZ});
        float[] up = normalize(new float[]{upX, upY, upZ});
        float[] s = cross(f, up);
        s = normalize(s);
        float[] u = cross(s, f);
        
        float[] result = new float[16];
        result[0] = s[0];
        result[1] = u[0];
        result[2] = -f[0];
        result[3] = 0;
        
        result[4] = s[1];
        result[5] = u[1];
        result[6] = -f[1];
        result[7] = 0;
        
        result[8] = s[2];
        result[9] = u[2];
        result[10] = -f[2];
        result[11] = 0;
        
        result[12] = -dot(s, new float[]{eyeX, eyeY, eyeZ});
        result[13] = -dot(u, new float[]{eyeX, eyeY, eyeZ});
        result[14] = dot(f, new float[]{eyeX, eyeY, eyeZ});
        result[15] = 1;
        r=result;
        return this;
    }
    

    public Matrix4 translate(float x, float y, float z) {
        //r.translate(new Vector3f(x, y, z));
        new Matrix4f().set(r).translate( x, y, z).get(r);
        return this;
    }
    public Matrix4 translate(Vector3 pos) {
        //r.translate(new Vector3f(x, y, z));
        new Matrix4f().set(r).translate(pos.x,pos.y,pos.z).get(r);
        return this;
    }

    public Matrix4 rotate(float radians, Vector3 vector3) {
        new Matrix4f().set(r).rotate((float)java.lang.Math.toRadians(radians),vector3.asVector3f() ).get(r);;
        return this;
    }

    public Matrix4 scale(Vector3 scale) {
        new Matrix4f().set(r).scale(scale.x,scale.y, scale.z).get(r);
        return this;

    }

    public float[] getMatrix4f() {
        return r;
    }

    public float[] get(float[] mmatrix) {
        mmatrix=r;
        return mmatrix;
    }

    public Matrix4 perspective(float fov, float aspect, float near, float far) {
        float f = (float)(1.0f / java.lang.Math.tan(fov / 2.0f));
        float[] m = new float[16];
        m[0] = f / aspect;
        m[5] = f;
        m[10] = (far + near) / (near - far);
        m[11] = -1;
        m[14] = (2 * far * near) / (near - far);
        r=m;
        return this;
    }
    public Matrix4 ortho(float left, float right, float bottom, float top, float near, float far) {
        new Matrix4f().set(r).ortho( left, right, bottom, top, near, far);
    return this;
}

    public void get(FloatBuffer buffer) {
        buffer=Utils.Array_to_Buffer(r);
    }

    public FloatBuffer getAsBuffer() {
        // TODO Auto-generated method stub
        return Utils.Array_to_Buffer(r);
    }

    public Matrix4 invert() {
        Matrix4f mat=new Matrix4f().set(r).invert();
        r=mat.get(r);
        return this;
    }

    public Matrix4 multiply(Matrix4 inverse_bindpose) {
        Matrix4f mat1=new Matrix4f(),mat2=new Matrix4f();
        mat1.set(r);
        mat2.set(inverse_bindpose.r);
        mat1.mul(mat2);
        return new Matrix4().set((mat1.get(r)));
    }

    public Vector3 multiply(Vector3 vector3) {
        Matrix4f mat=new Matrix4f().set(r);
        Vector4f v=mat.transformProject(new Vector4f(vector3.x,vector3.y,vector3.z,1));
        return new Vector3(v.x,v.y,v.z);
    }
}
