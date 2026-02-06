package com.rebuild;

import com.njst.gaming.*;
import com.njst.gaming.Math.Vector3;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Main application class, refactored to extend Engine.
 */
public class RotatingCube extends Engine {

    public RotatingCube() {
        this.title = "Rotating Cube Demo";
    }

    @Override
    protected void onInit() {
        System.out.println("Initializing RotatingCube Demo");
        scene.loader = new DefaultLoader();
    }

    @Override
    protected void onKey(int key, int action) {
        switch (key) {
            case GLFW_KEY_X:
                if (action == GLFW_PRESS) scene.camera_should_move = true;
                if (action == GLFW_RELEASE) scene.camera_should_move = false;
                break;
            case GLFW_KEY_Z:
                if (action == GLFW_PRESS) scene.speed *= 10;
                if (action == GLFW_RELEASE) scene.speed /= 10;
                break;
            case GLFW_KEY_Y:
                scene.renderer.camera.targetPosition = new Vector3();
                break;
            case GLFW_KEY_O:
                if (action == GLFW_PRESS) {
                    scene.KEY_ANIMATIONS.forEach((value) -> {
                        value.time = 0;
                        value.start();
                    });
                }
                break;
            case GLFW_KEY_Q:
                scene.addTetra();
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
