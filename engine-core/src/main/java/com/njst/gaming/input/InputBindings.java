package com.njst.gaming.input;

import java.util.HashMap;
import java.util.Map;

public class InputBindings {
    private final Map<Integer, Integer> keyBindings = new HashMap<>();
    private final Map<Integer, Integer> mouseBindings = new HashMap<>();

    public void bindKey(int keyCode, int buttonCode) {
        keyBindings.put(keyCode, buttonCode);
    }

    public void bindMouseButton(int mouseButton, int buttonCode) {
        mouseBindings.put(mouseButton, buttonCode);
    }

    public int resolveKey(int keyCode) {
        return keyBindings.getOrDefault(keyCode, -1);
    }

    public int resolveMouseButton(int mouseButton) {
        return mouseBindings.getOrDefault(mouseButton, -1);
    }
}
