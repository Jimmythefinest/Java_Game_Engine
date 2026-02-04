package com.njst.gaming.Utils;

import com.njst.gaming.Animations.Skeleton;
import com.njst.gaming.Natives.SSBO;
import org.lwjgl.opengl.GL15;

import java.util.*;

public class SkeletonSSBOManager {

    private final SSBO ssbo;
    private final List<Skeleton> skeletons = new ArrayList<>();
    private final List<Integer> boneOffsets = new ArrayList<>();
    private int totalBoneCount = 0;

    public SkeletonSSBOManager() {
        ssbo = new SSBO();
    }

    /**
     * Registers a skeleton and allocates space in the buffer.
     */
    public void addSkeleton(Skeleton skeleton) {
        boneOffsets.add(totalBoneCount);
        skeletons.add(skeleton);
        totalBoneCount += skeleton.getBoneCount();
    }

    /**
     * Uploads bone matrices of all skeletons into one buffer.
     */
    public void updateAll() {
        float[] fullData = new float[totalBoneCount * 16];

        for (int i = 0; i < skeletons.size(); i++) {
            Skeleton skel = skeletons.get(i);
            int offset = boneOffsets.get(i);
            List<float[]> matrices = skel.getBoneMatrices(); // New method below
            for (int j = 0; j < matrices.size(); j++) {
                System.arraycopy(matrices.get(j), 0, fullData, (offset + j) * 16, 16);
            }
        }

        ssbo.setData(fullData, GL15.GL_DYNAMIC_DRAW);
    }

    public void bindToShader(int bindingIndex) {
        ssbo.bind();
        ssbo.bindToShader(bindingIndex);
    }

    public int getOffsetForSkeleton(Skeleton skeleton) {
        int index = skeletons.indexOf(skeleton);
        return (index != -1) ? boneOffsets.get(index) : -1;
    }
}
