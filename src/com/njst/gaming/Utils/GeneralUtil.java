package com.njst.gaming.Utils;

import java.io.*;
import java.util.ArrayList;
public class GeneralUtil {
    public static float[] from4to3(int length,float[] from){
        float[] result=new float[length*3];
        for(int i=0;i<length;i++){
            result[i*3]=from[i*4];
            result[i*3+1]=from[i*4+1];
            result[i*3+2]=from[i*4+2];
        }
        return result;  
 
    } 
    public static float getMaxValue(float[] array) {
        // Check if the array is empty
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array must not be empty");
        }

        // Initialize max with the first element of the array
        float max = array[0];

        // Iterate through the array to find the maximum value
        for (float value : array) {
            if (value > max) {
                max = value; // Update max if a larger value is found
            }
        }

        return max; // Return the maximum value found
    }
    public static double getMaxValue(double[] array) {
        // Check if the array is empty
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array must not be empty");
        }

        // Initialize max with the first element of the array
        double max = array[0];

        // Iterate through the array to find the maximum value
        for (double value : array) {
            if (value > max) {
                max = value; // Update max if a larger value is found
            }
        }

        return max; // Return the maximum value found
    }
    public static float getMinValue(float[] array) {
        // Check if the array is empty
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array must not be empty");
        }

        // Initialize min with the first element of the array
        float min = array[0];

        // Iterate through the array to find the minimum value
        for (float value : array) {
            if (value < min) {
                min = value; // Update min if a smaller value is found
            }
        }

        return min; // Return the minimum value found
    }
    public static double getMinValue(double[] array) {
        // Check if the array is empty
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array must not be empty");
        }

        // Initialize min with the first element of the array
        double min = array[0];

        // Iterate through the array to find the minimum value
        for (double value : array) {
            if (value < min) {
                min = value; // Update min if a smaller value is found
            }
        }

        return min; // Return the minimum value found
    }
    public static String readFile(String filePath) {
        StringBuilder shaderSource = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                shaderSource.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load shader: " + filePath);
        }
        return shaderSource.toString();
    }
    public static void save_to_file(String path,String data){
        try {
            File file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file);
            writer.write(data);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
     public static float[] flatten2D(float[][] mat) {
        ArrayList<Float> flattened=new ArrayList<>();
      for(float[] a:mat){
        for(float b:a){
          flattened.add(b);        
        }
      }      
      float[] final_=new float[flattened.size()];
      int a=0;
      for(Float f:flattened){
        final_[a++]=f;
      }
      return  final_;
    }
    public static  float[] flatten3D(float mat[][][]){
      ArrayList<float[]> layers=new ArrayList<>() ;
      for(float[][] a:mat){
        layers.add(flatten2D(a));
      }
      ArrayList<Float> flattened=new ArrayList<>();
      for(float[] a:layers){
        for(float b:a){
          flattened.add(b);        
        }
      }      
      float[] final_=new float[flattened.size()];
      int a=0;
      for(Float f:flattened){
        final_[a++]=f;
      }
      return  final_;
    }
    public static int sum(int[] arr){
      int sum=0;
      for(int t:arr){
        sum+=t;
      }
      return sum;    
   }

    public static String floatatostring(float[] mm, int n) {
        String se = "{";
        for (int i = 0; i < mm.length / n; i++) {
            String rows = "   {";
            for (int i1 = 0; i1 < n; i1++) {
                if (i1 != 0) {
                    rows = rows + ",";
                }
                rows = rows + mm[(i * n) + i1];
            }
            se = se + rows + "}\n";
        }
        return se + "}";
    }

    public static String intatostring(int[] mm, int n) {
        String se = "{";
        for (int i = 0; i < mm.length / n; i++) {
            String rows = "   {";
            for (int i1 = 0; i1 < n; i1++) {
                if (i1 != 0) {
                    rows = rows + ",";
                }
                rows = rows + mm[(i * n) + i1];
            }
            se = se + rows + "}\n";
        }
        return se + "}";
    }

}
