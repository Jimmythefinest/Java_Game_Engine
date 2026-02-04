package com.njst.gaming.Animations;

import com.njst.gaming.Bone;
import com.njst.gaming.Math.Quaternion;
import com.njst.gaming.Math.Vector3;

import java.util.ArrayList;
import java.util.List;



public class KeyframeAnimation extends Animation{
    public List<Keyframe> keyframes; // List of keyframes for the animation
    // private float duration; // Total duration of the animation
    // private boolean active; // Is the animation currently active?
    public Runnable onfinish;
    public KeyframeAnimation(Bone bone) {
        this.bone = bone;
        this.keyframes = new ArrayList<>();
        this.active = false;
    }

    public void addKeyframe(float time, Vector3 position, Vector3 rotation) {
        keyframes.add(new Keyframe(time, position, rotation));
        // Update the duration of the animation
        if (time > duration) {
            duration = time;
        }
    }
   public  float time;
   public float speed=1;
    public void animate() {
  //  bone.translate(new Vector3(0,0.01f,0));
          
        if (!active) return;
        time+=speed;
        // Update the animation based on the current time
        float currentTime = time;//(System.currentTimeMillis() / 1000.0f) % duration; // Example of getting current time
        Keyframe previousKeyframe = null;
        Keyframe nextKeyframe = null;

        // Find the two keyframes surrounding the current time
        for (Keyframe keyframe : keyframes) {
            if (keyframe.time <= currentTime) {
                previousKeyframe = keyframe;
            } else if (keyframe.time > currentTime && nextKeyframe == null) {
                nextKeyframe = keyframe;
                break;
            }
        }
        if(nextKeyframe==null &&onfinish!=null){
          onfinish.run();
        }

        if (previousKeyframe != null && nextKeyframe != null) {
            // Interpolate between the two keyframes
            float t = (currentTime - previousKeyframe.time) / (nextKeyframe.time - previousKeyframe.time);
            Vector3 interpolatedPosition = interpolate(previousKeyframe.position, nextKeyframe.position, t);
            Vector3 interpolatedRotation = interpolate(previousKeyframe.rotation, nextKeyframe.rotation, t);

            // Update the bone's position and rotation
            //Vector3 temp=bone.global_position.clone();
           // bone.translate(new Vector3().sub(temp));
          //  bone.translate(interpolatedPosition);
            bone.position_to_parent = interpolatedPosition;
            bone.rotate( interpolatedRotation.sub(bone.rotation));
          //  bone.rotate(new Vector3(0,0,1));

        }
        
    }

    
    private Vector3 interpolate(Vector3 start, Vector3 end, float t) {
        // Linear interpolation
        Quaternion q1=Quaternion.fromEuler(start.x,start.y,start.z);
        Quaternion q2=Quaternion.fromEuler(end.x,end.y,end.z);
        Quaternion q3=Quaternion.slerp(q1,q2,t);
       
        return new Vector3(q3.toEuler() );
    }
    
    public void start() {
        active = true;
    }

    public void stop() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }
    public static class Keyframe {
    public float time; // Time at which this keyframe occurs
    public Vector3 position; // Position of the bone
    public Vector3 rotation; // Rotation of the bone

    public Keyframe(float time, Vector3 position, Vector3 rotation) {
        this.time = time;
        this.position = position;
        this.rotation = rotation;
    }
}
}