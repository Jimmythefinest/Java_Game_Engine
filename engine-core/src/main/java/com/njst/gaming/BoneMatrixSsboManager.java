package com.njst.gaming;

import com.njst.gaming.graphics.GraphicsDevice;

import java.util.List;

public interface BoneMatrixSsboManager {
    boolean isSupported(GraphicsDevice graphicsDevice);

    int registerSkeleton(List<Bone> bones);

    int reserveSkeleton(int boneCount);

    void upload(GraphicsDevice graphicsDevice);

    void setExternalSkeletonBufferActive(boolean externalSkeletonBufferActive);
}
