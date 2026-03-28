package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Scene;
import com.njst.gaming.data;
import com.njst.gaming.objects.GameObject;

import java.io.File;

public class BattleArenaDemoLoader implements Scene.SceneLoader {
    private static final String SKYBOX_FILE = "desertstorm.jpg";

    @Override
    public void load(Scene scene) {
        int skyboxTexture = scene.renderer.getGraphicsDevice().loadTexture(resolveTexturePath(SKYBOX_FILE));

        GameObject skybox = new GameObject(new SphereGeometry(1f, 20, 20), skyboxTexture);
        skybox.ambientlight_multiplier = 5f;
        skybox.shininess = 1f;
        skybox.setScale(100f, 100f, 100f);
        skybox.setPosition(0f, 0f, 0f);
        scene.renderer.skybox = skybox;
        scene.addGameObject(skybox);
    }

    private String resolveTexturePath(String fileName) {
        File desktopResource = new File(data.rootDirectory, fileName);
        if (desktopResource.isFile()) {
            return desktopResource.getPath();
        }
        return fileName;
    }
}
