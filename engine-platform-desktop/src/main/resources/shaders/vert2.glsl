#version 330 core

layout(location = 0) in vec3 position; // Vertex position

out vec3 fragColor; // Output color to fragment shader
void main() {
    gl_Position = vec4(position, 1.0); // Transform the vertex position
}
