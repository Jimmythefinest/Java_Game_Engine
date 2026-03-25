package com.njst.gaming.Geometries;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Utils.PerlinNoise;

public class TerrainGeometry extends Geometry {
    private static final int DEFAULT_EROSION_ITERATIONS = 16;
    private static final float DEFAULT_EROSION_STRENGTH = 0.2f;
    private static final float DEFAULT_EROSION_THRESHOLD = 0.3f;
    private static final int DEFAULT_EROSION_PADDING = 10;

    private int width;
    private int depth;
    public float[][] heightMap;

    public TerrainGeometry(int width, int depth, float[][] heightMap) {
        this.width = width;
        this.depth = depth;
        this.heightMap = heightMap;
        calculateBounds();
    }

    public TerrainGeometry(int width, int depth) {
        this.width = width;
        this.depth = depth;
        TerrainGenerator generator = new TerrainGenerator(width, depth, 20f, 10f, 0L, 0, 0,
                DEFAULT_EROSION_ITERATIONS, DEFAULT_EROSION_STRENGTH, DEFAULT_EROSION_THRESHOLD,
                DEFAULT_EROSION_PADDING);
        this.heightMap = generator.generateHeightMap();
        calculateBounds();
    }

    public static TerrainGeometry createChunk(int width, int depth, long seed, int worldStartX, int worldStartZ,
            float noiseScale, float heightScale) {
        return createChunk(width, depth, seed, worldStartX, worldStartZ, noiseScale, heightScale,
                DEFAULT_EROSION_ITERATIONS, DEFAULT_EROSION_STRENGTH, DEFAULT_EROSION_THRESHOLD,
                DEFAULT_EROSION_PADDING);
    }

    public static TerrainGeometry createChunk(int width, int depth, long seed, int worldStartX, int worldStartZ,
            float noiseScale, float heightScale, int erosionIterations, float erosionStrength,
            float erosionThreshold, int erosionPadding) {
        TerrainGenerator generator = new TerrainGenerator(width, depth, noiseScale, heightScale, seed, worldStartX,
                worldStartZ, erosionIterations, erosionStrength, erosionThreshold, erosionPadding);
        return new TerrainGeometry(width, depth, generator.generateHeightMap());
    }

    public float sampleHeight(float localX, float localZ) {
        if (heightMap == null || heightMap.length == 0 || heightMap[0].length == 0) {
            return 0f;
        }

        float clampedX = clamp(localX, 0, width - 1);
        float clampedZ = clamp(localZ, 0, depth - 1);
        int x0 = (int) java.lang.Math.floor(clampedX);
        int z0 = (int) java.lang.Math.floor(clampedZ);
        int x1 = java.lang.Math.min(x0 + 1, width - 1);
        int z1 = java.lang.Math.min(z0 + 1, depth - 1);
        float tx = clampedX - x0;
        float tz = clampedZ - z0;

        float h00 = heightMap[x0][z0];
        float h10 = heightMap[x1][z0];
        float h01 = heightMap[x0][z1];
        float h11 = heightMap[x1][z1];
        float hx0 = h00 + ((h10 - h00) * tx);
        float hx1 = h01 + ((h11 - h01) * tx);
        return hx0 + ((hx1 - hx0) * tz);
    }

    private float clamp(float value, float minValue, float maxValue) {
        return java.lang.Math.max(minValue, java.lang.Math.min(maxValue, value));
    }

    private void calculateBounds() {
        float minHeight = Float.MAX_VALUE;
        float maxHeight = -Float.MAX_VALUE;

        for (int i = 0; i < heightMap.length; i++) {
            for (int j = 0; j < heightMap[i].length; j++) {
                if (heightMap[i][j] < minHeight) {
                    minHeight = heightMap[i][j];
                }
                if (heightMap[i][j] > maxHeight) {
                    maxHeight = heightMap[i][j];
                }
            }
        }

        this.min = new Vector3(-0.5f * width, minHeight, -0.5f * depth);
        this.max = new Vector3(0.5f * width, maxHeight, 0.5f * depth);
    }

    @Override
    public float[] getVertices() {
        float[] vertices = new float[width * depth * 3];
        int index = 0;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < depth; j++) {
                vertices[index++] = i;
                vertices[index++] = heightMap[i][j];
                vertices[index++] = j;
            }
        }

        return vertices;
    }

    @Override
    public float[] getTextureCoordinates() {
        float[] textureCoords = new float[width * depth * 2];
        int index = 0;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < depth; j++) {
                textureCoords[index++] = (float) i % 2;
                textureCoords[index++] = (float) j % 2;
            }
        }

        return textureCoords;
    }

    @Override
    public float[] getNormals() {
        float[] normals = new float[width * depth * 3];
        for (int i = 0; i < normals.length; i += 3) {
            normals[i] = 0;
            normals[i + 1] = 1;
            normals[i + 2] = 0;
        }
        return normals;
    }

    @Override
    public int[] getIndices() {
        int[] indices = new int[(width - 1) * (depth - 1) * 6];
        int index = 0;

        for (int i = 0; i < width - 1; i++) {
            for (int j = 0; j < depth - 1; j++) {
                int topLeft = i * depth + j;
                int topRight = topLeft + 1;
                int bottomLeft = (i + 1) * depth + j;
                int bottomRight = bottomLeft + 1;

                indices[index++] = topRight;
                indices[index++] = bottomLeft;
                indices[index++] = topLeft;
                indices[index++] = bottomRight;
                indices[index++] = bottomLeft;
                indices[index++] = topRight;
            }
        }

        return indices;
    }

    public static class TerrainGenerator {
        private static final int[] NEIGHBOR_OFFSET_X = { -1, 0, 1, -1, 1, -1, 0, 1 };
        private static final int[] NEIGHBOR_OFFSET_Z = { -1, -1, -1, 0, 0, 1, 1, 1 };

        private final int width;
        private final int depth;
        private final PerlinNoise perlinNoise;
        private final float scale;
        private final float heightScale;
        private final int worldStartX;
        private final int worldStartZ;
        private final int erosionIterations;
        private final float erosionStrength;
        private final float erosionThreshold;
        private final int erosionPadding;

        public TerrainGenerator(int width, int depth, float scale) {
            this(width, depth, scale, 10f, 0L, 0, 0, DEFAULT_EROSION_ITERATIONS,
                    DEFAULT_EROSION_STRENGTH, DEFAULT_EROSION_THRESHOLD, DEFAULT_EROSION_PADDING);
        }

        public TerrainGenerator(int width, int depth, float scale, float heightScale, long seed, int worldStartX,
                int worldStartZ) {
            this(width, depth, scale, heightScale, seed, worldStartX, worldStartZ,
                    DEFAULT_EROSION_ITERATIONS, DEFAULT_EROSION_STRENGTH, DEFAULT_EROSION_THRESHOLD,
                    DEFAULT_EROSION_PADDING);
        }

        public TerrainGenerator(int width, int depth, float scale, float heightScale, long seed, int worldStartX,
                int worldStartZ, int erosionIterations, float erosionStrength, float erosionThreshold,
                int erosionPadding) {
            this.width = width;
            this.depth = depth;
            this.scale = scale;
            this.heightScale = heightScale;
            this.worldStartX = worldStartX;
            this.worldStartZ = worldStartZ;
            this.erosionIterations = java.lang.Math.max(0, erosionIterations);
            this.erosionStrength = clamp01(erosionStrength);
            this.erosionThreshold = java.lang.Math.max(0f, erosionThreshold);
            this.erosionPadding = java.lang.Math.max(0, erosionPadding);
            this.perlinNoise = new PerlinNoise(seed);
        }

        public float[][] generateHeightMap() {
            float[][] baseMap = generateBaseHeightMap(width, depth, worldStartX, worldStartZ);
            if (erosionIterations <= 0 || erosionStrength <= 0f || erosionPadding <= 0) {
                return baseMap;
            }

            int padding = java.lang.Math.min(erosionPadding, java.lang.Math.max(width, depth));
            float[][] paddedMap = generateBaseHeightMap(width + (padding * 2), depth + (padding * 2),
                    worldStartX - padding, worldStartZ - padding);
            float[][] erodedMap = applyThermalErosion(paddedMap);
            return cropHeightMap(erodedMap, padding, width, depth);
        }

        private float[][] generateBaseHeightMap(int mapWidth, int mapDepth, int startX, int startZ) {
            float[][] generatedMap = new float[mapWidth][mapDepth];

            for (int x = 0; x < mapWidth; x++) {
                for (int z = 0; z < mapDepth; z++) {
                    float sampleX = (startX + x) / scale;
                    float sampleZ = (startZ + z) / scale;
                    generatedMap[x][z] = (float) (perlinNoise.noise(sampleX, sampleZ) * heightScale);
                }
            }

            return generatedMap;
        }

        private float[][] applyThermalErosion(float[][] sourceMap) {
            int mapWidth = sourceMap.length;
            int mapDepth = sourceMap[0].length;
            float[][] currentMap = sourceMap;
            float[][] nextMap = new float[mapWidth][mapDepth];

            for (int iteration = 0; iteration < erosionIterations; iteration++) {
                for (int x = 0; x < mapWidth; x++) {
                    System.arraycopy(currentMap[x], 0, nextMap[x], 0, mapDepth);
                }

                for (int x = 1; x < mapWidth - 1; x++) {
                    for (int z = 1; z < mapDepth - 1; z++) {
                        float currentHeight = currentMap[x][z];
                        int lowestNeighborX = x;
                        int lowestNeighborZ = z;
                        float lowestNeighborHeight = currentHeight;

                        for (int i = 0; i < NEIGHBOR_OFFSET_X.length; i++) {
                            int neighborX = x + NEIGHBOR_OFFSET_X[i];
                            int neighborZ = z + NEIGHBOR_OFFSET_Z[i];
                            float neighborHeight = currentMap[neighborX][neighborZ];
                            if (neighborHeight < lowestNeighborHeight) {
                                lowestNeighborHeight = neighborHeight;
                                lowestNeighborX = neighborX;
                                lowestNeighborZ = neighborZ;
                            }
                        }

                        float slope = currentHeight - lowestNeighborHeight;
                        if (slope <= erosionThreshold) {
                            continue;
                        }

                        float sediment = (slope - erosionThreshold) * erosionStrength;
                        nextMap[x][z] -= sediment;
                        nextMap[lowestNeighborX][lowestNeighborZ] += sediment;
                    }
                }

                float[][] swap = currentMap;
                currentMap = nextMap;
                nextMap = swap;
            }

            return currentMap;
        }

        private float[][] cropHeightMap(float[][] map, int padding, int outputWidth, int outputDepth) {
            float[][] cropped = new float[outputWidth][outputDepth];
            for (int x = 0; x < outputWidth; x++) {
                System.arraycopy(map[x + padding], padding, cropped[x], 0, outputDepth);
            }
            return cropped;
        }

        private float clamp01(float value) {
            return java.lang.Math.max(0f, java.lang.Math.min(1f, value));
        }
    }
}
