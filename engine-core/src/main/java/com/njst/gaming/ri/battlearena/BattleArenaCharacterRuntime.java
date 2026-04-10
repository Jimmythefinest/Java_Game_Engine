package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Bone;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.collision.Collider;
import com.njst.gaming.objects.Weighted_GameObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BattleArenaCharacterRuntime {
    final BattleArenaCharacterController controller;
    final ArrayList<Bone> bones;
    final Bone rootBone;
    final Bone hipBone;
    final Vector3 rootBasePosition;
    final Weighted_GameObject meshObject;
    final Map<String, ArrayList<KeyframeAnimation>> animationSets;
    final Map<String, BattleArenaCharacterDefinition.EventDefinition> eventDefinitions;
    final ArrayList<Collider> hitboxColliders;

    BattleArenaCharacterRuntime(BattleArenaCharacterController controller,
                                BattleArenaCharacterAssembly assembly,
                                BattleArenaCharacterDefinition definition) {
        this.controller = controller;
        this.bones = assembly.bones;
        this.rootBone = assembly.rootBone;
        this.hipBone = assembly.hipBone;
        this.rootBasePosition = assembly.rootBasePosition;
        this.meshObject = assembly.meshObject;
        this.animationSets = createAnimationSets(assembly);
        this.eventDefinitions = createEventDefinitions(definition);
        this.hitboxColliders = createHitboxColliders();
        controller.configureCharacterData(animationSets, eventDefinitions);
    }

    Vector3 getPosition() {
        return controller.getPlayerPosition();
    }

    float getHeadingDegrees() {
        return controller.getPlayerHeadingDegrees();
    }

    boolean isPunching() {
        return controller.isPunching();
    }

    void onHitTaken() {
        controller.triggerHitReact();
    }

    List<Collider> getHitboxColliders() {
        return hitboxColliders;
    }

    ArrayList<KeyframeAnimation> animationSet(String key) {
        ArrayList<KeyframeAnimation> animations = animationSets.get(key);
        return animations != null ? animations : new ArrayList<KeyframeAnimation>();
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

    private Map<String, ArrayList<KeyframeAnimation>> createAnimationSets(BattleArenaCharacterAssembly assembly) {
        LinkedHashMap<String, ArrayList<KeyframeAnimation>> sets = new LinkedHashMap<>();
        sets.put(BattleArenaCharacterController.ANIM_IDLE, copy(assembly.idleAnimations));
        sets.put(BattleArenaCharacterController.ANIM_WALK, copy(assembly.walkAnimations));
        sets.put(BattleArenaCharacterController.ANIM_WALK_BACKWARD, copy(assembly.walkBackwardAnimations));
        sets.put(BattleArenaCharacterController.ANIM_RUN, copy(assembly.runAnimations));
        sets.put(BattleArenaCharacterController.ANIM_JUMP, copy(assembly.jumpAnimations));
        sets.put(BattleArenaCharacterController.ANIM_PUNCH, copy(assembly.punchAnimations));
        sets.put(BattleArenaCharacterController.ANIM_TAKE_HIT, copy(assembly.takeHitAnimations));
        return sets;
    }

    private Map<String, BattleArenaCharacterDefinition.EventDefinition> createEventDefinitions(
            BattleArenaCharacterDefinition definition) {
        LinkedHashMap<String, BattleArenaCharacterDefinition.EventDefinition> events = new LinkedHashMap<>();
        if (definition != null && definition.events != null) {
            events.putAll(definition.events);
        }
        return events;
    }

    private ArrayList<Collider> createHitboxColliders() {
        ArrayList<Collider> colliders = new ArrayList<>();
        colliders.add(new BattleArenaHitboxCollider(
                this,
                BattleArenaHitboxCollider.Type.BODY,
                meshObject.name + "_BodyHurtbox",
                new Vector3(0f, 1.0f, 0f),
                new Vector3(0.45f, 1.0f, 0.35f)));
        colliders.add(new BattleArenaHitboxCollider(
                this,
                BattleArenaHitboxCollider.Type.PUNCH,
                meshObject.name + "_PunchHitbox",
                new Vector3(0f, 1.1f, 0.85f),
                new Vector3(0.35f, 0.35f, 0.45f)));
        return colliders;
    }
}
