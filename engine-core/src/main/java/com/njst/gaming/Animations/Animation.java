package com.njst.gaming.Animations;

import com.njst.gaming.Bone;
import java.io.Serializable;

public  class Animation implements Serializable {
    private static final long serialVersionUID = 1L;
    public float duration=0; // Total duration of the animation
    public  boolean active; // Is the animation currently active?
    public transient Runnable onfinish;
    public transient Bone bone; // The bone this animation is associated with (re-wired after deserialization)

    public  void animate(){

    };
    public void start(){
        
    }
}
