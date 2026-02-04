package com.njst.gaming.Geometries;

public class CustomGeometry extends Geometry {
    private float[] vertices;
    private int[] indices;
    private float[] normals;
    private float[] textureCoordinates;

    public CustomGeometry(float[] vertices, int[] indices, float[] normals, float[] textureCoordinates) {
        this.vertices = vertices;
        this.indices = indices;
        this.normals = normals;
        this.textureCoordinates = textureCoordinates;
    }

    @Override
    public float[] getVertices() {
        return vertices;
    }

    @Override
    public int[] getIndices() {
        return indices;
    }

    @Override
    public float[] getNormals() {
        return normals;
    }

    @Override
    public float[] getTextureCoordinates() {
        return textureCoordinates;
    }
}
