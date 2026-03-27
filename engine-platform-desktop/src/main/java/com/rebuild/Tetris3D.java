package com.rebuild;

import com.njst.gaming.Engine;
import com.njst.gaming.TetraLoader;

public class Tetris3D extends Engine {
    private final TetraLoader tetrisLoader = new TetraLoader();

    public Tetris3D() {
        this.title = "3D Tetris";
    }

    @Override
    protected void onInit() {
        scene.loader = tetrisLoader;
    }

    @Override
    protected void onKey(int key, int action) {
    }

    public static void main(String[] args) {
        new Tetris3D().run();
    }
}
