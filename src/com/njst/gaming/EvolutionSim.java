package com.njst.gaming;
import java.util.ArrayList;

import com.njst.gaming.Geometries.SphereGeometry;
import com.njst.gaming.Geometries.TerrainGeometry;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.ai.NeuralNetwork;
import com.njst.gaming.objects.GameObject;

public class EvolutionSim implements Scene.SceneLoader{
    ArrayList<GameObject> skins=new ArrayList<>();
    ArrayList<lifeform> lifeforms=new ArrayList<>();
    @Override
    public void load(Scene s) {
         int skybox = ShaderProgram.loadTexture("/jimmy/desertstorm.jpg");
        GameObject skyboxo = new GameObject(
            new SphereGeometry(1, 20, 20), skybox);
        skyboxo.scale = new float[] { 500, 500, 500 };
        skyboxo.updateModelMatrix();
        s.addGameObject(skyboxo);
        int texure= ShaderProgram.loadTexture("/jimmy/j.jpg");
        GameObject plane=new GameObject(new TerrainGeometry(128,128), texure);
      //  plane.scale=new float[]{100,1,100};
        plane.updateModelMatrix();
        s.addGameObject(plane);
        for(int i=20;i>0;i--){
            GameObject skin=new GameObject(new SphereGeometry(1,20,20), texure);
            skins.add(skin);
            lifeform lifeform2=new lifeform();
            lifeform2.skin=skins.size()-1;
            lifeform2.network=new NeuralNetwork(new int[]{3,3,4}, i, false);
            

        }

       
        for (GameObject gameObject : skins) {
            s.addGameObject(gameObject);
        }
        

    }

    public static class lifeform {
        int skin;
        Vector3 position=new Vector3();
        NeuralNetwork network;
        public lifeform(){

        }
        public void act(){
        }
        
    }

    
}