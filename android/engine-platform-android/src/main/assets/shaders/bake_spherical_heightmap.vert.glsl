#version 450 core
layout(location = 0) in vec3 position;

uniform mat4 uMMatrix;
uniform mat4 uBakeView;
uniform mat4 uBakeProj;

out vec3 localPosition;

void main() {
    localPosition = position;
    gl_Position = uBakeProj * uBakeView * uMMatrix * vec4(position, 1.0);
}
