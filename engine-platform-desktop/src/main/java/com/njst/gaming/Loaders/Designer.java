package com.njst.gaming.Loaders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.lwjgl.opengl.GL15;

import com.njst.gaming.Bone;
import com.njst.gaming.Scene;
import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Animations.HeadLookIKAnimation;
import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Geometries.WeightedGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.SSBO;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.data;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.objects.Weighted_GameObject;
import com.njst.gaming.skeleton.Skeleton;
import com.njst.gaming.skeleton.Skeleton.Skeletal_Animation;

public class Designer implements Scene.SceneLoader {
    private static final String MODEL_PATH = data.rootDirectory + "/Defeated.fbx";
    private static final String MODEL_TEXTURE_PATH = data.rootDirectory + "/j.jpg";
    private static final String GROUND_TEXTURE_PATH = data.rootDirectory + "/WaterPlain0012_1_350.jpg";
    private static final String SKYBOX_TEXTURE_PATH = data.rootDirectory + "/desertstorm.jpg";
    private static final float MODEL_SCALE = 100.0f;

    @Override
    public void load(Scene scene) {
        try {
            setupScene(scene);

            Skeleton skeleton = new Skeleton(
                    FBXBoneLoader.loadBones(MODEL_PATH, new HashMap<String, KeyframeAnimation>(), MODEL_SCALE));
            ArrayList<Bone> bones = skeleton.get_Bone_List();

            Skeleton.Skeletal_Animation baseAnimation = loadAnimation(skeleton, 0);
            if (baseAnimation != null) {
                baseAnimation.start();
                skeleton.animations.add(baseAnimation);
             //   registerMappedAnimations(scene, baseAnimation);
            }

            skeleton.root_bone.update();
            for (Bone bone : bones) {
                bone.calculate_bind_matrix();
            }

            WeightedGeometry geometry = FBXBoneLoader.loadModel(MODEL_PATH, bones, 1, 1.0f);
            int modelTexture = ShaderProgram.loadTexture(MODEL_TEXTURE_PATH);
            Weighted_GameObject body = new Weighted_GameObject(geometry, modelTexture);
            body.name = "DesignerHumanoid";
            body.setPosition(0.0f, 0.0f, 0.0f);
            scene.addGameObject(body);

            Bone headBone = findHeadBone(bones);
            if (headBone != null) {
                scene.animations.add(new HeadLookIKAnimation(scene, skeleton.root_bone, headBone));
            }

            attachBoneBufferUpdater(scene, bones, skeleton.root_bone);
            focusCamera(scene, headBone);
        } catch (Exception e) {
            System.err.println("Failed to load Designer scene: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupScene(Scene scene) {
        int skyboxTexture = ShaderProgram.loadTexture(SKYBOX_TEXTURE_PATH);
        int groundTexture = ShaderProgram.loadTexture(GROUND_TEXTURE_PATH);

        GameObject skybox = new GameObject(new SphereGeometry(1, 20, 20), skyboxTexture);
        skybox.ambientlight_multiplier = 5;
        skybox.shininess = 1;
        skybox.scale = new float[] { 100, 100, 100 };
        skybox.updateModelMatrix();
        scene.renderer.skybox = skybox;
        scene.addGameObject(skybox);

        GameObject ground = new GameObject(new CubeGeometry(), groundTexture);
        ground.scale = new float[] { 100, 1, 100 };
        ground.move(0, -1, 0);
        ground.updateModelMatrix();
        scene.addGameObject(ground);
    }

    private Skeleton.Skeletal_Animation loadAnimation(Skeleton skeleton, int animationIndex) {
        try {
            Map<String, KeyframeAnimation> animationMap = FBXAnimationLoader.extractAnimation(MODEL_PATH, animationIndex,
                    MODEL_SCALE);
            Skeletal_Animation animation = new Skeletal_Animation();
            animation.set_Animation_map(animationMap);
            skeleton.map(animation);
            return animation;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void registerMappedAnimations(Scene scene, Skeleton.Skeletal_Animation animation) {
        animation.get_Animation_map().forEach((name, value) -> {
            if (value instanceof KeyframeAnimation && value.bone != null) {
                KeyframeAnimation keyframeAnimation = (KeyframeAnimation) value;
                keyframeAnimation.onfinish = new Runnable() {
                    @Override
                    public void run() {
                        keyframeAnimation.time = 0;
                    }
                };
                scene.KEY_ANIMATIONS.add(keyframeAnimation);
                scene.animations.add(keyframeAnimation);
            }
        });
    }

    private void attachBoneBufferUpdater(Scene scene, ArrayList<Bone> bones, Bone rootBone) {
        final SSBO boneBuffer = new SSBO();
        boneBuffer.setData(createBoneData(bones), GL15.GL_STATIC_DRAW);
        boneBuffer.bind();
        boneBuffer.bindToShader(2);

        scene.animations.add(new Animation() {
            @Override
            public void animate() {
                rootBone.update();
                boneBuffer.setData(createBoneData(bones), GL15.GL_STATIC_DRAW);
                boneBuffer.bind();
                boneBuffer.bindToShader(2);
            }
        });
    }

    private float[] createBoneData(ArrayList<Bone> bones) {
        float[] boneData = new float[bones.size() * 16];
        for (int i = 0; i < bones.size(); i++) {
            System.arraycopy(bones.get(i).getAnimationMatrix().r, 0, boneData, i * 16, 16);
        }
        return boneData;
    }

    private Bone findHeadBone(ArrayList<Bone> bones) {
        Bone bestFallback = null;
        for (Bone bone : bones) {
            String normalized = normalizeName(bone.name);
            if (normalized.contains("head")) {
                return bone;
            }
            if (bestFallback == null && normalized.contains("neck")) {
                bestFallback = bone;
            }
        }
        return bestFallback;
    }

    private String normalizeName(String boneName) {
        return boneName == null ? "" : boneName.toLowerCase(Locale.ROOT).replace(":", "").replace("_", "");
    }

    private void focusCamera(Scene scene, Bone headBone) {
        scene.renderer.camera.cameraPosition = new Vector3(0.0f, 1.8f, -6.0f);
        if (headBone != null) {
            Vector3 target = headBone.get_globalposition().clone();
            scene.renderer.camera.targetPosition = target.add(new Vector3(0.0f, 0.1f, 0.0f));
        } else {
            scene.renderer.camera.targetPosition = new Vector3(0.0f, 1.5f, 0.0f);
        }
    }
}
