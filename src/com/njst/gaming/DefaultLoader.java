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
import com.njst.gaming.Natives.*;
import com.njst.gaming.Utils.GeneralUtil;
import com.njst.gaming.ai.Character;
import com.njst.gaming.objects.Bone_object;
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
      // System.err.println();
      GameObject tetra=new GameObject(t, texture1);
      scene.addGameObject(tetra);
      Map<String, KeyframeAnimation> fbxanims =FBXAnimationLoader.extractAnimation("/jimmy/Defeated.fbx", 3, 100);
      // Map<String, KeyframeAnimation> fbxanims1 =FBXAnimationLoader.extractAnimation("/jimmy/Defeated.fbx", 0, 100);
      // final int[]  b11={0};
      // fbxanims.forEach((name,value)->{
      //   // if(b11[0]>5)return;
      //   System.out.println(name);
      //   for(int i=0;i<value.keyframes.size();i++){
      //     // System.out.println(value.keyframes.get(i).time);
      //     // System.out.println(value.keyframes.get(i).position);
      //     fbxanims.get(name).keyframes.get(i).rotation.y*=-1;
      //     // fbxanims.get(name).keyframes.get(i).rotation.z*=-1;
      //     // fbxanims.get(name).keyframes.get(i).position.z*=-1;
      //     if(!fbxanims1.containsKey(name))return;
      //     if(i>20)return;
      //     if(fbxanims.get(name).keyframes.get(i).position.sub(fbxanims1.get(name).keyframes.get(i).position).distance(new Vector3())>0.001f){

      //     }
          
      //     if(fbxanims.get(name).keyframes.get(i).rotation.sub(fbxanims1.get(name).keyframes.get(i).rotation).distance(new Vector3())>0.001f){
      //      System.out.println(name);
      //   KeyframeAnimation v1=fbxanims1.get(name);
      //    System.out.println(value.keyframes.get(i).time);
      //     System.out.println(value.keyframes.get(i).rotation);
      //     System.out.println(v1.keyframes.get(i).time);
      //     System.out.println(v1.keyframes.get(i).rotation);
          
          // }
          
      //     // System.out.println(v1.keyframes.get(i).time);
      //     // System.out.println(v1.keyframes.get(i).position);
      //     // System.out.println();
      //   }
      //   b11[0]+=1;
      // });
      // // // fbxanims=fbxanims1;
      // ArrayList<String> names=new ArrayList<>();
      // HashMap<String,Object> json_data=new HashMap<>();
      // fbxanims.forEach((name, value) -> {
      //   // System.err.println(value.bone.name);
      //   json_data.put(name,value);
      //   names.add(name);
      // });

      // Gson g=new Gson();
      
      // json_data.put("Names", names);
      // System.out.println(g.toJson(json_data));obj.bone =
      
      
      Skeleton skeleton=new Skeleton(FBXBoneLoader.loadBones("/jimmy/Defeated.fbx", new HashMap<String,KeyframeAnimation>(), 100));// ;walkAnimation();
      Skeletal_Animation skeletal_Animation1=new Skeletal_Animation();
      skeletal_Animation1.set_Animation_map(fbxanims);
      skeleton.map(skeletal_Animation1);
      skeletal_Animation1.start();
      ArrayList<Bone> bones = skeleton.get_Bone_List();//FBXBoneLoader.get_array(obj.bone);
      for(int[] i={0};i[0]<6;i[0]++){
        if(scene.MOTION_ANIMATIONS.size()<=i[0]){
          while(scene.MOTION_ANIMATIONS.size()<=i[0]){
            scene.MOTION_ANIMATIONS.add(new ArrayList<>());
          }
        }
        Map<String, KeyframeAnimation> temp_anims =FBXAnimationLoader.extractAnimation("/jimmy/Defeated.fbx", i[0], 100);
        Skeletal_Animation skeleton_Animation = new Skeletal_Animation();
        skeleton_Animation.set_Animation_map(temp_anims);
        skeleton.map(skeleton_Animation);
        skeleton.animations.add(skeleton_Animation);
        // mapBone(bones, temp_anims);
        temp_anims.forEach((na,value)->{
          scene.MOTION_ANIMATIONS.get(i[0]).add(value);
          // scene.animations.add(value);
          // value.start();
        });
      }
      skeleton.root_bone.update();
      // System.out.println(g.toJson(obj.bone));
      
      FBXBoneLoader.loadModel("/jimmy/Defeated.fbx", bones, 1, 1.0f);
      Weighted_GameObject test = new Weighted_GameObject(
          FBXBoneLoader.loadModel("/jimmy/Defeated.fbx", bones, 1, 1.0f),
          texture1);
      scene.addGameObject(test);
      // // // GeneralUtil.save_to_file("/jimmy/ch.json", g.toJson(test.geo));

      // test.translate(new Vector3(2, 0, 0));
      // Weighted_GameObject test1 = new Weighted_GameObject(
      //     FBXBoneLoader.loadModel_invert_xz("/jimmy/a.fbx", bones, 3, 100),
      //     fbx_textures[0]);
      // scene.addGameObject(test1);
      // Weighted_GameObject test2 = new Weighted_GameObject(
      //     FBXBoneLoader.loadModel_invert_xz("/jimmy/a.fbx", bones, 1, 100),
      //     fbx_textures[0]);
      // scene.addGameObject(test2);
      // Weighted_GameObject test3 = new Weighted_GameObject(
      //     FBXBoneLoader.loadModel_invert_xz("/jimmy/P.fbx", bones, 0, 100),
      //     fbx_textures[0]);
      // test3.translate(new Vector3(3f,0,0));
      // scene.addGameObject(test3);
      // Weighted_GameObject test4 = new Weighted_GameObject(
      //     FBXBoneLoader.loadModel_invert_xz("/jimmy/P.fbx", bones, 1, 100),
      //     fbx_textures[1]);
      // test4.translate(new Vector3(3f,0,0));

      // scene.addGameObject(test4);

      // // for(int i=0;i<200;i++){
      // // if(i%4==0)System.out.println("");
      // // System.out.printf(test.geo.getWeightss()[i]+" ");
      // // }

      // // scene.animation_groups.put("walk", fbxanims);
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

        //   value.keyframes.forEach((value1) -> {
        //     // if(value1.rotation.x<0){
        //     // value1.rotation.x=360+value1.rotation.x;
        //     // }
        //     value1.rotation.add(value.bone.rotation);
        //     if (value1.rotation.z + 1 > 360) {
        //       Keyframe frame = new Keyframe(value1.time + 1, value1.position,
        //           new Vector3(value1.rotation.x, value1.rotation.y, 0));
        //       value.keyframes.add(frame);
        //       System.out.println("adding frame");
        //     }
        //     if (value1.rotation.x + 1 > 360) {
        //       Keyframe frame = new Keyframe(value1.time + 1, value1.position,
        //           new Vector3(0, value1.rotation.y, value1.rotation.z));
        //       value.keyframes.add(frame);
        //       System.out.println("adding frame");
        //     }
        //     if (value1.rotation.y + 1 > 360) {
        //       Keyframe frame = new Keyframe(value1.time + 1, value1.position,
        //           new Vector3(value1.rotation.x, 0, value1.rotation.z));
        //       value.keyframes.add(frame);
        //       System.out.println("adding frame");
        //     }
        //     if (value.bone.name.equals("mixamorig:LeftArm")) {
        //       // if(value1.rotation.y<0){
        //       // value1.rotation.y=360+value1.rotation.y;
        //       // }
        //       // if(value1.rotation.z<0){
        //       // value1.rotation.z=360+value1.rotation.z;
        //       // }
        //       // System.err.println(" "+value1.time);
        //       // System.err.println(" "+value1.position);
        //       // System.err.println(" "+value1.rotation);
        //       // System.err.println(" ");
        //     }
        //   });

        // }
    }});
      // // obj.bone.translate(new Vector3(0,5,0));
      // // String[] names = { "idle", "idle1", "idle2", "run", "sneak", "a", "b", "walk" };
      // // for (int i = 0; i < 6; i++) {
      // //   scene.animation_groups.put(names[i], FBXAnimationLoader.extractAnimation("/jimmy/Defeated.fbx", i, 100));
      // //   mapBone(bones, scene.animation_groups.get(names[i]));
      // //   final int x=i;
      // //   scene.animation_groups.get(names[i]).forEach((name, value) -> {
      // //     // System.err.println(value.bone.name);
      // //     if (value.bone != null) {
      // //       value.onfinish = new Runnable() {
      // //         public void run() {
      // //           value.time = 0;
      // //         }
      // //       };
      // //       anims.add(value);
      // //       value.start();
      // //     }else{
      // //       System.out.println("Unmapped: "+names[x]+" :"+name);
      // //     }
      // //   });
      // // }
      // plane.scale = new float[] { 0.1f, 0.1f, 0.1f };
      // // scene.renderer.test=test;
      // scene.addGameObject(plane);
      // // test.translate(new Vector3(1,0,0));
      // // test.rotate(270, 0, 0);

      // // s.renderer.screen=sc;
      scene.renderer.skybox = (skyboxo);
      skyboxo.ambientlight_multiplier = 5;
      skyboxo.shininess = 1;
      skyboxo.scale = new float[] { 100, 100, 100 };
      skyboxo.updateModelMatrix();
      scene.addGameObject(skyboxo);
    skeleton.animations.get(1).start();
      // sc.move(0f, 10f, 10f);
      // sc.scale = new float[] { 10, 1, 10 };

      // c.ambientlight_multiplier = 4;
      // c.move(-1, 0, 0);
      // c.scale = new float[] { 5f, 5f, 5f };
      // c.rotate(20, 40, 90);
      // c.updateModelMatrix();

      // c1.move(-50, 0, -50);
      // // c1.ambientlight_multiplier=5;

      // // scene.addGameObject(c1);
      // scene.addGameObject(obj);
      // // scene.addGameObject(plane);

      // scene.renderer.screen = plane;
      // scene.renderer.screen = sc;

      // final Bone hipbone = new Bone();
      // final Bone b = new Bone();
      // b.position_to_parent = new Vector3(0, 0, 0.5f);
      // hipbone.Children.add(b);
      // hipbone.translate(new Vector3(0, 10, 0));
      // final Bone b1 = new Bone();
      // b1.position_to_parent = (new Vector3(0, 1, 0));
      // b.Children.add(b1);
      // b1.update();
      // HashMap<String, KeyframeAnimation> animation_list = KeyFrameParser
      //     .parseAnimations(GeneralUtil.readFile("/keyframes.json"));
      // KeyframeAnimation anim1 = animation_list.get("HipBone");
      // anim1.bone = hipbone;
      // anim1.start();
      // animation_list.get("rightthigh").bone = b;
      // animation_list.get("rightthigh").start();
      // anims.add(animation_list.get("rightthigh"));

      // anims.add(anim1);
      // GameObject grass = new GameObject(
      // new
      // ObjLoader("/jimmy/10436_Cactus_v1_L3.123ce67a3113-eb68-4881-a89e-34941294e48f/10436_Cactus_v1_L3.123ce67a3113-eb68-4881-a89e-34941294e48f/10436_Cactus_v1_max2010_it2.obj")
      // ,ShaderProgram.loadTexture("/jimmy/10436_Cactus_v1_L3.123ce67a3113-eb68-4881-a89e-34941294e48f/10436_Cactus_v1_L3.123ce67a3113-eb68-4881-a89e-34941294e48f/10436_Cactus_v1_Diffuse.jpg"));

      // grass.scale=new float[]{0.1f,0.1f,0.1f};
      // grass.rotate(-90f,0f,0f);
      // BVHLoader loader1=;//
      // Gson.des(GeneralUtil.readFile("/users/noliw/OneDrive/Documents/untitled.bvh"));
      // loader.print_bone(loader.parse());
   
      skeleton.root_bone.update();

      Animation move = new Animation() {
        public void animate() {
          // obj.bone.Children.get(0).Children.get(0).Children.get(1).rotate(new
          // Vector3(1, 0f, 0));
        }
      };
      anims.add(move);
      // s.addGameObject(c);
      // s.addGameObject(c1);
      // s.log.logToRootDirectory("hi");
      // s.info="hi";
      // plane.scale=new float[]{100,1,100};
      // plane.move(-50, 0, -50);
      // plane.generateBuffers();
      // plane.rotate(180,0,0);
      plane.updateModelMatrix();
      c.velocity = new float[] { 0.003f, 0, 0.000f };
      GameObject gobj = new GameObject(
          new ObjLoader("/jimmy/ninja/ninjaHead_Low.obj"),
          ShaderProgram.loadTexture("/jimmy/ninja/displacement.jpg"));

      gobj.scale = new float[] { 0.1f, 0.1f, 0.1f };
      gobj.move(0, 0, 1f);
      scene.addGameObject(gobj);
      // obj.bone.translate(new Vector3(-2,0,0));

      hall.scale = new float[] { 0.1f, 0.1f, 0.1f };
      // s.addGameObject(hall);
      // walkingAnimation walk = new walkingAnimation(b, b1);
      scene.heightMap = terrain.heightMap;

      // walk.start();

      for (Animation anim : anims) {
        scene.animations.add(anim);
      }
      // s.animations.add(frame2);
      plane.move(0, -1, 0);
      // c1.addAnimation(animate);
      c.name = "Plane";
      Character ch = new Character();
      ch.scene = scene;
      ch.skin = new GameObject(
          new CubeGeometry(),
          texture);
      scene.addGameObject(ch.skin);
      scene.animations.add(ch);
      final SSBO bonesbbo = new SSBO();

      // test_bone.rotate(new Vector3(0,90,0));
      float[] bone_data = new float[bones.size() * 16];
      // obj.bone.translate(new Vector3(0,0,+0.050f));
      for (int i = 0; i < bones.size(); i++) {
        bones.get(i).calculate_bind_matrix();
        System.arraycopy(bones.get(i).getAnimationMatrix().r, 0, bone_data, i * 16, 16);
      }

      bonesbbo.setData(bone_data, GL15.GL_STATIC_DRAW);
      bonesbbo.bind();
      bonesbbo.bindToShader(2);
      Animation update_Bones = new Animation() {
        @Override
        public void animate() { 
          for (Bone iterable_element : bones) {
          if(iterable_element.name.equals("mixamorig:LeftUpLeg")) iterable_element.rotate(new Vector3(1,0,0));
          // iterable_element.name.replace("_", ":");
         ;
        }
          float[] bone_data = new float[bones.size() * 16];
          for (int i = 0; i < bones.size(); i++) {
            System.arraycopy(bones.get(i).getAnimationMatrix().r, 0, bone_data, i * 16, 16);
          }
          // bones.get(0).rotate(new Vector3(0,1,0));

          // obj.bone.Children.get(0).Children.get(0).Children.get(1).rotate(new
          // Vector3(1,0,0));
          bonesbbo.setData(bone_data, GL15.GL_STATIC_DRAW);
          bonesbbo.bind();
          bonesbbo.bindToShader(2);
        }
      };
      scene.animations.add(update_Bones);
      Bone test_bone = new Bone();
      test_bone.parentposition = (new Vector3(0, 2, 0));
      test_bone.rotate(new Vector3(0, 0, 0));
      test_bone.calculate_bind_matrix();
      // obj.bone=test_bone;

      // test_bone.rotation=(new Vector3(0,0,90));
      test_bone.scale = new Vector3(new float[] { 0.10f, 0.10f, 0.10f });
      // bonesbbo.setData(test_bone.getAnimationMatrix().r, GL15.GL_STATIC_DRAW);
      // bonesbbo.bind();
      // printMatrix(test_bone.getAnimationMatrix());
      // bonesbbo.bindToShader(2);
    } catch (Exception e) {
      scene.log.logToRootDirectory(e.getMessage());
      for (StackTraceElement er : e.getStackTrace()) {
        scene.log.logToRootDirectory(er.getClassName() + er.getMethodName()
            + er.getLineNumber());
        // log.logToRootDirectory(er.getMethodName());
      }
    }

  }

  public static void printMatrix(Matrix4 mat) {
    for (int i = 0; i < 16; i++) {
      if (i % 4 == 0)
        System.out.println();
      System.out.printf(mat.r[i] + " ");
    }
  }

  public Bone walkAnimation() throws Exception {
    final Bone hipbone = new Bone();
    final Bone rightthigh = new Bone();
    rightthigh.position_to_parent = new Vector3(0.5f, 0, 0);
    hipbone.Children.add(rightthigh);
    hipbone.translate(new Vector3(0, 1, 0));
    final Bone rightcalf = new Bone();
    rightcalf.position_to_parent = (new Vector3(0, 1, 0));
    rightthigh.Children.add(rightcalf);
    rightcalf.update();
    final HashMap<String, KeyframeAnimation> animation_list = KeyFrameParser.parseAnimations(
        GeneralUtil.readFile("/users/noliw/keyframes-2.json"));
    // KeyframeAnimation anim1 = animation_list.get("HipBone");
    // anim1.bone=hipbone;
    Animation animation = new Animation() {
      public void animate() {
        hipbone.rotate(new Vector3(0.1f, 0, 0));
        hipbone.update();
      }
    };
    anims.add(animation);
    // anim1.start();
    animation_list.get("rightthigh").bone = rightthigh;
    animation_list.get("rightthigh").start();
    anims.add(animation_list.get("rightthigh"));
    animation_list.get("rightthigh").onfinish = new Runnable() {
      public void run() {
        animation_list.get("rightthigh").time = 0;
      }
    };
    animation_list.get("rightcalf").bone = rightcalf;
    animation_list.get("rightcalf").start();
    anims.add(animation_list.get("rightcalf"));
    animation_list.get("rightcalf").onfinish = new Runnable() {
      public void run() {
        animation_list.get("rightcalf").time = 0;
      }
    };
    final Bone leftthigh = new Bone();
    leftthigh.position_to_parent = new Vector3(-0.5f, 0, 0);
    hipbone.Children.add(leftthigh);
    hipbone.translate(new Vector3(0, 10, 0));
    final Bone leftcalf = new Bone();
    leftcalf.position_to_parent = (new Vector3(0, 1, 0));
    leftthigh.Children.add(leftcalf);
    leftcalf.update();
    // anim1.bone=hipbone;
    // anim1.start();
    animation_list.get("leftthigh").bone = leftthigh;
    animation_list.get("leftthigh").start();
    anims.add(animation_list.get("leftthigh"));
    animation_list.get("leftthigh").onfinish = new Runnable() {
      public void run() {
        animation_list.get("leftthigh").time = 0;
      }
    };
    animation_list.get("leftcalf").bone = leftcalf;
    animation_list.get("leftcalf").start();
    anims.add(animation_list.get("leftcalf"));
    animation_list.get("leftcalf").onfinish = new Runnable() {
      public void run() {
        animation_list.get("leftcalf").time = 0;
      }
    };
    animation_list.get("leftcalf").speed = 0.5f;
    animation_list.get("leftthigh").speed = 0.5f;
    animation_list.get("rightcalf").speed = 0.5f;
    animation_list.get("rightthigh").speed = 0.5f;
    // anims.add(anim1);
    return hipbone;

  }

  public Bone Load_Bones(String path) {
    String json = GeneralUtil.readFile(path); // Replace with your actual JSON string

    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(Bone.class, new BoneDeserializer());
    Gson gson = gsonBuilder.create();
    Bone root = gson.fromJson(json, Bone.class);
    final HashMap<String, KeyframeAnimation> animation_list = KeyFrameParser.parseAnimations(
        GeneralUtil.readFile("/jimmy/ts.json"));
    mapBone(root.Children.get(2), animation_list);
    return root;
  }

  public void mapBone(Bone bone, Map<String, KeyframeAnimation> animation_list) {
    try {
      if(animation_list.containsKey(bone.name)){
        animation_list.get(bone.name).bone = bone;

      }
      for (Bone animation : bone.Children) {
        mapBone(animation, animation_list);
      }
    } catch (Exception e) {
      System.out.println("faileed to map" + bone.name);
    }
  }
  public void mapBone(ArrayList<Bone> bones, Map<String, KeyframeAnimation> animation_list) {
    try {
      for (Bone bone : bones) {
        if(animation_list.containsKey(bone.name)){
          animation_list.get(bone.name).bone = bone;
        }
      
      }
    } catch (Exception e) {
      // System.out.println("faileed to map" + bone.name);
    }
  }

  public static void maqin(String[] args) {

    printMatrix(new Matrix4().identity());
    System.out.println();

    Bone t = new Bone();
    t.translate(new Vector3(1, 1, 1));

    t.calculate_bind_matrix();
    printMatrix(t.inverse_bindpose);
    System.out.println();
    printMatrix(t.inverse_bindpose
        .multiply(new Matrix4().identity().rotate(90, new Vector3(0, 1, 0)).translate(new Vector3(1, 0, 0))));
  }

}
