package com.rebuild;

import com.njst.gaming.Engine;
import com.njst.gaming.Loaders.BipedDesignerLoader;

public class BipedDesignerDemo extends Engine {
    private final BipedDesignerLoader loader = new BipedDesignerLoader();

    public BipedDesignerDemo() {
        this.title = "Biped Designer";
    }

    @Override
    protected void onInit() {
        scene.loader = loader;
    }

    @Override
    protected void onKey(int key, int action) {
    }

    public static void main(String[] args) {
        new BipedDesignerDemo().run();
    }
}
