package com.njst.gaming;

import com.njst.gaming.Animations.Animation;
import com.njst.gaming.Animations.KeyframeAnimation;
import com.njst.gaming.Geometries.*;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Loaders.FBXAnimationLoader;
import com.njst.gaming.Loaders.FBXBoneLoader;
import com.njst.gaming.Natives.*;
import com.njst.gaming.objects.GameObject;
import com.njst.gaming.objects.Weighted_GameObject;
import com.njst.gaming.skeleton.Skeleton;
import com.njst.gaming.skeleton.Skeleton.Skeletal_Animation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.lwjgl.opengl.GL15;

public class DefaultLoader implements Scene.SceneLoader {
  private static final int NPC_COUNT = 3;
  private static final float NPC_SPACING = 3.0f;
  private static final String EXPORTED_WEIGHTED_GEOMETRY_PATH = data.rootDirectory + "/weighted_geometry/defeated_mesh_1.ser";
  private static final String EXPORTED_BONE_NAMES_PATH = data.rootDirectory + "/weighted_geometry/defeated_bone_names.json";
  private static final String EXPORTED_BONES_PATH = data.rootDirectory + "/weighted_geometry/defeated_bones.ser";
  private static final String EXPORTED_ANIMATIONS_PATH = data.rootDirectory + "/weighted_geometry/defeated_animations.ser";

  private void exportWeightedGeometry(WeightedGeometry geometry) throws Exception {
    File exportFile = new File(EXPORTED_WEIGHTED_GEOMETRY_PATH);
    ensureParentDirectory(exportFile);
    try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(exportFile))) {
      outputStream.writeObject(geometry);
    }
  }

  private void exportBoneNames(ArrayList<Bone> bonesList) throws Exception {
    File exportFile = new File(EXPORTED_BONE_NAMES_PATH);
    ensureParentDirectory(exportFile);
    List<String> boneNames = new ArrayList<>();
    for (Bone bone : bonesList) {
      boneNames.add(bone.name);
    }
    try (FileWriter writer = new FileWriter(exportFile)) {
      writer.write(toJsonArray(boneNames));
    }
  }

  private void exportBones(ArrayList<Bone> bonesList) throws Exception {
    File exportFile = new File(EXPORTED_BONES_PATH);
    ensureParentDirectory(exportFile);
    try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(exportFile))) {
      outputStream.writeObject(bonesList);
    }
  }

  private void exportAnimations(Map<String, KeyframeAnimation> animations) throws Exception {
    File exportFile = new File(EXPORTED_ANIMATIONS_PATH);
    ensureParentDirectory(exportFile);
    try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(exportFile))) {
      outputStream.writeObject(new HashMap<>(animations));
    }
  }

  private void ensureParentDirectory(File file) {
    File parent = file.getParentFile();
    if (parent != null && !parent.exists()) {
      parent.mkdirs();
    }
  }

  private String toJsonArray(List<String> values) {
    StringBuilder json = new StringBuilder();
    json.append("[\n");
    for (int i = 0; i < values.size(); i++) {
      json.append("  \"")
          .append(escapeJson(values.get(i)))
          .append("\"");
      if (i + 1 < values.size()) {
        json.append(',');
      }
      json.append('\n');
    }
    json.append(']');
    return json.toString();
  }

  private String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    String backslash = String.valueOf((char) 92);
    String quote = String.valueOf((char) 34);
    return value
        .replace(backslash, backslash + backslash)
        .replace(quote, backslash + quote)
        .replace(String.valueOf((char) 10), backslash + "n")
        .replace(String.valueOf((char) 13), backslash + "r")
        .replace(String.valueOf((char) 9), backslash + "t");
  }

  public ArrayList<Animation> anims;
  public Bone[] bones;

  public void load(final Scene scene) {
    try {
      anims = new ArrayList<>();

      int skyboxTex = ShaderProgram.loadTexture(data.rootDirectory + "/desertstorm.jpg");
      int groundTex = ShaderProgram.loadTexture(data.rootDirectory + "/WaterPlain0012_1_350.jpg");
      int modelTex = ShaderProgram.loadTexture(data.rootDirectory + "/j.jpg");

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

      Map<String, KeyframeAnimation> fbxanims = FBXAnimationLoader
          .extractAnimation(data.rootDirectory + "/Defeated.fbx", 3, 100);
      exportAnimations(fbxanims);

      Skeleton skeleton = new Skeleton(
          FBXBoneLoader.loadBones(data.rootDirectory + "/Defeated.fbx", new HashMap<String, KeyframeAnimation>(), 100));
      Skeletal_Animation skeletal_Animation1 = new Skeletal_Animation();
      skeletal_Animation1.set_Animation_map(fbxanims);
      skeleton.map(skeletal_Animation1);
      skeletal_Animation1.start();

      ArrayList<Bone> bonesList = skeleton.get_Bone_List();

      for (int[] i = { 0 }; i[0] < 6; i[0]++) {
        if (scene.MOTION_ANIMATIONS.size() <= i[0]) {
          while (scene.MOTION_ANIMATIONS.size() <= i[0]) {
            scene.MOTION_ANIMATIONS.add(new ArrayList<>());
          }
        }
        Map<String, KeyframeAnimation> temp_anims = FBXAnimationLoader.extractAnimation(
            data.rootDirectory + "/Defeated.fbx", i[0],
            100);
        Skeletal_Animation skeleton_Animation = new Skeletal_Animation();
        skeleton_Animation.set_Animation_map(temp_anims);
        skeleton.map(skeleton_Animation);
        skeleton.animations.add(skeleton_Animation);

        temp_anims.forEach((na, value) -> {
          scene.MOTION_ANIMATIONS.get(i[0]).add(value);
        });
      }

      skeleton.root_bone.update();

      WeightedGeometry npcGeometry = FBXBoneLoader.loadModel(data.rootDirectory + "/Defeated.fbx", bonesList, 1, 1.0f);
      exportWeightedGeometry(npcGeometry);
      exportBoneNames(bonesList);
      exportBones(bonesList);
      Random rnd = new Random();
      int side = (int) Math.ceil(Math.sqrt(NPC_COUNT));

      for (int i = 0; i < NPC_COUNT; i++) {
        Weighted_GameObject npc = new Weighted_GameObject(npcGeometry, modelTex);
        int row = i / side;
        int col = i % side;
        float x = (col - (side - 1) * 0.5f) * NPC_SPACING + (rnd.nextFloat() - 0.5f) * 0.4f;
        float z = (row - (side - 1) * 0.5f) * NPC_SPACING + (rnd.nextFloat() - 0.5f) * 0.4f;
        npc.setPosition(x, 0, z);
        npc.name = "NPC_" + i;
        scene.addGameObject(npc);
      }

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
