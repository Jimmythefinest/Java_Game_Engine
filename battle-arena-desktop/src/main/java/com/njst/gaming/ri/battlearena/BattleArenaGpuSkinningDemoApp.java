package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Engine;
import com.njst.gaming.Scene;
import com.njst.gaming.input.InputBindings;
import com.njst.gaming.input.MouseButtons;
import com.njst.gaming.ri.battlearena.controls.BattleArenaActions;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_0;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_9;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_G;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Q;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SLASH;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_X;

public final class BattleArenaGpuSkinningDemoApp extends Engine {
    public BattleArenaGpuSkinningDemoApp() {
        title = "Battle Arena GPU Skinning Probe";
    }

    @Override
    protected void configureInputBindings(InputBindings bindings) {
        bindings.bindKey(GLFW_KEY_W, BattleArenaActions.FORWARD);
        bindings.bindKey(GLFW_KEY_S, BattleArenaActions.BACKWARD);
        bindings.bindKey(GLFW_KEY_A, BattleArenaActions.ROTATE);
        bindings.bindKey(GLFW_KEY_D, BattleArenaActions.TURN_LEFT);
        bindings.bindKey(GLFW_KEY_LEFT_SHIFT, BattleArenaActions.RUN);
        bindings.bindKey(GLFW_KEY_RIGHT_SHIFT, BattleArenaActions.RUN);
        bindings.bindKey(GLFW_KEY_SPACE, BattleArenaActions.JUMP);
        bindings.bindKey(GLFW_KEY_E, BattleArenaActions.PUNCH);
        bindings.bindKey(GLFW_KEY_SLASH, BattleArenaActions.FIREBALL);
        bindings.bindKey(GLFW_KEY_G, BattleArenaActions.MUD_WALL);
        bindings.bindKey(GLFW_KEY_Q, BattleArenaActions.KICK);
        bindings.bindKey(GLFW_KEY_X, BattleArenaActions.BURST);
        bindings.bindKey(GLFW_KEY_LEFT, BattleArenaActions.STEP_LEFT);
        bindings.bindKey(GLFW_KEY_RIGHT, BattleArenaActions.STEP_RIGHT);
        bindings.bindKey(GLFW_KEY_9, BattleArenaActions.SNAP);
        bindings.bindKey(GLFW_KEY_0, BattleArenaActions.TOGGLE_HITBOXES);
        bindings.bindMouseButton(MouseButtons.LEFT, BattleArenaActions.LOOK);
        bindings.bindMousePointer(BattleArenaActions.LOOK_POINTER);
    }

    @Override
    protected void onInit() {
        scene.loader = new BattleArenaGpuSkinningDemoLoader();
    }

    @Override
    protected void onKey(int key, int action) {
    }

    public static void main(String[] args) {
        new BattleArenaGpuSkinningDemoApp().run();
    }

    class ExampleLoader implements Scene.SceneLoader {
        @Override
        public void load(Scene scene) {
        }
    }
}
