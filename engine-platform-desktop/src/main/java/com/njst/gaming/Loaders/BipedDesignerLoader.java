package com.njst.gaming.Loaders;

import java.util.ArrayList;
import java.util.HashMap;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_M;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_N;

import com.njst.gaming.Bone;
import com.njst.gaming.Scene;
import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Animations.BipedWalkAnimation;
import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Animations.MixamoBoneMap;
import com.njst.gaming.Animations.TerrainAwareBipedWalkAnimation;
import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Geometries.TerrainGeometry;
import com.njst.gaming.Geometries.WeightedGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.SSBO;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.Utils.GeneralUtil;
import com.njst.gaming.data;
import com.njst.gaming.objects.Bone_object;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.objects.Weighted_GameObject;

import org.lwjgl.opengl.GL15;

public class BipedDesignerLoader implements Scene.SceneLoader {
    private static final String RIG_PATH = data.rootDirectory + "/Defeated.fbx";
    private static final String BONE_NAME_DUMP_PATH = data.rootDirectory + "/mixamo_bone_names.txt";
    private static final String MODEL_TEXTURE_PATH = data.rootDirectory + "/j.jpg";
    private TerrainGeometry terrainGeometry;
    private Vector3 terrainOrigin;

    @Override
    public void load(Scene scene) {
        setupEnvironment(scene);
        if (loadRigAndExportBoneNames(scene)) {
            scene.renderer.camera.cameraPosition = new Vector3(0.0f, 1.9f, -7.0f);
            scene.renderer.camera.targetPosition = new Vector3(0.0f, 1.2f, 0.0f);
            return;
        }

        Bone root = createBone("Root", new Vector3(0.0f, 0.0f, 0.0f), new Vector3(0.12f, 0.08f, 0.12f));
        root.set_Parent_position(new Vector3(0.0f, 0.0f, 0.0f));
        root.set_Parent_rotation(new Vector3(0.0f, 0.0f, 0.0f));

        Bone hips = child(root, "Hips", new Vector3(0.0f, 1.1f, 0.0f), new Vector3(0.22f, 0.18f, 0.14f));
        Bone spine = child(hips, "Spine", new Vector3(0.0f, 0.45f, 0.0f), new Vector3(0.18f, 0.35f, 0.12f));
        Bone head = child(spine, "Head", new Vector3(0.0f, 0.45f, 0.0f), new Vector3(0.18f, 0.22f, 0.18f));

        Bone leftUpperArm = child(spine, "LeftUpperArm", new Vector3(-0.28f, 0.28f, 0.0f),
                new Vector3(0.08f, 0.28f, 0.08f));
        Bone leftLowerArm = child(leftUpperArm, "LeftLowerArm", new Vector3(0.0f, -0.38f, 0.0f),
                new Vector3(0.07f, 0.25f, 0.07f));
        Bone rightUpperArm = child(spine, "RightUpperArm", new Vector3(0.28f, 0.28f, 0.0f),
                new Vector3(0.08f, 0.28f, 0.08f));
        Bone rightLowerArm = child(rightUpperArm, "RightLowerArm", new Vector3(0.0f, -0.38f, 0.0f),
                new Vector3(0.07f, 0.25f, 0.07f));

        Bone leftUpperLeg = child(hips, "LeftUpperLeg", new Vector3(-0.14f, -0.48f, 0.0f),
                new Vector3(0.1f, 0.42f, 0.1f));
        Bone leftLowerLeg = child(leftUpperLeg, "LeftLowerLeg", new Vector3(0.0f, -0.55f, 0.0f),
                new Vector3(0.09f, 0.4f, 0.09f));
        Bone leftFoot = child(leftLowerLeg, "LeftFoot", new Vector3(0.0f, -0.46f, 0.1f),
                new Vector3(0.14f, 0.05f, 0.24f));

        Bone rightUpperLeg = child(hips, "RightUpperLeg", new Vector3(0.14f, -0.48f, 0.0f),
                new Vector3(0.1f, 0.42f, 0.1f));
        Bone rightLowerLeg = child(rightUpperLeg, "RightLowerLeg", new Vector3(0.0f, -0.55f, 0.0f),
                new Vector3(0.09f, 0.4f, 0.09f));
        Bone rightFoot = child(rightLowerLeg, "RightFoot", new Vector3(0.0f, -0.46f, 0.1f),
                new Vector3(0.14f, 0.05f, 0.24f));

        root.update();

        Bone_object skeletonView = new Bone_object(new CubeGeometry(),
                ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg"));
        skeletonView.bone = root;
        skeletonView.name = "BipedSkeleton";
        scene.addGameObject(skeletonView);

        scene.animations.add(new BipedWalkAnimation(root, hips, spine, head, leftUpperLeg, leftLowerLeg,
                rightUpperLeg, rightLowerLeg, leftUpperArm, rightUpperArm));

        scene.renderer.camera.cameraPosition = new Vector3(0.0f, 1.9f, -7.0f);
        scene.renderer.camera.targetPosition = new Vector3(0.0f, 1.2f, 0.0f);
    }

    private boolean loadRigAndExportBoneNames(Scene scene) {
        try {
            Bone rootBone = FBXBoneLoader.loadBones(RIG_PATH, new HashMap<String, KeyframeAnimation>(), 100.0f);
            rootBone.update();
            ArrayList<Bone> bones = FBXBoneLoader.get_array(rootBone);

            GeneralUtil.save_to_file(BONE_NAME_DUMP_PATH, getBoneNames(bones));
            MixamoBoneMap boneMap = MixamoBoneMap.resolve(bones, rootBone);

            for (Bone bone : bones) {
                bone.calculate_bind_matrix();
            }

            Bone_object rigView = new Bone_object(new CubeGeometry(),
                    ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg"));
            rigView.bone = rootBone;
            rigView.name = "MixamoRig";
            scene.addGameObject(rigView);

            loadSkinnedModel(scene, bones);
            attachBoneBufferUpdater(scene, bones, rootBone);

            if (boneMap != null) {
                final TerrainAwareBipedWalkAnimation walkAnimation = new TerrainAwareBipedWalkAnimation(
                        boneMap, terrainGeometry, terrainOrigin);
                scene.animations.add(walkAnimation);
                scene.actions.put(GLFW_KEY_N, new Runnable() {
                    @Override
                    public void run() {
                        walkAnimation.speedUp();
                        System.out.println("Walk speed x" + walkAnimation.getSpeedMultiplier());
                    }
                });
                scene.actions.put(GLFW_KEY_M, new Runnable() {
                    @Override
                    public void run() {
                        walkAnimation.slowDown();
                        System.out.println("Walk speed x" + walkAnimation.getSpeedMultiplier());
                    }
                });
                System.out.println("Resolved Mixamo control bones and attached procedural walk animation");
            } else {
                System.err.println("Failed to resolve required Mixamo control bones from " + RIG_PATH);
            }

            System.out.println("Loaded rig from " + RIG_PATH);
            System.out.println("Saved " + bones.size() + " bone names to " + BONE_NAME_DUMP_PATH);
            return true;
        } catch (Exception e) {
            System.err.println("Rig load failed for " + RIG_PATH + ": " + e.getMessage());
            return false;
        }
    }

    private void loadSkinnedModel(Scene scene, ArrayList<Bone> bones) {
        WeightedGeometry modelGeometry = FBXBoneLoader.loadModel(RIG_PATH, bones, 1, 1.0f);
        int modelTexture = ShaderProgram.loadTexture(MODEL_TEXTURE_PATH);
        Weighted_GameObject model = new Weighted_GameObject(modelGeometry, modelTexture);
        model.name = "MixamoModel";
        model.setPosition(0.0f, 0.0f, 0.0f);
        scene.addGameObject(model);
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

    private String getBoneNames(ArrayList<Bone> bones) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bones.size(); i++) {
            builder.append(i)
                    .append(": ")
                    .append(bones.get(i).name)
                    .append('\n');
        }
        return builder.toString();
    }

    private void setupEnvironment(Scene scene) {
        int skyboxTexture = ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg");
        int groundTexture = ShaderProgram.loadTexture(data.rootDirectory + "/WaterPlain0012_1_350.jpg");

        GameObject skybox = new GameObject(new SphereGeometry(1, 20, 20), skyboxTexture);
        skybox.scale = new float[] { 100, 100, 100 };
        skybox.ambientlight_multiplier = 5;
        skybox.shininess = 1;
        skybox.updateModelMatrix();
        scene.renderer.skybox = skybox;
        scene.addGameObject(skybox);

        terrainGeometry = new TerrainGeometry(120, 120,new float[120][120]);
        terrainOrigin = new Vector3(-60.0f, -1.2f, -60.0f);
        GameObject ground = new GameObject(terrainGeometry, groundTexture);
        ground.setPosition(terrainOrigin.x, terrainOrigin.y, terrainOrigin.z);
        ground.updateModelMatrix();
        scene.addGameObject(ground);
    }

    private Bone createBone(String name, Vector3 positionToParent, Vector3 scale) {
        Bone bone = new Bone();
        bone.name = name;
        bone.position_to_parent = positionToParent;
        bone.scale = scale;
        return bone;
    }

    private Bone child(Bone parent, String name, Vector3 positionToParent, Vector3 scale) {
        Bone bone = createBone(name, positionToParent, scale);
        bone.set_Parent_position(parent.get_globalposition());
        bone.set_Parent_rotation(parent.get_globalrotation());
        parent.Children.add(bone);
        return bone;
    }
}
