#version 450 core
layout(location = 0) in vec3 position;

uniform mat4 uMMatrix;
uniform mat4 uLightSpaceMatrix;

void main() {
    gl_Position = uLightSpaceMatrix * uMMatrix * vec4(position, 1.0);
}
