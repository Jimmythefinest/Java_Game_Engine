package com.njst.gaming.Geometries;

import com.njst.gaming.Math.Vector3;

public class SphericalHeightmapGeometry extends Geometry {
    private final int width;
    private final int height;
    private final float baseRadius;
    private final float[][] heightSamples;

    private float[] vertices;
    private float[] normals;
    private float[] textureCoordinates;
    private int[] indices;

    public SphericalHeightmapGeometry(float[][] heightSamples, float baseRadius) {
        if (heightSamples == null || heightSamples.length < 2 || heightSamples[0].length < 2) {
            throw new IllegalArgumentException("heightSamples must be at least 2x2.");
        }
        this.height = heightSamples.length;
        this.width = heightSamples[0].length;
        this.heightSamples = copy(heightSamples);
        this.baseRadius = baseRadius;
        rebuild();
    }

    private void rebuild() {
        int vertexCount = width * height;
        vertices = new float[vertexCount * 3];
        normals = new float[vertexCount * 3];
        textureCoordinates = new float[vertexCount * 2];
        indices = new int[(height - 1) * width * 6];

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        int vertexIndex = 0;
        int uvIndex = 0;
        for (int row = 0; row < height; row++) {
            float v = row / (float) (height - 1);
            float polar = v * (float) Math.PI;
            float sinPolar = (float) Math.sin(polar);
            float y = (float) Math.cos(polar);
            for (int col = 0; col < width; col++) {
                float u = col / (float) (width - 1);
                float azimuth = (u - 0.5f) * (float) (Math.PI * 2.0);
                float radius = baseRadius + heightSamples[row][col];
                float x = (float) Math.cos(azimuth) * sinPolar * radius;
                float z = (float) Math.sin(azimuth) * sinPolar * radius;
                float py = y * radius;

                vertices[vertexIndex++] = x;
                vertices[vertexIndex++] = py;
                vertices[vertexIndex++] = z;

                textureCoordinates[uvIndex++] = u;
                textureCoordinates[uvIndex++] = v;

                if (x < minX) minX = x;
                if (py < minY) minY = py;
                if (z < minZ) minZ = z;
                if (x > maxX) maxX = x;
                if (py > maxY) maxY = py;
                if (z > maxZ) maxZ = z;
            }
        }

        min = new Vector3(minX, minY, minZ);
        max = new Vector3(maxX, maxY, maxZ);

        computeNormals();
        buildIndices();
    }

    private void computeNormals() {
        for (int i = 0; i < normals.length; i++) {
            normals[i] = 0f;
        }

        for (int row = 0; row < height - 1; row++) {
            for (int col = 0; col < width; col++) {
                int nextCol = (col + 1) % width;

                int topLeft = row * width + col;
                int topRight = row * width + nextCol;
                int bottomLeft = (row + 1) * width + col;
                int bottomRight = (row + 1) * width + nextCol;

                accumulateTriangleNormal(topLeft, bottomLeft, topRight);
                accumulateTriangleNormal(topRight, bottomLeft, bottomRight);
            }
        }

        for (int i = 0; i < normals.length; i += 3) {
            Vector3 normal = new Vector3(normals[i], normals[i + 1], normals[i + 2]);
            if (normal.length() <= 0.000001f) {
                normal = new Vector3(vertices[i], vertices[i + 1], vertices[i + 2]).normalize();
            } else {
                normal.normalize();
            }
            normals[i] = normal.x;
            normals[i + 1] = normal.y;
            normals[i + 2] = normal.z;
        }
    }

    private void accumulateTriangleNormal(int index0, int index1, int index2) {
        Vector3 v0 = vertexAt(index0);
        Vector3 v1 = vertexAt(index1);
        Vector3 v2 = vertexAt(index2);
        Vector3 edge1 = v1.sub(v0, new Vector3());
        Vector3 edge2 = v2.sub(v0, new Vector3());
        Vector3 faceNormal = edge1.cross(edge2);
        addNormal(index0, faceNormal);
        addNormal(index1, faceNormal);
        addNormal(index2, faceNormal);
    }

    private void addNormal(int vertexIndex, Vector3 normal) {
        int base = vertexIndex * 3;
        normals[base] += normal.x;
        normals[base + 1] += normal.y;
        normals[base + 2] += normal.z;
    }

    private Vector3 vertexAt(int vertexIndex) {
        int base = vertexIndex * 3;
        return new Vector3(vertices[base], vertices[base + 1], vertices[base + 2]);
    }

    private void buildIndices() {
        int index = 0;
        for (int row = 0; row < height - 1; row++) {
            for (int col = 0; col < width; col++) {
                int nextCol = (col + 1) % width;

                int topLeft = row * width + col;
                int topRight = row * width + nextCol;
                int bottomLeft = (row + 1) * width + col;
                int bottomRight = (row + 1) * width + nextCol;

                indices[index++] = topRight;
                indices[index++] = bottomLeft;
                indices[index++] = topLeft;

                indices[index++] = bottomRight;
                indices[index++] = bottomLeft;
                indices[index++] = topRight;
            }
        }
    }

    private float[][] copy(float[][] source) {
        float[][] result = new float[source.length][];
        for (int i = 0; i < source.length; i++) {
            result[i] = new float[source[i].length];
            System.arraycopy(source[i], 0, result[i], 0, source[i].length);
        }
        return result;
    }

    @Override
    public float[] getVertices() {
        return vertices;
    }

    @Override
    public float[] getTextureCoordinates() {
        return textureCoordinates;
    }

    @Override
    public float[] getNormals() {
        return normals;
    }

    @Override
    public int[] getIndices() {
        return indices;
    }
}
