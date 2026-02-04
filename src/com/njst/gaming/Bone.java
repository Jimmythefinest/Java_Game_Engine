package com.njst.gaming;

import java.util.ArrayList;

import org.joml.Math;

import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Vector3;

public class Bone { 
   public ArrayList<Bone> Children;
public String name="Default_Bone_Name";
public Vector3 position_to_parent = new Vector3();
    public Vector3 global_position = new Vector3();
    public Vector3 rotation = new Vector3();
    public Vector3 scale = new Vector3(1, 1, 1);
    public Vector3 global_rotation = new Vector3();
    Matrix4 inverse_bindpose=new Matrix4();
    Vector3 bind_pos,bind_rot;
    public Vector3 parentposition = new Vector3(), parent_rotation = new Vector3();

    public Bone() {
        Children = new ArrayList<>();
    }

    public void calculate_bind_matrix(){
        Matrix4 modelMatrix=new Matrix4().identity();
        //bone.global_position.mul(x*0.03f);
        modelMatrix.translate(this.get_globalposition().x, this.get_globalposition().y, this.get_globalposition().z); // Translate to the desired position
        modelMatrix.rotate((float) (this.get_globalrotation().x), new Vector3(1, 0, 0)); // Rotate around the Y-axis
        modelMatrix.rotate((float) (this.get_globalrotation().y), new Vector3(0, 1, 0)); // Rotate around the Y-axis
        modelMatrix.rotate((float) (this.get_globalrotation().z), new Vector3(0, 0, 1)); // Rotate around the Y-axis
        modelMatrix.scale(scale); // Scale the square (1.0 means no scaling)

        bind_pos=get_globalposition();
        bind_rot=get_globalrotation();
        inverse_bindpose=modelMatrix.invert();
    }
    public Matrix4 getAnimationMatrix(){
        Matrix4 modelMatrix=new Matrix4().identity();
        //bone.global_position.mul(x*0.03f);
        Vector3 anim_pos=get_globalposition();
        Vector3 anim_rot=get_globalrotation();
        modelMatrix.translate(anim_pos.x, anim_pos.y, anim_pos.z); // Translate to the desired position
        modelMatrix.rotate((float) (anim_rot.x), new Vector3(1, 0, 0)); // Rotate around the Y-axis
        modelMatrix.rotate((float) (anim_rot.y), new Vector3(0, 1, 0)); // Rotate around the Y-axis
        modelMatrix.rotate((float) (anim_rot.z), new Vector3(0, 0, 1)); // Rotate around the Y-axis
        modelMatrix.scale(scale); // Scale the square (1.0 means no scaling)
        
        return modelMatrix.multiply(inverse_bindpose);
        
    }
    public void translate(Vector3 r) {
        global_position.add(r);
        position_to_parent.add(r);
        for (Bone child : Children) {
            child.global_position.add(r);
            child.set_Parent_position(global_position);
        }
    }

    public void set_Parent_position(Vector3 pos) {
        parentposition = pos;
    }

    public void set_Parent_rotation(Vector3 rot) {
        parent_rotation = rot;
    }

    public void rotate(Vector3 rotation1) {
        this.rotation.add(rotation1);
        for (Bone bone : Children) {
            bone.global_position = bone.position_to_parent.clone()
                .rotateZ((float) Math.toRadians(get_globalrotation().z))
                .rotateY((float) Math.toRadians(get_globalrotation().y))
                .rotateX((float) Math.toRadians(get_globalrotation().x))
                .add(global_position);
            bone.set_Parent_rotation(get_globalrotation());
            bone.set_Parent_position(get_globalposition());
            bone.rotate(new Vector3());
        }
    }

    public void update() {
        this.rotate(new Vector3());
    }

    public Vector3 get_globalrotation() {
        return parent_rotation.clone().add(rotation);
    }

    public Vector3 get_globalposition() {
        return position_to_parent.clone()
        .rotateZ((float) Math.toRadians(parent_rotation.z))
        .rotateY((float) Math.toRadians(parent_rotation.y))
        .rotateX((float) Math.toRadians(parent_rotation.x)).add(parentposition);
    }

    // Set the local position of the bone
    public void setPosition(Vector3 position) {
        this.global_position.set(position);
        // Update the position relative to the parent
        if (parentposition != null) {
            this.position_to_parent = global_position.clone().sub(parentposition, new Vector3());
        }
        // Update the positions of child bones
        for (Bone child : Children) {
            child.set_Parent_position(global_position);
            child.setPosition(child.position_to_parent.clone());
        }
    }

    // Set the local rotation of the bone
    public void setRotation(Vector3 rotation) {
        this.rotation.set(rotation);
        // Update the global rotation based on the parent's rotation
        for (Bone bone : Children) {
            bone.set_Parent_rotation(get_globalrotation());
            bone.rotate(new Vector3()); // Update child bone rotation
        }
    }
    public String toString(){
        String c="";
        for (Bone bone : Children) {
            c+=bone.toString();
            }
        return "Bone{" +
        "global_position:" + global_position +
        ", parent_rotation:" + parent_rotation +
        ", rotation:" + rotation +
        ", position_to_parent:" + position_to_parent +
        ", Children:[" + c +
        "]}";
    }
}