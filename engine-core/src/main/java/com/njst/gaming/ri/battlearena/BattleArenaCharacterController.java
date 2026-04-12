package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.input.ActionInput;
import com.njst.gaming.input.PointerState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

final class BattleArenaCharacterController {
    static final String EVENT_PUNCH_STARTED = "punch_started";
    static final String EVENT_KICK_STARTED = "kick_started";
    static final String EVENT_HIT_TAKEN = "hit_taken";
    static final String THEN_RESUME_BASE = "resume_base";

    static final String ANIM_IDLE = "idle";
    static final String ANIM_WALK = "walk";
    static final String ANIM_WALK_BACKWARD = "walk_backward";
    static final String ANIM_RUN = "run";
    static final String ANIM_JUMP = "jump";
    static final String ANIM_PUNCH = "punch";
    static final String ANIM_KICK = "kick";
    static final String ANIM_TAKE_HIT = "take_hit";

    private static final float WALK_SPEED = 0.02f;
    private static final float RUN_SPEED = 0.035f;
    private static final float TURN_SPEED_DEGREES = 3.2f;
    private static final float JUMP_VELOCITY = 0.22f;
    private static final float JUMP_GRAVITY = 0.012f;

    interface TerrainHeightSampler {
        float sample(float worldX, float worldZ);
    }

    private final Vector3 playerPosition = new Vector3(0f, 0f, 0f);
    private float playerHeadingDegrees = 0f;
    private Map<String, ArrayList<KeyframeAnimation>> animationSets = new HashMap<>();
    private Map<String, BattleArenaCharacterDefinition.EventDefinition> eventDefinitions = new HashMap<>();
    private ArrayList<KeyframeAnimation> currentAnimationSet = null;
    private TerrainHeightSampler terrainHeightSampler = (x, z) -> 0f;
    private boolean playerMoving = false;
    private boolean playerMovingBackward = false;
    private boolean playerRunning = false;
    private float jumpHeight = 0f;
    private float verticalVelocity = 0f;
    private boolean jumping = false;
    private boolean punching = false;
    private boolean kicking = false;
    private boolean takingHit = false;
    private final BattleArenaCharacterControlState playerControls = new BattleArenaCharacterControlState();

    void configureCharacterData(Map<String, ArrayList<KeyframeAnimation>> animationSets,
                                Map<String, BattleArenaCharacterDefinition.EventDefinition> eventDefinitions) {
        this.animationSets = animationSets != null ? new HashMap<>(animationSets) : new HashMap<String, ArrayList<KeyframeAnimation>>();
        this.eventDefinitions = eventDefinitions != null
                ? new HashMap<String, BattleArenaCharacterDefinition.EventDefinition>(eventDefinitions)
                : new HashMap<String, BattleArenaCharacterDefinition.EventDefinition>();
        currentAnimationSet = null;
        updateMovementAnimationState();
    }

    void setTerrainHeightSampler(TerrainHeightSampler terrainHeightSampler) {
        this.terrainHeightSampler = terrainHeightSampler != null ? terrainHeightSampler : (x, z) -> 0f;
    }

    void reset() {
        playerPosition.set(0f, 0f, 0f);
        playerHeadingDegrees = 0f;
        currentAnimationSet = null;
        playerMoving = false;
        playerMovingBackward = false;
        playerRunning = false;
        jumpHeight = 0f;
        verticalVelocity = 0f;
        jumping = false;
        punching = false;
        kicking = false;
        takingHit = false;
    }

    void update(ActionInput actions, PointerState movementPointer, float sceneSpeed) {
        playerControls.capturePlayerInput(actions, movementPointer);
        update(playerControls, sceneSpeed);
    }

    void update(BattleArenaCharacterControlState controls, float sceneSpeed) {
        if (controls == null) {
            return;
        }

        if (controls.punchPressed && !punching && !animationSet(ANIM_PUNCH).isEmpty()) {
            triggerConfiguredEvent(
                    EVENT_PUNCH_STARTED,
                    () -> punching = true,
                    this::finishPunch);
        }

        if (controls.kickPressed && !kicking && !animationSet(ANIM_KICK).isEmpty()) {
            triggerConfiguredEvent(
                    EVENT_KICK_STARTED,
                    () -> kicking = true,
                    this::finishKick);
        }

        if (punching || kicking || takingHit) {
            updateJumpPhysics(sceneSpeed);
            snapPlayerToGround();
            return;
        }

        float forwardInput = clamp(controls.forwardInput);
        float turnInput = clamp(controls.turnInput);

        if (controls.jumpPressed && !jumping) {
            jumping = true;
            verticalVelocity = JUMP_VELOCITY * sceneSpeed;
            updateMovementAnimationState();
        }

        playerHeadingDegrees += turnInput * TURN_SPEED_DEGREES * sceneSpeed;

        boolean wantsToRun = controls.runDown;
        float movementSpeed = wantsToRun ? RUN_SPEED : WALK_SPEED;
        float moveAmount = forwardInput * movementSpeed * sceneSpeed;
        boolean isMovingNow = Math.abs(moveAmount) > 0.0001f;
        boolean isMovingBackwardNow = moveAmount < -0.0001f;
        boolean isRunningNow = isMovingNow && wantsToRun;
        if (isMovingNow != playerMoving
                || isMovingBackwardNow != playerMovingBackward
                || isRunningNow != playerRunning) {
            playerMoving = isMovingNow;
            playerMovingBackward = isMovingBackwardNow;
            playerRunning = isRunningNow;
            updateMovementAnimationState();
        }

        if (isMovingNow) {
            float headingRadians = (float) Math.toRadians(playerHeadingDegrees);
            playerPosition.x += (float) Math.sin(headingRadians) * moveAmount;
            playerPosition.z += (float) Math.cos(headingRadians) * moveAmount;
        }

        updateJumpPhysics(sceneSpeed);
        snapPlayerToGround();
    }

    Vector3 getPlayerPosition() {
        return playerPosition;
    }

    float getPlayerHeadingDegrees() {
        return playerHeadingDegrees;
    }

    boolean isPunching() {
        return punching;
    }

    boolean isKicking() {
        return kicking;
    }

    void triggerHitReact(String hitboxName, String animationKey) {
        String resolvedAnimationKey = animationKey;
        if (resolvedAnimationKey == null || resolvedAnimationKey.trim().isEmpty()) {
            BattleArenaCharacterDefinition.EventDefinition eventDefinition = eventDefinitions.get(EVENT_HIT_TAKEN);
            resolvedAnimationKey = eventDefinition != null ? eventDefinition.play : ANIM_TAKE_HIT;
        }
        ArrayList<KeyframeAnimation> hitAnimations = animationSet(resolvedAnimationKey);
        if (takingHit || hitAnimations.isEmpty()) {
            return;
        }
        takingHit = true;
        setCurrentAnimationSet(hitAnimations, resolveBaseAnimationSet(), this::finishHitReact);
    }

    void setPlayerPosition(float x, float y, float z) {
        playerPosition.set(x, y, z);
        snapPlayerToGround();
    }

    void setPlayerHeadingDegrees(float headingDegrees) {
        playerHeadingDegrees = headingDegrees;
    }

    private void updateJumpPhysics(float sceneSpeed) {
        if (!jumping) {
            jumpHeight = 0f;
            verticalVelocity = 0f;
            return;
        }

        jumpHeight += verticalVelocity;
        verticalVelocity -= JUMP_GRAVITY * sceneSpeed;
        if (jumpHeight <= 0f && verticalVelocity <= 0f) {
            jumpHeight = 0f;
            verticalVelocity = 0f;
            jumping = false;
            updateMovementAnimationState();
        }
    }

    private void snapPlayerToGround() {
        playerPosition.y = terrainHeightSampler.sample(playerPosition.x, playerPosition.z) + jumpHeight;
    }

    private void setCurrentAnimationSet(ArrayList<KeyframeAnimation> nextAnimationSet) {
        if (nextAnimationSet == null || nextAnimationSet.isEmpty() || currentAnimationSet == nextAnimationSet) {
            return;
        }
        if (currentAnimationSet != null) {
            for (KeyframeAnimation animation : currentAnimationSet) {
                animation.stop();
                animation.time = 0f;
            }
        }
        for (KeyframeAnimation animation : nextAnimationSet) {
            animation.time = 0f;
            animation.start();
        }
        currentAnimationSet = nextAnimationSet;
    }

    private void setCurrentAnimationSet(ArrayList<KeyframeAnimation> nextAnimationSet,
                                        ArrayList<KeyframeAnimation> nextAnimationSetOnFinish,
                                        Runnable onAnimationSetFinished) {
        if (nextAnimationSet == null || nextAnimationSet.isEmpty()) {
            return;
        }
        setCurrentAnimationSet(nextAnimationSet);
        for (KeyframeAnimation animation : nextAnimationSet) {
            animation.speed = 1f;
            animation.onfinish = () -> onAnimationFinished(
                    animation,
                    nextAnimationSet,
                    nextAnimationSetOnFinish,
                    onAnimationSetFinished);
        }
    }

    private void updateMovementAnimationState() {
        ArrayList<KeyframeAnimation> takeHitAnimations = animationSet(ANIM_TAKE_HIT);
        ArrayList<KeyframeAnimation> punchAnimations = animationSet(ANIM_PUNCH);
        ArrayList<KeyframeAnimation> kickAnimations = animationSet(ANIM_KICK);
        ArrayList<KeyframeAnimation> jumpAnimations = animationSet(ANIM_JUMP);
        if (takingHit) {
            setCurrentAnimationSet(takeHitAnimations);
            return;
        }
        if (punching) {
            setCurrentAnimationSet(punchAnimations);
            return;
        }
        if (kicking) {
            setCurrentAnimationSet(kickAnimations);
            return;
        }
        if (jumping) {
            if (!jumpAnimations.isEmpty()) {
                setCurrentAnimationSet(jumpAnimations);
            }
            return;
        }
        if (!playerMoving) {
            setCurrentAnimationSet(animationSet(ANIM_IDLE));
            return;
        }
        if (playerMovingBackward) {
            setCurrentAnimationSet(animationSet(ANIM_WALK_BACKWARD));
            return;
        }
        setCurrentAnimationSet(playerRunning ? animationSet(ANIM_RUN) : animationSet(ANIM_WALK));
    }

    private void finishPunch() {
        punching = false;
    }

    private void finishKick() {
        kicking = false;
    }

    private void finishHitReact() {
        takingHit = false;
    }

    private void onAnimationFinished(KeyframeAnimation finishedAnimation,
                                     ArrayList<KeyframeAnimation> sourceAnimationSet,
                                     ArrayList<KeyframeAnimation> nextAnimationSetOnFinish,
                                     Runnable onAnimationSetFinished) {
        finishedAnimation.stop();
        finishedAnimation.time = 0f;
        if (currentAnimationSet != sourceAnimationSet) {
            return;
        }
        for (KeyframeAnimation animation : sourceAnimationSet) {
            if (animation.isActive()) {
                return;
            }
        }
        if (onAnimationSetFinished != null) {
            onAnimationSetFinished.run();
        }
        if (nextAnimationSetOnFinish != null && !nextAnimationSetOnFinish.isEmpty()) {
            setCurrentAnimationSet(nextAnimationSetOnFinish);
            return;
        }
        updateMovementAnimationState();
    }

    private ArrayList<KeyframeAnimation> resolveBaseAnimationSet() {
        ArrayList<KeyframeAnimation> jumpAnimations = animationSet(ANIM_JUMP);
        if (jumping && !jumpAnimations.isEmpty()) {
            return jumpAnimations;
        }
        if (!playerMoving) {
            return animationSet(ANIM_IDLE);
        }
        if (playerMovingBackward) {
            return animationSet(ANIM_WALK_BACKWARD);
        }
        return playerRunning ? animationSet(ANIM_RUN) : animationSet(ANIM_WALK);
    }

    private ArrayList<KeyframeAnimation> animationSet(String key) {
        ArrayList<KeyframeAnimation> animations = animationSets.get(key);
        return animations != null ? animations : new ArrayList<KeyframeAnimation>();
    }

    private void triggerConfiguredEvent(String eventName, Runnable onEventStarted, Runnable onEventFinished) {
        BattleArenaCharacterDefinition.EventDefinition eventDefinition = eventDefinitions.get(eventName);
        if (eventDefinition == null || eventDefinition.play == null || eventDefinition.play.trim().isEmpty()) {
            return;
        }

        ArrayList<KeyframeAnimation> eventAnimationSet = animationSet(eventDefinition.play);
        if (eventAnimationSet.isEmpty()) {
            return;
        }

        if (onEventStarted != null) {
            onEventStarted.run();
        }
        setCurrentAnimationSet(
                eventAnimationSet,
                resolveFollowupAnimationSet(eventDefinition.then),
                onEventFinished);
    }

    private ArrayList<KeyframeAnimation> resolveFollowupAnimationSet(String followupKey) {
        if (followupKey == null || followupKey.trim().isEmpty() || THEN_RESUME_BASE.equals(followupKey)) {
            return resolveBaseAnimationSet();
        }
        return animationSet(followupKey);
    }

    private float clamp(float value) {
        return Math.max(-1f, Math.min(1f, value));
    }
}
