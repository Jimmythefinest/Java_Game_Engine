#version 310 es
precision highp float;
precision highp int;

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 texture_coordinate;
layout(location = 3) in vec4 weights;
layout(location = 4) in ivec4 bones;

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

layout(std430, binding = 2) buffer BoneData {
    mat4 bone[];
};

uniform mat4 uMMatrix;
uniform int boneStartIndex;

void main() {
    vec4 skinnedPosition = vec4(0.0);
    vec3 skinnedNormal = vec3(0.0);
    for (int i = 0; i < 4; i++) {
        float weight = weights[i];
        if (weight <= 0.0) {
            continue;
        }
        mat4 boneMatrix = bone[boneStartIndex + bones[i]];
        skinnedPosition += (boneMatrix * vec4(position, 1.0)) * weight;
        skinnedNormal += (mat3(boneMatrix) * normal) * weight;
    }

    vec4 worldPosition = uMMatrix * skinnedPosition;
    gl_Position = perspective * view * worldPosition;
    fragPosition = worldPosition.xyz;
    fragNormal = normalize(mat3(uMMatrix) * skinnedNormal);
    fragTexCoord = texture_coordinate;
}
