package com.njst.gaming.Animations;

import com.njst.gaming.Bone;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Scene;

public class HeadLookIKAnimation extends Animation {
    private final Scene scene;
    private final Bone rootBone;
    private final Bone headBone;
    private final Vector3 baseRotation;
    private final float maxYawDegrees;
    private final float maxPitchDegrees;
    private final float smoothing;

    public HeadLookIKAnimation(Scene scene, Bone rootBone, Bone headBone) {
        this(scene, rootBone, headBone, 75.0f, 40.0f, 0.2f);
    }

    public HeadLookIKAnimation(Scene scene, Bone rootBone, Bone headBone, float maxYawDegrees,
            float maxPitchDegrees, float smoothing) {
        this.scene = scene;
        this.rootBone = rootBone;
        this.headBone = headBone;
        this.baseRotation = headBone.rotation.clone();
        this.maxYawDegrees = maxYawDegrees;
        this.maxPitchDegrees = maxPitchDegrees;
        this.smoothing = smoothing;
    }

    @Override
    public void animate() {
        if (scene == null || scene.renderer == null || scene.renderer.camera == null || rootBone == null || headBone == null) {
            return;
        }

        Vector3 toCamera = scene.renderer.camera.cameraPosition.clone().sub(headBone.get_globalposition());
        float distance = toCamera.length();
        if (distance < 0.0001f) {
            return;
        }
        toCamera.mul(1.0f / distance);

        float desiredYaw = (float) java.lang.Math.toDegrees(java.lang.Math.atan2(toCamera.x, toCamera.z));
        float flatDistance = (float) java.lang.Math.sqrt((toCamera.x * toCamera.x) + (toCamera.z * toCamera.z));
        float desiredPitch = (float) -java.lang.Math.toDegrees(
                java.lang.Math.atan2(toCamera.y, java.lang.Math.max(0.0001f, flatDistance)));

        float targetYaw = baseRotation.y + clamp(normalizeAngle(desiredYaw - headBone.parent_rotation.y),
                -maxYawDegrees, maxYawDegrees);
        float targetPitch = baseRotation.x + clamp(normalizeAngle(desiredPitch - headBone.parent_rotation.x),
                -maxPitchDegrees, maxPitchDegrees);

        headBone.rotation.x = lerpAngle(headBone.rotation.x, targetPitch, smoothing);
        headBone.rotation.y = lerpAngle(headBone.rotation.y, targetYaw, smoothing);
        headBone.rotation.z = lerpAngle(headBone.rotation.z, baseRotation.z, smoothing);
        rootBone.update();
    }

    private static float clamp(float value, float min, float max) {
        return java.lang.Math.max(min, java.lang.Math.min(max, value));
    }

    private static float normalizeAngle(float angle) {
        float normalized = angle;
        while (normalized > 180.0f) {
            normalized -= 360.0f;
        }
        while (normalized < -180.0f) {
            normalized += 360.0f;
        }
        return normalized;
    }

    private static float lerpAngle(float current, float target, float alpha) {
        float delta = normalizeAngle(target - current);
        return current + (delta * alpha);
    }
}
