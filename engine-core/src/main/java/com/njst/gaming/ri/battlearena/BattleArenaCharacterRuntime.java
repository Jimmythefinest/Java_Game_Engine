package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Bone;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.objects.Weighted_GameObject;

import java.util.ArrayList;

final class BattleArenaCharacterRuntime {
    final BattleArenaCharacterController controller;
    final ArrayList<Bone> bones;
    final Bone rootBone;
    final Bone hipBone;
    final Vector3 rootBasePosition;
    final Weighted_GameObject meshObject;
    final ArrayList<KeyframeAnimation> idleAnimations;
    final ArrayList<KeyframeAnimation> walkAnimations;
    final ArrayList<KeyframeAnimation> walkBackwardAnimations;
    final ArrayList<KeyframeAnimation> runAnimations;
    final ArrayList<KeyframeAnimation> jumpAnimations;

    BattleArenaCharacterRuntime(BattleArenaCharacterController controller, BattleArenaCharacterAssembly assembly) {
        this.controller = controller;
        this.bones = assembly.bones;
        this.rootBone = assembly.rootBone;
        this.hipBone = assembly.hipBone;
        this.rootBasePosition = assembly.rootBasePosition;
        this.meshObject = assembly.meshObject;
        this.idleAnimations = copy(assembly.idleAnimations);
        this.walkAnimations = copy(assembly.walkAnimations);
        this.walkBackwardAnimations = copy(assembly.walkBackwardAnimations);
        this.runAnimations = copy(assembly.runAnimations);
        this.jumpAnimations = copy(assembly.jumpAnimations);
        controller.configureAnimationSets(
                idleAnimations,
                walkAnimations,
                walkBackwardAnimations,
                runAnimations,
                jumpAnimations);
    }

    Vector3 getPosition() {
        return controller.getPlayerPosition();
    }

    float getHeadingDegrees() {
        return controller.getPlayerHeadingDegrees();
    }

    void syncRig() {
        Vector3 position = getPosition();
        rootBone.position_to_parent.set(
                rootBasePosition.x + position.x,
                rootBasePosition.y + position.y,
                rootBasePosition.z + position.z);
        rootBone.set_Parent_position(new Vector3(0f, 0f, 0f));
        rootBone.set_Parent_rotation(new Vector3(0f, 0f, 0f));
        rootBone.update();
        rootBone.rotate(new Vector3());
    }

    void applyHeadingToRig() {
        if (hipBone == null) {
            return;
        }
        rootBone.setRotation(new Vector3(0f, getHeadingDegrees(), 0f));
        hipBone.rotate(new Vector3());
    }

    private ArrayList<KeyframeAnimation> copy(ArrayList<KeyframeAnimation> source) {
        return new ArrayList<>(source);
    }
}
