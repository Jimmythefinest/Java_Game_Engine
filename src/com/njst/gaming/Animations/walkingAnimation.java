package com.njst.gaming.Animations;

import com.njst.gaming.Bone;
import com.njst.gaming.Animations.KeyframeAnimation.*;
import com.njst.gaming.Math.Vector3;

import java.util.ArrayList;
import java.util.List;

public class walkingAnimation extends Animation{
    private Bone thighBone; // The upper leg bone
    private Bone calfBone;  // The lower leg bone
    private List<Keyframe> thighKeyframes; // Keyframes for the thigh bone
    private List<Keyframe> calfKeyframes;  // Keyframes for the calf bone
    private float duration; // Total duration of the animation
    private boolean active; // Is the animation currently active?

    public walkingAnimation(Bone thighBone, Bone calfBone) {
        this.thighBone = thighBone;
        this.calfBone = calfBone;
        this.thighKeyframes = new ArrayList<>();
        this.calfKeyframes = new ArrayList<>();
        this.active = false;
        createWalkingKeyframes();
    }

    private void createWalkingKeyframes() {
        // Define keyframes for the thigh bone
        thighKeyframes.add(new Keyframe(0.0f, new Vector3(0, 0, 0), new Vector3(0, 0, 0))); // Start position
        thighKeyframes.add(new Keyframe(0.5f, new Vector3(0, 0, 0), new Vector3(0, 30, 0))); // Raise thigh
        thighKeyframes.add(new Keyframe(1.0f, new Vector3(0, 0, 0), new Vector3(0, 0, 0))); // Return to start
        thighKeyframes.add(new Keyframe(1.5f, new Vector3(0, 0, 0), new Vector3(0, -30, 0))); // Lower thigh
        thighKeyframes.add(new Keyframe(2.0f, new Vector3(0, 0, 0), new Vector3(0, 0, 0))); // Return to start

        // Define keyframes for the calf bone
        calfKeyframes.add(new Keyframe(0.0f, new Vector3(0, 0, 0), new Vector3(0, 0, 0))); // Start position
        calfKeyframes.add(new Keyframe(0.5f, new Vector3(0, 0, 0), new Vector3(0, 15, 0))); // Raise calf
        calfKeyframes.add(new Keyframe(1.0f, new Vector3(0, 0, 0), new Vector3(0, 0, 0))); // Return to start
        calfKeyframes.add(new Keyframe(1.5f, new Vector3(0, 0, 0), new Vector3(0, -15, 0))); // Lower calf
        calfKeyframes.add(new Keyframe(2.0f, new Vector3(0, 0, 0), new Vector3(0, 0, 0))); // Return to start

        // Set the total duration of the walking cycle
        duration = 2.0f; // Total duration of the walking cycle
    }

    public void animate() {
        if (!active) return; 
        // Update the animation based on the current time
        float currentTime = (System.currentTimeMillis() / 1000.0f) % duration; // Example of getting current time

        // Update thigh bone
        updateBone(thighBone, thighKeyframes, currentTime);
        // Update calf bone
        updateBone(calfBone, calfKeyframes, currentTime);
    }

    private void updateBone(Bone bone, List<Keyframe> keyframes, float currentTime) {
        // Find the two keyframes surrounding the current time
        Keyframe startKeyframe = null;
        Keyframe endKeyframe = null;

        for (int i = 0; i < keyframes.size() - 1; i++) {
            if (currentTime >= keyframes.get(i).time && currentTime < keyframes.get(i + 1).time) {
                startKeyframe = keyframes.get(i);
                endKeyframe = keyframes.get(i + 1);
                break;
            }
        }

        if (startKeyframe != null && endKeyframe != null) {
            // Interpolate between the two keyframes
            float t = (currentTime - startKeyframe.time) / (endKeyframe.time - startKeyframe.time);
            Vector3 interpolatedPosition = startKeyframe.position.lerp( endKeyframe.position, t);
            Vector3 interpolatedRotation = startKeyframe.rotation.lerp( endKeyframe.rotation, t);

            // Update the bone's position and rotation
            bone.setPosition(interpolatedPosition);
            bone.setRotation(interpolatedRotation);
        }
    }

    public void start() {
        active = true; // Start the animation
    }

    public void stop() {
        active = false; // Stop the animation
    }
}