package com.njst.gaming.ri.battlearena;

public interface BattleArenaGpuSkeletonPoseSource {
    String currentGpuAnimationKey();

    float currentGpuAnimationFrame();

    int gpuBoneBufferStartIndex();

    int gpuBoneCount();
}
