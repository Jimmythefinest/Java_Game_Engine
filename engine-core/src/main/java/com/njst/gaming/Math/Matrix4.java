package com.njst.gaming.Math;

import java.io.Serializable;
import java.nio.*;

import org.joml.*;

import com.njst.gaming.Utils.Utils;

/**
 * Mutable 4x4 matrix wrapper used for transforms, view matrices, and projections.
 */
public class Matrix4 implements Serializable {
    private static final long serialVersionUID = 1L;
    public float[] r;

    public Matrix4() {
        r = new float[16];
    }

    /** Resets this matrix to identity. */
    public Matrix4 identity() {
        r = new Matrix4f().identity().get(r);// .setIdentityM(r, 0);

        return this;
    }

    /**
     * Builds a look-at view matrix from camera vectors.
     *
     * @param camera_position the eye position
     * @param target_position the point being looked at
     * @param up the up direction
     */
    public Matrix4 lookAt(Vector3 camera_position, Vector3 target_position, Vector3 up) {
        // r.setLookAt();
        lookAt(camera_position.x, camera_position.y, camera_position.z,
                target_position.x, target_position.y, target_position.z,
                up.x, up.y, up.z);
        return this;
    }

    // Helper vector functions
    private float[] normalize(float[] v) {
        float length = (float) java.lang.Math.sqrt(dot(v, v));
        return new float[] { v[0] / length, v[1] / length, v[2] / length };
    }

    private float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    /**
     * Replaces the internal backing array reference.
     *
     * @param r the raw 16-float matrix array to use
     */
    public Matrix4 set(float[] r) {
        this.r = r;
        return this;
    }

    private float[] cross(float[] a, float[] b) {
        return new float[] {
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]
        };
    }

    // Simple lookAt matrix (column-major order)
    /**
     * Builds a look-at view matrix from scalar camera values.
     *
     * @param eyeX eye position x
     * @param eyeY eye position y
     * @param eyeZ eye position z
     * @param centerX target position x
     * @param centerY target position y
     * @param centerZ target position z
     * @param upX up direction x
     * @param upY up direction y
     * @param upZ up direction z
     */
    public Matrix4 lookAt(float eyeX, float eyeY, float eyeZ,
            float centerX, float centerY, float centerZ,
            float upX, float upY, float upZ) {
        float[] f = normalize(new float[] { centerX - eyeX, centerY - eyeY, centerZ - eyeZ });
        float[] up = normalize(new float[] { upX, upY, upZ });
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

        result[12] = -dot(s, new float[] { eyeX, eyeY, eyeZ });
        result[13] = -dot(u, new float[] { eyeX, eyeY, eyeZ });
        result[14] = dot(f, new float[] { eyeX, eyeY, eyeZ });
        result[15] = 1;
        r = result;
        return this;
    }

    /**
     * Applies translation to the current matrix.
     *
     * @param x translation on the x axis
     * @param y translation on the y axis
     * @param z translation on the z axis
     */
    public Matrix4 translate(float x, float y, float z) {
        // r.translate(new Vector3f(x, y, z));
        new Matrix4f().set(r).translate(x, y, z).get(r);
        return this;
    }

    /**
     * Applies translation to the current matrix.
     *
     * @param pos translation vector
     */
    public Matrix4 translate(Vector3 pos) {
        // r.translate(new Vector3f(x, y, z));
        new Matrix4f().set(r).translate(pos.x, pos.y, pos.z).get(r);
        return this;
    }

    /**
     * Applies axis-angle rotation.
     * Current engine call sites pass degrees even though the parameter is named radians.
     *
     * @param radians rotation amount, currently used like degrees in engine call sites
     * @param vector3 rotation axis
     */
    public Matrix4 rotate(float radians, Vector3 vector3) {
        new Matrix4f().set(r).rotate((float) java.lang.Math.toRadians(radians), vector3.asVector3f()).get(r);
        ;
        return this;
    }

    /**
     * Applies quaternion rotation to the current matrix.
     *
     * @param quaternion quaternion rotation to apply
     */
    public Matrix4 rotate(Quaternion quaternion) {
        new Matrix4f().set(r)
                .rotate(new Quaternionf(quaternion.x, quaternion.y, quaternion.z, quaternion.w))
                .get(r);
        return this;
    }

    /**
     * Applies scale to the current matrix.
     *
     * @param scale scale factors for x, y, and z
     */
    public Matrix4 scale(Vector3 scale) {
        new Matrix4f().set(r).scale(scale.x, scale.y, scale.z).get(r);
        return this;

    }

    public float[] getMatrix4f() {
        return r;
    }

    public float[] get(float[] mmatrix) {
        mmatrix = r;
        return mmatrix;
    }

    /**
     * Builds a perspective projection matrix.
     * The math here expects {@code fov} in radians.
     *
     * @param fov field of view
     * @param aspect viewport aspect ratio
     * @param near near clipping plane
     * @param far far clipping plane
     */
    public Matrix4 perspective(float fov, float aspect, float near, float far) {
        float f = (float) (1.0f / java.lang.Math.tan(fov / 2.0f));
        float[] m = new float[16];
        m[0] = f / aspect;
        m[5] = f;
        m[10] = (far + near) / (near - far);
        m[11] = -1;
        m[14] = (2 * far * near) / (near - far);
        r = m;
        return this;
    }

    /**
     * Builds an orthographic projection matrix.
     *
     * @param left left bound
     * @param right right bound
     * @param bottom bottom bound
     * @param top top bound
     * @param near near clipping plane
     * @param far far clipping plane
     */
    public Matrix4 ortho(float left, float right, float bottom, float top, float near, float far) {
        new Matrix4f().set(r).ortho(left, right, bottom, top, near, far);
        return this;
    }

    /**
     * Returns a buffer view of this matrix.
     * Prefer {@link #getAsBuffer()} when you need the created buffer.
     *
     * @param buffer nominal output buffer
     */
    public void get(FloatBuffer buffer) {
        buffer = Utils.Array_to_Buffer(r);
    }

    /** Returns this matrix as a newly created FloatBuffer. */
    public FloatBuffer getAsBuffer() {
        return Utils.Array_to_Buffer(r);
    }

    /** Inverts this matrix in place. */
    public Matrix4 invert() {
        Matrix4f mat = new Matrix4f().set(r).invert();
        r = mat.get(r);
        return this;
    }

    /**
     * Returns a new matrix containing {@code this * other}.
     *
     * @param inverse_bindpose right-hand matrix operand
     */
    public Matrix4 multiply(Matrix4 inverse_bindpose) {
        Matrix4f mat1 = new Matrix4f(), mat2 = new Matrix4f();
        mat1.set(r);
        mat2.set(inverse_bindpose.r);
        mat1.mul(mat2);
        return new Matrix4().set((mat1.get(r)));
    }

    /**
     * Transforms a point vector using homogeneous coordinates and projection divide.
     *
     * @param vector3 point to transform
     */
    public Vector3 multiply(Vector3 vector3) {
        Matrix4f mat = new Matrix4f().set(r);
        Vector4f v = mat.transformProject(new Vector4f(vector3.x, vector3.y, vector3.z, 1));
        return new Vector3(v.x, v.y, v.z);
    }
}
