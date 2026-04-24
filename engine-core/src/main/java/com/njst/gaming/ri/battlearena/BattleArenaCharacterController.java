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
    static final String EVENT_CAST_STARTED = "cast_started";
    static final String EVENT_STEP_LEFT_STARTED = "step_left_started";
    static final String EVENT_STEP_RIGHT_STARTED = "step_right_started";
    static final String EVENT_HIT_TAKEN = "hit_taken";
    static final String THEN_RESUME_BASE = "resume_base";

    static final String ANIM_IDLE = "idle";
    static final String ANIM_WALK = "walk";
    static final String ANIM_WALK_BACKWARD = "walk_backward";
    static final String ANIM_RUN = "run";
    static final String ANIM_JUMP = "jump";
    static final String ANIM_PUNCH = "punch";
    static final String ANIM_KICK = "kick";
    static final String ANIM_CAST = "cast";
    static final String ANIM_LEFTSIDE_STEP = "leftside_step";
    static final String ANIM_RIGHTSIDE_STEP = "rightside_step";
    static final String ANIM_TAKE_HIT = "take_hit";

    private static final float WALK_SPEED = 0.02f;
    private static final float RUN_SPEED = 0.035f;
    private static final float TURN_SPEED_DEGREES = 3.2f;
    private static final float JUMP_VELOCITY = 0.22f;
    private static final float JUMP_GRAVITY = 0.012f;
    private static final float RECOIL_DAMPING = 0.82f;
    private static final float RECOIL_STOP_SPEED = 0.0005f;
    private static final float SIDE_STEP_DISTANCE = 0.75f;
    private static final float PUNCH_LUNGE_DISTANCE = 0.2f;
    private static final float PUNCH_LUNGE_DURATION_SECONDS = 0.1f;
    private static final float FORWARD_CHARGE_DISTANCE = 2.8f;
    private static final float BACKWARD_SHADOW_STEP_DISTANCE = 2.2f;
    private static final float CHARGE_DURATION_SECONDS = 0.18f;
    private static final float BURST_DIRECTION_THRESHOLD = 0.15f;

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
    private boolean casting = false;
    private boolean sideSteppingLeft = false;
    private boolean sideSteppingRight = false;
    private boolean takingHit = false;
    private final Vector3 recoilVelocity = new Vector3();
    private float sideStepVelocity = 0f;
    private float sideStepRemainingSeconds = 0f;
    private float forwardLungeVelocity = 0f;
    private float forwardLungeRemainingSeconds = 0f;
    private final BattleArenaCharacterControlState playerControls = new BattleArenaCharacterControlState();
    private ArrayList<KeyframeAnimation> activeAnimations = new ArrayList<>();

    void configureCharacterData(Map<String, ArrayList<KeyframeAnimation>> animationSets,
                                Map<String, BattleArenaCharacterDefinition.EventDefinition> eventDefinitions,
                                ArrayList<KeyframeAnimation> activeAnimations) {
        this.animationSets = animationSets != null ? new HashMap<>(animationSets) : new HashMap<String, ArrayList<KeyframeAnimation>>();
        this.eventDefinitions = eventDefinitions != null
                ? new HashMap<String, BattleArenaCharacterDefinition.EventDefinition>(eventDefinitions)
                : new HashMap<String, BattleArenaCharacterDefinition.EventDefinition>();
        this.activeAnimations = activeAnimations != null ? activeAnimations : new ArrayList<KeyframeAnimation>();
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
        casting = false;
        sideSteppingLeft = false;
        sideSteppingRight = false;
        takingHit = false;
        recoilVelocity.set(0f, 0f, 0f);
        sideStepVelocity = 0f;
        sideStepRemainingSeconds = 0f;
        forwardLungeVelocity = 0f;
        forwardLungeRemainingSeconds = 0f;
    }

    void update(ActionInput actions, PointerState movementPointer, float sceneSpeed) {
        playerControls.capturePlayerInput(actions, movementPointer);
        update(playerControls, sceneSpeed);
    }

    void update(BattleArenaCharacterControlState controls, float sceneSpeed) {
        if (controls == null) {
            return;
        }

        float forwardInput = clamp(controls.forwardInput);
        float turnInput = clamp(controls.turnInput);
        boolean combatActionActive = punching || kicking || casting || takingHit;
        boolean chargeTriggered = tryStartDirectionalCharge(forwardInput, controls.burstPressed, combatActionActive);
        boolean chargeActive = isChargeActive();

        if (controls.punchPressed && !combatActionActive && !animationSet(ANIM_PUNCH).isEmpty()) {
            triggerConfiguredEvent(
                    EVENT_PUNCH_STARTED,
                    () -> {
                        punching = true;
                        startForwardLunge(PUNCH_LUNGE_DISTANCE, PUNCH_LUNGE_DURATION_SECONDS);
                    },
                    this::finishPunch);
        }

        if (controls.kickPressed && !combatActionActive && !animationSet(ANIM_KICK).isEmpty()) {
            triggerConfiguredEvent(
                    EVENT_KICK_STARTED,
                    () -> kicking = true,
                    this::finishKick);
        }

        if (controls.castFireballPressed && !combatActionActive && !animationSet(ANIM_CAST).isEmpty()) {
            triggerConfiguredEvent(
                    EVENT_CAST_STARTED,
                    () -> casting = true,
                    this::finishCast);
        }

        boolean sideStepActive = sideSteppingLeft || sideSteppingRight;
        if (controls.stepLeftPressed && !sideStepActive && !animationSet(ANIM_LEFTSIDE_STEP).isEmpty()) {
            startSideStepLeft();
        }
        if (controls.stepRightPressed && !sideStepActive && !animationSet(ANIM_RIGHTSIDE_STEP).isEmpty()) {
            startSideStepRight();
        }

        boolean locomotionAnimationBlocked = punching || kicking || casting || sideSteppingLeft || sideSteppingRight || takingHit;
        float animationForwardInput = chargeActive ? Math.signum(forwardLungeVelocity) : forwardInput;
        boolean animationRunDown = controls.runDown || (chargeActive && forwardLungeVelocity > 0f);
        updateMovementIntent(animationForwardInput, animationRunDown, sceneSpeed, !locomotionAnimationBlocked);

        if (controls.jumpPressed && !jumping) {
            jumping = true;
            verticalVelocity = JUMP_VELOCITY * sceneSpeed;
            updateMovementAnimationState();
        }

        if (!takingHit) {
            playerHeadingDegrees += turnInput * TURN_SPEED_DEGREES * sceneSpeed;
        }

        boolean locomotionBlocked = sideSteppingLeft || sideSteppingRight || takingHit || chargeActive || chargeTriggered;

        boolean wantsToRun = controls.runDown;
        float movementSpeed = wantsToRun ? RUN_SPEED : WALK_SPEED;
        float moveAmount = forwardInput * movementSpeed * sceneSpeed;
        boolean isMovingNow = Math.abs(moveAmount) > 0.0001f;

        if (!locomotionBlocked && isMovingNow) {
            float headingRadians = (float) Math.toRadians(playerHeadingDegrees);
            playerPosition.x += (float) Math.sin(headingRadians) * moveAmount;
            playerPosition.z += (float) Math.cos(headingRadians) * moveAmount;
        }

        updateSideStepMotion(sceneSpeed);
        updateForwardLungeMotion(sceneSpeed);
        applyRecoilMotion(sceneSpeed);
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

    boolean isCasting() {
        return casting;
    }

    boolean isSideSteppingLeft() {
        return sideSteppingLeft;
    }

    boolean isSideSteppingRight() {
        return sideSteppingRight;
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

    void applyHitRecoil(Vector3 direction, float strength) {
        if (direction == null || strength <= 0f) {
            return;
        }
        Vector3 horizontalDirection = new Vector3(direction.x, 0f, direction.z);
        float length = horizontalDirection.length();
        if (length <= 0.0001f) {
            return;
        }
        horizontalDirection.mul(1f / length);
        recoilVelocity.set(
                horizontalDirection.x * strength,
                0f,
                horizontalDirection.z * strength);
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

    private void applyRecoilMotion(float sceneSpeed) {
        if (Math.abs(recoilVelocity.x) <= RECOIL_STOP_SPEED && Math.abs(recoilVelocity.z) <= RECOIL_STOP_SPEED) {
            recoilVelocity.set(0f, 0f, 0f);
            return;
        }
        playerPosition.x += recoilVelocity.x * sceneSpeed;
        playerPosition.z += recoilVelocity.z * sceneSpeed;
        float damping = (float) Math.pow(RECOIL_DAMPING, Math.max(sceneSpeed, 1f));
        recoilVelocity.mul(damping);
    }

    private void updateSideStepMotion(float sceneSpeed) {
        if (!sideSteppingLeft && !sideSteppingRight) {
            sideStepVelocity = 0f;
            sideStepRemainingSeconds = 0f;
            return;
        }
        float frameSeconds = frameSeconds(sceneSpeed);
        if (frameSeconds <= 0f || sideStepRemainingSeconds <= 0f) {
            finishSideStep();
            return;
        }
        float appliedSeconds = Math.min(frameSeconds, sideStepRemainingSeconds);
        float headingRadians = (float) Math.toRadians(playerHeadingDegrees);
        float lateralDirection = sideSteppingLeft ? -1f : 1f;
        float sideX = lateralDirection * -(float) Math.cos(headingRadians);
        float sideZ = lateralDirection * (float) Math.sin(headingRadians);
        playerPosition.x += sideX * sideStepVelocity * appliedSeconds;
        playerPosition.z += sideZ * sideStepVelocity * appliedSeconds;
        sideStepRemainingSeconds -= appliedSeconds;
        if (sideStepRemainingSeconds <= 0f) {
            finishSideStep();
        }
    }

    private void updateForwardLungeMotion(float sceneSpeed) {
        float frameSeconds = frameSeconds(sceneSpeed);
        if (frameSeconds <= 0f || forwardLungeRemainingSeconds <= 0f || Math.abs(forwardLungeVelocity) <= 0f) {
            forwardLungeVelocity = 0f;
            forwardLungeRemainingSeconds = 0f;
            return;
        }
        float appliedSeconds = Math.min(frameSeconds, forwardLungeRemainingSeconds);
        float headingRadians = (float) Math.toRadians(playerHeadingDegrees);
        playerPosition.x += (float) Math.sin(headingRadians) * forwardLungeVelocity * appliedSeconds;
        playerPosition.z += (float) Math.cos(headingRadians) * forwardLungeVelocity * appliedSeconds;
        forwardLungeRemainingSeconds -= appliedSeconds;
        if (forwardLungeRemainingSeconds <= 0f) {
            forwardLungeVelocity = 0f;
            forwardLungeRemainingSeconds = 0f;
        }
    }

    private void setCurrentAnimationSet(ArrayList<KeyframeAnimation> nextAnimationSet) {
        if (nextAnimationSet == null || nextAnimationSet.isEmpty() || currentAnimationSet == nextAnimationSet) {
            return;
        }
        if (currentAnimationSet != null) {
            clearLatchedStateForAnimationSet(currentAnimationSet);
            for (KeyframeAnimation animation : currentAnimationSet) {
                animation.stop();
                animation.time = 0f;
                unregisterActiveAnimation(animation);
            }
        }
        for (KeyframeAnimation animation : nextAnimationSet) {
            animation.time = 0f;
            animation.start();
            registerActiveAnimation(animation);
        }
        currentAnimationSet = nextAnimationSet;
    }

    private void clearLatchedStateForAnimationSet(ArrayList<KeyframeAnimation> animationSet) {
        if (animationSet == null || animationSet.isEmpty()) {
            return;
        }
        if (animationSet == animationSets.get(ANIM_PUNCH)) {
            finishPunch();
            return;
        }
        if (animationSet == animationSets.get(ANIM_KICK)) {
            kicking = false;
            return;
        }
        if (animationSet == animationSets.get(ANIM_CAST)) {
            finishCast();
            return;
        }
        if (animationSet == animationSets.get(ANIM_LEFTSIDE_STEP)) {
            finishSideStep();
            return;
        }
        if (animationSet == animationSets.get(ANIM_RIGHTSIDE_STEP)) {
            finishSideStep();
            return;
        }
        if (animationSet == animationSets.get(ANIM_TAKE_HIT)) {
            takingHit = false;
        }
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
        ArrayList<KeyframeAnimation> castAnimations = animationSet(ANIM_CAST);
        ArrayList<KeyframeAnimation> sideStepAnimations = animationSet(ANIM_LEFTSIDE_STEP);
        ArrayList<KeyframeAnimation> rightSideStepAnimations = animationSet(ANIM_RIGHTSIDE_STEP);
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
        if (casting) {
            setCurrentAnimationSet(castAnimations);
            return;
        }
        if (sideSteppingLeft) {
            setCurrentAnimationSet(sideStepAnimations);
            return;
        }
        if (sideSteppingRight) {
            setCurrentAnimationSet(rightSideStepAnimations);
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
        forwardLungeVelocity = 0f;
        forwardLungeRemainingSeconds = 0f;
    }

    private void finishKick() {
        kicking = false;
    }

    private void finishCast() {
        casting = false;
    }

    private void finishSideStep() {
        sideSteppingLeft = false;
        sideSteppingRight = false;
        sideStepVelocity = 0f;
        sideStepRemainingSeconds = 0f;
    }

    private void finishHitReact() {
        takingHit = false;
    }

    private void startForwardLunge(float distance, float durationSeconds) {
        if (distance == 0f || durationSeconds <= 0f) {
            forwardLungeVelocity = 0f;
            forwardLungeRemainingSeconds = 0f;
            return;
        }
        forwardLungeRemainingSeconds = durationSeconds;
        forwardLungeVelocity = distance / durationSeconds;
    }

    private boolean tryStartDirectionalCharge(float forwardInput, boolean burstPressed, boolean combatActionActive) {
        if (!burstPressed || combatActionActive || sideSteppingLeft || sideSteppingRight || takingHit) {
            return false;
        }
        float burstDistance = forwardInput <= -BURST_DIRECTION_THRESHOLD
                ? -BACKWARD_SHADOW_STEP_DISTANCE
                : FORWARD_CHARGE_DISTANCE;
        startForwardLunge(burstDistance, CHARGE_DURATION_SECONDS);
        return true;
    }

    private boolean isChargeActive() {
        return forwardLungeRemainingSeconds > 0f && Math.abs(forwardLungeVelocity) > 0f;
    }

    private float frameSeconds(float sceneSpeed) {
        return Math.max(sceneSpeed, 0f) / 60f;
    }

    private void onAnimationFinished(KeyframeAnimation finishedAnimation,
                                     ArrayList<KeyframeAnimation> sourceAnimationSet,
                                     ArrayList<KeyframeAnimation> nextAnimationSetOnFinish,
                                     Runnable onAnimationSetFinished) {
        finishedAnimation.stop();
        finishedAnimation.time = 0f;
        unregisterActiveAnimation(finishedAnimation);
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
            ArrayList<KeyframeAnimation> followup = nextAnimationSetOnFinish;
            ArrayList<KeyframeAnimation> baseAnimationSet = resolveBaseAnimationSet();
            if (followup == animationSet(ANIM_IDLE) && baseAnimationSet != followup) {
                followup = baseAnimationSet;
            }
            setCurrentAnimationSet(followup);
            return;
        }
        updateMovementAnimationState();
    }

    private ArrayList<KeyframeAnimation> resolveBaseAnimationSet() {
        ArrayList<KeyframeAnimation> jumpAnimations = animationSet(ANIM_JUMP);
        if (jumping && !jumpAnimations.isEmpty()) {
            return jumpAnimations;
        }
        if (sideSteppingLeft) {
            return animationSet(ANIM_LEFTSIDE_STEP);
        }
        if (sideSteppingRight) {
            return animationSet(ANIM_RIGHTSIDE_STEP);
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

    private void registerActiveAnimation(KeyframeAnimation animation) {
        if (animation == null || activeAnimations == null || activeAnimations.contains(animation)) {
            return;
        }
        activeAnimations.add(animation);
    }

    private void unregisterActiveAnimation(KeyframeAnimation animation) {
        if (animation == null || activeAnimations == null) {
            return;
        }
        activeAnimations.remove(animation);
    }

    private void startSideStepLeft() {
        startSideStep(
                animationSet(ANIM_LEFTSIDE_STEP),
                EVENT_STEP_LEFT_STARTED,
                true);
    }

    private void startSideStepRight() {
        startSideStep(
                animationSet(ANIM_RIGHTSIDE_STEP),
                EVENT_STEP_RIGHT_STARTED,
                false);
    }

    private void startSideStep(ArrayList<KeyframeAnimation> stepAnimations,
                               String eventName,
                               boolean stepLeft) {
        finishSideStep();
        if (stepAnimations.isEmpty()) {
            return;
        }
        float durationSeconds = animationDurationSeconds(stepAnimations);
        if (durationSeconds <= 0f) {
            return;
        }
        sideSteppingLeft = stepLeft;
        sideSteppingRight = !stepLeft;
        sideStepRemainingSeconds = durationSeconds;
        sideStepVelocity = SIDE_STEP_DISTANCE / durationSeconds;
        BattleArenaCharacterDefinition.EventDefinition eventDefinition = eventDefinitions.get(eventName);
        if (eventDefinition != null && eventDefinition.play != null && !eventDefinition.play.trim().isEmpty()) {
            triggerConfiguredEvent(
                    eventName,
                    null,
                    this::finishSideStep);
            return;
        }
        setCurrentAnimationSet(stepAnimations, animationSet(ANIM_IDLE), this::finishSideStep);
    }

    private float animationDurationSeconds(ArrayList<KeyframeAnimation> animations) {
        float maxDurationSeconds = 0f;
        for (KeyframeAnimation animation : animations) {
            if (animation == null) {
                continue;
            }
            float framesPerSecond = animation.framesPerSecond > 0f
                    ? animation.framesPerSecond
                    : BattleArenaCharacterDefinition.DEFAULT_ANIMATION_FPS;
            if (framesPerSecond <= 0f) {
                continue;
            }
            maxDurationSeconds = Math.max(maxDurationSeconds, animation.duration / framesPerSecond);
        }
        return maxDurationSeconds;
    }

    private ArrayList<KeyframeAnimation> resolveFollowupAnimationSet(String followupKey) {
        if (followupKey == null || followupKey.trim().isEmpty() || THEN_RESUME_BASE.equals(followupKey)) {
            return resolveBaseAnimationSet();
        }
        return animationSet(followupKey);
    }

    private void updateMovementIntent(float forwardInput,
                                      boolean wantsToRun,
                                      float sceneSpeed,
                                      boolean updateAnimationState) {
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
            if (updateAnimationState) {
                updateMovementAnimationState();
            }
        }
    }

    private float clamp(float value) {
        return Math.max(-1f, Math.min(1f, value));
    }
}
