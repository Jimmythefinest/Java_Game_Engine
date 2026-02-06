package com.njst.gaming.Loaders;

import com.njst.gaming.Animations.SkeletalAnimationController;
import com.njst.gaming.Animations.Skeletal_Animation;
import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Bone;
import com.njst.gaming.Geometries.WeightedGeometry;
import com.njst.gaming.objects.Weighted_GameObject;
import com.njst.gaming.skeleton.Skeleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory class for loading skeletal models and their associated animations.
 */
public class SkeletalModelLoader {

    /**
     * Loads a skeletal model from an FBX file.
     * 
     * @param fbxPath      Path to the FBX file.
     * @param texture      Texture handle for the model.
     * @param scale        Scaling factor.
     * @param bindingPoint SSBO binding point for bone matrices.
     * @return A configured Weighted_GameObject.
     */
    public static Weighted_GameObject load(String fbxPath, int texture, float scale, int bindingPoint) {
        // 1. Load the skeleton hierarchy
        Bone rootBone = FBXBoneLoader.loadBones(fbxPath, new HashMap<>(), scale);
        Skeleton skeleton = new Skeleton(rootBone);
        
        // 2. Load the weighted geometry linked to the bone list
        WeightedGeometry geometry = FBXBoneLoader.loadModel(fbxPath, skeleton.get_Bone_List(), 1, scale);
        
        // 3. Create the GameObject
        Weighted_GameObject gameObject = new Weighted_GameObject(geometry, texture);
        
        // 4. Create and link the animation controller
        SkeletalAnimationController controller = new SkeletalAnimationController(skeleton, bindingPoint);
        gameObject.setAnimationController(controller);
        
        return gameObject;
    }

    /**
     * Loads an animation from an FBX file and maps it to the provided skeleton.
     * 
     * @param fbxPath     Path to the FBX file containing the animation.
     * @param name        Name to give the animation.
     * @param animationId Index of the animation in the FBX file.
     * @param scale       Scaling factor.
     * @param skeleton    The skeleton to map the animation to.
     * @return A Skeletal_Animation object.
     */
    public static Skeletal_Animation loadAnimation(String fbxPath, String name, int animationId, float scale, Skeleton skeleton) {
        Map<String, KeyframeAnimation> animMap = FBXAnimationLoader.extractAnimation(fbxPath, animationId, scale);
        Skeletal_Animation animation = new Skeletal_Animation(name, animMap);
        skeleton.map(animation);
        return animation;
    }
}
