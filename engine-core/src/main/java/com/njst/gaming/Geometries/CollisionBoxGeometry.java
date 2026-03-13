package com.njst.gaming.Geometries;

import com.njst.gaming.Math.Vector3;

public class CollisionBoxGeometry extends Geometry {
    private final float[] vertices = new float[8 * 3];

    private static final int[] LINE_INDICES = new int[] {
            0, 1, 1, 2, 2, 3, 3, 0,
            4, 5, 5, 6, 6, 7, 7, 4,
            0, 4, 1, 5, 2, 6, 3, 7
    };

    public CollisionBoxGeometry(Vector3 min, Vector3 max) {
        setBounds(min, max);
    }

    public void setBounds(Vector3 min, Vector3 max) {
        if (min == null || max == null) {
            return;
        }
        this.min = new Vector3(min);
        this.max = new Vector3(max);

        int i = 0;
        vertices[i++] = min.x; vertices[i++] = min.y; vertices[i++] = min.z; // 0
        vertices[i++] = max.x; vertices[i++] = min.y; vertices[i++] = min.z; // 1
        vertices[i++] = max.x; vertices[i++] = max.y; vertices[i++] = min.z; // 2
        vertices[i++] = min.x; vertices[i++] = max.y; vertices[i++] = min.z; // 3

        vertices[i++] = min.x; vertices[i++] = min.y; vertices[i++] = max.z; // 4
        vertices[i++] = max.x; vertices[i++] = min.y; vertices[i++] = max.z; // 5
        vertices[i++] = max.x; vertices[i++] = max.y; vertices[i++] = max.z; // 6
        vertices[i++] = min.x; vertices[i++] = max.y; vertices[i++] = max.z; // 7
    }

    @Override
    public float[] getVertices() {
        return vertices;
    }

    @Override
    public float[] getTextureCoordinates() {
        return new float[8 * 2];
    }

    @Override
    public float[] getNormals() {
        return new float[8 * 3];
    }

    @Override
    public int[] getIndices() {
        return LINE_INDICES;
    }
}
