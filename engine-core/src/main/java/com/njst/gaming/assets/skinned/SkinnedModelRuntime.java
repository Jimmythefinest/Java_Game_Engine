package com.njst.gaming.assets.skinned;

import com.njst.gaming.Bone;
import com.njst.gaming.Geometries.WeightedGeometry;
import com.njst.gaming.Math.Vector3;

import java.util.ArrayList;
import java.util.List;

public class SkinnedModelRuntime {
    public final Bone rootBone;
    public final ArrayList<Bone> bones;
    public final ArrayList<WeightedGeometry> meshes;
    public final String texturePath;

    private SkinnedModelRuntime(Bone rootBone, ArrayList<Bone> bones, ArrayList<WeightedGeometry> meshes, String texturePath) {
        this.rootBone = rootBone;
        this.bones = bones;
        this.meshes = meshes;
        this.texturePath = texturePath;
    }

    public static SkinnedModelRuntime fromAsset(SkinnedModelAsset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must not be null.");
        }
        if (asset.bones == null || asset.bones.isEmpty()) {
            throw new IllegalArgumentException("Asset must contain at least one bone.");
        }

        ArrayList<Bone> runtimeBones = new ArrayList<>(asset.bones.size());
        for (SkinnedBoneAsset sourceBone : asset.bones) {
            Bone bone = new Bone();
            bone.name = sourceBone.name;
            bone.position_to_parent = toVector(sourceBone.positionToParent, new Vector3());
            bone.rotation = toVector(sourceBone.rotation, new Vector3());
            bone.scale = toVector(sourceBone.scale, new Vector3(1f, 1f, 1f));
            runtimeBones.add(bone);
        }

        Bone root = null;
        for (int i = 0; i < asset.bones.size(); i++) {
            SkinnedBoneAsset sourceBone = asset.bones.get(i);
            Bone bone = runtimeBones.get(i);
            if (sourceBone.parentIndex >= 0) {
                Bone parent = runtimeBones.get(sourceBone.parentIndex);
                parent.Children.add(bone);
                bone.set_Parent_position(parent.get_globalposition());
                bone.set_Parent_rotation(parent.get_globalrotation());
            } else {
                root = bone;
                bone.set_Parent_position(new Vector3(0f, 0f, 0f));
                bone.set_Parent_rotation(new Vector3(0f, 0f, 0f));
            }
        }

        if (root == null) {
            throw new IllegalArgumentException("Asset does not define a root bone.");
        }

        root.update();
        for (Bone bone : runtimeBones) {
            bone.calculate_bind_matrix();
        }

        ArrayList<WeightedGeometry> runtimeMeshes = new ArrayList<>();
        for (SkinnedMeshAsset mesh : asset.meshes) {
            runtimeMeshes.add(new WeightedGeometry(
                    mesh.vertices,
                    mesh.normals,
                    mesh.textureCoordinates,
                    mesh.weights,
                    mesh.indices,
                    mesh.boneIndices));
        }

        return new SkinnedModelRuntime(root, runtimeBones, runtimeMeshes, asset.texturePath);
    }

    private static Vector3 toVector(float[] values, Vector3 fallback) {
        if (values == null || values.length < 3) {
            return fallback;
        }
        return new Vector3(values[0], values[1], values[2]);
    }
}
