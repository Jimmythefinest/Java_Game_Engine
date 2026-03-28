package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Engine;
import com.njst.gaming.ri.battlearena.BattleArenaDemoLoader;

public class BattleArenaApp extends Engine {
    private final BattleArenaDemoLoader loader = new BattleArenaDemoLoader();

    public BattleArenaApp() {
        this.title = "Battle Arena";
    }

    @Override
    protected void onInit() {
        scene.loader = loader;
    }

    @Override
    protected void onKey(int key, int action) {
    }

    public static void main(String[] args) {
        new BattleArenaApp().run();
    }
}
