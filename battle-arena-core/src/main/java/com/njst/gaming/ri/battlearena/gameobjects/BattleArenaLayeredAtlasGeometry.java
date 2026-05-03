package com.njst.gaming.ri.battlearena.gameobjects;

import com.njst.gaming.Geometries.Geometry;

public final class BattleArenaLayeredAtlasGeometry extends Geometry {
    private static final int VERTICES_PER_LAYER = 4;
    private static final int FLOATS_PER_VERTEX = 3;
    private static final int UV_FLOATS_PER_LAYER = 8;
    private static final int[] FRAME_OFFSETS = {0, 3, 7, 11};
    private static final float[] LAYER_DEPTH_OFFSETS = {-0.18f, -0.06f, 0.06f, 0.18f};
    private static final float[] LAYER_WIDTHS = {1.0f, 0.92f, 1.08f, 0.84f};
    private static final float[] LAYER_HEIGHTS = {1.0f, 1.12f, 0.9f, 1.04f};

    private final int layerCount;
    private final float[] vertices;
    private final float[] normals;
    private final float[] textureCoordinates;
    private final int[] indices;

    public BattleArenaLayeredAtlasGeometry(int columns, int rows, int frame) {
        this(columns, rows, frame, 4);
    }

    public BattleArenaLayeredAtlasGeometry(int columns, int rows, int frame, int layerCount) {
        this.layerCount = Math.max(1, layerCount);
        this.vertices = new float[this.layerCount * VERTICES_PER_LAYER * FLOATS_PER_VERTEX];
        this.normals = new float[this.layerCount * VERTICES_PER_LAYER * FLOATS_PER_VERTEX];
        this.textureCoordinates = new float[this.layerCount * UV_FLOATS_PER_LAYER];
        this.indices = new int[this.layerCount * 6];
        buildVertices();
        buildIndices();
        setFrame(columns, rows, frame);
    }

    public void setFrame(int columns, int rows, int frame) {
        int safeColumns = Math.max(1, columns);
        int safeRows = Math.max(1, rows);
        int frameCount = safeColumns * safeRows;
        for (int layer = 0; layer < layerCount; layer++) {
            int safeFrame = ((frame + frameOffsetForLayer(layer)) % frameCount + frameCount) % frameCount;
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
        for (int layer = 0; layer < layerCount; layer++) {
            float halfWidth = valueForLayer(LAYER_WIDTHS, layer) * 0.5f;
            float halfHeight = valueForLayer(LAYER_HEIGHTS, layer) * 0.5f;
            float depth = depthForLayer(layer);
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
        for (int layer = 0; layer < layerCount; layer++) {
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

    private float depthForLayer(int layer) {
        if (layerCount == 1) {
            return 0f;
        }
        if (layerCount == LAYER_DEPTH_OFFSETS.length && layer < LAYER_DEPTH_OFFSETS.length) {
            return LAYER_DEPTH_OFFSETS[layer];
        }
        float t = layer / (float) (layerCount - 1);
        return -0.18f + (0.36f * t);
    }

    private int frameOffsetForLayer(int layer) {
        if (layer < FRAME_OFFSETS.length) {
            return FRAME_OFFSETS[layer];
        }
        return (layer * 3) + (layer / 2);
    }

    private float valueForLayer(float[] values, int layer) {
        return values[layer % values.length];
    }
}
