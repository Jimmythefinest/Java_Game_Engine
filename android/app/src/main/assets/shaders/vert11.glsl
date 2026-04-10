#version 310 es
precision highp float;

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 texture_coordinate;

out vec3 fragPosition;
out vec3 fragNormal;
out vec2 fragTexCoord;

layout(std430, binding = 0) buffer CameraData {
    mat4 perspective;
    mat4 view;
    vec3 eyepos;
    float pad0;
    vec3 lightpos;
    float pad1;
};

uniform mat4 uMMatrix;

void main() {
    vec4 worldPosition = uMMatrix * vec4(position, 1.0);
    gl_Position = perspective * view * worldPosition;
    fragPosition = worldPosition.xyz;
    fragNormal = normalize(mat3(uMMatrix) * normal);
    fragTexCoord = texture_coordinate;
}
