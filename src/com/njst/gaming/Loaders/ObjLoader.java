package com.njst.gaming.Loaders;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.njst.gaming.Geometries.Geometry;

public class ObjLoader extends Geometry {
    private List<Float> vertices;
    private List<Float> textureCoordinates;
    private List<Float> normals;
    private List<Integer> indices;
    private List<Integer> indices_t;
    private List<Integer> indices_n;

    public ObjLoader(String filePath) {
        vertices = new ArrayList<>();
        textureCoordinates = new ArrayList<>();
        normals = new ArrayList<>();
        indices = new ArrayList<>();
        indices_t = new ArrayList<>();
        indices_n = new ArrayList<>();
        loadObj(filePath);
    }

    private void loadObj(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length == 0) continue;

                switch (parts[0]) {
                    case "v":
                        // Vertex position
                        vertices.add(Float.parseFloat(parts[1]));
                        vertices.add(Float.parseFloat(parts[2]));
                        vertices.add(Float.parseFloat(parts[3]));
                        break;
                    case "vt":
                        // Texture coordinates
                        textureCoordinates.add(Float.parseFloat(parts[1]));
                        textureCoordinates.add(Float.parseFloat(parts[2]));
                        break;
                    case "vn":
                        // Normals
                        normals.add(Float.parseFloat(parts[1]));
                        normals.add(Float.parseFloat(parts[2]));
                        normals.add(Float.parseFloat(parts[3]));
                        break;
                    case "f":
                        // Face indices
                        int[] ints={0,1,2,2,3,0};
                        if(parts.length==4){
                          ints=new int[]{0,1,2};
                        }
                        
                        for (int i = 0; i < ints.length; i++) {
                            String[] vertexData = parts[ints[i]+1].split("/");
                            indices.add(Integer.parseInt(vertexData[0])-1); // Vertex index
                            indices_t.add(Integer.parseInt(vertexData[1])-1);
                            indices_n.add(Integer.parseInt(vertexData[2])-1);
                            // Optionally handle texture and normal indices if needed
                        }
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public float[] getVertices() {
        float[] result = new float[indices.size()*3];
        for (int i = 0; i < indices.size(); i++) {
            result[i*3] = vertices.get(indices.get(i)*3);
            result[i*3+1] = vertices.get(indices.get(i)*3+1);
            result[i*3+2] = vertices.get(indices.get(i)*3+2);
        
        }
        return result;
    }

    @Override
    public float[] getTextureCoordinates() {
        float[] result = new float[indices.size()*2];
        for (int i = 0; i < indices.size(); i++) {
            result[i*2] = textureCoordinates.get(indices_t.get(i)*2);
            result[i*2+1] = textureCoordinates.get(indices_t.get(i)*2+1);
        }
        return result;
    }
    public String Error="None";
    @Override
    public float[] getNormals() {
        float[] result = new float[indices.size()*3];
        for (int i = 0; i < indices.size(); i++) {
            result[i*3] = normals.get(indices_n.get(i)*3);
            result[i*3+1] = normals.get(indices_n.get(i)*3+1);
            result[i*3+2] = normals.get(indices_n.get(i)*3+2);
        
        }
        return result;
    }

    @Override
    public int[] getIndices() {
        int[] result = new int[indices.size()];
        for (int i = 0; i < indices.size(); i+=3) {
            result[i] =(int)i;
            result[i+1] =(int)(i+1);
            result[i+2] =(int)(i+2);
        }
        return result;
    }
}