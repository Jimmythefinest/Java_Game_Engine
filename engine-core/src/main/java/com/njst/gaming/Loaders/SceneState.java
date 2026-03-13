package com.njst.gaming.Loaders;

import java.util.ArrayList;

import com.njst.gaming.Math.Vector3;
import com.njst.gaming.Utils.GeneralUtil;

public class SceneState {

    public static class GameObject_data{
        ArrayList<String> texture_map=new ArrayList<>();
        ArrayList<Vector3> pos=new ArrayList<>();
        ArrayList<Vector3> rot=new ArrayList<>();
        ArrayList<Vector3> scales=new ArrayList<>();
        ArrayList<Integer> textures=new ArrayList<>();
        static int i=0;
        public void save_to_file(String path){
            String data="";
            data+=(pos.size()+"\n");
            data+=(texture_map.size()+"\n");
            for(Vector3 pos:this.pos){
                for (float iterable_element : pos.toArray()) {
                    data+=iterable_element+" ";
                }
                data+="\n";
            }
            GeneralUtil.save_to_file(path, data);
        }
    }
    
}
