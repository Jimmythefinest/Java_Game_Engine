package com.njst.gaming.ri.battlearena;

import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Geometries.TerrainGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Scene;
import com.njst.gaming.data;
import com.njst.gaming.input.ActionInput;
import com.njst.gaming.input.PointerState;
import com.njst.gaming.objects.GameObject;

import java.io.File;

public class BattleArenaDemoLoader implements Scene.SceneLoader {
    private static final String SKYBOX_FILE = "desertstorm.jpg";
    private static final String GROUND_FILE = "j.jpg";
    private static final int GROUND_SIZE = 96;
    private static final float MOVE_SPEED = 0.12f;
    private static final float MOVE_DEADZONE = 0.12f;

    @Override
    public void load(Scene scene) {
        int skyboxTexture = scene.renderer.getGraphicsDevice().loadTexture(resolveTexturePath(SKYBOX_FILE));
        int groundTexture = scene.renderer.getGraphicsDevice().loadTexture(resolveTexturePath(GROUND_FILE));

        GameObject skybox = new GameObject(new SphereGeometry(1f, 20, 20), skyboxTexture);
        skybox.ambientlight_multiplier = 5f;
        skybox.shininess = 1f;
        skybox.setScale(100f, 100f, 100f);
        skybox.setPosition(0f, 0f, 0f);
        scene.renderer.skybox = skybox;
        scene.addGameObject(skybox);

        GameObject ground = new GameObject(
                new TerrainGeometry(GROUND_SIZE, GROUND_SIZE, new float[GROUND_SIZE][GROUND_SIZE]),
                groundTexture);
        ground.ambientlight_multiplier = 1.35f;
        ground.shininess = 3f;
        ground.setPosition(-GROUND_SIZE * 0.5f, -0.75f, -GROUND_SIZE * 0.5f);
        scene.addGameObject(ground);

        scene.renderer.camera.lookAt(new Vector3(0f, 1.5f, -8f), new Vector3(0f, 1.5f, 0f), new Vector3(0f, 1f, 0f));

        ActionInput actions = scene.actionInput;
        PointerState movementPointer = scene.pointer(BattleArenaActions.MOVE_POINTER);
        scene.registerPointerInput(BattleArenaActions.LOOK_POINTER,
                (activeScene, pointer) -> handlePointerLook(activeScene, actions, pointer));
        scene.animations.add(new Animation() {
            @Override
            public void animate() {
                applyMovementPointer(scene, movementPointer);

                if (actions.button(BattleArenaActions.FORWARD).isDown()) {
                    scene.renderer.camera.moveForward(MOVE_SPEED * scene.speed);
                }
                if (actions.button(BattleArenaActions.BACKWARD).isDown()) {
                    scene.renderer.camera.moveForward(-MOVE_SPEED * scene.speed);
                }
                if (actions.button(BattleArenaActions.TURN_LEFT).isDown()) {
                    rotateCamera(scene, -0.03f);
                }
                if (actions.button(BattleArenaActions.ROTATE).isDown()) {
                    rotateCamera(scene, 0.03f);
                }
            }
        });
    }

    private void applyMovementPointer(Scene scene, PointerState movementPointer) {
        if (!movementPointer.isActive()) {
            return;
        }
        float moveX = applyDeadzone(movementPointer.getX());
        float moveY = applyDeadzone(movementPointer.getY());
        if (moveY != 0f) {
            scene.renderer.camera.moveForward(-moveY * MOVE_SPEED * scene.speed);
        }
        if (moveX != 0f) {
            scene.renderer.camera.moveStrafe(moveX * MOVE_SPEED * scene.speed);
        }
    }

    private float applyDeadzone(float value) {
        if (Math.abs(value) < MOVE_DEADZONE) {
            return 0f;
        }
        return Math.max(-1f, Math.min(1f, value));
    }

    private void handlePointerLook(Scene scene, ActionInput actions, PointerState pointer) {
        if (!actions.button(BattleArenaActions.LOOK).isDown()) {
            return;
        }
        if (pointer.getDeltaX() == 0f && pointer.getDeltaY() == 0f) {
            return;
        }
        Vector3 direction = scene.renderer.camera.targetPosition.clone().sub(scene.renderer.camera.cameraPosition).normalize();
        scene.renderer.camera.targetPosition = direction
                .rotateX(pointer.getDeltaY() / 80f)
                .rotateY(pointer.getDeltaX() / 80f)
                .add(scene.renderer.camera.cameraPosition.clone());
    }

    private void rotateCamera(Scene scene, float radians) {
        Vector3 direction = scene.renderer.camera.targetPosition.clone().sub(scene.renderer.camera.cameraPosition);
        Vector3 rotated = direction.rotateY(radians);
        scene.renderer.camera.targetPosition = rotated.add(scene.renderer.camera.cameraPosition.clone());
    }

    private String resolveTexturePath(String fileName) {
        File desktopResource = new File(data.rootDirectory, fileName);
        if (desktopResource.isFile()) {
            return desktopResource.getPath();
        }
        return fileName;
    }
}
