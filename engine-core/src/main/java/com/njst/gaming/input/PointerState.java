package com.njst.gaming.input;

/**
 * Tracks the state of a continuous pointer (e.g. mouse cursor or touch point).
 * Manages absolute coordinates as well as frame-to-frame delta movements.
 */
public class PointerState {
    private float x;
    private float y;
    private float deltaX;
    private float deltaY;
    private boolean active;
    private boolean hasSample;

    public void beginFrame() {
        deltaX = 0f;
        deltaY = 0f;
    }

    public void setPosition(float x, float y) {
        if (hasSample) {
            deltaX += x - this.x;
            deltaY += y - this.y;
        }
        this.x = x;
        this.y = y;
        hasSample = true;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getDeltaX() {
        return deltaX;
    }

    public float getDeltaY() {
        return deltaY;
    }

    public boolean isActive() {
        return active;
    }
}
