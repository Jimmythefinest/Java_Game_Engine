package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Engine;
import com.njst.gaming.Scene;
import com.njst.gaming.input.InputBindings;

public final class BattleArenaGpuSkinningDemoApp extends Engine {
    public BattleArenaGpuSkinningDemoApp() {
        title = "Battle Arena GPU Skinning Probe";
    }

    @Override
    protected void configureInputBindings(InputBindings bindings) {
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
