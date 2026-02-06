package com.njst.gaming.Animations;

import com.njst.gaming.Bone;
import com.njst.gaming.Natives.SSBO;
import com.njst.gaming.skeleton.Skeleton;
import org.lwjgl.opengl.GL15;
import java.util.ArrayList;

/**
 * Manages the skeletal animation state and SSBO synchronization for a model.
 */
public class SkeletalAnimationController {
    private Skeleton skeleton;
    private SSBO ssbo;
    private int bindingPoint;

    public SkeletalAnimationController(Skeleton skeleton, int bindingPoint) {
        this.skeleton = skeleton;
        this.bindingPoint = bindingPoint;
        this.ssbo = new SSBO();
        updateGPU();
    }

    /**
     * Updates the skeletal state and synchronizes with the GPU.
     */
    public void update() {
        // Currently, animations are updated via their own animate() calls which usually 
        // manipulate the bones. Here we just ensure the GPU is in sync.
        updateGPU();
    }

    /**
     * Calculates the animation matrices for all bones and uploads them to the SSBO.
     */
    private void updateGPU() {
        ArrayList<Bone> bones = skeleton.get_Bone_List();
        float[] boneData = new float[bones.size() * 16];
        for (int i = 0; i < bones.size(); i++) {
            System.arraycopy(bones.get(i).getAnimationMatrix().r, 0, boneData, i * 16, 16);
        }
        ssbo.setData(boneData, GL15.GL_DYNAMIC_DRAW);
    }

    /**
     * Binds the SSBO to the configured shader binding point.
     */
    public void bind() {
        ssbo.bindToShader(bindingPoint);
    }

    public Skeleton getSkeleton() {
        return skeleton;
    }
    
    public int getBindingPoint() {
        return bindingPoint;
    }

    /**
     * Cleans up GPU resources.
     */
    public void cleanup() {
        if (ssbo != null) {
            ssbo.delete();
        }
    }
}
