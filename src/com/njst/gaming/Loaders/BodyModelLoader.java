package com.njst.gaming.Loaders;

import com.njst.gaming.Bone;
import com.njst.gaming.Geometries.WeightedGeometry;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.objects.Weighted_GameObject;
import com.njst.gaming.skeleton.Skeleton;
import com.njst.gaming.Animations.KeyframeAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BodyModelLoader {
    
    private Skeleton skeleton;
    private Weighted_GameObject bodyModel;
    private List<Bone> bones;
    public BodyModelLoader() {
        bones = new ArrayList<>();
    }
    
    public Weighted_GameObject loadBodyModel(String modelPath, String texturePath) {
        try {
            // Load bone structure from FBX
            Map<String, KeyframeAnimation> animations = FBXAnimationLoader.extractAnimation(modelPath, 0, 100);
            Bone rootBone = FBXBoneLoader.loadBones(modelPath, animations, 1.0f);
            
            // Create skeleton
            skeleton = new Skeleton(rootBone);
            bones = skeleton.getBoneList();
            
            // Load model geometry with bone weights
            int textureId = ShaderProgram.loadTexture(texturePath);
            WeightedGeometry geometry = FBXBoneLoader.loadModel(modelPath, bones, 0, 1.0f);
            
            // Create weighted game object
            bodyModel = new Weighted_GameObject(geometry, textureId);
            bodyModel.setPosition(0.0f, 0.0f, 0.0f);
            bodyModel.updateModelMatrix();
            
            // Setup bone visualization
            setupBoneVisualization();
            
            return bodyModel;
            
        } catch (Exception e) {
            System.err.println("Error loading body model: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private void setupBoneVisualization() {
        // Create bone hierarchy visualization
        for (Bone bone : bones) {
            bone.calculate_bind_matrix();
        }
    }
    
    public Skeleton getSkeleton() {
        return skeleton;
    }
    
    public List<Bone> getBones() {
        return bones;
    }
    
    public int getBoneCount() {
        return bones.size();
    }
    
    public float[] getBoneMatrices() {
        float[] matrices = new float[bones.size() * 16];
        for (int i = 0; i < bones.size(); i++) {
            float[] matrix = bones.get(i).getAnimationMatrix().r;
            System.arraycopy(matrix, 0, matrices, i * 16, 16);
        }
        return matrices;
    }
    
    public void updateBoneMatrices() {
        for (Bone bone : bones) {
            bone.update();
        }
    }
}
