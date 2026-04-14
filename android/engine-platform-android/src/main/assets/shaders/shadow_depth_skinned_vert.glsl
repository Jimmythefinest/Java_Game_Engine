#version 450 core
layout(location = 0) in vec3 position;
layout(location = 3) in vec4 weights;
layout(location = 4) in ivec4 bones;

layout(std430, binding = 2) buffer BoneData {
    mat4 bone[];
};

uniform mat4 uMMatrix;
uniform mat4 uLightSpaceMatrix;
uniform int boneStartIndex;

void main() {
    vec4 skinnedPosition = vec4(0.0);
    for (int i = 0; i < 4; i++) {
        float weight = weights[i];
        if (weight <= 0.0) {
            continue;
        }
        mat4 boneMatrix = bone[boneStartIndex + bones[i]];
        skinnedPosition += (boneMatrix * vec4(position, 1.0)) * weight;
    }
    gl_Position = uLightSpaceMatrix * uMMatrix * skinnedPosition;
}
