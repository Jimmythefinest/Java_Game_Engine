package com.njst.gaming;

import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.*;
import com.njst.gaming.objects.GameObject;

import java.util.ArrayList;

public class simulationLoader extends Animation implements Scene.SceneLoader {
    ArrayList<quark> Quarks;
    ArrayList<GameObject> skins;

    public void load(Scene scene) {

        int skybox = ShaderProgram.loadTexture("/jimmy/desertstorm.jpg");
        GameObject skyboxo = new GameObject(
                new SphereGeometry(1, 20, 20), skybox);
        skyboxo.scale = new float[] { 500, 500, 500 };
        skyboxo.updateModelMatrix();
        scene.addGameObject(skyboxo);

        Quarks = new ArrayList<>();
        skins = new ArrayList<>();
        scene.animations.add(this);
        int text = ShaderProgram.loadTexture("/jimmy/images (2).jpeg");
        for (int i2 = 0; i2 < 1; i2++) {
            for (int i1 = 0; i1 < 1; i1++) {
                for (int i = 0; i < 4; i++) {

                    quark q = new quark(-.1f, 0.001f, new Vector3(i*5,0 +(int) (Math.random()* 20), 0 +(int) (Math.random()* 20)), new Vector3());
                    add_Quark(q);
                    q.skin = 0;
                    
                    // if (Math.random() < 0.1 && i == 0) {
                    //     q.charge *= 2;
                        
                    // }
                    q.skin = skins.size();

                    GameObject c1 = new GameObject(
                                new SphereGeometry(1, 4, 4), text);
                    skins.add(c1);
                    if (i % 2 == 0) {
                        q.charge *=-1;
                       // q.mass = 15f;
                        c1.texture=skybox;
                    }
                    if(i==3||i==2){
                        q.charge*=-1;
                    }
                }
            }
        }
        for (GameObject g : skins) {
            scene.addGameObject(g);
        }

    }

    public void animate() {

        float a = 0.05f; // Coulomb-like term strength
        float k = 0.2f; // String tension (linear term)
        for (int i = 0; i < Quarks.size(); i++) {
            Vector3 force = new Vector3();
        // System.err.println("Quark"+i);

            for (int j = 0; j < Quarks.size(); j++) {
                if (i == j)
                    continue;

                quark q1 = Quarks.get(i);
                quark q2 = Quarks.get(j);
                Vector3 dir = q2.position.clone().sub(q1.position);
                float dist = dir.length();
                if (dist > 309f)
                    continue;
                // if (dist <0.1f)
                //     dir=new Vector3().sub(dir);
                dir.normalize();

                float magnitude = (a / (dist * dist)+k) * q1.charge * q2.charge;
                Vector3 f = dir.mul(magnitude);
                force.add(f);
            }

            force.mul(1.0f / Quarks.get(i).mass);
            Quarks.get(i).velocity.add(force);
        }
        for (int i = 0; i < Quarks.size(); i++) {
            Quarks.get(i).velocity.add(new Vector3(0,-9.81f,0));
            if(Quarks.get(i).position.length()>30)Quarks.get(i).velocity=new Vector3().sub(Quarks.get(i).velocity);
            Quarks.get(i).position.add(Quarks.get(i).velocity.clone().mul(1.0f / 90.0f));
            skins.get(Quarks.get(i).skin).position = Quarks.get(i).position;
            skins.get(i).updateModelMatrix();
        }
        // System.err.println("Total energy"+total);
        steps++;
        if(steps<1){
            animate();
        }else{
            steps=0;
            return;
        }
    }
    int steps=0;
    public void add_Quark(quark newQuark) {
        int insertIndex = 0;
        for (; insertIndex < Quarks.size(); insertIndex++) {
            quark q = Quarks.get(insertIndex);
            if (newQuark.position.x < q.position.x ||
                    (newQuark.position.x == q.position.x && newQuark.position.y < q.position.y) ||
                    (newQuark.position.x == q.position.x && newQuark.position.y == q.position.y
                            && newQuark.position.z < q.position.z)) {
                break;
            }
        }
        Quarks.add(insertIndex, newQuark);
    }

    public static class quark {
        public float charge;
        public float mass;
        public Vector3 position, velocity;
        int skin;

        public quark(float charge, float mass, Vector3 position, Vector3 velocity) {
            this.charge = charge;
            this.mass = mass;
            this.position = position;
            this.velocity = velocity;
        }
    }

}