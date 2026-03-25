package com.rebuild;

import com.njst.gaming.Engine;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Loaders.OpenWorldLoader;
import com.njst.gaming.Utils.ScreenshotUtil;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_P;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Y;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Z;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

/**
 * Desktop entry point for the open world demo.
 */
public class RotatingCube extends Engine {
    private final OpenWorldLoader openWorldLoader = new OpenWorldLoader();

    public RotatingCube() {
        this.title = "Open World Demo";
    }

    @Override
    protected void onInit() {
        System.out.println("Initializing Open World Demo");
        scene.loader = openWorldLoader;
    }

    @Override
    protected void onKey(int key, int action) {
        switch (key) {
            case GLFW_KEY_Z:
                if (action == GLFW_PRESS) {
                    scene.speed *= 10;
                }
                if (action == GLFW_RELEASE) {
                    scene.speed /= 10;
                }
                break;
            case GLFW_KEY_Y:
                if (action == GLFW_PRESS) {
                    scene.renderer.camera.targetPosition = new Vector3();
                }
                break;
            case GLFW_KEY_P:
                if (action == GLFW_PRESS) {
                    ScreenshotUtil.takeScreenshot(width, height);
                }
                break;
            default:
                if (scene.actions.containsKey(key) && action == GLFW_PRESS) {
                    scene.actions.get(key).run();
                }
                break;
        }
    }

    public static void main(String[] args) {
        new RotatingCube().run();
    }
}
