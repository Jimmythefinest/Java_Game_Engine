package com.njst.gaming.Loaders;

import com.njst.gaming.Scene;
import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.collision.CollisionEvent;
import com.njst.gaming.collision.CollisionEventType;
import com.njst.gaming.collision.GameObjectColliderAdapter;
import com.njst.gaming.data;
import com.njst.gaming.objects.CollisionBoxGameObject;
import com.njst.gaming.objects.GameObject;

public class CollisionBoxRuntimeDemoLoader implements Scene.SceneLoader {
    private static final float SEPARATION_SLOP = 0.001f;

    private static class MotionState {
        float direction = -1f;
    }

    @Override
    public void load(Scene scene) {
        int texture = scene.renderer.getGraphicsDevice().loadTexture(data.rootDirectory + "/images (2).jpeg");
        int skyboxTexture = scene.renderer.getGraphicsDevice().loadTexture(data.rootDirectory + "/desertstorm.jpg");

        GameObject skybox = new GameObject(new CubeGeometry(), skyboxTexture);
        skybox.ambientlight_multiplier = 3f;
        skybox.shininess = 1f;
        skybox.setScale(100f, 100f, 100f);
        skybox.updateModelMatrix();
        scene.renderer.skybox = skybox;

        final GameObject leftBox = new GameObject(new CubeGeometry(), texture);
        leftBox.name = "LeftBox";
        leftBox.setPosition(-1.75f, 0f, 0f);
        leftBox.setScale(1f, 1f, 1f);
        leftBox.updateModelMatrix();

        final GameObject rightBox = new GameObject(new CubeGeometry(), texture);
        rightBox.name = "RightBox";
        rightBox.setPosition(1.75f, 0f, 0f);
        rightBox.setScale(1f, 1f, 1f);
        rightBox.updateModelMatrix();

        scene.addGameObject(leftBox);
        scene.addGameObject(rightBox);
        scene.addGameObject(new CollisionBoxGameObject(leftBox, texture));
        scene.addGameObject(new CollisionBoxGameObject(rightBox, texture));

        scene.getCollisionWorld().addCollider(new GameObjectColliderAdapter(leftBox, 1, -1, false, true));
        scene.getCollisionWorld().addCollider(new GameObjectColliderAdapter(rightBox));

        final MotionState motionState = new MotionState();
        scene.getCollisionWorld().addListener(event -> {
            resolveMovingBox(event, rightBox, motionState);
            logCollisionEvent(event);
        });

        scene.animations.add(new Animation() {
            @Override
            public void animate() {
                float nextX = rightBox.position.x + (0.03f * motionState.direction);
                if (nextX < -1.75f) {
                    nextX = -1.75f;
                    motionState.direction = 1f;
                } else if (nextX > 1.75f) {
                    nextX = 1.75f;
                    motionState.direction = -1f;
                }
                rightBox.setPosition(nextX, rightBox.position.y, rightBox.position.z);
            }
        });
    }

    private void resolveMovingBox(CollisionEvent event, GameObject movingBox, MotionState motionState) {
        Object firstOwner = event.getFirst().getOwner();
        Object secondOwner = event.getSecond().getOwner();
        if (event.getType() == CollisionEventType.EXIT) {
            return;
        }

        if (secondOwner == movingBox) {
            Vector3 correction = event.getManifold().getNormal().mul(event.getManifold().getPenetrationDepth() + SEPARATION_SLOP);
            movingBox.translate(correction);
            reverseIfPushingIntoSurface(event.getManifold().getNormal(), motionState);
            return;
        }

        if (firstOwner == movingBox) {
            Vector3 correction = event.getManifold().getNormal().mul(-(event.getManifold().getPenetrationDepth() + SEPARATION_SLOP));
            movingBox.translate(correction);
            reverseIfPushingIntoSurface(new Vector3(event.getManifold().getNormal()).mul(-1f), motionState);
        }
    }

    private void reverseIfPushingIntoSurface(Vector3 surfaceNormal, MotionState motionState) {
        if (surfaceNormal.x == 0f) {
            return;
        }
        if ((motionState.direction < 0f && surfaceNormal.x > 0f)
                || (motionState.direction > 0f && surfaceNormal.x < 0f)) {
            motionState.direction *= -1f;
        }
    }

    private void logCollisionEvent(CollisionEvent event) {
        if (event.getType() == CollisionEventType.STAY) {
            return;
        }
        Object firstOwner = event.getFirst().getOwner();
        Object secondOwner = event.getSecond().getOwner();
        String firstName = firstOwner instanceof GameObject ? ((GameObject) firstOwner).name : String.valueOf(firstOwner);
        String secondName = secondOwner instanceof GameObject ? ((GameObject) secondOwner).name : String.valueOf(secondOwner);
        Vector3 contact = event.getManifold().getContactPoint();
        System.out.println("Collision " + event.getType()
                + ": " + firstName
                + " <-> " + secondName
                + " at " + contact);
    }
}
