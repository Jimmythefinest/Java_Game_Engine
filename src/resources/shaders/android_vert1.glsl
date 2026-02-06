#version 310 es
uniform mat4 uVMatrix;        // View matrix
uniform mat4 uPMatrix;  // Projection matrix
uniform mat4 uMMatrix;  // Projection matrix

layout(location = 0) in vec4 position; // Vertex position
layout(location = 1) in vec3 color;    // Vertex color
layout(location = 2) in vec2 texture_coordinate;    // Vertex color
layout(location = 3) in vec3 normal;    // Vertex color

out vec3 fragColor; // Output color to fragment shader
out vec3 fragpos;
out vec3 frag_Normal;
void main() {
     gl_Position =uPMatrix*uVMatrix* position; // Transform the vertex position
     fragColor = vec3(texture_coordinate,0); // Pass the color to the fragment shader
     fragpos=vec3(uMMatrix*position);
     frag_Normal=vec3(mat3(uMMatrix)* normal);

}