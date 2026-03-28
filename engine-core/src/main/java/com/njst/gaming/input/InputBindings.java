package com.njst.gaming.input;

import java.util.HashMap;
import java.util.Map;

public class InputBindings {
    private final Map<Integer, String> keyBindings = new HashMap<>();
    private final Map<Integer, String> mouseBindings = new HashMap<>();
    private String mousePointerId;

    public void bindKey(int keyCode, String actionId) {
        keyBindings.put(keyCode, actionId);
    }

    public void bindKey(int keyCode, int buttonCode) {
        bindKey(keyCode, Integer.toString(buttonCode));
    }

    public void bindMouseButton(int mouseButton, String actionId) {
        mouseBindings.put(mouseButton, actionId);
    }

    public void bindMouseButton(int mouseButton, int buttonCode) {
        bindMouseButton(mouseButton, Integer.toString(buttonCode));
    }

    public void bindMousePointer(String pointerId) {
        mousePointerId = pointerId;
    }

    public String resolveKeyAction(int keyCode) {
        return keyBindings.get(keyCode);
    }

    public String resolveMouseButtonAction(int mouseButton) {
        return mouseBindings.get(mouseButton);
    }

    public String resolveMousePointer() {
        return mousePointerId;
    }
}
