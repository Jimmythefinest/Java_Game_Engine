package com.njst.ai;

import java.io.*;
import java.util.*;

public class Formatter {
   public static List<String> loadDataset(String filePath) throws IOException {
        List<String> data = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line;
        int open=0;
        String tab="   ";
        
        while ((line = br.readLine()) != null) {
            // Split the line by commas to get pixel values
            
            String f=repeat(tab,open)+line;
            if(line.contains("{")){
              open++;
            }
            if(line.contains("}")){
              open--;
              if(!line.replace("}","stsshh").split("stsshh")[0].
               replace(" ","").equals("")){
                f=repeat(tab,open)+line.replace("}","}"+repeat(tab,open));
              }
            }
            
            
            data.add(f);  // Add the image to the dataset
        }
        
        br.close();
        return data;
    }
    public static void main(String[] args) {
        try {
           for(String s:loadDataset("/sdcard/Character.java")){
             System.out.println(s);           
            }
        
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String repeat(String data,int times){
      String str="";
      for(int i=0;i<times;i++){
        str+=data;
      }
      return str;
    }
 }
