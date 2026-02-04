package  com.njst.gaming;
public class data{
    public static final float[] VERTICES1 = {
        -1,  1,  1,   // Front top left
         1,  1,  1,   // Front top right
        -1, -1,  1,   // Front bottom left
         1, -1,  1,   // Front bottom right
        -1,  1, -1,   // Back top left
         1,  1, -1,   // Back top right
        -1, -1, -1,   // Back bottom left
         1, -1, -1    // Back bottom right
    };
// Vertices for a cube (36 vertices)
public static final float[] VERTICES = {
    // Front face
    
     0.5f, -0.5f,  0.5f, // Vertex 1
     0.5f,  0.5f,  0.5f, // Vertex 2
    -0.5f,  0.5f,  0.5f, // Vertex 3
    -0.5f, -0.5f,  0.5f, // Vertex 0
    // Back face
    -0.5f, -0.5f, -0.5f, // Vertex 4
    -0.5f,  0.5f, -0.5f, // Vertex 5
     0.5f,  0.5f, -0.5f, // Vertex 6
     0.5f, -0.5f, -0.5f, // Vertex 7

    // Left face
    -0.5f, -0.5f, -0.5f, // Vertex 8
    -0.5f,  0.5f, -0.5f, // Vertex 9
    -0.5f,  0.5f,  0.5f, // Vertex 10
    -0.5f, -0.5f,  0.5f, // Vertex 11

    // Right face
     0.5f, -0.5f, -0.5f, // Vertex 12
     0.5f,  0.5f, -0.5f, // Vertex 13
     0.5f,  0.5f,  0.5f, // Vertex 14
     0.5f, -0.5f,  0.5f, // Vertex 15

    // Bottom face
    -0.5f, -0.5f, -0.5f, // Vertex 16
     0.5f, -0.5f, -0.5f, // Vertex 17
     0.5f, -0.5f,  0.5f, // Vertex 18
    -0.5f, -0.5f,  0.5f, // Vertex 19

    // Top face
    -0.5f,  0.5f, -0.5f, // Vertex 20
    -0.5f,  0.5f,  0.5f, // Vertex 21
     0.5f,  0.5f,  0.5f, // Vertex 22
     0.5f,  0.5f, -0.5f  // Vertex 23
};
public static final float[] COLORS = {
        1, 0, 0, 1,   // Red
        0, 1, 0, 1,   // Green
        0, 0, 1, 1,   // Blue
        1, 1, 0, 1,   // Yellow
        1, 0, 1, 1,   // Magenta
        0, 1, 1, 1,   // Cyan
        1, 0.5f, 0, 1, // Orange
        0.5f, 0, 0.5f, 1  // Purple
    };

    public static final short[] INDICES1 = {
        0, 1, 2, 1, 3, 2,    // Front face
        4, 5, 6, 5, 7, 6,    // Back face
        0, 1, 4, 1, 5, 4,    // Top face
        2, 3, 6, 3, 7, 6,    // Bottom face
        0, 2, 4, 2, 6, 4,    // Left face
        1, 3, 5, 3, 7, 5     // Right face
    };
    // Indices for the cube (36 indices)
    public static final short[] INDICES = {
    0, 1, 2, 2, 3, 0, // Front face
    4, 5, 6, 6, 7, 4, // Back face
    8, 9, 10, 10, 11, 8, // Left face
    12, 13, 14, 14, 15, 12, // Right face
    16, 17, 18, 18, 19, 16, // Bottom face
    20, 21, 22, 22, 23, 20  // Top face
};
public static final float[] NORMALS1 = {
        // Define normals for each vertex of the cube
        // Front face
        0, 0, 1,1,
        0, 0, 1,1,
        0, 0, 1,1,
        0, 0, 1,1,
        // Back face
        0, 0, -1,1,
        0, 0, -1,1,
        0, 0, -1,1,
        0, 0, -1,1
        // Add normals for other faces as needed
    };
    
    public static final float[] TEX_COORDS1 = {
        0.0f, 0.0f, // Front top left
        1.0f, 0.0f, // Front top right
        0.0f, 1.0f, // Front bottom left
        1.0f, 1.0f, // Front bottom right
        0.0f, 0.0f, // Back top left
        1.0f, 0.0f, // Back top right
        0.0f, 1.0f, // Back bottom left
        1.0f, 1.0f  // Back bottom right
    };
    // Texture coordinates for a cube (24 texture coordinates)
    public static final float[] TEX_COORDS = {
    // Texture coordinates for front face
    0.0f, 0.0f, // Vertex 0
    1.0f, 0.0f, // Vertex 1
    1.0f, 1.0f, // Vertex 2
    0.0f, 1.0f, // Vertex 3

    // Texture coordinates for back face
    0.0f, 0.0f, // Vertex 4
    1.0f, 0.0f, // Vertex 5
    1.0f, 1.0f, // Vertex 6
    0.0f, 1.0f, // Vertex 7

    // Texture coordinates for left face
    0.0f, 0.0f, // Vertex 8
    1.0f, 0.0f, // Vertex 9
    0.0f, 1.0f, // Vertex 11
    1.0f, 1.0f, // Vertex 10

    // Texture coordinates for right face
    0.0f, 0.0f, // Vertex 12
    1.0f, 0.0f, // Vertex 13
    1.0f, 1.0f, // Vertex 14
    0.0f, 1.0f, // Vertex 15

    // Texture coordinates for bottom face
    0.0f, 0.0f, // Vertex 16
    1.0f, 0.0f, // Vertex 17
    1.0f, 1.0f, // Vertex 18
    0.0f, 1.0f, // Vertex 19

    // Texture coordinates for top face
    0.0f, 0.0f, // Vertex 20
    1.0f, 0.0f, // Vertex 21
    1.0f, 1.0f, // Vertex 22
    0.0f, 1.0f  // Vertex 23
};
    // Normals for a cube (24 normals)
    public static final float[] NORMALS = {
    // Normals for front face
    0.0f, 0.0f, 1.0f, // Normal for Vertex 0
    0.0f, 0.0f, 1.0f, // Normal for Vertex 1
    0.0f, 0.0f, 1.0f, // Normal for Vertex 2
    0.0f, 0.0f, 1.0f, // Normal for Vertex 3

    // Normals for back face
    0.0f, 0.0f, -1.0f,  // Normal for Vertex 4
    0.0f, 0.0f, -1.0f,  // Normal for Vertex 5
    0.0f, 0.0f, -1.0f,  // Normal for Vertex 6
    0.0f, 0.0f, -1.0f,  // Normal for Vertex 7

    // Normals for left face
    -1.0f, 0.0f, 0.0f, // Normal for Vertex 8
    -1.0f, 0.0f, 0.0f, // Normal for Vertex 9
    -1.0f, 0.0f, 0.0f, // Normal for Vertex 10
    -1.0f, 0.0f, 0.0f, // Normal for Vertex 11

    // Normals for right face
    1.0f, 0.0f, 0.0f,  // Normal for Vertex 12
    1.0f, 0.0f, 0.0f,  // Normal for Vertex 13
    1.0f, 0.0f, 0.0f,  // Normal for Vertex 14
    1.0f, 0.0f, 0.0f,  // Normal for Vertex 15

    // Normals for bottom face
    0.0f, -1.0f, 0.0f, // Normal for Vertex 16
    0.0f, -1.0f, 0.0f, // Normal for Vertex 17
    0.0f, -1.0f, 0.0f, // Normal for Vertex 18
    0.0f, -1.0f, 0.0f, // Normal for Vertex 19

    // Normals for top face
    0.0f, 1.0f, 0.0f,  // Normal for Vertex 20
    0.0f, 1.0f, 0.0f,  // Normal for Vertex 21
    0.0f, 1.0f, 0.0f,  // Normal for Vertex 22
    0.0f, 1.0f, 0.0f   // Normal for Vertex 23
};


}