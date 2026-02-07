package com.njst.gaming.Loaders;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_0;

import java.util.ArrayList;
import java.util.Scanner;

import com.njst.gaming.Scene;
import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Scene.SceneLoader;
import com.njst.gaming.data;
import com.njst.gaming.Simulations.Creature;
import com.njst.gaming.Utils.GeneralUtil;
import com.njst.gaming.Geometries.CubeGeometry;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Geometries.TerrainGeometry;
import com.njst.gaming.Math.Tetrahedron;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.ComputeShader;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.objects.instancedGameObject;

public class Designer extends Animation implements SceneLoader {
    instancedGameObject skin;
    ArrayList<Creature> creatures = new ArrayList<>();
    private int population_cap = 1000;

    @Override
    public void load(Scene s) {
        int skybox = ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg");
        int texture = ShaderProgram.loadTexture(data.rootDirectory + "/out.png");
        GameObject skyboxo = new GameObject(
                new SphereGeometry(1, 20, 20), skybox);
        skyboxo.scale = new float[] { 500, 500, 500 };
        s.addGameObject(skyboxo);
        GameObject plane = new GameObject(new TerrainGeometry(100, 100, new float[100][100]), texture);
        s.addGameObject(plane);
        Thread t = new Thread() {
            public void run() {
                Scanner scan = new Scanner(System.in);
                String line;
                System.out.println("hello");
                while (!((line = scan.next()).equals("bye"))) {
                    System.out.println("hello" + line);
                    if (line.split(" ")[0].equals("add")) {
                        add(s, texture);
                    }

                }
                scan.close();

            }
        };
        s.actions.put(GLFW_KEY_0, new Runnable() {

            @Override
            public void run() {
                add(s, texture);
            }

        });
        t.start();
        // run_ComputeShader();#
        skin = new instancedGameObject(new CubeGeometry(), texture);
        for (int i = 0; i < population_cap; i++) {
            creatures.add(new Creature());

            skin.matrices.add(creatures.get(creatures.size() - 1).render_data);
        }
        s.addGameObject(skin);
        s.animations.add(this);
    }

    public void add(Scene s, int texture) {

        Tetrahedron t = new Tetrahedron();
        t.v1 = new Vector3((float) Math.random(), (float) Math.random(), (float) Math.random());
        t.v2 = new Vector3((float) Math.random(), (float) Math.random(), (float) Math.random());
        t.v3 = new Vector3((float) Math.random(), (float) Math.random(), (float) Math.random());
        t.v4 = new Vector3((float) Math.random(), (float) Math.random(), (float) Math.random());
        GameObject g = new GameObject(t, texture);
        // g.gen erateBuffers();
        s.addGameObject(g);
    }

    public static void run_ComputeShader() {
        ComputeShader shader = new ComputeShader(GeneralUtil.readFile(data.rootDirectory + "/forwardpass.glsl.txt"));
        float[] input = new float[10];
        System.err.println(shader.err);
        int i = 0;
        for (@SuppressWarnings("unused")
        float na : input) {
            input[i] = 3 * i;
            i++;
        }
        shader.bindBufferToShader(5, input);
        shader.dispatch(input.length, 1, 1);
        float[] results = shader.getBufferData(5);
        System.out.println("Compute Results" + results[1]);
    }

    @Override
    public void animate() {
        ArrayList<Creature> removelist = new ArrayList<>();
        // int i=0;
        // creatures.get(0).Update();
        for (Creature c : creatures) {
            c.Update();
            if (c.position.length() > 20) {

                removelist.add(c);
                // creatures.remove(c);
            }
        }
        for (Creature x : removelist) {
            skin.matrices.remove(x.render_data);
            creatures.remove(x);

        }
        if (creatures.size() < 50) {
            while (creatures.size() < population_cap) {
                creatures.add(new Creature());
                creatures
                        .get(creatures.size() - 1).brain = creatures.get((int) (Math.random() * creatures.size())).brain
                                .copy();
                // creatures
                // .get(creatures.size() - 1).brain.mutate(mutation_rate, mutation_strength);
                skin.matrices.add(creatures.get(creatures.size() - 1).render_data);
            }
        }
    }

}
