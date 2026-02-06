package com.njst.gaming.Loaders;

import com.google.gson.*;
import com.njst.gaming.Bone;
import com.njst.gaming.data;
import com.njst.gaming.Math.Vector3;

import java.io.FileReader;
import java.lang.reflect.Type;

import org.joml.Math;

public class BoneDeserializer implements JsonDeserializer<Bone> {

    @Override
    public Bone deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        Bone bone = new Bone();
        bone.name = jsonObject.get("name").getAsString();
        System.out.println(bone.name);
        // Deserialize position_to_parent
        JsonArray positionArray = jsonObject.getAsJsonArray("position");
        bone.position_to_parent = (new Vector3(positionArray.get(0).getAsFloat(), positionArray.get(1).getAsFloat(),
                positionArray.get(2).getAsFloat()).mul(0.03F));

        // Deserialize global_position
        // JsonArray globalPositionArray = jsonObject.getAsJsonArray("global_position");
        // bone.global_position.set(globalPositionArray.get(0).getAsFloat(),
        // globalPositionArray.get(1).getAsFloat(),
        // globalPositionArray.get(2).getAsFloat());

        // Deserialize rotation
        Vector3 head = new Vector3(
                jsonObject.getAsJsonArray("head").get(0).getAsFloat(),
                jsonObject.getAsJsonArray("head").get(2).getAsFloat(),
                jsonObject.getAsJsonArray("head").get(1).getAsFloat());
        Vector3 tail = new Vector3(
                jsonObject.getAsJsonArray("tail").get(0).getAsFloat(),
                jsonObject.getAsJsonArray("tail").get(2).getAsFloat(),
                jsonObject.getAsJsonArray("tail").get(1).getAsFloat());
        float scale = head.distance(tail);
        bone.scale = new Vector3(scale / 3, scale, scale / 3).mul(0.03f);

        // JsonArray rotationArray = jsonObject.getAsJsonArray("rotation");
        bone.rotation = calculate_angle(head, tail);// .set( (float)(rotationArray.get(1).getAsFloat()),
                                                    // (float)(rotationArray.get(2).getAsFloat()),(float)(rotationArray.get(0).getAsFloat()));

        // Deserialize scale (if needed)
        // JsonArray scaleArray = jsonObject.getAsJsonArray("scale");
        // bone.scale.set(scaleArray.get(0).getAsFloat(),
        // scaleArray.get(1).getAsFloat(), scaleArray.get(2).getAsFloat());

        // Deserialize global_rotation
        // JsonArray globalRotationArray = jsonObject.getAsJsonArray("global_rotation");
        // bone.global_rotation.set(globalRotationArray.get(0).getAsFloat(),
        // globalRotationArray.get(1).getAsFloat(),
        // globalRotationArray.get(2).getAsFloat());

        // Deserialize parent_position
        // JsonArray parentPositionArray = jsonObject.getAsJsonArray("position");
        // bone.parentposition.set(parentPositionArray.get(0).getAsFloat(),
        // parentPositionArray.get(1).getAsFloat(),
        // parentPositionArray.get(2).getAsFloat());

        // Deserialize parent_rotation
        // JsonArray parentRotationArray = jsonObject.getAsJsonArray("parent_rotation");
        // bone.parent_rotation.set(parentRotationArray.get(0).getAsFloat(),
        // parentRotationArray.get(1).getAsFloat(),
        // parentRotationArray.get(2).getAsFloat());

        // Deserialize children
        JsonArray childrenArray = jsonObject.getAsJsonArray("children");
        for (JsonElement childElement : childrenArray) {
            Bone childBone = context.deserialize(childElement, Bone.class);
            bone.Children.add(childBone);
        }
        bone.update();
        return bone;
    }

    public Vector3 calculate_angle(Vector3 head, Vector3 tail) {
        Vector3 v1 = head.sub(tail);
        v1.normalize();
        float angle_x = (float) Math.toDegrees(Math.atan2(v1.y, v1.z)) + 180;
        // float angle_y=(float)Math.toDegrees(Math.atan2(v1.z,v1.x))+90;
        if (v1.z == 0) {
            angle_x = 0;
        }
        Vector3 rotation = new Vector3(angle_x, 0, 0);
        System.out.println(rotation.toString());
        return rotation;
    }

    public static void main(String[] args) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Bone.class, new BoneDeserializer())
                .create();

        try {
            JsonElement json = JsonParser.parseReader(new FileReader(data.rootDirectory + "/ts.json"));
            Bone bone = gson.fromJson(json, Bone.class);
            System.out.println(bone.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}