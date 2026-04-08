package com.njst.gaming;

import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Math.Tetrahedron;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Physics.*;
import com.njst.gaming.collision.CollisionWorld;
import com.njst.gaming.collision.DefaultCollisionWorld;
import com.njst.gaming.input.ActionInput;
import com.njst.gaming.input.InputBindings;
import com.njst.gaming.input.InputSystem;
import com.njst.gaming.input.PointerInputHandler;
import com.njst.gaming.input.PointerState;
import com.njst.gaming.objects.GameObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class Scene {
    public CopyOnWriteArrayList<GameObject> objects;
    public CopyOnWriteArrayList<Animation> animations;
    public Tetrahedron temp = new Tetrahedron();
    public HashMap<String, Map<String, KeyframeAnimation>> animation_groups;
    public HashMap<Integer, Runnable> actions = new HashMap<>();
    public HashMap<String, Runnable> commands = new HashMap<>();
    public ArrayList<KeyframeAnimation> KEY_ANIMATIONS;
    public ArrayList<ArrayList<KeyframeAnimation>> MOTION_ANIMATIONS;
    public final InputSystem inputSystem;
    public final ActionInput actionInput;
    public final InputBindings inputBindings;
    private final Map<String, PointerInputHandler> pointerHandlers;
    public Renderer renderer;
    public String cameraForwardAction;
    public String cameraBackwardAction;
    public String cameraLeftAction;
    public String cameraRightAction;
    String info = "";
    public String dat = "";
    public float speed = 1;
    PhysicsEngine physics;
    private CollisionWorld collisionWorld;
    private long lastCollisionUpdateNanos;
    RootLogger log;
    public float[][] heightMap;
    private OpenWorldTerrainManager openWorldTerrainManager;
    public SceneLoader loader = new SceneLoader() {
        public void load(Scene s) {
        }
    };
    public boolean object_should_move = false;
    public boolean camera_should_move = false;
    public boolean camera_should_move_up = false;

    public Scene() {
        objects = new CopyOnWriteArrayList<>();
        animation_groups = new HashMap<>();
        animations = new CopyOnWriteArrayList<>();
        MOTION_ANIMATIONS = new ArrayList<>();
        KEY_ANIMATIONS = new ArrayList<>();
        inputSystem = new InputSystem();
        actionInput = new ActionInput(inputSystem);
        inputBindings = new InputBindings();
        pointerHandlers = new HashMap<>();
        log = new RootLogger(data.rootDirectory + "/Scene.log");
        log.logToRootDirectory("hiisis");
        physics = new PhysicsEngine(this);
        collisionWorld = new DefaultCollisionWorld();
        lastCollisionUpdateNanos = 0L;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public CollisionWorld getCollisionWorld() {
        return collisionWorld;
    }

    public void setCollisionWorld(CollisionWorld collisionWorld) {
        if (collisionWorld != null) {
            this.collisionWorld = collisionWorld;
        }
    }

    public void onDrawFrame() {
        if (openWorldTerrainManager != null && renderer != null && renderer.camera != null) {
            openWorldTerrainManager.update(renderer.camera.cameraPosition);
        }
        if (renderer != null && renderer.camera != null) {
            updateCameraMovement();
        }
        for (Animation i : animations) {
            i.animate();
        }
        if (collisionWorld != null) {
            collisionWorld.update(computeCollisionDeltaSeconds());
        }
    }

    private float computeCollisionDeltaSeconds() {
        long now = System.nanoTime();
        if (lastCollisionUpdateNanos == 0L) {
            lastCollisionUpdateNanos = now;
            return 1f / 60f;
        }
        float deltaSeconds = (now - lastCollisionUpdateNanos) / 1_000_000_000f;
        lastCollisionUpdateNanos = now;
        if (deltaSeconds < 0f) {
            return 0f;
        }
        return Math.min(deltaSeconds, 0.1f);
    }

    private void updateCameraMovement() {
        float forward = 0f;
        if (camera_should_move || isActionDown(cameraForwardAction)) {
            forward += 0.1f * speed;
        }
        if (isActionDown(cameraBackwardAction)) {
            forward -= 0.1f * speed;
        }
        if (forward != 0f) {
            renderer.camera.moveForward(forward);
        }

        float strafe = 0f;
        if (isActionDown(cameraLeftAction)) {
            strafe += 0.1f * speed;
        }
        if (isActionDown(cameraRightAction)) {
            strafe -= 0.1f * speed;
        }
        if (strafe != 0f) {
            renderer.camera.moveStrafe(strafe);
        }

        if (camera_should_move_up) {
            Vector3 verticalOffset = new Vector3(0, 0.05f, 0);
            renderer.camera.cameraPosition.add(verticalOffset);
            renderer.camera.targetPosition.add(verticalOffset);
        }
    }

    private boolean isActionDown(String actionId) {
        return actionId != null && inputSystem.button(actionId).isDown();
    }

    public void registerPointerInput(String pointerId, PointerInputHandler handler) {
        if (pointerId == null || pointerId.isEmpty()) {
            throw new IllegalArgumentException("Pointer id must not be empty.");
        }
        if (handler == null) {
            pointerHandlers.remove(pointerId);
            return;
        }
        pointerHandlers.put(pointerId, handler);
    }

    public PointerState pointer(String pointerId) {
        return inputSystem.pointer(pointerId);
    }

    public void handlePointerInput(String pointerId, float x, float y) {
        PointerState pointer = inputSystem.pointer(pointerId);
        pointer.setPosition(x, y);
        PointerInputHandler handler = pointerHandlers.get(pointerId);
        if (handler != null) {
            handler.onPointerMoved(this, pointer);
        }
    }

    public void enableOpenWorld(OpenWorldTerrainManager openWorldTerrainManager) {
        this.openWorldTerrainManager = openWorldTerrainManager;
    }

    public OpenWorldTerrainManager getOpenWorldTerrainManager() {
        return openWorldTerrainManager;
    }

    public void addGameObject(GameObject r) {
        if (objects.size() != 0) {
            int i = 0;
            for (GameObject gameobject : objects) {
                if (r.collisionBounds[0] < gameobject.collisionBounds[0]) {
                    objects.add(i, r);
                    return;
                }
                i++;
            }
        }
        objects.add(r);
    }

    public boolean removeGameObject(GameObject obj) {
        return objects.remove(obj);
    }

    public void addTetra() {
        if (temp.ray_intersects(renderer.camera.cameraPosition.clone(),
                renderer.camera.targetPosition.clone().sub(renderer.camera.cameraPosition))) {
            System.out.println("Adding");
            ArrayList<Vector3> lis = new ArrayList<>();
            lis.add(temp.v1);
            lis.add(temp.v2);
            lis.add(temp.v3);
            lis.add(temp.v4);

            lis.sort(Comparator.comparingDouble(p -> p.distance((renderer.camera.cameraPosition))));
            Tetrahedron n = new Tetrahedron();
            n.v1 = lis.get(0);
            n.v2 = lis.get(1);
            n.v3 = lis.get(2);
            n.v4 = renderer.camera.cameraPosition.clone().sub(n.v1).mul(0.6f).add(n.v1);
            int texture = 0;
            if (renderer != null) {
                texture = renderer.getGraphicsDevice().loadTexture(data.rootDirectory + "/images (2).jpeg");
            }
            GameObject obj = new GameObject(n, texture);
            if (renderer != null) {
                obj.setGraphicsDevice(renderer.getGraphicsDevice());
            }
            obj.generateBuffers();
            addGameObject(obj);
        }
    }

    public int wheretoaddgameobject(float a) {
        int min = 0, max = objects.size();
        boolean c = (objects.size() != 0);
        if (c) {
            return 0;
        }
        int pos = 0;
        while (c) {
            int average = (int) java.lang.Math.floor((min + max) / 2);
            if (objects.get(pos).collisionBounds[0] > a) {
                max = average;
            } else {
                min = average;
            }
            c = (max - min) > 10;
            pos++;
        }
        if (max == min) {
            return min;
        }
        pos = min;
        while (true) {
            if (objects.get(pos).collisionBounds[0] > a) {
                return pos;
            }
            pos++;
            if (pos == objects.size()) {
                return pos;
            }
        }
    }

    public void testCollisions() {
    }

    public interface SceneLoader {
        void load(Scene s);
    }
}
