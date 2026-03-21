package com.njst.gaming.Animations;

import com.njst.gaming.Bone;

public class BipedWalkAnimation extends Animation {
    private final Bone root;
    private final Bone hips;
    private final Bone spine;
    private final Bone head;
    private final Bone leftUpperLeg;
    private final Bone leftLowerLeg;
    private final Bone rightUpperLeg;
    private final Bone rightLowerLeg;
    private final Bone leftUpperArm;
    private final Bone rightUpperArm;
    private final VectorState hipsBase;
    private final VectorState spineBase;
    private final VectorState headBase;
    private final VectorState leftUpperLegBase;
    private final VectorState leftLowerLegBase;
    private final VectorState rightUpperLegBase;
    private final VectorState rightLowerLegBase;
    private final VectorState leftUpperArmBase;
    private final VectorState rightUpperArmBase;
    private float phase;
    private final float speed;

    public BipedWalkAnimation(Bone root, Bone hips, Bone spine, Bone head,
            Bone leftUpperLeg, Bone leftLowerLeg, Bone rightUpperLeg, Bone rightLowerLeg,
            Bone leftUpperArm, Bone rightUpperArm) {
        this(root, hips, spine, head, leftUpperLeg, leftLowerLeg, rightUpperLeg, rightLowerLeg, leftUpperArm,
                rightUpperArm, 0.08f);
    }

    public BipedWalkAnimation(Bone root, Bone hips, Bone spine, Bone head,
            Bone leftUpperLeg, Bone leftLowerLeg, Bone rightUpperLeg, Bone rightLowerLeg,
            Bone leftUpperArm, Bone rightUpperArm, float speed) {
        this.root = root;
        this.hips = hips;
        this.spine = spine;
        this.head = head;
        this.leftUpperLeg = leftUpperLeg;
        this.leftLowerLeg = leftLowerLeg;
        this.rightUpperLeg = rightUpperLeg;
        this.rightLowerLeg = rightLowerLeg;
        this.leftUpperArm = leftUpperArm;
        this.rightUpperArm = rightUpperArm;
        this.speed = speed;
        this.hipsBase = VectorState.capture(hips);
        this.spineBase = VectorState.capture(spine);
        this.headBase = VectorState.capture(head);
        this.leftUpperLegBase = VectorState.capture(leftUpperLeg);
        this.leftLowerLegBase = VectorState.capture(leftLowerLeg);
        this.rightUpperLegBase = VectorState.capture(rightUpperLeg);
        this.rightLowerLegBase = VectorState.capture(rightLowerLeg);
        this.leftUpperArmBase = VectorState.capture(leftUpperArm);
        this.rightUpperArmBase = VectorState.capture(rightUpperArm);
    }

    @Override
    public void animate() {
        phase += speed;

        float legSwing = (float) java.lang.Math.sin(phase) * 28.0f;
        float kneeLift = java.lang.Math.max(0.0f, (float) java.lang.Math.sin(phase)) * 22.0f;
        float oppositeKneeLift = java.lang.Math.max(0.0f, (float) java.lang.Math.sin(phase + java.lang.Math.PI))
                * 22.0f;
        float armSwing = (float) java.lang.Math.sin(phase) * 18.0f;
        float hipBob = (float) java.lang.Math.abs(java.lang.Math.sin(phase * 2.0f)) * 0.06f;
        float torsoTwist = (float) java.lang.Math.sin(phase) * 6.0f;

        hips.position_to_parent.set(hipsBase.position.x, hipsBase.position.y + hipBob, hipsBase.position.z);
        hips.rotation.set(hipsBase.rotation.x, hipsBase.rotation.y + (torsoTwist * 0.35f), hipsBase.rotation.z);
        spine.rotation.set(spineBase.rotation.x - 2.0f, spineBase.rotation.y + torsoTwist, spineBase.rotation.z);
        head.rotation.set(headBase.rotation.x + 2.0f, headBase.rotation.y - (torsoTwist * 0.5f), headBase.rotation.z);

        leftUpperLeg.rotation.set(leftUpperLegBase.rotation.x + legSwing, leftUpperLegBase.rotation.y,
                leftUpperLegBase.rotation.z);
        rightUpperLeg.rotation.set(rightUpperLegBase.rotation.x - legSwing, rightUpperLegBase.rotation.y,
                rightUpperLegBase.rotation.z);
        leftLowerLeg.rotation.set(leftLowerLegBase.rotation.x + kneeLift, leftLowerLegBase.rotation.y,
                leftLowerLegBase.rotation.z);
        rightLowerLeg.rotation.set(rightLowerLegBase.rotation.x + oppositeKneeLift, rightLowerLegBase.rotation.y,
                rightLowerLegBase.rotation.z);

        leftUpperArm.rotation.set(leftUpperArmBase.rotation.x - armSwing, leftUpperArmBase.rotation.y,
                leftUpperArmBase.rotation.z);
        rightUpperArm.rotation.set(rightUpperArmBase.rotation.x + armSwing, rightUpperArmBase.rotation.y,
                rightUpperArmBase.rotation.z);

        root.update();
    }

    private static class VectorState {
        private final com.njst.gaming.Math.Vector3 position;
        private final com.njst.gaming.Math.Vector3 rotation;

        private VectorState(com.njst.gaming.Math.Vector3 position, com.njst.gaming.Math.Vector3 rotation) {
            this.position = position;
            this.rotation = rotation;
        }

        private static VectorState capture(Bone bone) {
            return new VectorState(bone.position_to_parent.clone(), bone.rotation.clone());
        }
    }
}
