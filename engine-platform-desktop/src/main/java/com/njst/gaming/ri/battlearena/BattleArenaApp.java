package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Engine;
import com.njst.gaming.input.InputBindings;
import com.njst.gaming.*;

import com.njst.gaming.input.MouseButtons;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;

public class BattleArenaApp extends Engine {
    private final BattleArenaDemoLoader loader = new BattleArenaDemoLoader();

    public BattleArenaApp() {
        this.title = "Battle Arena";
    }

    @Override
    protected void configureInputBindings(InputBindings bindings) {
        bindings.bindKey(GLFW_KEY_W, BattleArenaActions.FORWARD);
        bindings.bindKey(GLFW_KEY_S, BattleArenaActions.BACKWARD);
        bindings.bindKey(GLFW_KEY_A, BattleArenaActions.ROTATE);
        bindings.bindKey(GLFW_KEY_D, BattleArenaActions.TURN_LEFT );
        bindings.bindKey(GLFW_KEY_SPACE, BattleArenaActions.JUMP);
        bindings.bindMouseButton(MouseButtons.LEFT, BattleArenaActions.LOOK);
        bindings.bindMousePointer(BattleArenaActions.LOOK_POINTER);
    }

    @Override
    protected void onInit() {
        scene.loader =loader;
    }

    @Override
    protected void onKey(int key, int action) {
    }

    public static void main(String[] args) {
        new BattleArenaApp().run();
    }
}
