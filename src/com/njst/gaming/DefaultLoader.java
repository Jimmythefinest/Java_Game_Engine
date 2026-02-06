package com.njst.gaming;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Animations.KeyFrameParser;
import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Geometries.*;
import com.njst.gaming.Loaders.BoneDeserializer;
import com.njst.gaming.Loaders.FBXAnimationLoader;
import com.njst.gaming.Loaders.FBXBoneLoader;
import com.njst.gaming.Loaders.ObjLoader;
import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Tetrahedron;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.ShaderProgram;
import com.njst.gaming.ai.Character;
import com.njst.gaming.objects.Bone_object;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.objects.Weighted_GameObject;
import com.njst.gaming.skeleton.Skeleton;
import com.njst.gaming.Animations.Skeletal_Animation;
import com.njst.gaming.Animations.SkeletalAnimationController;
import com.njst.gaming.Loaders.SkeletalModelLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.opengl.GL15;

public class DefaultLoader implements Scene.SceneLoader {
  public ArrayList<Animation> anims;

  public void load(final Scene scene) {
    try {
      anims = new ArrayList<>();
      // ShaderProgram p=new ShaderProgram(Material.vertexShaderCode,
      // Material.fragmentShaderCode);
      int texture = ShaderProgram.loadTexture("/jimmy/desertstorm.jpg");
      int skybox = ShaderProgram.loadTexture("/jimmy/desertstorm.jpg");
      int texture1 = ShaderProgram.loadTexture("/jimmy/j.jpg");
      // ObjLoader loader = new ObjLoader("/jimmy/bone.obj");
      TerrainGeometry terrain = new TerrainGeometry(100, 100);

      GameObject c = new GameObject(
          new SphereGeometry(20, 20, 1), skybox);
      GameObject skyboxo = new GameObject(
          new SphereGeometry(1, 20, 20), skybox);
      GameObject hall = new GameObject(
          new ObjLoader("/jimmy/hall.obj"),
          ShaderProgram.loadTexture("/jimmy/hall.png"));
      GameObject plane = new GameObject(new CubeGeometry(), // new TerrainGeometry(100, 100, new float[100][100]),
          ShaderProgram.loadTexture("/jimmy/WaterPlain0012_1_350.jpg"));
      Bone_object obj = new Bone_object(
          new ObjLoader("/jimmy/cube.obj"),
          texture);
       Tetrahedron  t=new Tetrahedron();
        t.v1=new Vector3(0,0,-1);
        t.v2=new Vector3(0,1,1);
        t.v3=new Vector3(1,1,0);
        t.v4=new Vector3(0,1,0);
      GameObject tetra=new GameObject(t, texture1);
      scene.addGameObject(tetra);
      Map<String, KeyframeAnimation> fbxanims =FBXAnimationLoader.extractAnimation("/jimmy/Defeated.fbx", 3, 100);
      
      
      // Use SkeletalModelLoader to load characters
      Weighted_GameObject test = SkeletalModelLoader.load("/jimmy/Defeated.fbx", texture1, 1.0f, 2);
      Skeleton skeleton = test.getAnimationController().getSkeleton();
      
      // Load animations
      Skeletal_Animation skeletal_Animation1 = SkeletalModelLoader.loadAnimation("/jimmy/Defeated.fbx", "idle", 3, 100, skeleton);
      skeletal_Animation1.start();

      for(int i = 0; i < 6; i++) {
        SkeletalModelLoader.loadAnimation("/jimmy/Defeated.fbx", "motion_" + i, i, 100, skeleton);
      }
      
      scene.addGameObject(test);

      // Add a regular animation to sync the renderer with the skeleton updates
      Animation update_Bones = new Animation() {
        @Override
        public void animate() { 
          test.getAnimationController().update();
        }
      };
      scene.animations.add(update_Bones);
      fbxanims.forEach((na, value) -> {
        // System.err.println(value.bone.name);
        if (value.bone != null) {
          value.onfinish = new Runnable() {
            public void run() {
              value.time = 0;
            }
          };
          anims.add(value);
          // value.speed=0.1f;
          scene.KEY_ANIMATIONS.add(value);
          if (value.bone.name.contains("LeftUp"));
          // value.start();

    }});
      scene.renderer.skybox = (skyboxo);
      skyboxo.ambientlight_multiplier = 5;
      skyboxo.shininess = 1;
      skyboxo.scale = new float[] { 100, 100, 100 };
      skyboxo.updateModelMatrix();
      scene.addGameObject(skyboxo);
   
      skeleton.root_bone.update();
      plane.updateModelMatrix();
      c.velocity = new float[] { 0.003f, 0, 0.000f };
      GameObject gobj = new GameObject(
          new ObjLoader("/jimmy/ninja/ninjaHead_Low.obj"),
          ShaderProgram.loadTexture("/jimmy/ninja/displacement.jpg"));

      gobj.scale = new float[] { 0.1f, 0.1f, 0.1f };
      gobj.move(0, 0, 1f);
      scene.addGameObject(gobj);

      hall.scale = new float[] { 0.1f, 0.1f, 0.1f };
      scene.heightMap = terrain.heightMap;

      for (Animation anim : anims) {
        scene.animations.add(anim);
      }
      // s.animations.add(frame2);
      plane.move(0, -1, 0);
      c.name = "Plane";
      Character ch = new Character();
      ch.scene = scene;
      ch.skin = new GameObject(
          new CubeGeometry(),
          texture);
      scene.addGameObject(ch.skin);
      scene.animations.add(ch);
      // Manual bone update logic removed, handled by SkeletalAnimationController
    } catch (Exception e) {
      scene.log.logToRootDirectory(e.getMessage());
      for (StackTraceElement er : e.getStackTrace()) {
        scene.log.logToRootDirectory(er.getClassName() + er.getMethodName()
            + er.getLineNumber());
        // log.logToRootDirectory(er.getMethodName());
      }
    }
  }
}
