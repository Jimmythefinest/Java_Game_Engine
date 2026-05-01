package com.njst.gaming;

import com.njst.gaming.graphics.BufferHandle;
import com.njst.gaming.graphics.GraphicsDevice;

import java.util.ArrayList;
import java.util.List;

public final class BoneSsboManager implements BoneMatrixSsboManager {
    private static final int BONE_MATRIX_BINDING = 2;

    private final ArrayList<List<Bone>> skeletons = new ArrayList<>();
    private BufferHandle skeletonBuffer;
    private boolean externalSkeletonBufferActive;
    private int reservedBoneCount;

    @Override
    public boolean isSupported(GraphicsDevice graphicsDevice) {
        return graphicsDevice != null;
    }

    @Override
    public int registerSkeleton(List<Bone> bones) {
        if (bones == null || bones.isEmpty()) {
            return 0;
        }
        int startIndex = totalBoneCount();
        skeletons.add(bones);
        return startIndex;
    }

    @Override
    public int reserveSkeleton(int boneCount) {
        if (boneCount <= 0) {
            return totalBoneCount();
        }
        int startIndex = totalBoneCount();
        reservedBoneCount += boneCount;
        return startIndex;
    }

    @Override
    public void upload(GraphicsDevice graphicsDevice) {
        if (externalSkeletonBufferActive || graphicsDevice == null || skeletons.isEmpty()) {
            return;
        }
        if (skeletonBuffer == null) {
            skeletonBuffer = graphicsDevice.createShaderStorageBuffer();
        }
        float[] boneData = createPackedBoneData();
        skeletonBuffer.setData(boneData, graphicsDevice.dynamicDrawUsage());
        skeletonBuffer.bind();
        skeletonBuffer.bindToShader(BONE_MATRIX_BINDING);
    }

    @Override
    public void setExternalSkeletonBufferActive(boolean externalSkeletonBufferActive) {
        this.externalSkeletonBufferActive = externalSkeletonBufferActive;
    }

    public boolean isExternalSkeletonBufferActive() {
        return externalSkeletonBufferActive;
    }

    public int skeletonCount() {
        return skeletons.size();
    }

    public int totalBoneCount() {
        int totalBones = reservedBoneCount;
        for (List<Bone> skeleton : skeletons) {
            totalBones += skeleton.size();
        }
        return totalBones;
    }

    private float[] createPackedBoneData() {
        float[] boneData = new float[totalBoneCount() * 16];
        int offset = 0;
        for (List<Bone> skeleton : skeletons) {
            for (Bone bone : skeleton) {
                System.arraycopy(bone.getAnimationMatrix().r, 0, boneData, offset, 16);
                offset += 16;
            }
        }
        return boneData;
    }
}
