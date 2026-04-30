package com.njst.gaming;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;

import com.njst.gaming.Math.Matrix4;
import com.njst.gaming.Math.Quaternion;
import com.njst.gaming.Math.Vector3;

public class Bone implements Serializable {
    private static final long serialVersionUID = 1L;

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
    private Quaternion parent_orientation = new Quaternion();
    private Quaternion global_orientation = new Quaternion();
    private transient Quaternion local_orientation = new Quaternion();

    public Bone() {
        Children = new ArrayList<>();
    }

    public void calculate_bind_matrix(){
        ensureQuaternionState();
        Matrix4 modelMatrix=new Matrix4().identity();
        Vector3 bindPosition = get_globalposition();
        Quaternion bindOrientation = getGlobalQuaternion();
        modelMatrix.translate(bindPosition);
        modelMatrix.rotate(bindOrientation);
        modelMatrix.scale(scale);

        bind_pos=bindPosition;
        bind_rot=new Vector3(bindOrientation.toEuler());
        inverse_bindpose=modelMatrix.invert();
    }
    public Matrix4 getAnimationMatrix(){
        ensureQuaternionState();
        Matrix4 modelMatrix=new Matrix4().identity();
        Vector3 anim_pos=get_globalposition();
        Quaternion animOrientation = getGlobalQuaternion();
        modelMatrix.translate(anim_pos);
        modelMatrix.rotate(animOrientation);
        modelMatrix.scale(scale);
        
        return modelMatrix.multiply(inverse_bindpose);
        
    }

    public float[] copyInverseBindPose() {
        return inverse_bindpose != null && inverse_bindpose.r != null
                ? inverse_bindpose.r.clone()
                : new Matrix4().identity().r;
    }
    public void translate(Vector3 r) {
        position_to_parent.add(r);
        update();
    }

    public void set_Parent_position(Vector3 pos) {
        parentposition.set(pos);
    }

    public void set_Parent_rotation(Vector3 rot) {
        parent_rotation.set(rot);
        parent_orientation.setFromEuler(rot.x, rot.y, rot.z);
    }

    public void rotate(Vector3 rotation1) {
        this.rotation.add(rotation1);
        update();
    }

    public void update() {
        ensureQuaternionState();
        local_orientation.setFromEuler(rotation.x, rotation.y, rotation.z);
        parent_orientation.multiply(local_orientation, global_orientation).normalize();
        global_orientation.toEuler(global_rotation);
        parent_orientation.rotateVector(position_to_parent, global_position);
        global_position.add(parentposition);
        for (Bone child : Children) {
            child.parent_orientation.set(global_orientation);
            child.parent_rotation.set(global_rotation);
            child.parentposition.set(global_position);
            child.update();
        }
    }

    public Vector3 get_globalrotation() {
        ensureQuaternionState();
        return new Vector3(global_orientation.toEuler());
    }

    public Vector3 get_globalposition() {
        ensureQuaternionState();
        Quaternion parentQuaternion = getParentQuaternion();
        return parentQuaternion.rotateVector(position_to_parent).add(parentposition);
    }

    // Set the local position of the bone
    public void setPosition(Vector3 position) {
        position_to_parent.set(position);
        update();
    }

    // Set the local rotation of the bone
    public void setRotation(Vector3 rotation) {
        this.rotation.set(rotation);
        update();
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

    private Quaternion getLocalQuaternion() {
        ensureQuaternionState();
        return local_orientation.setFromEuler(rotation.x, rotation.y, rotation.z);
    }

    private Quaternion getParentQuaternion() {
        ensureQuaternionState();
        return new Quaternion(
            parent_orientation.x,
            parent_orientation.y,
            parent_orientation.z,
            parent_orientation.w
        ).normalize();
    }

    private Quaternion getGlobalQuaternion() {
        ensureQuaternionState();
        return getParentQuaternion().multiply(getLocalQuaternion()).normalize();
    }

    private void ensureQuaternionState() {
        if (parent_orientation == null) {
            parent_orientation = Quaternion.fromEuler(parent_rotation.x, parent_rotation.y, parent_rotation.z)
                .normalize();
        }
        if (local_orientation == null) {
            local_orientation = new Quaternion();
        }
        if (global_orientation == null) {
            global_orientation = parent_orientation.multiply(getLocalQuaternion()).normalize();
        }
    }

    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
        ensureQuaternionState();
    }
}
