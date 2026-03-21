package com.njst.gaming.Animations;

import com.njst.gaming.Bone;
import com.njst.gaming.Geometries.TerrainGeometry;
import com.njst.gaming.Math.Vector3;

public class TerrainAwareBipedWalkAnimation extends Animation {
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
    private final float circleRadius;
    private final float travelSpeed;
    private final float gaitSpeed;
    private float speedMultiplier = 1.0f;
    private float travelAngle;
    private float gaitPhase;

    public TerrainAwareBipedWalkAnimation(MixamoBoneMap bones, TerrainGeometry terrain, Vector3 terrainOrigin) {
        this(bones, terrain, terrainOrigin, 18.0f, 0.006f, 0.075f);
    }

    public TerrainAwareBipedWalkAnimation(MixamoBoneMap bones, TerrainGeometry terrain, Vector3 terrainOrigin,
            float circleRadius, float travelSpeed, float gaitSpeed) {
        this.bones = bones;
        this.terrain = terrain;
        this.terrainOrigin = terrainOrigin.clone();
        this.circleRadius = circleRadius;
        this.travelSpeed = travelSpeed;
        this.gaitSpeed = gaitSpeed;
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

    @Override
    public void animate() {
        float currentTravelSpeed = travelSpeed * speedMultiplier;
        float currentGaitSpeed = gaitSpeed * speedMultiplier;
        travelAngle += currentTravelSpeed;
        gaitPhase += currentGaitSpeed;

        float tangentX = (float) -java.lang.Math.sin(travelAngle);
        float tangentZ = (float) java.lang.Math.cos(travelAngle);
        float rightX = tangentZ;
        float rightZ = -tangentX;
        float heading = (float) java.lang.Math.toDegrees(java.lang.Math.atan2(tangentX, tangentZ));

        float rootX = (float) java.lang.Math.cos(travelAngle) * circleRadius;
        float rootZ = (float) java.lang.Math.sin(travelAngle) * circleRadius;
        float leftSupportX = rootX + (rightX * leftUpperLegBase.position.x);
        float leftSupportZ = rootZ + (rightZ * leftUpperLegBase.position.x);
        float rightSupportX = rootX + (rightX * rightUpperLegBase.position.x);
        float rightSupportZ = rootZ + (rightZ * rightUpperLegBase.position.x);

        float centerHeight = sampleTerrainHeight(rootX, rootZ);
        float leftHeight = sampleTerrainHeight(leftSupportX, leftSupportZ) - leftFootHeightFromRoot;
        float rightHeight = sampleTerrainHeight(rightSupportX, rightSupportZ) - rightFootHeightFromRoot;
        float rootY =0;// java.lang.Math.max(centerHeight, java.lang.Math.max(leftHeight, rightHeight)) + 0.06f;

        bones.root.position_to_parent.set(rootBase.position.x + rootX, rootBase.position.y + rootY,
                rootBase.position.z + rootZ);
        bones.root.rotation.set(rootBase.rotation.x, rootBase.rotation.y + heading, rootBase.rotation.z);
		  bones.root.update();
        float legSwing = (float) java.lang.Math.sin(gaitPhase) * 22.0f;
        float armSwing = (float) java.lang.Math.sin(gaitPhase) * 16.0f;
        float torsoTwist = (float) java.lang.Math.sin(gaitPhase) * 4.0f;
        float hipBob = (float) java.lang.Math.abs(java.lang.Math.sin(gaitPhase * 2.0f)) * 0.05f;

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

        bones.root.update();

        float leftStride = (float) java.lang.Math.sin(gaitPhase) * 0.45f;
        float rightStride = (float) java.lang.Math.sin(gaitPhase + java.lang.Math.PI) * 0.45f;
        float leftLift = java.lang.Math.max(0.0f, (float) java.lang.Math.sin(gaitPhase));
        float rightLift = java.lang.Math.max(0.0f, (float) java.lang.Math.sin(gaitPhase + java.lang.Math.PI));

        applyFootIk(bones.leftUpperLeg, bones.leftLowerLeg, bones.leftFoot,
                leftUpperLegBase, leftLowerLegBase, leftFootBase,
                rootX, rootZ, tangentX, tangentZ, rightX, rightZ, leftStride,
                leftUpperLegBase.position.x, leftLift > 0.35f);
        applyFootIk(bones.rightUpperLeg, bones.rightLowerLeg, bones.rightFoot,
                rightUpperLegBase, rightLowerLegBase, rightFootBase,
                rootX, rootZ, tangentX, tangentZ, rightX, rightZ, rightStride,
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

        upperLeg.rotation.set(upperBase.rotation);
        lowerLeg.rotation.set(lowerBase.rotation);
        foot.rotation.set(footBase.rotation);

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

        bones.root.update();
        Vector3 groundedFoot = foot.get_globalposition();
        float terrainY = sampleTerrainHeight(groundedFoot.x, groundedFoot.z);
        if (groundedFoot.y < terrainY + 0.01f) {
            float penetration = (terrainY + 0.01f) - groundedFoot.y;
            bones.hips.position_to_parent.y += penetration;
        }
    }

    public void speedUp() {
        speedMultiplier = java.lang.Math.min(3.0f, speedMultiplier * 1.2f);
    }

    public void slowDown() {
        speedMultiplier = java.lang.Math.max(0.2f, speedMultiplier / 1.2f);
    }

    public float getSpeedMultiplier() {
        return speedMultiplier;
    }

    private float sampleTerrainHeight(float worldX, float worldZ) {
        float localX = worldX - terrainOrigin.x;
        float localZ = worldZ - terrainOrigin.z;
        int x0 = clamp((int) java.lang.Math.floor(localX), 0, terrain.heightMap.length - 1);
        int z0 = clamp((int) java.lang.Math.floor(localZ), 0, terrain.heightMap[0].length - 1);
        int x1 = clamp(x0 + 1, 0, terrain.heightMap.length - 1);
        int z1 = clamp(z0 + 1, 0, terrain.heightMap[0].length - 1);

        float tx = clamp01(localX - x0);
        float tz = clamp01(localZ - z0);

        float h00 = terrain.heightMap[x0][z0];
        float h10 = terrain.heightMap[x1][z0];
        float h01 = terrain.heightMap[x0][z1];
        float h11 = terrain.heightMap[x1][z1];

        float hx0 = lerp(h00, h10, tx);
        float hx1 = lerp(h01, h11, tx);
        return terrainOrigin.y + lerp(hx0, hx1, tz);
    }

    private static float lerp(float a, float b, float t) {
        return a + ((b - a) * t);
    }

    private static int clamp(int value, int min, int max) {
        return java.lang.Math.max(min, java.lang.Math.min(max, value));
    }

    private static float clamp01(float value) {
        return java.lang.Math.max(0.0f, java.lang.Math.min(1.0f, value));
    }

    private static class BoneState {
        private final Vector3 position;
        private final Vector3 rotation;

        private BoneState(Vector3 position, Vector3 rotation) {
            this.position = position;
            this.rotation = rotation;
        }

        private static BoneState capture(Bone bone) {
            return new BoneState(bone.position_to_parent.clone(), bone.rotation.clone());
        }
    }
}
