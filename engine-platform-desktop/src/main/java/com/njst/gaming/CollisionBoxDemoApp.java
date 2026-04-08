package com.njst.gaming;

import com.njst.gaming.Loaders.CollisionBoxRuntimeDemoLoader;

public class CollisionBoxDemoApp extends Engine {
    private final CollisionBoxRuntimeDemoLoader loader = new CollisionBoxRuntimeDemoLoader();

    public CollisionBoxDemoApp() {
        title = "Collision Box Demo";
    }

    @Override
    protected void onKey(int key, int action) {
    }

    @Override
    protected void onInit() {
        scene.loader = loader;
        renderer.camera.cameraPosition.set(0f, 1.5f, -8f);
        renderer.camera.targetPosition.set(0f, 0f, 0f);
    }

    public static void main(String[] args) {
        new CollisionBoxDemoApp().run();
    }
}
