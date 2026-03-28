package com.njst.gaming.input;

public final class ActionInput {
    private final InputSystem inputSystem;

    public ActionInput(InputSystem inputSystem) {
        this.inputSystem = inputSystem;
    }

    public ButtonState button(String actionId) {
        return inputSystem.button(actionId);
    }

    public boolean isDown(String actionId) {
        return button(actionId).isDown();
    }
}
