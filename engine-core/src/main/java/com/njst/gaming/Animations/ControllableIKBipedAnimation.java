package com.njst.gaming.Animations;

import com.njst.gaming.Bone;
import com.njst.gaming.Geometries.TerrainGeometry;
import com.njst.gaming.Math.Vector3;

public class ControllableIKBipedAnimation extends Animation {
    private final MixamoBoneMap bones;
    private final TerrainGeometry terrain;
    private final Vector3 terrainOrigin;
    private final BoneState rootBase;
    private final BoneState hipsBase;
    private final BoneState spineBase;
    private final BoneState headBase;
    private final BoneState leftUpperLegBase;
    private final BoneState leftLowerLegBase;
    private final BoneState leftFootBase;
    private final BoneState rightUpperLegBase;
    private final BoneState rightLowerLegBase;
    private final BoneState rightFootBase;
    private final BoneState leftUpperArmBase;
    private final BoneState rightUpperArmBase;
    
    private final float leftFootHeightFromRoot;
    private final float rightFootHeightFromRoot;

    private float gaitPhase;
    private float speedMultiplier = 1.0f;
    private float forwardInput = 0f;
    private float turnInput = 0f;
    
    private final Vector3 worldPosition = new Vector3();
    private float headingDegrees = 0f;

    public ControllableIKBipedAnimation(MixamoBoneMap bones, TerrainGeometry terrain, Vector3 terrainOrigin) {
        this.bones = bones;
        this.terrain = terrain;
        this.terrainOrigin = terrainOrigin.clone();
        
        this.rootBase = BoneState.capture(bones.root);
        this.hipsBase = BoneState.capture(bones.hips);
        this.spineBase = BoneState.capture(bones.chest != null ? bones.chest : bones.spine);
        this.headBase = BoneState.capture(bones.head);
        this.leftUpperLegBase = BoneState.capture(bones.leftUpperLeg);
        this.leftLowerLegBase = BoneState.capture(bones.leftLowerLeg);
        this.leftFootBase = BoneState.capture(bones.leftFoot);
        this.rightUpperLegBase = BoneState.capture(bones.rightUpperLeg);
        this.rightLowerLegBase = BoneState.capture(bones.rightLowerLeg);
        this.rightFootBase = BoneState.capture(bones.rightFoot);
        this.leftUpperArmBase = BoneState.capture(bones.leftUpperArm);
        this.rightUpperArmBase = BoneState.capture(bones.rightUpperArm);
        
        bones.root.update();
        this.leftFootHeightFromRoot = bones.leftFoot.get_globalposition().y - bones.root.get_globalposition().y;
        this.rightFootHeightFromRoot = bones.rightFoot.get_globalposition().y - bones.root.get_globalposition().y;
    }

    public void setInputs(float forward, float turn) {
        this.forwardInput = forward;
        this.turnInput = turn;
    }

    public void setWorldState(Vector3 pos, float heading) {
        this.worldPosition.set(pos);
        this.headingDegrees = heading;
    }

    @Override
    public void animate() {
        if (java.lang.Math.abs(forwardInput) > 0.01f || java.lang.Math.abs(turnInput) > 0.01f) {
            gaitPhase += 0.12f * speedMultiplier;
        } else {
            // Gradually return to idle phase?
            gaitPhase %= (float)(java.lang.Math.PI * 2.0);
        }

        float headingRadians = (float) java.lang.Math.toRadians(headingDegrees);
        float tangentX = (float) java.lang.Math.sin(headingRadians);
        float tangentZ = (float) java.lang.Math.cos(headingRadians);
        float rightX = tangentZ;
        float rightZ = -tangentX;

        // Base walking pose (FK)
        float legSwing = (float) java.lang.Math.sin(gaitPhase) * 22.0f * forwardInput;
        float armSwing = (float) java.lang.Math.sin(gaitPhase) * 16.0f * forwardInput;
        float torsoTwist = (float) java.lang.Math.sin(gaitPhase) * 4.0f * forwardInput;
        float hipBob = (float) java.lang.Math.abs(java.lang.Math.sin(gaitPhase * 2.0f)) * 0.05f * java.lang.Math.abs(forwardInput);

        bones.hips.position_to_parent.set(hipsBase.position.x, hipsBase.position.y + hipBob, hipsBase.position.z);
        bones.hips.rotation.set(hipsBase.rotation.x, hipsBase.rotation.y + (torsoTwist * 0.4f), hipsBase.rotation.z);
        (bones.chest != null ? bones.chest : bones.spine).rotation.set(
                spineBase.rotation.x - 1.5f,
                spineBase.rotation.y + torsoTwist,
                spineBase.rotation.z);
        bones.head.rotation.set(headBase.rotation.x + 1.5f, headBase.rotation.y - (torsoTwist * 0.5f),
                headBase.rotation.z);

        bones.leftUpperLeg.rotation.set(leftUpperLegBase.rotation.x + legSwing, leftUpperLegBase.rotation.y,
                leftUpperLegBase.rotation.z);
        bones.rightUpperLeg.rotation.set(rightUpperLegBase.rotation.x - legSwing, rightUpperLegBase.rotation.y,
                rightUpperLegBase.rotation.z);
        bones.leftLowerLeg.rotation.set(leftLowerLegBase.rotation.x, leftLowerLegBase.rotation.y,
                leftLowerLegBase.rotation.z);
        bones.rightLowerLeg.rotation.set(rightLowerLegBase.rotation.x, rightLowerLegBase.rotation.y,
                rightLowerLegBase.rotation.z);
        bones.leftFoot.rotation.set(leftFootBase.rotation);
        bones.rightFoot.rotation.set(rightFootBase.rotation);

        bones.leftUpperArm.rotation.set(leftUpperArmBase.rotation.x - armSwing, leftUpperArmBase.rotation.y,
                leftUpperArmBase.rotation.z);
        bones.rightUpperArm.rotation.set(rightUpperArmBase.rotation.x + armSwing, rightUpperArmBase.rotation.y,
                rightUpperArmBase.rotation.z);

        bones.root.position_to_parent.set(
                rootBase.position.x + worldPosition.x,
                rootBase.position.y + worldPosition.y,
                rootBase.position.z + worldPosition.z);
        bones.root.rotation.set(rootBase.rotation.x, rootBase.rotation.y + headingDegrees, rootBase.rotation.z);
        bones.root.update();

        // Apply IK
        float leftStride = (float) java.lang.Math.sin(gaitPhase) * 0.45f * forwardInput;
        float rightStride = (float) java.lang.Math.sin(gaitPhase + java.lang.Math.PI) * 0.45f * forwardInput;
        float leftLift = java.lang.Math.max(0.0f, (float) java.lang.Math.sin(gaitPhase)) * java.lang.Math.abs(forwardInput);
        float rightLift = java.lang.Math.max(0.0f, (float) java.lang.Math.sin(gaitPhase + java.lang.Math.PI)) * java.lang.Math.abs(forwardInput);

        applyFootIk(bones.leftUpperLeg, bones.leftLowerLeg, bones.leftFoot,
                leftUpperLegBase, leftLowerLegBase, leftFootBase,
                worldPosition.x, worldPosition.z, tangentX, tangentZ, rightX, rightZ, leftStride,
                leftUpperLegBase.position.x, leftLift > 0.35f);
        applyFootIk(bones.rightUpperLeg, bones.rightLowerLeg, bones.rightFoot,
                rightUpperLegBase, rightLowerLegBase, rightFootBase,
                worldPosition.x, worldPosition.z, tangentX, tangentZ, rightX, rightZ, rightStride,
                rightUpperLegBase.position.x, rightLift > 0.35f);

        bones.root.update();
    }

    private void applyFootIk(Bone upperLeg, Bone lowerLeg, Bone foot,
            BoneState upperBase, BoneState lowerBase, BoneState footBase,
            float rootX, float rootZ, float tangentX, float tangentZ, float rightX, float rightZ,
            float stride, float lateralOffset, boolean swingPhase) {
        float targetX = rootX + (tangentX * stride) + (rightX * lateralOffset);
        float targetZ = rootZ + (tangentZ * stride) + (rightZ * lateralOffset);
        float targetY = sampleTerrainHeight(targetX, targetZ) + (swingPhase ? 0.18f : 0.03f);

        for (int i = 0; i < 6; i++) {
            bones.root.update();
            Vector3 footPos = foot.get_globalposition();
            float errorY = targetY - footPos.y;
            if (java.lang.Math.abs(errorY) < 0.01f) {
                break;
            }

            float directionScale = swingPhase ? 18.0f : 28.0f;
            upperLeg.rotation.x += errorY * directionScale;
            lowerLeg.rotation.x += errorY * (directionScale * 1.1f);

            float horizontalError = ((targetX - footPos.x) * tangentX) + ((targetZ - footPos.z) * tangentZ);
            upperLeg.rotation.x += horizontalError * 12.0f;
            lowerLeg.rotation.x -= horizontalError * 5.0f;
        }
    }

    private float sampleTerrainHeight(float worldX, float worldZ) {
        if (terrain == null || terrain.heightMap == null) return 0f;
        float localX = worldX - terrainOrigin.x;
        float localZ = worldZ - terrainOrigin.z;
        int x0 = (int) java.lang.Math.floor(localX);
        int z0 = (int) java.lang.Math.floor(localZ);
        x0 = java.lang.Math.max(0, java.lang.Math.min(terrain.heightMap.length - 1, x0));
        z0 = java.lang.Math.max(0, java.lang.Math.min(terrain.heightMap[0].length - 1, z0));
        return terrainOrigin.y + terrain.heightMap[x0][z0];
    }

    private static class BoneState {
        private final Vector3 position;
        private final Vector3 rotation;

        private BoneState(Vector3 position, Vector3 rotation) {
            this.position = position;
            this.rotation = rotation;
        }

        private static BoneState capture(Bone bone) {
            if (bone == null) return null;
            return new BoneState(bone.position_to_parent.clone(), bone.rotation.clone());
        }
    }
}
