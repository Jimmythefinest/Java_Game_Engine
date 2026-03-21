package com.njst.gaming.Geometries;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Utils.PerlinNoise;

public class TerrainGeometry extends Geometry {
    private int width; // Width of the terrain
    private int depth; // Depth of the terrain
    public float[][] heightMap; // 2D array to hold height values for the terrain

    // Constructor that takes a width, depth, and a height map
    public TerrainGeometry(int width, int depth, float[][] heightMap) {
        this.width = width;
        this.depth = depth;
        this.heightMap = heightMap;
        calculateBounds();
    }
    public TerrainGeometry(int width, int depth) {
        this.width = width;
        this.depth = depth;
        TerrainGenerator g=new TerrainGenerator(width, depth, 20);
        this.heightMap = g.generateHeightMap();
        calculateBounds();
        //heightMap[1][1]=10f;
    }

    public static TerrainGeometry createChunk(int width, int depth, long seed, int worldStartX, int worldStartZ,
            float noiseScale, float heightScale) {
        TerrainGenerator generator = new TerrainGenerator(width, depth, noiseScale, heightScale, seed, worldStartX, worldStartZ);
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

    // Calculate the min and max bounds based on the heightMap
    private void calculateBounds() {
        float minHeight = Float.MAX_VALUE;
        float maxHeight = Float.MIN_VALUE;

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

        // Set the min and max vectors based on the calculated heights
        this.min = new Vector3(-0.5f * width, minHeight, -0.5f * depth);
        this.max = new Vector3(0.5f * width, maxHeight, 0.5f * depth);
    }

    @Override
    public float[] getVertices() {
        float[] vertices = new float[width * depth * 3];
        int index = 0;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < depth; j++) {
                vertices[index++] = i; // x
                vertices[index++] = heightMap[i][j]; // y (height)
                vertices[index++] = j; // z
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
                textureCoords[index++] = (float) i %2;// (width - 1); // u
                textureCoords[index++] = (float) j %2; //(depth - 1); // v
            }
        }

        return textureCoords;
    }

    @Override
    public float[] getNormals() {
        float[] normals = new float[width * depth * 3];
        // Normals calculation can be complex; here we will just set them to (0, 1, 0)
        for (int i = 0; i < normals.length; i += 3) {
            normals[i] = 0;   // x
            normals[i + 1] = 1; // y
            normals[i + 2] = 0; // z
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

                // First triangle
                indices[index++] = (int) topRight;

                indices[index++] = (int) bottomLeft;
                
                indices[index++] = (int) topLeft;
                
                // Second triangle
                indices[index++] = (int) bottomRight;
            
                indices[index++] = (int) bottomLeft;
                
                indices[index++] = (int) topRight;
                }
        }

        return indices;
    }
    
    public static class TerrainGenerator {
    private int width;
    private int depth;
    PerlinNoise perlinNoise;

    private float scale; // Scale for the noise
    private float heightScale;
    private int worldStartX;
    private int worldStartZ;

    public TerrainGenerator(int width, int depth, float scale) {
        this(width, depth, scale, 10f, 0L, 0, 0);
    }

    public TerrainGenerator(int width, int depth, float scale, float heightScale, long seed, int worldStartX, int worldStartZ) {
        this.width = width;
        this.depth = depth;
        this.scale = scale;
        this.heightScale = heightScale;
        this.worldStartX = worldStartX;
        this.worldStartZ = worldStartZ;
        perlinNoise = new PerlinNoise(seed);
    }

    public float[][] generateHeightMap() {
        float[][] heightMap = new float[width][depth];

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                // Generate height using Perlin noise
                float sampleX = (worldStartX + x) / scale;
                float sampleZ = (worldStartZ + z) / scale;
                heightMap[x][z] = (float) (perlinNoise.noise(sampleX, sampleZ) * heightScale);
            }
        }

        return heightMap;
    }
}
}
