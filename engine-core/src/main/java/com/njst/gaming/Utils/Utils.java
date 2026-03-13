package com.njst.gaming.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class Utils{
    public static FloatBuffer Array_to_Buffer(float[] data){
            ByteBuffer bbColors = ByteBuffer.allocateDirect(data.length * 4);
            bbColors.order(ByteOrder.nativeOrder());
            FloatBuffer colorBuffer = bbColors.asFloatBuffer();
            colorBuffer.put(data);
            colorBuffer.position(0);
            return colorBuffer;

    }
    public static ShortBuffer Array_to_Buffer(short[] INDICES){
        ByteBuffer bbIndices = ByteBuffer.allocateDirect(INDICES.length * 2);
        bbIndices.order(ByteOrder.nativeOrder());
        ShortBuffer indexBuffer = bbIndices.asShortBuffer();
        indexBuffer.put(INDICES);
        indexBuffer.position(0);
                return indexBuffer;

    }
    public static IntBuffer Array_to_Buffer(int[] INDICES){
        ByteBuffer bbIndices = ByteBuffer.allocateDirect(INDICES.length * Integer.BYTES);
        bbIndices.order(ByteOrder.nativeOrder());
        IntBuffer indexBuffer = bbIndices.asIntBuffer();
        indexBuffer.put(INDICES);
        indexBuffer.position(0);
                return indexBuffer;

    }
    public static FloatBuffer mallocFloat(int i) {
        ByteBuffer bbColors = ByteBuffer.allocateDirect(i* 4);
        bbColors.order(ByteOrder.nativeOrder());
        return bbColors.asFloatBuffer();
        
    }
    public static IntBuffer mallocInt(int i) {
        ByteBuffer bbColors = ByteBuffer.allocateDirect(i* 4);
        bbColors.order(ByteOrder.nativeOrder());
        return bbColors.asIntBuffer();
        
    }
    
        
}