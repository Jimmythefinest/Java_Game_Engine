package com.njst.gaming.Animations;

import java.util.ArrayList;

public class AnimationGroup extends Animation{
    public String Name;
    ArrayList<Animation> animations=new ArrayList<>();
    public float phase;
    int frame;
    public boolean repeat=false;

    public AnimationGroup(){
    }
    public AnimationGroup(String Name){
        this();
        this.Name=Name;

    }
    public void stop(boolean should_finish_first){
        if(!should_finish_first){
            for(Animation anim:animations){
                anim.active=false;
            }
        }

    }
    public ArrayList<Animation> get_animations(){
        return animations;
    }
    public void start(){
        active=true;
        for(Animation anim:animations){
            anim.start();
        }
    }
    public void set_onfinish(Runnable run) {

    }
    public void animate(){
        // System.out.println("nn");
        if(!active)return;
        // if(frame>duration){
        //     if(!repeat)return;
        //     frame=0;
        // }
        for(Animation anim:animations){
            anim.animate();
        }
    }
    public void add_animation(Animation anim){
        if(this.animations.contains(anim))return;
        this.animations.add(anim);
        duration=anim.duration>duration?anim.duration:duration;
    }
}
