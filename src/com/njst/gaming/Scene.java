package com.njst.gaming;

import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Math.Tetrahedron;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.*;
import com.njst.gaming.Physics.*;
import com.njst.gaming.objects.GameObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class Scene {
    public ArrayList<GameObject> objects;
    public ArrayList<Animation> animations;
    public Tetrahedron temp = new Tetrahedron();
    public HashMap<String, Map<String, KeyframeAnimation>> animation_groups;
    public HashMap<Integer,Runnable> actions=new HashMap<>();
    public HashMap<String,Runnable> commands=new HashMap<>();
    public ArrayList<KeyframeAnimation> KEY_ANIMATIONS;

    public ArrayList<ArrayList<KeyframeAnimation>> MOTION_ANIMATIONS;
    public boolean righmouse = false;
    public Renderer renderer;
    String info = "";
    public String dat = "";
    public float speed = 1;
    PhysicsEngine physics;
    RootLogger log;
    public float[][] heightMap;
    public SceneLoader loader = new SceneLoader() {
        public void load(Scene s) {

        }
    };
    public boolean object_should_move = false;
    public boolean camera_should_move = false;
    public boolean camera_should_move_up = false;

    public Scene() {
        objects = new ArrayList<GameObject>();
        animation_groups = new HashMap<>();
        animations = new ArrayList<>();
        MOTION_ANIMATIONS = new ArrayList<>();
        KEY_ANIMATIONS = new ArrayList<>();
        // renderer = new Renderer();
        // renderer.scene = this;
        log = new RootLogger("/home/nj/Scene.log");
        log.logToRootDirectory("hiisis");
        physics = new PhysicsEngine(this);

    }

    public Renderer getRenderer() {
        return renderer;
    }

    public void onDrawFrame() {
        if (camera_should_move) {
            // renderer.camera.moveForward(0.2f*speed);
        }
        if (camera_should_move_up) {
            renderer.camera.cameraPosition.add(new Vector3(0, 0.05f, 0));
            renderer.camera.targetPosition.add(new Vector3(0, 0.05f, 0));
        }
        for (Animation i : animations) {
            i.animate();
        }
        // physics.simulate(1f/60f);
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
            GameObject obj = new GameObject(n, ShaderProgram.loadTexture("/jimmy/images (2).jpeg"));
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

        // x log.logToRootDirectory(objects.get(0).vboIds[1]+"");
    }

    public interface SceneLoader {
        public void load(Scene s);
    }

    public static String floatatostring(float[] mm, int n) {
        String se = "{";
        for (int i = 0; i < mm.length / n; i++) {
            String rows = "   {";
            for (int i1 = 0; i1 < n; i1++) {
                if (i1 != 0) {
                    rows = rows + ",";
                }
                rows = rows + mm[(i * n) + i1];
            }
            se = se + rows + "}\n";
        }
        return se + "}";
    }

    public static String floatatostring(int[] mm, int n) {
        String se = "{";
        for (int i = 0; i < mm.length / n; i++) {
            String rows = "   {";
            for (int i1 = 0; i1 < n; i1++) {
                if (i1 != 0) {
                    rows = rows + ",";
                }
                rows = rows + mm[(i * n) + i1];
            }
            se = se + rows + "}\n";
        }
        return se + "}";
    }

    public void cursorMoved(double pos_x, double pos_y) {
        // System.out.println("cursor position"+pos_x+" : "+pos_y);
        float newx = (float) pos_x;
        float newy = (float) pos_y;
        // Quaternion q=new Quaternion();
        // renderer.camera.cameraPosition.add(new Vector3(0,0,01f));
        // System.out.println(renderer.camera.cameraPosition);

        // s.renderer.camera.cameraPosition.add(new
        // Vector3((newy-lastY)/80,(newx-lastX)/80,0));
        // s.renderer.camera.cameraPosition=original_position;//.add(new
        // Vector3((float)Math.sin(newx-lastX),0,0-(float)Math.sin(newx-lastX)));
        if (righmouse)
            renderer.camera.targetPosition = renderer.camera.targetPosition.sub(
                    renderer.camera.cameraPosition).normalize()
                    .rotateX((newy - lastY) / 80)
                    .rotateY((newx - lastX) / 80)
                    .add(renderer.camera.cameraPosition);

        // s.renderer.camera.upDirection.add(new Vector3(0,(0)/80,(newx-lastX)/80));

        lastX = newx;
        lastY = newy;
    }

    float lastX = 0, lastY = 0;

}
