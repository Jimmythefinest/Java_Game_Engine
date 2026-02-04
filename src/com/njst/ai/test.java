package com.njst.ai;

import com.njst.gaming.Math.Matrix4;


public class test {

  public static void main(String[] args) {
      Matrix4 mat=new Matrix4().identity();
      mat.translate(1,0,1);
      int x=0;
      for(float i:mat.r){
        System.out.printf(i+" ");
        x++;
        if(x%4==0){
          System.out.println();         
        }
      }
      mat.invert();
      float[] d3=mat.r;
      x=0;
      System.out.println("") ;      
      for(float i:mat.r){
        System.out.printf(i+" ");
        x++;
        if(x%4==0){
          System.out.println();         
        }
      }
      for(float i:d3){
        System.out.printf(i+" ");
        x++;
        if(x%4==0){
          System.out.println();         
        }
      }
  }

}
