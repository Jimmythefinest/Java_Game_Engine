package com.njst.gaming.Geometries;

public class SphereGeometry extends Geometry {
    private final float radius;
    private final int latitudeBands;
    private final int longitudeBands;

    public SphereGeometry(float radius, int latitudeBands, int longitudeBands) {
        this.radius = radius;
        this.latitudeBands = latitudeBands;
        this.longitudeBands = longitudeBands;
    }

    @Override
    public float[] getVertices() {
        int numVertices = (latitudeBands + 1) * (longitudeBands + 1);
        float[] vertices = new float[numVertices * 3]; // x, y, z for each vertex
        int index = 0;

        for (int lat = 0; lat <= latitudeBands; lat++) {
            double theta = lat * Math.PI / latitudeBands;
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            for (int lon = 0; lon <= longitudeBands; lon++) {
                double phi = lon * 2 * Math.PI / longitudeBands;
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                float x = (float) (radius * sinTheta * cosPhi);
                float y = (float) (radius * cosTheta);
                float z = (float) (radius * sinTheta * sinPhi);

                vertices[index++] = x;
                vertices[index++] = y;
                vertices[index++] = z;
            }
        }
        return vertices;
    }

    @Override
    public float[] getTextureCoordinates() {
        int numVertices = (latitudeBands + 1) * (longitudeBands + 1);
        float[] textureCoords = new float[numVertices * 2]; // u, v for each vertex
        int index = 0;

        for (int lat = 0; lat <= latitudeBands; lat++) {
            for (int lon = 0; lon <= longitudeBands; lon++) {
                float u = (float) lon / longitudeBands;
                float v = (float) lat / latitudeBands;

                textureCoords[index++] = u;
                textureCoords[index++] = v;
            }
        }
        return textureCoords;
    }

    @Override
    public float[] getNormals() {
        int numVertices = (latitudeBands + 1) * (longitudeBands + 1);
        float[] normals = new float[numVertices * 3]; // nx, ny, nz for each vertex
        int index = 0;

        for (int lat = 0; lat <= latitudeBands; lat++) {
            double theta = lat * Math.PI / latitudeBands;
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            for (int lon = 0; lon <= longitudeBands; lon++) {
                double phi = lon * 2 * Math.PI / longitudeBands;
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                // Normal is the same as the vertex direction for a sphere
                normals[index++] = (float) (sinTheta * cosPhi);
                normals[index++] = (float) (cosTheta);
                normals[index++] = (float) (sinTheta * sinPhi);
            }
        }
        return normals;
    }

    @Override
    public int[] getIndices() {
        int numIndices = latitudeBands * longitudeBands * 6;
        int[] indices = new int[numIndices];
        int index = 0;

        for (int lat = 0; lat < latitudeBands; lat++) {
            for (int lon = 0; lon < longitudeBands; lon++) {
                int first = (lat * (longitudeBands + 1)) + lon;
                int second = first + longitudeBands + 1;

                indices[index++] = (int) first;
                indices[index++] = (int) second;
                indices[index++] = (int) (first + 1);
                indices[index++] = (int) second;
                indices[index++] = (int) (second + 1);
                indices[index++] = (int) (first + 1);
            }
        }
        return indices;
    }
}