package com.njst.gaming.input;

import com.njst.gaming.Scene;

/**
 * Callback interface for responding to pointer (mouse/touch) movement events.
 */
public interface PointerInputHandler {
    void onPointerMoved(Scene scene, PointerState pointer);
}
