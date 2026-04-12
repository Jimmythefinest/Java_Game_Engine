package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Bone;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.objects.Weighted_GameObject;
import com.njst.gaming.skeleton.Skeleton;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

final class BattleArenaCharacterAssembly {
    ArrayList<Bone> bones = new ArrayList<>();
    Bone rootBone;
    Bone hipBone;
    Vector3 rootBasePosition = new Vector3(0f, 0f, 0f);
    Skeleton skeleton;
    Weighted_GameObject meshObject;
    Map<String, ArrayList<KeyframeAnimation>> animationSets = new LinkedHashMap<>();

    ArrayList<KeyframeAnimation> animationSet(String key) {
        ArrayList<KeyframeAnimation> animations = animationSets.get(key);
        if (animations == null) {
            animations = new ArrayList<>();
            animationSets.put(key, animations);
        }
        return animations;
    }
}
