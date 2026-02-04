package com.njst.gaming;

import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Geometries.*;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.*;
import com.njst.gaming.ai.*;
import com.njst.gaming.objects.GameObject;
import java.util.ArrayList;
import java.util.Random;

public class EvolutionSimulation extends Animation implements Scene.SceneLoader {
    ArrayList<Creature> creatures;
    ArrayList<GameObject> bodies;
    ArrayList<Food> foods;
    Random random = new Random();
    int texture, foodTexture;
    Scene s;

    public void load(Scene scene) {
        s = scene;

        int skybox = ShaderProgram.loadTexture( "/jimmy/desertstorm.jpg");
        GameObject skyboxo = new GameObject(new SphereGeometry(1, 20, 20), skybox);
        skyboxo.scale = new float[]{500, 500, 500};
        scene.addGameObject(skyboxo);

        texture = ShaderProgram.loadTexture( "/jimmy/j.jpg");
        foodTexture = ShaderProgram.loadTexture( "/jimmy/images (2).jpeg");

        bodies = new ArrayList<>();
        creatures = new ArrayList<>();
        foods = new ArrayList<>();

        scene.animations.add(this);

        GameObject plane = new GameObject(
            new TerrainGeometry(128, 128, new float[128][128]),
            ShaderProgram.loadTexture( "/jimmy/images (2).jpeg")
        );
        scene.addGameObject(plane);

        // Spawn food
        for (int i = 0; i < 100; i++) {
            Vector3 foodPos = new Vector3(-10 + random.nextFloat() * 20, 0, -10 + random.nextFloat() * 20);
            Food food = new Food(foodPos, foodTexture);
            foods.add(food);
            scene.addGameObject(food.body);
        }

        for (int i = 0; i < 200; i++) {
            Vector3 pos = new Vector3(-5 + random.nextFloat() * 10, 0, -5 + random.nextFloat() * 10);
            Creature c = new Creature(pos);
            creatures.add(c);

            GameObject go = new GameObject(new CubeGeometry(), texture);
            go.position = pos.clone();
            bodies.add(go);
            scene.addGameObject(go);
        }
    }

    public void animate() {
        ArrayList<Creature> survivors = new ArrayList<>();
        int average = 0;

        for (int i = 0; i < creatures.size(); i++) {
            Creature c = creatures.get(i);
            GameObject body = bodies.get(1);
            average += c.life;

            // Feed current state to the brain
            float[] input = new float[]{c.position.x, c.position.z};
            float[] output = c.brain.feedForward(input);

            // Movement and energy drain
            Vector3 movement = new Vector3(output[0] - 0.5f, 0, output[1] - 0.5f);
            c.position.add(movement.mul(0.1f));
            c.energy -= 0.1f + movement.length() * 0.5f;

            body.position = c.position;

            // Eat food
            for (int j = 0; j < foods.size(); j++) {
                Food f = foods.get(j);
                if (c.position.distance(f.position) < 0.5f) {
                    c.energy += 20f;
                    s.objects.remove(f.body);
                    foods.remove(j);
                    break;
                }
            }

            // Death by energy
            if (c.energy <= 0) {
                creatures.remove(i);
                s.objects.remove(body);
                bodies.remove(i);
                i--;
                continue;
            }

            // Survival check
            if (c.life < 25) {
                survivors.add(c);
                c.life++;
            } else {
                if (c.position.distance(new Vector3(5, 0, 5)) < 10) {
                    survivors.add(c);
                    c.life++;
                } else {
                    body.position = new Vector3(0, 0, 0);
                    creatures.remove(i);
                    bodies.remove(i);
                    i--;
                }
            }
        }

        s.dat = "Population: " + survivors.size() + "\nAverage age: " + (survivors.size() > 0 ? (average / survivors.size()) : 0);

        // Replenish food
         if(foods.size()<30){
        while (foods.size() < 100) {
            Vector3 foodPos = new Vector3(-10 + random.nextFloat() * 20, 0, -10 + random.nextFloat() * 20);
            Food food = new Food(foodPos, foodTexture);
            foods.add(food);
            s.addGameObject(food.body);
        }
        }

        // Evolve
        if (survivors.size() < 5) {
            evolve(survivors);
            s.log.logToRootDirectory("evolving");
        }
    }

    void evolve(ArrayList<Creature> survivors) {
        creatures.clear();
        // Do not clear bodies; reuse them

        while (survivors.size() < 200) {
            Creature parent = survivors.get(random.nextInt(survivors.size()));
            Creature child = parent.clone();
            child.brain.mutate(0.002f, 0.1f);
            child.position = new Vector3(-5 + random.nextFloat() * 10, 0, -5 + random.nextFloat() * 10);
            survivors.add(child);

            if (survivors.size() <= bodies.size()) {
                bodies.get(survivors.size() - 1).texture = texture;
            }
        }

        creatures.addAll(survivors);
    }

    static class Creature {
        Vector3 position;
        int life = 0;
        float energy = 100f;
        NeuralNetwork brain;

        public Creature(Vector3 position) {
            this.position = position;
            this.brain = new NeuralNetwork(new int[]{2, 5, 2}, 0.01f, true);
        }

        public Creature clone() {
            Creature copy = new Creature(this.position.clone());
            copy.brain = this.brain.copy();
            return copy;
        }
    }

    static class Food {
        Vector3 position;
        GameObject body;

        public Food(Vector3 position, int texture) {
            this.position = position;
            this.body = new GameObject(new CubeGeometry(), texture);
            this.body.scale = new float[]{0.2f, 0.2f, 0.2f};
            this.body.position = position.clone();
        }
    }
}
