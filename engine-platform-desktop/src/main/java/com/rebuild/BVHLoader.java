package com.rebuild;

import java.util.*;
import com.njst.gaming.Bone;
import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Utils.GeneralUtil;

public class BVHLoader {
    private Scanner scanner;
    private Bone rootBone;
    private Stack<Bone> boneStack = new Stack<>();

    public BVHLoader(String bvhData) {
        scanner = new Scanner(bvhData);
    }

    public Bone parse() {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.startsWith("HIERARCHY")) {
                continue;
            } else if (line.startsWith("ROOT")) {
                rootBone = parseBone(line.substring(5).trim(), null);
            }
        }
        return rootBone;
    }

    private Bone parseBone(String name, Bone parent) {
        Bone bone = new Bone();
        bone.name = name;
        if (parent != null) {
            parent.Children.add(bone);
        }

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.startsWith("OFFSET")) {
                String[] parts = line.split(" ");
                bone.global_position = new Vector3(
                    Float.parseFloat(parts[1]), 
                    Float.parseFloat(parts[2]), 
                    Float.parseFloat(parts[3])
                );
                if (parent != null) {
                    bone.position_to_parent = bone.global_position.clone().sub(parent.global_position, new Vector3());
                }
            } else if (line.startsWith("JOINT") || line.startsWith("End Site")) {
                boneStack.push(bone);
                parseBone(line.replace("JOINT", "").trim(), bone);
            } else if (line.startsWith("}")) {
                return bone;
            }
        }
        return bone;
    }
    public static void main(String[] args) {
        BVHLoader loader=new BVHLoader(GeneralUtil.readFile("/users/noliw/OneDrive/Documents/untitled.bvh"));
        loader.print_bone(loader.parse());
    }
    public void fix_bones(Bone b,Vector3 pos){
        float scale=b.position_to_parent.distance(new Vector3());
        b.scale=new Vector3(scale,scale,scale);
        
        for (Bone iterable_element : b.Children) {
            fix_bones(iterable_element,b.get_globalposition());
        }
    }
    int tabs=0;
    String tab="  ";
    public  void print_bone(Bone b){
        System.out.println(tab.repeat(tabs)+b.name);
        System.out.println(tab.repeat(tabs)+"Position"+b.position_to_parent.toString());
        tabs++;
        for (Bone child : b.Children) {
            print_bone(child);
        }
        tabs--;
    }
}