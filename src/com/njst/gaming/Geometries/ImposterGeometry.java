package com.njst.gaming.Geometries;

import com.njst.gaming.Math.Vector3;

/**
 * A simple 2D billboard quad geometry used for imposters.
 * It always faces the camera (handled by the generator's initial render 
 * and the billboarding logic or just simple orientation).
 */
public class ImposterGeometry extends Geometry {

    private float width, height;

    public ImposterGeometry(float width, float height) {
        this.width = width;
        this.height = height;
        
        this.min = new Vector3(-width / 2, 0, -0.01f);
        this.max = new Vector3(width / 2, height, 0.01f);
    }

    @Override
    public float[] getVertices() {
        return new float[] {
            -width/2, 0,      0,
             width/2, 0,      0,
             width/2, height, 0,
            -width/2, height, 0
        };
    }

    @Override
    public float[] getTextureCoordinates() {
        return new float[] {
            0, 1,
            1, 1,
            1, 0,
            0, 0
        };
    }

    @Override
    public float[] getNormals() {
        return new float[] {
            0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1
        };
    }

    @Override
    public int[] getIndices() {
        return new int[] {
            0, 1, 2,
            2, 3, 0
        };
    }
}
