package com.rebuild;

import com.njst.gaming.Engine;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.TetraLoader;
import com.njst.gaming.Utils.ScreenshotUtil;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_P;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Y;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Z;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;

/**
 * Desktop entry point for the open world demo.
 */
public class RotatingCube extends Engine {
        private final TetraLoader tetrisLoader = new TetraLoader();

    public RotatingCube() {
        this.title = "3D Tetris";
    }

    @Override
    protected void onInit() {
        System.out.println("Initializing 3D Tetris");
        scene.loader = tetrisLoader;
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
            case GLFW_KEY_LEFT:
                if ((action == GLFW_PRESS || action == GLFW_REPEAT) && scene.loader instanceof TetraLoader) {
                    ((TetraLoader) scene.loader).moveActiveLeft();
                }
                break;
            case GLFW_KEY_RIGHT:
                if ((action == GLFW_PRESS || action == GLFW_REPEAT) && scene.loader instanceof TetraLoader) {
                    ((TetraLoader) scene.loader).moveActiveRight();
                }
                break;
            case GLFW_KEY_DOWN:
                if ((action == GLFW_PRESS || action == GLFW_REPEAT) && scene.loader instanceof TetraLoader) {
                    ((TetraLoader) scene.loader).rotateActivePieceClockwise();
                }
                break;
            case GLFW_KEY_UP:
                if ((action == GLFW_PRESS || action == GLFW_REPEAT) && scene.loader instanceof TetraLoader) {
                    ((TetraLoader) scene.loader).rotateActivePieceCounterClockwise();
                }
                break;
            case GLFW_KEY_SPACE:
                if ((action == GLFW_PRESS || action == GLFW_REPEAT) && scene.loader instanceof TetraLoader) {
                    ((TetraLoader) scene.loader).softDropActivePiece();
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
