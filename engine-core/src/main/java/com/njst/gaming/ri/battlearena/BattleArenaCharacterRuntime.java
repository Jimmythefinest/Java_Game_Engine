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
    private static final float PUNCH_RECOIL_STRENGTH = 0.22f;
    private static final float KICK_RECOIL_STRENGTH = 0.32f;
    private static final float DEFAULT_RECOIL_STRENGTH = 0.18f;

    final BattleArenaCharacterController controller;
    final ArrayList<Bone> bones;
    final Bone rootBone;
    final Bone hipBone;
    final Vector3 rootBasePosition;
    final Weighted_GameObject meshObject;
    final BattleArenaCharacterDefinition definition;
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
        this.definition = definition;
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

    boolean isKicking() {
        return controller.isKicking();
    }

    boolean isSideSteppingLeft() {
        return controller.isSideSteppingLeft();
    }

    boolean isAnimationActive(String animationKey) {
        if (BattleArenaCharacterController.ANIM_PUNCH.equals(animationKey)) {
            return controller.isPunching();
        }
        if (BattleArenaCharacterController.ANIM_KICK.equals(animationKey)) {
            return controller.isKicking();
        }
        return false;
    }

    void onHitTaken(String hitboxName, String onHitAnimation) {
        controller.triggerHitReact(hitboxName, onHitAnimation);
    }

    void onHitTaken(BattleArenaCharacterRuntime attacker, String hitboxName, String onHitAnimation) {
        controller.triggerHitReact(hitboxName, onHitAnimation);
        controller.applyHitRecoil(resolveHitDirection(attacker), resolveRecoilStrength(attacker));
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

    void faceTowards(BattleArenaCharacterRuntime other) {
        if (other == null) {
            return;
        }
        Vector3 toOther = other.getPosition().clone().sub(getPosition());
        toOther.y = 0f;
        if (toOther.length() <= 0.0001f) {
            return;
        }
        controller.setPlayerHeadingDegrees((float) Math.toDegrees(Math.atan2(toOther.x, toOther.z)));
    }

    private Vector3 resolveHitDirection(BattleArenaCharacterRuntime attacker) {
        if (attacker == null) {
            return facingDirection();
        }
        Vector3 direction = getPosition().clone().sub(attacker.getPosition());
        direction.y = 0f;
        if (direction.length() > 0.0001f) {
            return direction;
        }
        return attacker.facingDirection();
    }

    private Vector3 facingDirection() {
        float headingRadians = (float) Math.toRadians(getHeadingDegrees());
        return new Vector3(
                (float) Math.sin(headingRadians),
                0f,
                (float) Math.cos(headingRadians));
    }

    private float resolveRecoilStrength(BattleArenaCharacterRuntime attacker) {
        if (attacker == null) {
            return DEFAULT_RECOIL_STRENGTH;
        }
        if (attacker.isKicking()) {
            return KICK_RECOIL_STRENGTH;
        }
        if (attacker.isPunching()) {
            return PUNCH_RECOIL_STRENGTH;
        }
        return DEFAULT_RECOIL_STRENGTH;
    }

    private ArrayList<KeyframeAnimation> copy(ArrayList<KeyframeAnimation> source) {
        return new ArrayList<>(source);
    }

    private Bone findBone(String nameFragment) {
        if (nameFragment == null) {
            return null;
        }
        String needle = nameFragment.toLowerCase();
        for (Bone bone : bones) {
            if (bone.name != null && bone.name.toLowerCase().contains(needle)) {
                return bone;
            }
        }
        return null;
    }

    private Map<String, ArrayList<KeyframeAnimation>> createAnimationSets(BattleArenaCharacterAssembly assembly) {
        LinkedHashMap<String, ArrayList<KeyframeAnimation>> sets = new LinkedHashMap<>();
        sets.put(BattleArenaCharacterController.ANIM_IDLE, copy(assembly.idleAnimations));
        sets.put(BattleArenaCharacterController.ANIM_WALK, copy(assembly.walkAnimations));
        sets.put(BattleArenaCharacterController.ANIM_WALK_BACKWARD, copy(assembly.walkBackwardAnimations));
        sets.put(BattleArenaCharacterController.ANIM_RUN, copy(assembly.runAnimations));
        sets.put(BattleArenaCharacterController.ANIM_JUMP, copy(assembly.jumpAnimations));
        sets.put(BattleArenaCharacterController.ANIM_PUNCH, copy(assembly.punchAnimations));
        sets.put(BattleArenaCharacterController.ANIM_KICK, copy(assembly.kickAnimations));
        sets.put(BattleArenaCharacterController.ANIM_LEFTSIDE_STEP, copy(assembly.leftsideStepAnimations));
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
        if (definition == null || definition.hitboxes == null) {
            return colliders;
        }

        for (Map.Entry<String, BattleArenaCharacterDefinition.HitboxDefinition> entry : definition.hitboxes.entrySet()) {
            BattleArenaCharacterDefinition.HitboxDefinition hitboxDefinition = entry.getValue();
            if (hitboxDefinition == null || hitboxDefinition.halfExtents == null || hitboxDefinition.halfExtents.length < 3) {
                continue;
            }

            Bone anchorBone = findBone(hitboxDefinition.followsBone);
            Vector3 center = hitboxDefinition.center != null && hitboxDefinition.center.length >= 3
                    ? new Vector3(hitboxDefinition.center)
                    : new Vector3(0f, 0f, 0f);
            Vector3 halfExtents = new Vector3(hitboxDefinition.halfExtents);
            BattleArenaHitboxCollider.Type type = BattleArenaCharacterDefinition.HITBOX_KIND_HITBOX.equals(hitboxDefinition.kind)
                    ? BattleArenaHitboxCollider.Type.HITBOX
                    : BattleArenaHitboxCollider.Type.HURTBOX;

            colliders.add(new BattleArenaHitboxCollider(
                    this,
                    type,
                    entry.getKey(),
                    hitboxDefinition.activeWhen,
                    hitboxDefinition.onHitAnimation,
                    anchorBone,
                    center,
                    halfExtents));
        }
        return colliders;
    }
}
