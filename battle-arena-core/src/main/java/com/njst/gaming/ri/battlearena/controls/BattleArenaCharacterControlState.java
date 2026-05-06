package com.njst.gaming.ri.battlearena.controls;

import com.njst.gaming.input.ActionInput;
import com.njst.gaming.input.PointerState;

public final class BattleArenaCharacterControlState {
    private static final float MOVE_DEADZONE = 0.12f;

    public float forwardInput;
    public float turnInput;
    public boolean runDown;
    public boolean jumpPressed;
    public boolean punchPressed;
    public boolean kickPressed;
    public boolean castFireballPressed;
    public boolean castMudWallPressed;
    public boolean guCreateWaterPressed;
    public boolean guCreateIcePressed;
    public boolean guCreateEarthPressed;
    public boolean guShapeSpearPressed;
    public boolean guShapeWallPressed;
    public boolean guCoolPressed;
    public boolean guHeatPressed;
    public boolean guMoveForwardPressed;
    public boolean guMoveForwardDown;
    public boolean stepLeftPressed;
    public boolean stepRightPressed;
    public boolean burstPressed;

    public void clear() {
        forwardInput = 0f;
        turnInput = 0f;
        runDown = false;
        jumpPressed = false;
        punchPressed = false;
        kickPressed = false;
        castFireballPressed = false;
        castMudWallPressed = false;
        guCreateWaterPressed = false;
        guCreateIcePressed = false;
        guCreateEarthPressed = false;
        guShapeSpearPressed = false;
        guShapeWallPressed = false;
        guCoolPressed = false;
        guHeatPressed = false;
        guMoveForwardPressed = false;
        guMoveForwardDown = false;
        stepLeftPressed = false;
        stepRightPressed = false;
        burstPressed = false;
    }

    public void capturePlayerInput(ActionInput actions, PointerState movementPointer) {
        clear();
        if (movementPointer != null && movementPointer.isActive()) {
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
        runDown = actions.button(BattleArenaActions.RUN).isDown();
        jumpPressed = actions.button(BattleArenaActions.JUMP).pressed();
        punchPressed = actions.button(BattleArenaActions.PUNCH).pressed();
        kickPressed = actions.button(BattleArenaActions.KICK).pressed();
        castFireballPressed = actions.button(BattleArenaActions.FIREBALL).pressed();
        castMudWallPressed = actions.button(BattleArenaActions.MUD_WALL).pressed();
        guCreateWaterPressed = actions.button(BattleArenaActions.CREATE_WATER).pressed();
        guCreateIcePressed = actions.button(BattleArenaActions.CREATE_ICE).pressed();
        guCreateEarthPressed = actions.button(BattleArenaActions.CREATE_EARTH).pressed();
        guShapeSpearPressed = actions.button(BattleArenaActions.SHAPE_SPEAR).pressed();
        guShapeWallPressed = actions.button(BattleArenaActions.SHAPE_WALL).pressed();
        guCoolPressed = actions.button(BattleArenaActions.COOL).pressed();
        guHeatPressed = actions.button(BattleArenaActions.HEAT).pressed();
        guMoveForwardPressed = actions.button(BattleArenaActions.MOVE_FORWARD).pressed();
        guMoveForwardDown = actions.button(BattleArenaActions.MOVE_FORWARD).isDown();
        stepLeftPressed = actions.button(BattleArenaActions.STEP_LEFT).pressed();
        stepRightPressed = actions.button(BattleArenaActions.STEP_RIGHT).pressed();
        burstPressed = actions.button(BattleArenaActions.BURST).pressed();
        forwardInput = clamp(forwardInput);
        turnInput = clamp(turnInput);
    }

    public void copyFrom(BattleArenaCharacterControlState other) {
        if (other == null) {
            clear();
            return;
        }
        forwardInput = other.forwardInput;
        turnInput = other.turnInput;
        runDown = other.runDown;
        jumpPressed = other.jumpPressed;
        punchPressed = other.punchPressed;
        kickPressed = other.kickPressed;
        castFireballPressed = other.castFireballPressed;
        castMudWallPressed = other.castMudWallPressed;
        guCreateWaterPressed = other.guCreateWaterPressed;
        guCreateIcePressed = other.guCreateIcePressed;
        guCreateEarthPressed = other.guCreateEarthPressed;
        guShapeSpearPressed = other.guShapeSpearPressed;
        guShapeWallPressed = other.guShapeWallPressed;
        guCoolPressed = other.guCoolPressed;
        guHeatPressed = other.guHeatPressed;
        guMoveForwardPressed = other.guMoveForwardPressed;
        guMoveForwardDown = other.guMoveForwardDown;
        stepLeftPressed = other.stepLeftPressed;
        stepRightPressed = other.stepRightPressed;
        burstPressed = other.burstPressed;
    }

    public void mergePressedEdgesFrom(BattleArenaCharacterControlState other) {
        if (other == null) {
            return;
        }
        jumpPressed |= other.jumpPressed;
        punchPressed |= other.punchPressed;
        kickPressed |= other.kickPressed;
        castFireballPressed |= other.castFireballPressed;
        castMudWallPressed |= other.castMudWallPressed;
        guCreateWaterPressed |= other.guCreateWaterPressed;
        guCreateIcePressed |= other.guCreateIcePressed;
        guCreateEarthPressed |= other.guCreateEarthPressed;
        guShapeSpearPressed |= other.guShapeSpearPressed;
        guShapeWallPressed |= other.guShapeWallPressed;
        guCoolPressed |= other.guCoolPressed;
        guHeatPressed |= other.guHeatPressed;
        guMoveForwardPressed |= other.guMoveForwardPressed;
        guMoveForwardDown |= other.guMoveForwardDown;
        stepLeftPressed |= other.stepLeftPressed;
        stepRightPressed |= other.stepRightPressed;
        burstPressed |= other.burstPressed;
    }

    public void clearPressedEdges() {
        jumpPressed = false;
        punchPressed = false;
        kickPressed = false;
        castFireballPressed = false;
        castMudWallPressed = false;
        guCreateWaterPressed = false;
        guCreateIcePressed = false;
        guCreateEarthPressed = false;
        guShapeSpearPressed = false;
        guShapeWallPressed = false;
        guCoolPressed = false;
        guHeatPressed = false;
        guMoveForwardPressed = false;
        guMoveForwardDown = false;
        stepLeftPressed = false;
        stepRightPressed = false;
        burstPressed = false;
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
