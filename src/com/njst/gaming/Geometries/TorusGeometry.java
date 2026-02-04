package com.njst.gaming.Geometries;

public class TorusGeometry extends Geometry {
    private float[] vertices;
    private float[] textureCoordinates;
    private float[] normals;
    private int[] indices;

    private int radialSegments;
    private int tubularSegments;
    private float radius;
    private float tubeRadius;

    public TorusGeometry(float radius, float tubeRadius, int radialSegments, int tubularSegments) {
        this.radius = radius;
        this.tubeRadius = tubeRadius;
        this.radialSegments = radialSegments;
        this.tubularSegments = tubularSegments;

        createGeometry();
    }

    private void createGeometry() {
        int vertexCount = (radialSegments + 1) * (tubularSegments + 1);
        vertices = new float[vertexCount * 3];
        textureCoordinates = new float[vertexCount * 2];
        normals = new float[vertexCount * 3];
        indices = new int[radialSegments * tubularSegments * 6];

        int vertexIndex = 0;
        int indexIndex = 0;

        for (int i = 0; i <= radialSegments; i++) {
            for (int j = 0; j <= tubularSegments; j++) {
                float u = (float) i / radialSegments * (float) Math.PI * 2;
                float v = (float) j / tubularSegments * (float) Math.PI * 2;

                // Vertex position
                float x = (float) ((radius + tubeRadius * Math.cos(v)) * Math.cos(u));
                float y = (float) ((radius + tubeRadius * Math.cos(v)) * Math.sin(u));
                float z = (float) (tubeRadius * Math.sin(v));

                vertices[vertexIndex++] = x;
                vertices[vertexIndex++] = y;
                vertices[vertexIndex++] = z;

                // Normal vector
                normals[vertexIndex - 3] = (float) (Math.cos(v) * Math.cos(u));
                normals[vertexIndex - 2] = (float) (Math.cos(v) * Math.sin(u));
                normals[vertexIndex - 1] = (float) (Math.sin(v));

                // Texture coordinates
                textureCoordinates[vertexIndex / 3 * 2 - 2] = (float) i / radialSegments;
                textureCoordinates[vertexIndex / 3 * 2 - 1] = (float) j / tubularSegments;
            }
        }

        // Create indices
        for (int i = 0; i < radialSegments; i++) {
            for (int j = 0; j < tubularSegments; j++) {
                int a = (i * (tubularSegments + 1)) + j;
                int b = ((i + 1) * (tubularSegments + 1)) + j;
                int c = ((i + 1) * (tubularSegments + 1)) + (j + 1);
                int d = (i * (tubularSegments + 1)) + (j + 1);

                // First triangle
                indices[indexIndex++] = (int) a;
                indices[indexIndex++] = (int) b;
                indices[indexIndex++] = (int) d;

                // Second triangle
                indices[indexIndex++] = (int) b;
                indices[indexIndex++] = (int) c;
                indices[indexIndex++] = (int) d;
            }
        }
    }

    @Override
        public
        float[] getVertices() {
            return vertices;
        }

    @Override
        public
        float[] getTextureCoordinates() {
            return textureCoordinates;
        }

    @Override
    public float[] getNormals() {
            return normals;
        }

    @Override
        public
        int[] getIndices() {
            return indices;
        }
}