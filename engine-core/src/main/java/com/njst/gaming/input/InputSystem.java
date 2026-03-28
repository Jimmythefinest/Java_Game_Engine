package com.njst.gaming.input;

import java.util.HashMap;
import java.util.Map;

public class InputSystem {
    private final Map<String, ButtonState> buttons = new HashMap<>();
    private final Map<String, PointerState> pointers = new HashMap<>();

    public void beginFrame() {
        for (ButtonState button : buttons.values()) {
            button.beginFrame();
        }
        for (PointerState pointer : pointers.values()) {
            pointer.beginFrame();
        }
    }

    public ButtonState button(String actionId) {
        if (actionId == null || actionId.isEmpty()) {
            throw new IllegalArgumentException("Action id must not be empty.");
        }
        return buttons.computeIfAbsent(actionId, ignored -> new ButtonState());
    }

    public ButtonState button(int buttonCode) {
        return button(Integer.toString(buttonCode));
    }

    public PointerState pointer(String pointerId) {
        if (pointerId == null || pointerId.isEmpty()) {
            throw new IllegalArgumentException("Pointer id must not be empty.");
        }
        return pointers.computeIfAbsent(pointerId, ignored -> new PointerState());
    }
}
