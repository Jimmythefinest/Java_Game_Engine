package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Bone;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.objects.Weighted_GameObject;
import com.njst.gaming.skeleton.Skeleton;

import java.util.ArrayList;

final class BattleArenaCharacterAssembly {
    ArrayList<Bone> bones = new ArrayList<>();
    Bone rootBone;
    Bone hipBone;
    Vector3 rootBasePosition = new Vector3(0f, 0f, 0f);
    Skeleton skeleton;
    Weighted_GameObject meshObject;
    ArrayList<KeyframeAnimation> idleAnimations = new ArrayList<>();
    ArrayList<KeyframeAnimation> walkAnimations = new ArrayList<>();
    ArrayList<KeyframeAnimation> walkBackwardAnimations = new ArrayList<>();
    ArrayList<KeyframeAnimation> runAnimations = new ArrayList<>();
    ArrayList<KeyframeAnimation> jumpAnimations = new ArrayList<>();
    ArrayList<KeyframeAnimation> punchAnimations = new ArrayList<>();
    ArrayList<KeyframeAnimation> kickAnimations = new ArrayList<>();
    ArrayList<KeyframeAnimation> leftsideStepAnimations = new ArrayList<>();
    ArrayList<KeyframeAnimation> takeHitAnimations = new ArrayList<>();
}
