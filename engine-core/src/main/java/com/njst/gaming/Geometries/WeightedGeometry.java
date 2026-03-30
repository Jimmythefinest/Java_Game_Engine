package com.njst.gaming.Geometries;

import java.io.Serializable;

public class WeightedGeometry extends Geometry implements Serializable {
    private static final long serialVersionUID = 1L;

    float[] vertices, normals, texture_coordinates, weights;
    int[] indices, bones;

    public WeightedGeometry(float[] vertices, float[] normals, float[] texture_coordinates, float[] weights, int[] indices, int[] bones) {
        this.vertices = vertices;
        this.normals = normals;
        this.texture_coordinates = texture_coordinates;
        this.weights = weights;
        this.indices = indices;
        this.bones = bones;
    }

    @Override
    public float[] getVertices() {
        return vertices;
    }

    @Override
    public float[] getTextureCoordinates() {
        return texture_coordinates;
    }

    @Override
    public float[] getNormals() {
        return normals;
    }

    @Override
    public int[] getIndices() {
        return indices;
    }

    public int[] getBoness() {
        return bones;
    }

    public float[] getWeightss() {
        return weights;
    }
}
