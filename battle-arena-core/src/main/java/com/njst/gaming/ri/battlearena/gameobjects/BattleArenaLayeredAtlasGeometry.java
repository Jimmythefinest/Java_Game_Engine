package com.njst.gaming.ri.battlearena.gameobjects;

import com.njst.gaming.Geometries.Geometry;

public final class BattleArenaLayeredAtlasGeometry extends Geometry {
    private static final int LAYER_COUNT = 4;
    private static final int VERTICES_PER_LAYER = 4;
    private static final int FLOATS_PER_VERTEX = 3;
    private static final int UV_FLOATS_PER_LAYER = 8;
    private static final int[] FRAME_OFFSETS = {0, 3, 7, 11};
    private static final float[] LAYER_DEPTH_OFFSETS = {-0.18f, -0.06f, 0.06f, 0.18f};
    private static final float[] LAYER_WIDTHS = {1.0f, 0.92f, 1.08f, 0.84f};
    private static final float[] LAYER_HEIGHTS = {1.0f, 1.12f, 0.9f, 1.04f};

    private final float[] vertices = new float[LAYER_COUNT * VERTICES_PER_LAYER * FLOATS_PER_VERTEX];
    private final float[] normals = new float[LAYER_COUNT * VERTICES_PER_LAYER * FLOATS_PER_VERTEX];
    private final float[] textureCoordinates = new float[LAYER_COUNT * UV_FLOATS_PER_LAYER];
    private final int[] indices = new int[LAYER_COUNT * 6];

    public BattleArenaLayeredAtlasGeometry(int columns, int rows, int frame) {
        buildVertices();
        buildIndices();
        setFrame(columns, rows, frame);
    }

    public void setFrame(int columns, int rows, int frame) {
        int safeColumns = Math.max(1, columns);
        int safeRows = Math.max(1, rows);
        int frameCount = safeColumns * safeRows;
        for (int layer = 0; layer < LAYER_COUNT; layer++) {
            int safeFrame = ((frame + FRAME_OFFSETS[layer]) % frameCount + frameCount) % frameCount;
            int column = safeFrame % safeColumns;
            int row = safeFrame / safeColumns;
            float u0 = column / (float) safeColumns;
            float u1 = (column + 1) / (float) safeColumns;
            float v0 = row / (float) safeRows;
            float v1 = (row + 1) / (float) safeRows;
            int offset = layer * UV_FLOATS_PER_LAYER;
            textureCoordinates[offset] = u0;
            textureCoordinates[offset + 1] = v1;
            textureCoordinates[offset + 2] = u1;
            textureCoordinates[offset + 3] = v1;
            textureCoordinates[offset + 4] = u1;
            textureCoordinates[offset + 5] = v0;
            textureCoordinates[offset + 6] = u0;
            textureCoordinates[offset + 7] = v0;
        }
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

    private void buildVertices() {
        for (int layer = 0; layer < LAYER_COUNT; layer++) {
            float halfWidth = LAYER_WIDTHS[layer] * 0.5f;
            float halfHeight = LAYER_HEIGHTS[layer] * 0.5f;
            float depth = LAYER_DEPTH_OFFSETS[layer];
            int vertexOffset = layer * VERTICES_PER_LAYER * FLOATS_PER_VERTEX;
            putVertex(vertexOffset, -halfWidth, -halfHeight, depth);
            putVertex(vertexOffset + 3, halfWidth, -halfHeight, depth);
            putVertex(vertexOffset + 6, halfWidth, halfHeight, depth);
            putVertex(vertexOffset + 9, -halfWidth, halfHeight, depth);

            putNormal(vertexOffset, 0f, 0f, 1f);
            putNormal(vertexOffset + 3, 0f, 0f, 1f);
            putNormal(vertexOffset + 6, 0f, 0f, 1f);
            putNormal(vertexOffset + 9, 0f, 0f, 1f);
        }
    }

    private void buildIndices() {
        for (int layer = 0; layer < LAYER_COUNT; layer++) {
            int vertex = layer * VERTICES_PER_LAYER;
            int index = layer * 6;
            indices[index] = vertex;
            indices[index + 1] = vertex + 1;
            indices[index + 2] = vertex + 2;
            indices[index + 3] = vertex + 2;
            indices[index + 4] = vertex + 3;
            indices[index + 5] = vertex;
        }
    }

    private void putVertex(int offset, float x, float y, float z) {
        vertices[offset] = x;
        vertices[offset + 1] = y;
        vertices[offset + 2] = z;
    }

    private void putNormal(int offset, float x, float y, float z) {
        normals[offset] = x;
        normals[offset + 1] = y;
        normals[offset + 2] = z;
    }
}
