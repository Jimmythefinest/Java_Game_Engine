package com.njst.gaming.assets.skinned;

import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Bone;
import com.njst.gaming.Geometries.WeightedGeometry;
import com.njst.gaming.Loaders.FBXBoneLoader;

import org.lwjgl.assimp.AIScene;

import java.util.ArrayList;
import java.util.HashMap;

import static org.lwjgl.assimp.Assimp.aiGetErrorString;
import static org.lwjgl.assimp.Assimp.aiImportFile;
import static org.lwjgl.assimp.Assimp.aiProcess_FixInfacingNormals;
import static org.lwjgl.assimp.Assimp.aiProcess_Triangulate;
import static org.lwjgl.assimp.Assimp.aiReleaseImport;

public final class FbxSkinnedModelExporter {
    private FbxSkinnedModelExporter() {
    }

    public static SkinnedModelAsset export(String fbxPath, String texturePath, float scale, int... meshIds) {
        Bone rootBone = FBXBoneLoader.loadBones(fbxPath, new HashMap<String, KeyframeAnimation>(), scale);
        rootBone.update();
        ArrayList<Bone> bones = FBXBoneLoader.get_array(rootBone);
        for (Bone bone : bones) {
            bone.calculate_bind_matrix();
        }

        int[] resolvedMeshIds = meshIds;
        if (resolvedMeshIds == null || resolvedMeshIds.length == 0) {
            resolvedMeshIds = allMeshIds(fbxPath);
        }

        SkinnedModelAsset asset = new SkinnedModelAsset();
        asset.sourceAsset = fbxPath;
        asset.texturePath = texturePath;
        for (int i = 0; i < bones.size(); i++) {
            Bone bone = bones.get(i);
            SkinnedBoneAsset boneAsset = new SkinnedBoneAsset();
            boneAsset.name = bone.name;
            boneAsset.parentIndex = findParentIndex(bones, bone);
            boneAsset.positionToParent = vectorArray(bone.position_to_parent.x, bone.position_to_parent.y, bone.position_to_parent.z);
            boneAsset.rotation = vectorArray(bone.rotation.x, bone.rotation.y, bone.rotation.z);
            boneAsset.scale = vectorArray(bone.scale.x, bone.scale.y, bone.scale.z);
            asset.bones.add(boneAsset);
        }

        for (int meshId : resolvedMeshIds) {
            WeightedGeometry geometry = FBXBoneLoader.loadModel(fbxPath, bones, meshId, scale);
            SkinnedMeshAsset meshAsset = new SkinnedMeshAsset();
            meshAsset.name = "mesh_" + meshId;
            meshAsset.vertices = geometry.getVertices();
            meshAsset.normals = geometry.getNormals();
            meshAsset.textureCoordinates = geometry.getTextureCoordinates();
            meshAsset.weights = geometry.getWeightss();
            meshAsset.indices = geometry.getIndices();
            meshAsset.boneIndices = geometry.getBoness();
            asset.meshes.add(meshAsset);
        }
        return asset;
    }

    public static void exportToFile(String fbxPath, String texturePath, String outputPath, float scale, int... meshIds) {
        SkinnedModelAsset asset = export(fbxPath, texturePath, scale, meshIds);
        SkinnedModelAssetIO.save(asset, outputPath);
    }

    private static int[] allMeshIds(String fbxPath) {
        AIScene scene = aiImportFile(fbxPath, aiProcess_Triangulate | aiProcess_FixInfacingNormals);
        if (scene == null) {
            throw new RuntimeException("Error loading FBX file: " + aiGetErrorString());
        }
        try {
            int meshCount = scene.mNumMeshes();
            int[] meshIds = new int[meshCount];
            for (int i = 0; i < meshCount; i++) {
                meshIds[i] = i;
            }
            return meshIds;
        } finally {
            aiReleaseImport(scene);
        }
    }

    private static int findParentIndex(ArrayList<Bone> bones, Bone child) {
        for (int i = 0; i < bones.size(); i++) {
            Bone candidate = bones.get(i);
            if (candidate.Children.contains(child)) {
                return i;
            }
        }
        return -1;
    }

    private static float[] vectorArray(float x, float y, float z) {
        return new float[] { x, y, z };
    }
}
