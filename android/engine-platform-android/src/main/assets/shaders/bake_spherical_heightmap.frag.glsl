#version 450 core

in vec3 localPosition;

uniform vec3 uLocalBakeCenter;

layout(location = 0) out float outRadius;

void main() {
    outRadius = length(localPosition - uLocalBakeCenter);
}
