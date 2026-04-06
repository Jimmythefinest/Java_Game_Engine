package com.njst.gaming.skeleton;

import com.njst.gaming.*;
import com.njst.gaming.Animations.Animation;
import java.util.*;
import java.util.Locale;

public class Skeleton{
	public Bone root_bone;
	ArrayList<Bone> bones;
    public ArrayList<Skeletal_Animation> animations=new ArrayList<>();
	public Skeleton(Bone parent){
		root_bone=parent;
        bones=get_array(root_bone);
	}
    public ArrayList<Bone> get_Bone_List(){
        return bones;
    }

	public static ArrayList<Bone> get_array(Bone root) {
        ArrayList<Bone> bones = new ArrayList<>();
        addtoArrayList(bones, root);
        return bones;
    }

    public static void addtoArrayList(ArrayList<Bone> list, Bone root) {
        list.add(root);
        for (Bone b : root.Children) {
            addtoArrayList(list, b);
        }
    }

    public void mapBone (Bone bone,Map<String, Animation> animation_list) {
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
  public void mapBone(ArrayList<Bone> bones, Map<String, ? extends Animation> animation_list) {
    try {
      Map<String, Animation> normalizedAnimations = new HashMap<>();
      for (Map.Entry<String, ? extends Animation> entry : animation_list.entrySet()) {
        normalizedAnimations.put(normalizeAnimationBindingName(entry.getKey()), entry.getValue());
      }
      for (Bone bone : bones) {
        if(animation_list.containsKey(bone.name)){
          animation_list.get(bone.name).bone = bone;
        }
        if (animation_list.containsKey(bone.name.replace(":", "").replace("_", ""))) {
            animation_list.get(bone.name.replace(":", "").replace("_", "")).bone = bone;
        }
        Animation normalizedMatch = normalizedAnimations.get(normalizeAnimationBindingName(bone.name));
        if (normalizedMatch != null) {
          normalizedMatch.bone = bone;
        }
      }
    } catch (Exception e) {
      // System.out.println("faileed to map" + bone.name);
    }
  }
  public void map(Skeletal_Animation s){
    mapBone(bones, s.get_Animation_map());
  }
  public static class Skeletal_Animation{
    public String NAme;
    Map<String, ? extends Animation> animation_list;
    public Skeletal_Animation(){

    }
    public  void setTarget(Skeleton s){
        s.map(this);
    }
    public Map<String, ? extends Animation> get_Animation_map(){
        return animation_list;
    }
    public void set_Animation_map(Map<String,? extends Animation> s){
        this.animation_list=s;
    }
    public void start(){
        animation_list.forEach((name,value)->{
            value.start();
        });
    }
}


  private static String normalizeAnimationBindingName(String name) {
    if (name == null) {
      return "";
    }
    String normalized = name.toLowerCase(Locale.ROOT);
    normalized = normalized.replace("$assimpfbx$rotation", "");
    normalized = normalized.replace("$assimpfbx$translation", "");
    normalized = normalized.replace("$assimpfbx$scaling", "");
    normalized = normalized.replaceFirst("^mixamorig\\d+", "mixamorig");
    normalized = normalized.replaceAll("[^a-z0-9]", "");
    return normalized;
  }

}
