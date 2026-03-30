package com.njst.gaming.assets.skinned;

import java.util.ArrayList;
import java.util.List;

public class SkinnedModelAsset {
    public String format = "njst-skinned-model";
    public int version = 1;
    public String sourceAsset;
    public String texturePath;
    public List<SkinnedBoneAsset> bones = new ArrayList<>();
    public List<SkinnedMeshAsset> meshes = new ArrayList<>();

    public SkinnedModelAsset() {
    }
}
