package com.njst.gaming.Loaders;

import com.njst.gaming.Scene;
import com.njst.gaming.Scene.SceneLoader;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.objects.Weighted_GameObject;
import com.njst.gaming.objects.Bone_object;
import com.njst.gaming.Bone;
import com.njst.gaming.Natives.SSBO;
import java.util.ArrayList;
import java.util.Map;
import org.lwjgl.opengl.GL15;

public class ai_training implements SceneLoader {

    @Override
    public void load(Scene scene) {
        // Create enhanced AI training environment with proper skybox, groundplane, and body model
        
        // Load textures for skybox and ground
        int skyboxTexture = ShaderProgram.loadTexture("/jimmy/desertstorm.jpg");
        int groundTexture = ShaderProgram.loadTexture("/jimmy/WaterPlain0012_1_350.jpg");
        // int bodyTexture = ShaderProgram.loadTexture("/jimmy/P.png");
        
        // Create skybox (large inverted sphere for sky dome)
        SphereGeometry skyGeometry = new SphereGeometry(1.0f, 32, 32);
        GameObject skybox = new GameObject(skyGeometry, skyboxTexture);
        skybox.setPosition(0.0f, 0.0f, 0.0f);
        skybox.setScale(100.0f, 100.0f, 100.0f);
        skybox.ambientlight_multiplier = 5.0f;
        skybox.shininess = 1.0f;
        scene.addGameObject(skybox);
        
        // Set skybox as renderer background
        scene.renderer.skybox = skybox;
        
        // Create ground plane with proper texture
        CubeGeometry groundGeometry = new CubeGeometry();
        GameObject groundPlane = new GameObject(groundGeometry, groundTexture);
        groundPlane.setPosition(0.0f, -1.0f, 0.0f);
        groundPlane.setScale(100.0f, 0.1f, 100.0f);
        groundPlane.updateModelMatrix();
        scene.addGameObject(groundPlane);
        
        // Create additional ground plane for AI training area
        GameObject trainingGround = new GameObject(new CubeGeometry(), groundTexture);
        trainingGround.setPosition(0.0f, -0.5f, 0.0f);
        trainingGround.setScale(50.0f, 0.05f, 50.0f);
        trainingGround.updateModelMatrix();
        scene.addGameObject(trainingGround);
        
        // Load body model with skeletal structure for AI training
        loadBodyModelWithBones(scene);
        
        System.out.println("Enhanced AI Training scene loaded successfully!");
        System.out.println("Scene contains: Skybox, groundplane, and body model with skeletal structure");
        System.out.println("Camera positioned for optimal AI training overview");
    }
    
    private void loadBodyModelWithBones(Scene scene) {
        try {
            // Load animations and bone structure
            Map<String, KeyframeAnimation> fbxanims = FBXAnimationLoader.extractAnimation("/jimmy/Defeated.fbx", 0, 100);
            // Map<String, KeyframeAnimation> fbxanims1 = FBXAnimationLoader.extractAnimation("/jimmy/Defeated.fbx", 1, 100);
            
            // Load bone hierarchy
            Bone rootBone = FBXBoneLoader.loadBones("/jimmy/Defeated.fbx", fbxanims, 1.0f);
            ArrayList<Bone> bones = FBXBoneLoader.get_array(rootBone);
            
            // Create skeleton for AI training
            // Skeleton skeleton = new Skeleton(rootBone);
            
            // Load body model with bone weights
            int[] fbx_textures = FBXBoneLoader.load_Textures("/jimmy/P.fbx");
            Weighted_GameObject bodyModel = new Weighted_GameObject(
                FBXBoneLoader.loadModel("/jimmy/Defeated.fbx", bones, 0, 1.0f),
                fbx_textures[0]);
            
            bodyModel.setPosition(0.0f, 0.0f, 0.0f);
            bodyModel.updateModelMatrix();
            scene.addGameObject(bodyModel);
            
            // Create bone visualization object
            Bone_object boneVisualizer = new Bone_object(
                new com.njst.gaming.Geometries.CubeGeometry(),
                ShaderProgram.loadTexture("/jimmy/desertstorm.jpg"));
            boneVisualizer.bone = rootBone;
            boneVisualizer.setPosition(2.0f, 0.0f, 0.0f);
            boneVisualizer.setScale(0.1f, 0.1f, 0.1f);
            scene.addGameObject(boneVisualizer);
            
            // Setup animations for AI training
            for (int i = 0; i < 6; i++) {
                final int animationIndex = i;
                if (scene.MOTION_ANIMATIONS.size() <= animationIndex) {
                    while (scene.MOTION_ANIMATIONS.size() <= animationIndex) {
                        scene.MOTION_ANIMATIONS.add(new ArrayList<>());
                    }
                }
                
                Map<String, KeyframeAnimation> temp_anims = FBXAnimationLoader.extractAnimation("/jimmy/Defeated.fbx", animationIndex, 100);
                mapBone(bones, temp_anims);
                
                temp_anims.forEach((na, value) -> {
                    scene.MOTION_ANIMATIONS.get(animationIndex).add(value);
                });
            }
            
            // Setup GPU bone matrices for AI training
            final SSBO bonesSSBO = new SSBO();
            float[] bone_data = new float[bones.size() * 16];
            
            for (int i = 0; i < bones.size(); i++) {
                bones.get(i).calculate_bind_matrix();
                System.arraycopy(bones.get(i).getAnimationMatrix().r, 0, bone_data, i * 16, 16);
            }
            
            bonesSSBO.setData(bone_data, GL15.GL_STATIC_DRAW);
            bonesSSBO.bind();
            bonesSSBO.bindToShader(2);
            
            // Create animation for bone updates
            Animation updateBones = new Animation() {
                @Override
                public void animate() {
                    rootBone.update();
                    
                    float[] bone_data = new float[bones.size() * 16];
                    for (int i = 0; i < bones.size(); i++) {
                        System.arraycopy(bones.get(i).getAnimationMatrix().r, 0, bone_data, i * 16, 16);
                    }
                    
                    bonesSSBO.setData(bone_data, GL15.GL_STATIC_DRAW);
                    bonesSSBO.bind();
                    bonesSSBO.bindToShader(2);
                }
            };
            scene.animations.add(updateBones);
            
            // Start default animation
            fbxanims.forEach((na, value) -> {
                if (value.bone != null) {
                    value.onfinish = new Runnable() {
                        public void run() {
                            value.time = 0;
                        }
                    };
                    value.start();
                }
            });
            
            System.out.println("Body model loaded with " + bones.size() + " bones for AI training");
            
        } catch (Exception e) {
            System.err.println("Error loading body model: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void mapBone(ArrayList<Bone> bones, Map<String, KeyframeAnimation> animation_list) {
        try {
            for (Bone bone : bones) {
                if (animation_list.containsKey(bone.name)) {
                    animation_list.get(bone.name).bone = bone;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to map bones: " + e.getMessage());
        }
    }
}
