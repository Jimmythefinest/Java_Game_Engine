package com.njst.gaming.ri.battlearena.gameobjects;

import com.njst.gaming.Geometries.Geometry;

public final class BattleArenaAtlasQuadGeometry extends Geometry {
    private static final float[] VERTICES = {
            -0.5f, -0.5f, 0f,
             0.5f, -0.5f, 0f,
             0.5f,  0.5f, 0f,
            -0.5f,  0.5f, 0f
    };
    private static final float[] NORMALS = {
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f
    };
    private static final int[] INDICES = {
            0, 1, 2,
            2, 3, 0
    };

    private final float[] textureCoordinates = new float[8];

    public BattleArenaAtlasQuadGeometry(int columns, int rows, int frame) {
        setFrame(columns, rows, frame);
    }

    public void setFrame(int columns, int rows, int frame) {
        int safeColumns = Math.max(1, columns);
        int safeRows = Math.max(1, rows);
        int frameCount = safeColumns * safeRows;
        int safeFrame = ((frame % frameCount) + frameCount) % frameCount;
        int column = safeFrame % safeColumns;
        int row = safeFrame / safeColumns;
        float u0 = column / (float) safeColumns;
        float u1 = (column + 1) / (float) safeColumns;
        float v0 = row / (float) safeRows;
        float v1 = (row + 1) / (float) safeRows;
        textureCoordinates[0] = u0;
        textureCoordinates[1] = v1;
        textureCoordinates[2] = u1;
        textureCoordinates[3] = v1;
        textureCoordinates[4] = u1;
        textureCoordinates[5] = v0;
        textureCoordinates[6] = u0;
        textureCoordinates[7] = v0;
    }

    @Override
    public float[] getVertices() {
        return VERTICES;
    }

    @Override
    public float[] getTextureCoordinates() {
        return textureCoordinates;
    }

    @Override
    public float[] getNormals() {
        return NORMALS;
    }

    @Override
    public int[] getIndices() {
        return INDICES;
    }
}
