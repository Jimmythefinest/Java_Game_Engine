package com.njst.gaming;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Handles keyboard and mouse input state.
 * Supports polling, edge detection, and cursor tracking.
 */
public class Input {
    private final boolean[] keys = new boolean[GLFW_KEY_LAST];
    private final boolean[] prevKeys = new boolean[GLFW_KEY_LAST];
    
    private final boolean[] mouseButtons = new boolean[GLFW_MOUSE_BUTTON_LAST];
    private final boolean[] prevMouseButtons = new boolean[GLFW_MOUSE_BUTTON_LAST];
    
    private double mouseX, mouseY;
    private double scrollX, scrollY;

    /**
     * Updates the previous frame's state. Should be called at the start of every frame.
     */
    public void update() {
        System.arraycopy(keys, 0, prevKeys, 0, GLFW_KEY_LAST);
        System.arraycopy(mouseButtons, 0, prevMouseButtons, 0, GLFW_MOUSE_BUTTON_LAST);
        scrollX = 0;
        scrollY = 0;
    }

    // Keyboard Methods

    public void setKey(int key, int action) {
        if (key >= 0 && key < GLFW_KEY_LAST) {
            keys[key] = (action != GLFW_RELEASE);
        }
    }

    /**
     * Checks if a key is currently being held down.
     * @param key The GLFW key code.
     * @return true if held, false otherwise.
     */
    public boolean isKeyDown(int key) {
        return key >= 0 && key < GLFW_KEY_LAST && keys[key];
    }

    /**
     * Checks if a key was pressed exactly this frame.
     * @param key The GLFW key code.
     * @return true if pressed this frame.
     */
    public boolean isKeyPressed(int key) {
        return key >= 0 && key < GLFW_KEY_LAST && keys[key] && !prevKeys[key];
    }

    /**
     * Checks if a key was released exactly this frame.
     * @param key The GLFW key code.
     * @return true if released this frame.
     */
    public boolean isKeyReleased(int key) {
        return key >= 0 && key < GLFW_KEY_LAST && !keys[key] && prevKeys[key];
    }

    // Mouse Methods

    public void setMouseButton(int button, int action) {
        if (button >= 0 && button < GLFW_MOUSE_BUTTON_LAST) {
            mouseButtons[button] = (action != GLFW_RELEASE);
        }
    }

    public boolean isMouseButtonDown(int button) {
        return button >= 0 && button < GLFW_MOUSE_BUTTON_LAST && mouseButtons[button];
    }

    public boolean isMouseButtonPressed(int button) {
        return button >= 0 && button < GLFW_MOUSE_BUTTON_LAST && mouseButtons[button] && !prevMouseButtons[button];
    }

    public void setMousePosition(double x, double y) {
        this.mouseX = x;
        this.mouseY = y;
    }

    public double getMouseX() { return mouseX; }
    public double getMouseY() { return mouseY; }

    public void setScroll(double xOffset, double yOffset) {
        this.scrollX = xOffset;
        this.scrollY = yOffset;
    }

    public double getScrollX() { return scrollX; }
    public double getScrollY() { return scrollY; }
}
