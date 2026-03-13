package com.njst.gaming.Animations;

import com.njst.gaming.Bone;

import java.util.ArrayList;
import java.util.List;

public class Skeleton {

    private Bone rootBone;
    private List<Bone> boneList;

    public Skeleton(Bone rootBone) {
        this.rootBone = rootBone;
        this.boneList = flattenBones(rootBone);
        initializeBindMatrices();
    }

    private void initializeBindMatrices() {
        for (Bone bone : boneList) {
            bone.calculate_bind_matrix();
        }
    }

    /**
     * Returns the list of bones for CPU-side access.
     */
    public List<Bone> getBoneList() {
        return boneList;
    }

    public Bone getRootBone() {
        return rootBone;
    }

    public int getBoneCount() {
        return boneList.size();
    }

    /**
     * Returns a list of bone matrices (as float[16] arrays) for uploading to the SSBO.
     */
    public List<float[]> getBoneMatrices() {
        List<float[]> result = new ArrayList<>();
        for (Bone bone : boneList) {
            result.add(bone.getAnimationMatrix().get(new float[16])); // Assumes bone.getAnimationMatrix().r is a float[16]
        }
        return result;
    }

    private List<Bone> flattenBones(Bone root) {
        List<Bone> list = new ArrayList<>();
        traverse(root, list);
        return list;
    }

    private void traverse(Bone bone, List<Bone> list) {
        if (bone == null) return;
        list.add(bone);
        for (Bone child : bone.Children) {
            traverse(child, list);
        }
    }
}
