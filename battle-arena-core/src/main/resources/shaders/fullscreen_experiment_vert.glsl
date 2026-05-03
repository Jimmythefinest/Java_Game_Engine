#version 330 core

layout(location = 0) in vec3 position;
layout(location = 2) in vec2 texture_coordinate;

out vec2 vUv;

void main() {
    vUv = texture_coordinate;
    gl_Position = vec4(position.xy, 0.0, 1.0);
}
