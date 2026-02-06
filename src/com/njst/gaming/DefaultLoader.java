package com.njst.gaming;

import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Geometries.*;
import com.njst.gaming.Loaders.FBXAnimationLoader;
import com.njst.gaming.Loaders.FBXBoneLoader;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Natives.*;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.objects.Weighted_GameObject;
import com.njst.gaming.skeleton.Skeleton;
import com.njst.gaming.skeleton.Skeleton.Skeletal_Animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.opengl.GL15;

public class DefaultLoader implements Scene.SceneLoader {
  public ArrayList<Animation> anims;
  public Bone[] bones;

  public void load(final Scene scene) {
    try {
      anims = new ArrayList<>();
      
      int skyboxTex = ShaderProgram.loadTexture("/jimmy/desertstorm.jpg");
      int groundTex = ShaderProgram.loadTexture("/jimmy/WaterPlain0012_1_350.jpg");
      int modelTex = ShaderProgram.loadTexture("/jimmy/j.jpg");

      GameObject skyboxo = new GameObject(
          new SphereGeometry(1, 20, 20), skyboxTex);
      skyboxo.ambientlight_multiplier = 5;
      skyboxo.shininess = 1;
      skyboxo.scale = new float[] { 100, 100, 100 };
      skyboxo.updateModelMatrix();
      scene.renderer.skybox = skyboxo;
      scene.addGameObject(skyboxo);

      GameObject plane = new GameObject(new CubeGeometry(), groundTex);
      plane.scale = new float[] { 100, 1, 100 };
      plane.move(0, -1, 0);
      plane.updateModelMatrix();
      scene.addGameObject(plane);

      Map<String, KeyframeAnimation> fbxanims = FBXAnimationLoader.extractAnimation("/jimmy/Defeated.fbx", 3, 100);
      
      Skeleton skeleton = new Skeleton(FBXBoneLoader.loadBones("/jimmy/Defeated.fbx", new HashMap<String, KeyframeAnimation>(), 100));
      Skeletal_Animation skeletal_Animation1 = new Skeletal_Animation();
      skeletal_Animation1.set_Animation_map(fbxanims);
      skeleton.map(skeletal_Animation1);
      skeletal_Animation1.start();
      
      ArrayList<Bone> bonesList = skeleton.get_Bone_List();
      
      for(int[] i={0};i[0]<6;i[0]++){
        if(scene.MOTION_ANIMATIONS.size()<=i[0]){
          while(scene.MOTION_ANIMATIONS.size()<=i[0]){
            scene.MOTION_ANIMATIONS.add(new ArrayList<>());
          }
        }
        Map<String, KeyframeAnimation> temp_anims = FBXAnimationLoader.extractAnimation("/jimmy/Defeated.fbx", i[0], 100);
        Skeletal_Animation skeleton_Animation = new Skeletal_Animation();
        skeleton_Animation.set_Animation_map(temp_anims);
        skeleton.map(skeleton_Animation);
        skeleton.animations.add(skeleton_Animation);
        
        temp_anims.forEach((na,value)->{
          scene.MOTION_ANIMATIONS.get(i[0]).add(value);
        });
      }
      
      skeleton.root_bone.update();
      
      Weighted_GameObject test = new Weighted_GameObject(
          FBXBoneLoader.loadModel("/jimmy/Defeated.fbx", bonesList, 1, 1.0f),
          modelTex);
      scene.addGameObject(test);

      fbxanims.forEach((na, value) -> {
        if (value.bone != null) {
          value.onfinish = new Runnable() {
            public void run() {
              value.time = 0;
            }
          };
          anims.add(value);
          scene.KEY_ANIMATIONS.add(value);
        }
      });

      skeleton.animations.get(1).start();
      skeleton.root_bone.update();

      for (Animation anim : anims) {
        scene.animations.add(anim);
      }

      final SSBO bonesbbo = new SSBO();
      float[] bone_data = new float[bonesList.size() * 16];
      for (int i = 0; i < bonesList.size(); i++) {
        bonesList.get(i).calculate_bind_matrix();
        System.arraycopy(bonesList.get(i).getAnimationMatrix().r, 0, bone_data, i * 16, 16);
      }

      bonesbbo.setData(bone_data, GL15.GL_STATIC_DRAW);
      bonesbbo.bind();
      bonesbbo.bindToShader(2);
      
      Animation update_Bones = new Animation() {
        @Override
        public void animate() { 
          float[] bone_data = new float[bonesList.size() * 16];
          for (int i = 0; i < bonesList.size(); i++) {
            System.arraycopy(bonesList.get(i).getAnimationMatrix().r, 0, bone_data, i * 16, 16);
          }
          bonesbbo.setData(bone_data, GL15.GL_STATIC_DRAW);
          bonesbbo.bind();
          bonesbbo.bindToShader(2);
        }
      };
      scene.animations.add(update_Bones);

    } catch (Exception e) {
      scene.log.logToRootDirectory(e.getMessage());
      for (StackTraceElement er : e.getStackTrace()) {
        scene.log.logToRootDirectory(er.getClassName() + er.getMethodName()
            + er.getLineNumber());
      }
    }
  }
}
