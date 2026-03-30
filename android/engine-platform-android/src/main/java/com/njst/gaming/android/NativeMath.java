package com.njst.gaming.android;

public class NativeMath {
    static {
        System.loadLibrary("njst_math");
    }

    /**
     * Multiplies two 4x4 column-major matrices.
     */
    public static native float[] multiplyMat4(float[] a, float[] b);

    /**
     * Adds two 3D vectors.
     */
    public static native float[] addVec3(float[] a, float[] b);

    /**
     * Calculates the dot product of two 3D vectors.
     */
    public static native float dotVec3(float[] a, float[] b);

    /**
     * Normalizes a 3D vector.
     */
    public static native float[] normalizeVec3(float[] v);
}
