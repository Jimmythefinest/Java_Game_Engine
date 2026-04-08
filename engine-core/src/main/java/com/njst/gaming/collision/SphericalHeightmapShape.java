package com.njst.gaming.collision;

import com.njst.gaming.Math.Vector3;

public class SphericalHeightmapShape implements CollisionShape {
    public static final String TYPE_ID = "spherical_heightmap";

    private final float[][] heightSamples;
    private final float baseRadius;
    private final Vector3 localCenter;
    private final float minHeight;
    private final float maxHeight;

    public SphericalHeightmapShape(float[][] heightSamples, float baseRadius) {
        this(heightSamples, baseRadius, new Vector3());
    }

    public SphericalHeightmapShape(float[][] heightSamples, float baseRadius, Vector3 localCenter) {
        if (heightSamples == null || heightSamples.length == 0 || heightSamples[0].length == 0) {
            throw new IllegalArgumentException("heightSamples must not be empty.");
        }
        this.heightSamples = copy(heightSamples);
        this.baseRadius = baseRadius;
        this.localCenter = new Vector3(localCenter);

        float localMinHeight = Float.POSITIVE_INFINITY;
        float localMaxHeight = Float.NEGATIVE_INFINITY;
        for (int row = 0; row < heightSamples.length; row++) {
            for (int col = 0; col < heightSamples[row].length; col++) {
                float sample = heightSamples[row][col];
                if (sample < localMinHeight) {
                    localMinHeight = sample;
                }
                if (sample > localMaxHeight) {
                    localMaxHeight = sample;
                }
            }
        }
        minHeight = localMinHeight;
        maxHeight = localMaxHeight;
    }

    @Override
    public String getTypeId() {
        return TYPE_ID;
    }

    public Vector3 getLocalCenter() {
        return new Vector3(localCenter);
    }

    public float getBaseRadius() {
        return baseRadius;
    }

    public int getSampleHeight() {
        return heightSamples.length;
    }

    public int getSampleWidth() {
        return heightSamples[0].length;
    }

    public float[][] getHeightSamplesCopy() {
        return copy(heightSamples);
    }

    public float getMinRadius() {
        return baseRadius + minHeight;
    }

    public float getMaxRadius() {
        return baseRadius + maxHeight;
    }

    public float sampleHeight(Vector3 localDirection) {
        Vector3 direction = normalizedCopy(localDirection);
        float u = (float) (Math.atan2(direction.z, direction.x) / (Math.PI * 2.0) + 0.5);
        float v = (float) (Math.acos(clamp(direction.y, -1f, 1f)) / Math.PI);

        int rowCount = heightSamples.length;
        int columnCount = heightSamples[0].length;

        float sampleX = wrap01(u) * columnCount;
        float sampleY = clamp(v, 0f, 1f) * (rowCount - 1);

        int x0 = floorIndex(sampleX, columnCount);
        int x1 = (x0 + 1) % columnCount;
        int y0 = clampIndex((int) Math.floor(sampleY), rowCount - 1);
        int y1 = clampIndex(y0 + 1, rowCount - 1);

        float tx = sampleX - (float) Math.floor(sampleX);
        float ty = sampleY - y0;

        float h00 = heightSamples[y0][x0];
        float h10 = heightSamples[y0][x1];
        float h01 = heightSamples[y1][x0];
        float h11 = heightSamples[y1][x1];

        float top = h00 + ((h10 - h00) * tx);
        float bottom = h01 + ((h11 - h01) * tx);
        return top + ((bottom - top) * ty);
    }

    public float sampleRadius(Vector3 localDirection) {
        return baseRadius + sampleHeight(localDirection);
    }

    private float[][] copy(float[][] source) {
        float[][] result = new float[source.length][];
        for (int i = 0; i < source.length; i++) {
            result[i] = new float[source[i].length];
            System.arraycopy(source[i], 0, result[i], 0, source[i].length);
        }
        return result;
    }

    private Vector3 normalizedCopy(Vector3 vector) {
        Vector3 copy = new Vector3(vector);
        float length = copy.length();
        if (length <= 0.000001f) {
            return new Vector3(1f, 0f, 0f);
        }
        return copy.mul(1f / length);
    }

    private int floorIndex(float value, int wrap) {
        int index = (int) Math.floor(value);
        int mod = index % wrap;
        return mod < 0 ? mod + wrap : mod;
    }

    private int clampIndex(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    private float wrap01(float value) {
        float wrapped = value - (float) Math.floor(value);
        return wrapped < 0f ? wrapped + 1f : wrapped;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
