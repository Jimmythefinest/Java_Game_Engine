package com.njst.gaming.Geometries;

public class CubeGeometry extends Geometry{ 
    public static float[] vertices = {
    -0.5f, -0.5f,  0.5f,
     0.5f, -0.5f,  0.5f,
     0.5f,  0.5f,  0.5f,
    -0.5f,  0.5f,  0.5f,

    -0.5f, -0.5f,  -0.5f,
     0.5f, -0.5f,  -0.5f,
     0.5f,  0.5f,  -0.5f,
    -0.5f,  0.5f,  -0.5f,

    0.5f, -0.5f,  -0.5f,
     0.5f, -0.5f,  0.5f,
     0.5f,  0.5f,  0.5f,
    0.5f,  0.5f,  -0.5f,

    -0.5f, -0.5f,  -0.5f,
    -0.5f, -0.5f,  0.5f,
    -0.5f,  0.5f,  0.5f,
    -0.5f,  0.5f,  -0.5f,


    -0.5f, 0.5f,  -0.5f,
    -0.5f, 0.5f,  0.5f,
    0.5f,  0.5f,  0.5f,
    0.5f,  0.5f,  -0.5f,



    -0.5f, -0.5f,  -0.5f,
    -0.5f, -0.5f,  0.5f,
    0.5f,  -0.5f,  0.5f,
    0.5f,  -0.5f,  -0.5f,
};

float[] colors = {
    1.0f, 0.0f, 0.0f, // Red
    0.0f, 1.0f, 0.0f, // Green
    0.0f, 0.0f, 1.0f, // Blue
    1.0f, 1.0f, 0.0f, // Yellow
    1.0f, 0.0f, 1.0f, // Magenta
    0.0f, 1.0f, 1.0f, // Cyan
    1.0f, 0.5f, 0.0f, // Orange
    0.5f, 0.0f, 0.5f, // Purple
    1.0f, 0.0f, 0.0f, // Red
    0.0f, 1.0f, 0.0f, // Green
    0.0f, 0.0f, 1.0f, // Blue
    1.0f, 1.0f, 0.0f, // Yellow
    1.0f, 0.0f, 1.0f, // Magenta
    0.0f, 1.0f, 1.0f, // Cyan
    1.0f, 0.5f, 0.0f, // Orange
    0.5f, 0.0f, 0.5f, // Purple
    1.0f, 0.0f, 0.0f, // Red
    0.0f, 1.0f, 0.0f, // Green
    0.0f, 0.0f, 1.0f, // Blue
    1.0f, 1.0f, 0.0f, // Yellow
    1.0f, 0.0f, 1.0f, // Magenta
    0.0f, 1.0f, 1.0f, // Cyan
    1.0f, 0.5f, 0.0f, // Orange
    0.5f, 0.0f, 0.5f  // Purple
};
public static float[] normals = {
    // Front face
    -1.0f, 0.0f, 1.0f, // Normal for vertex 0
    1.0f, 0.0f, 1.0f, // Normal for vertex 1
    1.0f, 0.0f, 1.0f, // Normal for vertex 2
    -1.0f, 0.0f, 1.0f, // Normal for vertex 3

    // Back face
    -1.0f, 0.0f, -1.0f, // Normal for vertex 4
    1.0f, 0.0f, -1.0f, // Normal for vertex 5
    1.0f, 0.0f, -1.0f, // Normal for vertex 6
    -1.0f, 0.0f, -1.0f, // Normal for vertex 7

    // Right face
    1.0f, 0.0f, 0.0f, // Normal for vertex 8
    1.0f, 0.0f, 0.0f, // Normal for vertex 9
    1.0f, 0.0f, 0.0f, // Normal for vertex 10
    1.0f, 0.0f, 0.0f, // Normal for vertex 11

    // Left face
    -1.0f, 0.0f, 0.0f, // Normal for vertex 12
    -1.0f, 0.0f, 0.0f, // Normal for vertex 13
    -1.0f, 0.0f, 0.0f, // Normal for vertex 14
    -1.0f, 0.0f, 0.0f, // Normal for vertex 15

    // Top face
    0.0f, 1.0f, 0.0f, // Normal for vertex 16
    0.0f, 1.0f, 0.0f, // Normal for vertex 17
    0.0f, 1.0f, 0.0f, // Normal for vertex 18
    0.0f, 1.0f, 0.0f, // Normal for vertex 19

    // Bottom face
    0.0f, -1.0f, 0.0f, // Normal for vertex 20
    0.0f, -1.0f, 0.0f, // Normal for vertex 21
    0.0f, -1.0f, 0.0f, // Normal for vertex 22
    0.0f, -1.0f, 0.0f  // Normal for vertex 23
};

public static final int[] indices = {
    
    0, 1, 2, // Front face
    2, 3, 0,

    4, 5, 6, // Back face
    6, 7, 4,
    
    8, 9, 10, // Right face
    10, 11, 8,

    12,13,14,//;eft face
    14,15,12,

    16,17,18,
    18,19,16,

    20,21,22,
    22,23,20
    
    // 1, 2, 6, // Right face
    // 6, 5, 1,
    
    // 2, 3, 7, // Bottom face
    // 7, 6, 2,
    
    // 3, 0, 4, // Left face
    // 4, 7, 3,
    
    // 8, 9, 10, // Front face
    // 10, 11, 8,
    
    // 12, 13, 14, // Back face
    // 14, 15, 12,
    
    // 8, 9, 13, // Top face
    // 13, 12, 8,
    
    // 9, 10, 14, // Right face
    // 14, 13, 9,
    
    // 10, 11, 15, // Bottom face
    // 15, 14, 10,
    
    // 11, 8, 12, // Left face
    // 12, 15, 11,
    
    // 16, 17, 18, // Front face
    // 18, 19, 16,
    
    // 20, 21, 22, // Back face
    // 22, 23, 20,
    
    // 16, 17, 21, // Top face
    // 21, 20, 16,
    
    // 17, 18, 22, // Right face
    // 22, 21, 17,
    
    // 18, 19, 23, // Bottom face
    // 23, 22, 18,
    
    // 19, 16, 20, // Left face
    // 20, 23, 19
};
public float[] texture_coordinates={
    0,0,
    1,0,
    1,1,
    0,1,

    0,0,
    1,0,
    1,1,
    0,1,

    0,0,
    1,0,
    1,1,
    0,1,

    0,0,
    1,0,
    1,1,
    0,1,

    0,0,
    1,0,
    1,1,
    0,1,

    0,0,
    1,0,
    1,1,
    0,1
};
    @Override
    public float[] getVertices() {
       return vertices;
     }
    @Override
    public float[] getTextureCoordinates() {
       return texture_coordinates;

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
