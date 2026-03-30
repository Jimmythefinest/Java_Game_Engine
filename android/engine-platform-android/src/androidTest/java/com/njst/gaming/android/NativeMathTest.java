package com.njst.gaming.android;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class NativeMathTest {

    @Test
    public void testMultiplyMat4_Identity() {
        float[] identity = new float[] {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
        };
        float[] result = NativeMath.multiplyMat4(identity, identity);
        assertArrayEquals(identity, result, 0.0001f);
    }

    @Test
    public void testAddVec3() {
        float[] a = new float[] { 1, 2, 3 };
        float[] b = new float[] { 4, 5, 6 };
        float[] expected = new float[] { 5, 7, 9 };
        float[] result = NativeMath.addVec3(a, b);
        assertArrayEquals(expected, result, 0.0001f);
    }

    @Test
    public void testDotVec3() {
        float[] a = new float[] { 1, 0, 0 };
        float[] b = new float[] { 1, 0, 0 };
        float result = NativeMath.dotVec3(a, b);
        assertEquals(1.0f, result, 0.0001f);

        float[] c = new float[] { 0, 1, 0 };
        result = NativeMath.dotVec3(a, c);
        assertEquals(0.0f, result, 0.0001f);
    }

    @Test
    public void testNormalizeVec3() {
        float[] v = new float[] { 3, 4, 0 };
        float[] result = NativeMath.normalizeVec3(v);
        float[] expected = new float[] { 0.6f, 0.8f, 0.0f };
        assertArrayEquals(expected, result, 0.0001f);
        
        float length = (float) Math.sqrt(result[0]*result[0] + result[1]*result[1] + result[2]*result[2]);
        assertEquals(1.0f, length, 0.0001f);
    }
}
