package com.njst.gaming.Animations;

import com.njst.gaming.skeleton.Skeleton;
import java.util.Map;

/**
 * Represents a collection of animations mapped to bones in a skeleton.
 */
public class Skeletal_Animation {
    public String name;
    private Map<String, ? extends Animation> animationMap;

    public Skeletal_Animation() {
    }

    public Skeletal_Animation(String name, Map<String, ? extends Animation> animationMap) {
        this.name = name;
        this.animationMap = animationMap;
    }

    public void setTarget(Skeleton s) {
        s.map(this);
    }

    public Map<String, ? extends Animation> getAnimationMap() {
        return animationMap;
    }

    public void setAnimationMap(Map<String, ? extends Animation> animationMap) {
        this.animationMap = animationMap;
    }

    /**
     * Starts all individual bone animations.
     */
    public void start() {
        if (animationMap != null) {
            animationMap.forEach((name, value) -> {
                if (value != null) {
                    value.start();
                }
            });
        }
    }
}
