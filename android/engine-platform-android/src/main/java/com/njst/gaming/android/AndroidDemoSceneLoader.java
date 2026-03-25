package com.njst.gaming.android;

import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Scene;
import com.njst.gaming.objects.GameObject;

public class AndroidDemoSceneLoader implements Scene.SceneLoader {
    private GameObject demoCube;

    @Override
    public void load(Scene scene) {
        int texture = scene.renderer.getGraphicsDevice().loadTexture("generated:white");
        demoCube = new GameObject(new CubeGeometry(), texture);
        demoCube.setScale(1.5f, 1.5f, 1.5f);
        demoCube.setPosition(0f, 0f, 0f);
        scene.addGameObject(demoCube);
    }

    public GameObject getDemoCube() {
        return demoCube;
    }
}
