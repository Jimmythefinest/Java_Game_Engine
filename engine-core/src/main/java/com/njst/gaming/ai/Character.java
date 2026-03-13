package com.njst.gaming.ai;

import com.njst.gaming.*;
import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.objects.GameObject;


public class Character extends Animation{
     public GameObject skin;
     public Scene scene;
     Vector3 target;
     public Character(){
             target=new Vector3(45,0,40);

     }
     public void animate(){
        float terrain_height=scene.heightMap[(int)skin.position.x+50][(int)skin.position.z+50];
        if(skin.position.y>terrain_height){
             skin.velocity[1]-=9.81/500;
         }if(skin.position.y<0.5f+terrain_height){
             skin.velocity[1]*=-1;
         }
        if(target.distance(skin.position)<20){
            target=new Vector3((float)Math.random()*100f-50,(float)skin.velocity[1],(float)Math.random()*100f-50);
        }
       Vector3 motion=target.clone().sub(skin.position).normalize().mul(0.01f);
       motion.y=skin.velocity[1];
       
       if((int)skin.position.x+50>=scene.heightMap.length){
            skin.position=new Vector3(-50,-50,-50);
       }
       skin.position.add(motion);
        
     }
}
