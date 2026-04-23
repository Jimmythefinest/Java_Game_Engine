package com.njst.gaming.ri.battlearena;

import com.njst.gaming.input.ActionInput;
import com.njst.gaming.input.PointerState;

final class BattleArenaCharacterControlState {
    private static final float MOVE_DEADZONE = 0.12f;

    float forwardInput;
    float turnInput;
    boolean runDown;
    boolean jumpPressed;
    boolean punchPressed;
    boolean kickPressed;
    boolean castFireballPressed;
    boolean castMudWallPressed;
    boolean stepLeftPressed;
    boolean stepRightPressed;

    void clear() {
        forwardInput = 0f;
        turnInput = 0f;
        runDown = false;
        jumpPressed = false;
        punchPressed = false;
        kickPressed = false;
        castFireballPressed = false;
        castMudWallPressed = false;
        stepLeftPressed = false;
        stepRightPressed = false;
    }

    void capturePlayerInput(ActionInput actions, PointerState movementPointer) {
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
        stepLeftPressed = actions.button(BattleArenaActions.STEP_LEFT).pressed();
        stepRightPressed = actions.button(BattleArenaActions.STEP_RIGHT).pressed();
        forwardInput = clamp(forwardInput);
        turnInput = clamp(turnInput);
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
