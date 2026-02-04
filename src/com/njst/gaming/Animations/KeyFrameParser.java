package com.njst.gaming.Animations;

import com.google.gson.*;
import com.njst.gaming.Math.Vector3;

import java.util.*;

public class KeyFrameParser {
     
public static void parseJson(JsonObject jsonObject) {}
//     try {
        
//         // Create a JSONObject from the JSON string
//        // JSONObject jsonObject = new JSONObject(jsonString);
//         KeyframeAnimation anim= new KeyframeAnimation(null);
//         // Get the "Name" field
//         String name = jsonObject.get("Name").getAsString();
//         System.out.println("Name: " + name);
        
//         // Get the "frames" array
//         JSONArray framesArray = jsonObject.get("frames");
        
//         // Loop through the frames array
//         for (int i = 0; i < framesArray.length(); i++) {
//             JSONObject frameObject = framesArray.getJSONObject(i);
            
//             // Get the "time" field
//             int time = frameObject.getInt("time");
//             System.out.println("Time: " + time);
            
//             // Get the "position" array
//             JSONArray positionArray = frameObject.getJSONArray("position");
//             float[] position = new float[positionArray.length()];
//             for (int j = 0; j < positionArray.length(); j++) {
//                 position[j] = (float)positionArray.getDouble(j);
//             }
//             System.out.println("Position: " + position[0] + ", " + position[1] + ", " + position[2]);
            
//             // Get the "rotation" array
//             JSONArray rotationArray = frameObject.getJSONArray("rotation");
//             float[] rotation = new float[rotationArray.length()];
//             for (int j = 0; j < rotationArray.length(); j++) {
//                 rotation[j] =(float) rotationArray.getDouble(j);
//             }
//             System.out.println("Rotation: " + rotation[0] + ", " + rotation[1] + ", " + rotation[2]);
//             anim.addKeyframe(time,
//                 new Vector3(position[0],position[2],position[1]),
//                 new Vector3(rotation[0],rotation[2], rotation[1])  
//             );
//         }
//         return anim;
//     } catch (JSONException e) {
//         e.printStackTrace();
//         return null;
//     }
// }
// public static HashMap<String,KeyframeAnimation> parseAnimations(String source)throws Exception{
//   HashMap<String,KeyframeAnimation> map=new HashMap<String,KeyframeAnimation>();
//   JSONArray animations=new JSONArray(source);
//   for (int i = 0; i < animations.length(); i++) {
//        JSONObject animation = animations.getJSONObject(i);
//        map.put(animation.getString("Name"),parseJson(animation));
//   }
         
//   return map;
// }
class AnimationData{
    private List<JsonAnimation> animations;
    public void setAnimations(List<JsonAnimation> frames) {
        this.animations = frames;
    }
    
    public List<JsonAnimation> getAnimations() {
        return animations;
    }
}
class JsonAnimation {
    private String Name;
    private List<Frame> frames;

    // Getters and Setters
    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public List<Frame> getFrames() {
        return frames;
    }

    public void setFrames(List<Frame> frames) {
        this.frames = frames;
    }
}

class Frame {
    private int time;
    private float[] position;
    private float[] rotation;

    // Getters and Setters
    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public float[] getPosition() {
        return position;
    }

    public void setPosition(float[] position) {
        this.position = position;
    }

    public float[] getRotation() {
        return rotation;
    }

    public void setRotation(float[] rotation) {
        this.rotation = rotation;
    }
}
public static HashMap<String,KeyframeAnimation> parseAnimations(String data) {
    //String data=GeneralUtil.readFile("/keyframes.json");
    Gson gson=new Gson();
    AnimationData anim=gson.fromJson(data, AnimationData.class);
    HashMap<String,KeyframeAnimation> map=new HashMap<>();
    
    for (JsonAnimation animation : anim.animations) {
        KeyframeAnimation keyframeAnimation=new KeyframeAnimation(null);
        for (Frame frame : animation.frames) {
            keyframeAnimation.addKeyframe(frame.getTime(), 
                new Vector3(frame.position[0],frame.position[2],frame.position[2]), 
                new Vector3(frame.rotation[0],frame.rotation[2],frame.rotation[1]));
        }
        map.put(animation.getName(), keyframeAnimation);
    }
    return map;
    // System.out.println(gson.toJson(map));

}

}
