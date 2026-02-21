package com.rebuild;

import com.njst.gaming.*;
import com.njst.gaming.Math.Vector3;
import static org.lwjgl.glfw.GLFW.*;
import com.njst.gaming.Loaders.*;

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
        scene.loader = new LODTestLoader();
    }

    @Override
    protected void onUpdate() {
        // Smooth WASD movement relative to camera direction
        float moveStep = 0.1f * scene.speed;

        if (input.isKeyDown(GLFW_KEY_W)) {
            renderer.camera.moveForward(moveStep);
        }
        if (input.isKeyDown(GLFW_KEY_S)) {
            renderer.camera.moveForward(-moveStep);
        }
        if (input.isKeyDown(GLFW_KEY_A)) {
            renderer.camera.moveStrafe(-moveStep);
        }
        if (input.isKeyDown(GLFW_KEY_D)) {
            renderer.camera.moveStrafe(moveStep);
        }
        if (input.isKeyDown(GLFW_KEY_E)) {
            renderer.camera.cameraPosition.add(new Vector3(0, moveStep, 0));
            renderer.camera.targetPosition.add(new Vector3(0, moveStep, 0));
        }
        if (input.isKeyDown(GLFW_KEY_Q)) {
            renderer.camera.cameraPosition.add(new Vector3(0, -moveStep, 0));
            renderer.camera.targetPosition.add(new Vector3(0, -moveStep, 0));
        }

        // Legacy movement check
        scene.camera_should_move = input.isKeyDown(GLFW_KEY_X);

        // Handle single-press events that can also be checked here
        if (input.isKeyPressed(GLFW_KEY_O)) {
            scene.KEY_ANIMATIONS.forEach((value) -> {
                value.time = 0;
                value.start();
            });
        }

        if (input.isKeyPressed(GLFW_KEY_Q)) {
            scene.addTetra();
        }
    }

    @Override
    protected void onKey(int key, int action) {
        // Polling is preferred for continuous movement,
        // but we can still use onKey for discrete events or state changes
        switch (key) {
            case GLFW_KEY_Z:
                if (action == GLFW_PRESS)
                    scene.speed *= 10;
                if (action == GLFW_RELEASE)
                    scene.speed /= 10;
                break;
            case GLFW_KEY_Y:
                if (action == GLFW_PRESS)
                    scene.renderer.camera.targetPosition = new Vector3();
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
