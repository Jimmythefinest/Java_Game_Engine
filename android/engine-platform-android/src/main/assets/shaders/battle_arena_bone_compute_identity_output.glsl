#version 430 core
// DISPATCH_LABEL: identity_output

layout(local_size_x = 1) in;

layout(std430, binding = 6) readonly buffer BoneMetadataBuffer {
    int metadata[];
};

layout(std430, binding = 2) writeonly buffer OutputMatrixBuffer {
    mat4 outputMatrices[];
};

layout(std430, binding = 12) readonly buffer InstanceStateBuffer {
    int instanceState[];
};

void main() {
    int instanceIndex = int(gl_WorkGroupID.x);
    int boneCount = metadata[0];
    int instanceStateOffset = instanceIndex * 4;
    int characterOutputOffset = instanceState[instanceStateOffset + 2];
    mat4 identity = mat4(1.0);

    for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
        outputMatrices[characterOutputOffset + boneIndex] = identity;
    }
}
