package com.njst.gaming.Animations;

import com.njst.gaming.Geometries.TerrainGeometry;
import com.njst.gaming.Math.Vector3;

public class ControllableBipedAnimation extends Animation {
    private final MixamoBoneMap bones;
    private final TerrainGeometry terrain;
    private final Vector3 terrainOrigin;
    private final BoneState rootBase;
    private final BoneState hipsBase;
    private final BoneState spineBase;
    private final BoneState headBase;
    private final BoneState leftUpperLegBase;
    private final BoneState leftLowerLegBase;
    private final BoneState rightUpperLegBase;
    private final BoneState rightLowerLegBase;
    private final BoneState leftUpperArmBase;
    private final BoneState leftLowerArmBase;
    private final BoneState rightUpperArmBase;
    private final BoneState rightLowerArmBase;
    private final Vector3 worldPosition = new Vector3();
    private float headingDegrees;
    private float forwardInput;
    private float turnInput;
    private float speedMultiplier = 1.0f;
    private float gaitPhase;
    private float verticalVelocity;
    private float currentSlopePitchDegrees;
    private float currentGroundDrop;
    private boolean jumpRequested;
    private boolean grounded = true;
    private static final float GRAVITY = 0.035f;
    private static final float JUMP_VELOCITY = 0.45f;

    public ControllableBipedAnimation(MixamoBoneMap bones, TerrainGeometry terrain, Vector3 terrainOrigin) {
        this.bones = bones;
        this.terrain = terrain;
        this.terrainOrigin = terrainOrigin.clone();
        this.rootBase = BoneState.capture(bones.root);
        this.hipsBase = BoneState.capture(bones.hips);
        this.spineBase = BoneState.capture(bones.chest != null ? bones.chest : bones.spine);
        this.headBase = BoneState.capture(bones.head);
        this.leftUpperLegBase = BoneState.capture(bones.leftUpperLeg);
        this.leftLowerLegBase = BoneState.capture(bones.leftLowerLeg);
        this.rightUpperLegBase = BoneState.capture(bones.rightUpperLeg);
        this.rightLowerLegBase = BoneState.capture(bones.rightLowerLeg);
        this.leftUpperArmBase = BoneState.capture(bones.leftUpperArm);
        this.leftLowerArmBase = BoneState.capture(bones.leftLowerArm);
        this.rightUpperArmBase = BoneState.capture(bones.rightUpperArm);
        this.rightLowerArmBase = BoneState.capture(bones.rightLowerArm);
        this.headingDegrees = rootBase.rotation.y;
        this.worldPosition.y = sampleTerrainHeight(0.0f, 0.0f);
        bones.root.update();
    }

    public void setMovementInput(float forward, float turn) {
        this.forwardInput = clamp(forward, -1.0f, 1.0f);
        this.turnInput = clamp(turn, -1.0f, 1.0f);
    }

    public void setSpeedMultiplier(float speedMultiplier) {
        this.speedMultiplier = clamp(speedMultiplier, 0.5f, 2.5f);
    }

    public void requestJump() {
        this.jumpRequested = true;
    }

    public Vector3 getFocusPosition() {
        return worldPosition.clone().add(new Vector3(0.0f, 1.6f, 0.0f));
    }

    public Vector3 getWorldPosition() {
        return worldPosition.clone();
    }

    public float getHeadingDegrees() {
        return headingDegrees;
    }

    @Override
    public void animate() {
        headingDegrees += turnInput * 3.2f;

        float moveAmount = forwardInput * 0.14f * speedMultiplier;
        if (java.lang.Math.abs(moveAmount) > 0.0001f) {
            float headingRadians = (float) java.lang.Math.toRadians(headingDegrees);
            worldPosition.x += (float) java.lang.Math.sin(headingRadians) * moveAmount;
            worldPosition.z += (float) java.lang.Math.cos(headingRadians) * moveAmount;
            gaitPhase += 0.11f;
        }

        float groundHeight = sampleTerrainHeight(worldPosition.x, worldPosition.z);
        currentSlopePitchDegrees = sampleSlopePitchDegrees();
        if (jumpRequested && grounded) {
            verticalVelocity = JUMP_VELOCITY;
            grounded = false;
        }
        jumpRequested = false;

        currentGroundDrop = 0.0f;
        if (grounded && verticalVelocity == 0.0f) {
            float groundDelta = groundHeight - worldPosition.y;
            if (groundDelta >= 0.0f) {
                worldPosition.y = groundHeight;
            } else {
                currentGroundDrop = -groundDelta;
                worldPosition.y += java.lang.Math.max(groundDelta, -0.08f);
            }
        } else if (!grounded || verticalVelocity > 0.0f) {
            worldPosition.y += verticalVelocity;
            verticalVelocity -= GRAVITY;
        }

        if (worldPosition.y <= groundHeight) {
            worldPosition.y = groundHeight;
            verticalVelocity = 0.0f;
            grounded = true;
        } else {
            grounded = false;
        }
        bones.root.position_to_parent.set(
                rootBase.position.x,
                rootBase.position.y,
                rootBase.position.z);
        bones.root.rotation.set(rootBase.rotation);

        if (!grounded) {
            applyJumpPose();
        } else if (java.lang.Math.abs(forwardInput) > 0.05f) {
            applyWalkPose();
        } else {
            applyIdlePose();
        }

        bones.root.update();
    }

    private void applyWalkPose() {
        float direction = forwardInput >= 0.0f ? 1.0f : -1.0f;
        float legSwing = (float) java.lang.Math.sin(gaitPhase) * 24.0f * direction;
        float kneeLift = java.lang.Math.max(0.0f, (float) java.lang.Math.sin(gaitPhase)) * 18.0f;
        float oppositeKneeLift = java.lang.Math.max(0.0f, (float) java.lang.Math.sin(gaitPhase + java.lang.Math.PI))
                * 18.0f;
        float armSwing = (float) java.lang.Math.sin(gaitPhase) * 16.0f * direction;
        float elbowBend = 18.0f + (float) java.lang.Math.abs(java.lang.Math.sin(gaitPhase + (java.lang.Math.PI / 4.0))) * 10.0f;
        float oppositeElbowBend = 18.0f
                + (float) java.lang.Math.abs(java.lang.Math.sin(gaitPhase + java.lang.Math.PI + (java.lang.Math.PI / 4.0))) * 10.0f;
        float hipBob = (float) java.lang.Math.abs(java.lang.Math.sin(gaitPhase * 2.0f)) * 0.05f;
        float torsoTwist = (float) java.lang.Math.sin(gaitPhase) * 4.5f * direction;
        float slopeCompensation = clamp(-currentSlopePitchDegrees * 1.4f, -10.0f, 10.0f);
        float downhillStep = clamp(currentGroundDrop * 180.0f, 0.0f, 12.0f);
        float downhillBend = java.lang.Math.max(0.0f, slopeCompensation) * 0.45f;

        bones.hips.position_to_parent.set(hipsBase.position.x, hipsBase.position.y + hipBob - (downhillStep * 0.0025f), hipsBase.position.z);
        bones.hips.rotation.set(hipsBase.rotation.x + (slopeCompensation * 0.25f) + (downhillStep * 0.15f), hipsBase.rotation.y + (torsoTwist * 0.35f),
                hipsBase.rotation.z);
        (bones.chest != null ? bones.chest : bones.spine).rotation.set(
                spineBase.rotation.x - 2.0f + slopeCompensation + (downhillStep * 0.35f),
                spineBase.rotation.y + torsoTwist,
                spineBase.rotation.z);
        bones.head.rotation.set(headBase.rotation.x + 2.0f + (slopeCompensation * 0.2f) - (downhillStep * 0.1f), headBase.rotation.y - (torsoTwist * 0.3f),
                headBase.rotation.z);

        bones.leftUpperLeg.rotation.set(leftUpperLegBase.rotation.x + legSwing - (slopeCompensation * 0.35f) - (downhillStep * 0.4f), leftUpperLegBase.rotation.y,
                leftUpperLegBase.rotation.z);
        bones.rightUpperLeg.rotation.set(rightUpperLegBase.rotation.x - legSwing - (slopeCompensation * 0.35f) - (downhillStep * 0.4f), rightUpperLegBase.rotation.y,
                rightUpperLegBase.rotation.z);
        bones.leftLowerLeg.rotation.set(leftLowerLegBase.rotation.x + kneeLift + downhillBend + downhillStep, leftLowerLegBase.rotation.y,
                leftLowerLegBase.rotation.z);
        bones.rightLowerLeg.rotation.set(rightLowerLegBase.rotation.x + oppositeKneeLift + downhillBend + downhillStep, rightLowerLegBase.rotation.y,
                rightLowerLegBase.rotation.z);

        applyArmPose(-armSwing, armSwing, elbowBend, oppositeElbowBend);
    }

    private void applyIdlePose() {
        float idlePhase = gaitPhase * 0.25f;
        float breathe = (float) java.lang.Math.sin(idlePhase) * 0.03f;
        float sway = (float) java.lang.Math.sin(idlePhase * 0.85f) * 2.5f;
        float torsoNod = (float) java.lang.Math.sin(idlePhase * 0.5f) * 1.4f;
        float headTilt = (float) java.lang.Math.sin(idlePhase * 0.7f) * 1.8f;
        float armSway = (float) java.lang.Math.sin(idlePhase * 0.9f) * 3.0f;
        float elbowBend = 10.0f + ((float) java.lang.Math.sin(idlePhase + 0.6f) * 2.5f);
        float hipShift = (float) java.lang.Math.sin(idlePhase * 0.6f) * 0.015f;
        float kneeRelax = 4.0f + ((float) java.lang.Math.sin(idlePhase * 0.8f) * 1.5f);

        bones.hips.position_to_parent.set(hipsBase.position.x + hipShift, hipsBase.position.y + breathe,
                hipsBase.position.z);
        bones.hips.rotation.set(hipsBase.rotation.x, hipsBase.rotation.y + (sway * 0.2f), hipsBase.rotation.z);
        (bones.chest != null ? bones.chest : bones.spine).rotation.set(
                spineBase.rotation.x - 1.0f + torsoNod,
                spineBase.rotation.y + sway,
                spineBase.rotation.z);
        bones.head.rotation.set(headBase.rotation.x + 1.0f - (torsoNod * 0.35f), headBase.rotation.y - sway,
                headBase.rotation.z + headTilt);

        bones.leftUpperLeg.rotation.set(leftUpperLegBase.rotation);
        bones.rightUpperLeg.rotation.set(rightUpperLegBase.rotation);
        bones.leftLowerLeg.rotation.set(leftLowerLegBase.rotation.x + kneeRelax, leftLowerLegBase.rotation.y,
                leftLowerLegBase.rotation.z);
        bones.rightLowerLeg.rotation.set(rightLowerLegBase.rotation.x + kneeRelax, rightLowerLegBase.rotation.y,
                rightLowerLegBase.rotation.z);
        applyArmPose(armSway, -armSway, elbowBend, elbowBend);
    }

    private void applyJumpPose() {
        float airborneLean = java.lang.Math.max(-12.0f, java.lang.Math.min(12.0f, verticalVelocity * 30.0f));
        float kneeTuck = verticalVelocity > 0.0f ? 26.0f : 14.0f;
        float armLift = verticalVelocity > 0.0f ? 10.0f : -6.0f;

        bones.hips.position_to_parent.set(hipsBase.position.x, hipsBase.position.y + 0.06f, hipsBase.position.z);
        bones.hips.rotation.set(hipsBase.rotation.x + 2.0f, hipsBase.rotation.y, hipsBase.rotation.z);
        (bones.chest != null ? bones.chest : bones.spine).rotation.set(
                spineBase.rotation.x - airborneLean,
                spineBase.rotation.y,
                spineBase.rotation.z);
        bones.head.rotation.set(headBase.rotation.x + (airborneLean * 0.25f), headBase.rotation.y,
                headBase.rotation.z);

        bones.leftUpperLeg.rotation.set(leftUpperLegBase.rotation.x + kneeTuck, leftUpperLegBase.rotation.y,
                leftUpperLegBase.rotation.z);
        bones.rightUpperLeg.rotation.set(rightUpperLegBase.rotation.x + kneeTuck, rightUpperLegBase.rotation.y,
                rightUpperLegBase.rotation.z);
        bones.leftLowerLeg.rotation.set(leftLowerLegBase.rotation.x + kneeTuck, leftLowerLegBase.rotation.y,
                leftLowerLegBase.rotation.z);
        bones.rightLowerLeg.rotation.set(rightLowerLegBase.rotation.x + kneeTuck, rightLowerLegBase.rotation.y,
                rightLowerLegBase.rotation.z);

        applyArmPose(armLift, -armLift, 8.0f, 8.0f);
    }

    private void applyArmPose(float leftSwing, float rightSwing, float leftElbowBend, float rightElbowBend) {
        bones.leftUpperArm.rotation.set(
                leftUpperArmBase.rotation.x + leftSwing,
                leftUpperArmBase.rotation.y,
                leftUpperArmBase.rotation.z - 70.0f);
        bones.rightUpperArm.rotation.set(
                rightUpperArmBase.rotation.x + rightSwing,
                rightUpperArmBase.rotation.y,
                rightUpperArmBase.rotation.z + 70.0f);

        if (bones.leftLowerArm != null && leftLowerArmBase != null) {
            bones.leftLowerArm.rotation.set(
                    leftLowerArmBase.rotation.x - leftElbowBend,
                    leftLowerArmBase.rotation.y,
                    leftLowerArmBase.rotation.z);
        }
        if (bones.rightLowerArm != null && rightLowerArmBase != null) {
            bones.rightLowerArm.rotation.set(
                    rightLowerArmBase.rotation.x - rightElbowBend,
                    rightLowerArmBase.rotation.y,
                    rightLowerArmBase.rotation.z);
        }
    }

    private float sampleTerrainHeight(float worldX, float worldZ) {
        if (terrain == null || terrain.heightMap == null || terrain.heightMap.length == 0 || terrain.heightMap[0].length == 0) {
            return 0.0f;
        }

        float localX = worldX - terrainOrigin.x;
        float localZ = worldZ - terrainOrigin.z;
        int x0 = clamp((int) java.lang.Math.floor(localX), 0, terrain.heightMap.length - 1);
        int z0 = clamp((int) java.lang.Math.floor(localZ), 0, terrain.heightMap[0].length - 1);
        int x1 = clamp(x0 + 1, 0, terrain.heightMap.length - 1);
        int z1 = clamp(z0 + 1, 0, terrain.heightMap[0].length - 1);

        float tx = clamp(localX - x0, 0.0f, 1.0f);
        float tz = clamp(localZ - z0, 0.0f, 1.0f);

        float h00 = terrain.heightMap[x0][z0];
        float h10 = terrain.heightMap[x1][z0];
        float h01 = terrain.heightMap[x0][z1];
        float h11 = terrain.heightMap[x1][z1];

        float hx0 = lerp(h00, h10, tx);
        float hx1 = lerp(h01, h11, tx);
        return terrainOrigin.y + lerp(hx0, hx1, tz);
    }

    private float sampleSlopePitchDegrees() {
        float headingRadians = (float) java.lang.Math.toRadians(headingDegrees);
        float probeDistance = 0.75f;
        float dirX = (float) java.lang.Math.sin(headingRadians);
        float dirZ = (float) java.lang.Math.cos(headingRadians);
        if (forwardInput < 0.0f) {
            dirX = -dirX;
            dirZ = -dirZ;
        }

        float aheadHeight = sampleTerrainHeight(worldPosition.x + (dirX * probeDistance),
                worldPosition.z + (dirZ * probeDistance));
        float behindHeight = sampleTerrainHeight(worldPosition.x - (dirX * probeDistance),
                worldPosition.z - (dirZ * probeDistance));
        return (float) java.lang.Math.toDegrees(java.lang.Math.atan2(aheadHeight - behindHeight, probeDistance * 2.0f));
    }

    private static float lerp(float a, float b, float t) {
        return a + ((b - a) * t);
    }

    private static int clamp(int value, int min, int max) {
        return java.lang.Math.max(min, java.lang.Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return java.lang.Math.max(min, java.lang.Math.min(max, value));
    }

    private static class BoneState {
        private final Vector3 position;
        private final Vector3 rotation;

        private BoneState(Vector3 position, Vector3 rotation) {
            this.position = position;
            this.rotation = rotation;
        }

        private static BoneState capture(com.njst.gaming.Bone bone) {
            if (bone == null) {
                return null;
            }
            return new BoneState(bone.position_to_parent.clone(), bone.rotation.clone());
        }
    }
}
