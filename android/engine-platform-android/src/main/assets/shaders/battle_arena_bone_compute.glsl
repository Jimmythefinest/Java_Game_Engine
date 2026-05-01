#version 430 core

#define MAX_BONES_PER_CHARACTER 128

layout(local_size_x = 128) in;

layout(std430, binding = 0) readonly buffer BoneMetadataBuffer {
    int metadata[];
};

layout(std430, binding = 1) readonly buffer LocalRestPositionBuffer {
    vec4 localRestPositions[];
};

layout(std430, binding = 2) readonly buffer LocalRotationBuffer {
    vec4 localRotations[];
};

layout(std430, binding = 3) readonly buffer InverseBindMatrixBuffer {
    mat4 inverseBindMatrices[];
};

layout(std430, binding = 4) writeonly buffer OutputMatrixBuffer {
    mat4 outputMatrices[];
};

layout(std430, binding = 5) readonly buffer LocalRestScaleBuffer {
    vec4 localRestScales[];
};

shared mat4 sharedGlobalMatrices[MAX_BONES_PER_CHARACTER];

mat4 localMatrix(vec3 translation, vec4 q) {
    q = normalize(q);
    float x = q.x;
    float y = q.y;
    float z = q.z;
    float w = q.w;
    float xx = x + x;
    float yy = y + y;
    float zz = z + z;
    float x2 = x * xx;
    float y2 = y * yy;
    float z2 = z * zz;
    float xy = x * yy;
    float xz = x * zz;
    float yz = y * zz;
    float wx = w * xx;
    float wy = w * yy;
    float wz = w * zz;

    return mat4(
        vec4(1.0 - (y2 + z2), xy + wz, xz - wy, 0.0),
        vec4(xy - wz, 1.0 - (x2 + z2), yz + wx, 0.0),
        vec4(xz + wy, yz - wx, 1.0 - (x2 + y2), 0.0),
        vec4(translation, 1.0)
    );
}

mat4 scaleMatrix(vec3 scale) {
    return mat4(
        vec4(scale.x, 0.0, 0.0, 0.0),
        vec4(0.0, scale.y, 0.0, 0.0),
        vec4(0.0, 0.0, scale.z, 0.0),
        vec4(0.0, 0.0, 0.0, 1.0)
    );
}

void main() {
    int boneIndex = int(gl_LocalInvocationID.x);
    int boneCount = metadata[0];
    int maxDepth = metadata[1];
    int rotationOffset = metadata[2];
    int frameIndex = metadata[3];
    int characterOutputOffset = metadata[4];

    if (boneCount > MAX_BONES_PER_CHARACTER) {
        return;
    }

    for (int depth = 0; depth <= maxDepth; depth++) {
        barrier();
        if (boneIndex < boneCount) {
            int boneMetadataOffset = 5 + boneIndex * 2;
            int parentIndex = metadata[boneMetadataOffset];
            int boneDepth = metadata[boneMetadataOffset + 1];
            if (boneDepth == depth) {
                int rotationIndex = rotationOffset + frameIndex * boneCount + boneIndex;
                mat4 local = localMatrix(
                    localRestPositions[boneIndex].xyz,
                    localRotations[rotationIndex]);
                if (parentIndex < 0) {
                    sharedGlobalMatrices[boneIndex] = local;
                } else {
                    sharedGlobalMatrices[boneIndex] = sharedGlobalMatrices[parentIndex] * local;
                }
            }
        }
        barrier();
    }

    if (boneIndex < boneCount) {
        outputMatrices[characterOutputOffset + boneIndex] =
            sharedGlobalMatrices[boneIndex]
            * scaleMatrix(localRestScales[boneIndex].xyz)
            * inverseBindMatrices[boneIndex];
    }
}
