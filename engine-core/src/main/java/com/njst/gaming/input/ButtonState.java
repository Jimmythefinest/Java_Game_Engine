package com.njst.gaming.input;

public class ButtonState {
    private boolean down;
    private boolean pressedThisFrame;
    private boolean releasedThisFrame;

    public void beginFrame() {
        pressedThisFrame = false;
        releasedThisFrame = false;
    }

    public void setDown(boolean down) {
        if (this.down == down) {
            return;
        }
        this.down = down;
        if (down) {
            pressedThisFrame = true;
        } else {
            releasedThisFrame = true;
        }
    }

    public boolean isDown() {
        return down;
    }

    public boolean pressed() {
        return pressedThisFrame;
    }

    public boolean released() {
        return releasedThisFrame;
    }
}
