package com.njst.gaming.input;

public class InputSystem {
    public final ButtonState[] buttons;
    public final PointerState pointer = new PointerState();

    public InputSystem() {
        buttons = new ButtonState[InputCodes.MAX_BUTTONS];
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = new ButtonState();
        }
    }

    public void beginFrame() {
        for (ButtonState button : buttons) {
            button.beginFrame();
        }
        pointer.beginFrame();
    }

    public ButtonState button(int buttonCode) {
        if (buttonCode < 0 || buttonCode >= buttons.length) {
            throw new IllegalArgumentException("Invalid button code: " + buttonCode);
        }
        return buttons[buttonCode];
    }
}
