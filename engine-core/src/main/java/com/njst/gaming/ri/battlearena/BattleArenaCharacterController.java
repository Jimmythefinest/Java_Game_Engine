package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.input.ActionInput;
import com.njst.gaming.input.PointerState;

import java.util.ArrayList;

final class BattleArenaCharacterController {
    private static final float MOVE_DEADZONE = 0.12f;
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
    private ArrayList<KeyframeAnimation> idleAnimations = new ArrayList<>();
    private ArrayList<KeyframeAnimation> walkAnimations = new ArrayList<>();
    private ArrayList<KeyframeAnimation> walkBackwardAnimations = new ArrayList<>();
    private ArrayList<KeyframeAnimation> runAnimations = new ArrayList<>();
    private ArrayList<KeyframeAnimation> jumpAnimations = new ArrayList<>();
    private ArrayList<KeyframeAnimation> currentAnimationSet = null;
    private TerrainHeightSampler terrainHeightSampler = (x, z) -> 0f;
    private boolean playerMoving = false;
    private boolean playerMovingBackward = false;
    private boolean playerRunning = false;
    private float jumpHeight = 0f;
    private float verticalVelocity = 0f;
    private boolean jumping = false;

    void configureAnimationSets(ArrayList<KeyframeAnimation> idleAnimations,
                                ArrayList<KeyframeAnimation> walkAnimations,
                                ArrayList<KeyframeAnimation> walkBackwardAnimations,
                                ArrayList<KeyframeAnimation> runAnimations,
                                ArrayList<KeyframeAnimation> jumpAnimations) {
        this.idleAnimations = idleAnimations;
        this.walkAnimations = walkAnimations;
        this.walkBackwardAnimations = walkBackwardAnimations;
        this.runAnimations = runAnimations;
        this.jumpAnimations = jumpAnimations;
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
    }

    void update(ActionInput actions, PointerState movementPointer, float sceneSpeed) {
        float forwardInput = 0f;
        float turnInput = 0f;

        if (movementPointer.isActive()) {
            forwardInput += -applyDeadzone(movementPointer.getY());
            turnInput += applyDeadzone(movementPointer.getX());
        }
        if (actions.button(BattleArenaActions.FORWARD).isDown()) {
            forwardInput += 1f;
        }
        if (actions.button(BattleArenaActions.BACKWARD).isDown()) {
            forwardInput -= 1f;
        }
        if (actions.button(BattleArenaActions.TURN_LEFT).isDown()) {
            turnInput -= 1f;
        }
        if (actions.button(BattleArenaActions.ROTATE).isDown()) {
            turnInput += 1f;
        }

        if (actions.button(BattleArenaActions.JUMP).pressed() && !jumping) {
            jumping = true;
            verticalVelocity = JUMP_VELOCITY * sceneSpeed;
            updateMovementAnimationState();
        }

        forwardInput = clamp(forwardInput);
        turnInput = clamp(turnInput);
        playerHeadingDegrees += turnInput * TURN_SPEED_DEGREES * sceneSpeed;

        boolean wantsToRun = actions.button(BattleArenaActions.RUN).isDown();
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

    private void updateMovementAnimationState() {
        if (jumping) {
            if (!jumpAnimations.isEmpty()) {
                setCurrentAnimationSet(jumpAnimations);
            }
            return;
        }
        if (!playerMoving) {
            setCurrentAnimationSet(idleAnimations);
            return;
        }
        if (playerMovingBackward) {
            setCurrentAnimationSet(walkBackwardAnimations);
            return;
        }
        setCurrentAnimationSet(playerRunning ? runAnimations : walkAnimations);
    }

    private float applyDeadzone(float value) {
        if (Math.abs(value) < MOVE_DEADZONE) {
            return 0f;
        }
        return clamp(value);
    }

    private float clamp(float value) {
        return Math.max(-1f, Math.min(1f, value));
    }
}
