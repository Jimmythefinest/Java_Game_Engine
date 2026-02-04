package com.njst.gaming.Geometries;

import java.util.ArrayList;

import com.njst.gaming.Math.Tetrahedron;
import com.njst.gaming.Math.Vector3;

public class TetraGeometry extends Geometry{
    ArrayList<Vector3> vertices;
    ArrayList<Vector3> normals;
    ArrayList<Vector3> texture_coordinates;
    ArrayList<Integer> indices;
    public TetraGeometry(Tetrahedron[] polygons){
        vertices=new ArrayList<>();
        normals=new ArrayList<>();
        texture_coordinates=new ArrayList<>();
        indices=new ArrayList<>();
        for (Tetrahedron tetra : polygons) {
            Vector3[] normals=new Vector3[3];
            float[] normals_array=tetra.getNormals();
            for(int i=0;i<4;i++){
                normals[i]=new Vector3(normals_array[i*3],normals_array[i*3+1],normals_array[i*3+2]);
            }
        }
        
    }
    public int addVertice(Vector3 vertice,Vector3 normal,Vector3 text){
        int min=0, max=vertices.size();
        if(max==0){
            vertices.add(vertice);
            normals.add(normal);
            texture_coordinates.add(text);
            return 0;

        }
        while(max-min>5){
            int mid_point=min+(int)(0.5*(max-min));
            if(vertices.get(mid_point).x>vertice.x){
                max=mid_point;
            }else{
                min=mid_point;
            }
        }
        for(int i=0;i<5;i++){
            if(vertices.get(min+i).x>vertice.x){
                vertices.add(min+i,vertice);
                normals.add(min+i,normal);
                texture_coordinates.add(text);
                move_indices(min+i-1);
                return min+i;

            }
            if(vertices.get(min+i).x==vertice.x&&vertices.get(min+i).distance(vertice)==0){
                return min+i;
            }
        }
        return -1;
    }
    public void move_indices(int start){
        for(int i=0;i>indices.size();i++){
            if(indices.get(i)>start){
                indices.set(i, indices.get(i));
            }
        }
    }
}