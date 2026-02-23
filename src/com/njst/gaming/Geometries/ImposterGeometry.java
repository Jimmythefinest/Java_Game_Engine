package com.njst.gaming.Geometries;

import com.njst.gaming.Math.Vector3;

/**
 * A simple 2D billboard quad geometry used for imposters.
 * It always faces the camera (handled by the generator's initial render 
 * and the billboarding logic or just simple orientation).
 */
public class ImposterGeometry extends Geometry {

    private float width, height;
    private boolean centerY = false;
    private float u0 = 0f, v0 = 0f, u1 = 1f, v1 = 1f;

    public ImposterGeometry(float width, float height) {
        this.width = width;
        this.height = height;
        this.centerY = false;
        updateBounds();
    }

    public ImposterGeometry(float width, float height, float u0, float v0, float u1, float v1) {
        this(width, height);
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
    }

    public ImposterGeometry(float width, float height, boolean centerY) {
        this.width = width;
        this.height = height;
        this.centerY = centerY;
        updateBounds();
    }

    public ImposterGeometry(float width, float height, float u0, float v0, float u1, float v1, boolean centerY) {
        this(width, height, centerY);
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
    }

    private void updateBounds() {
        if (centerY) {
            this.min = new Vector3(-width / 2, -height / 2, -0.01f);
            this.max = new Vector3(width / 2, height / 2, 0.01f);
        } else {
            this.min = new Vector3(-width / 2, 0, -0.01f);
            this.max = new Vector3(width / 2, height, 0.01f);
        }
    }

    @Override
    public float[] getVertices() {
        float y0 = centerY ? -height / 2 : 0;
        float y1 = centerY ? height / 2 : height;
        return new float[] {
            -width/2, y0,     0,
             width/2, y0,     0,
             width/2, y1,     0,
            -width/2, y1,     0
        };
    }

    @Override
    public float[] getTextureCoordinates() {
        return new float[] {
            u0, v0,
            u1, v0,
            u1, v1,
            u0, v1
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
