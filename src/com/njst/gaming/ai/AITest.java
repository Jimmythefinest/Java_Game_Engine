package com.njst.gaming.ai;
import    com.njst.ai.NeuralNetwork;
 
public class AITest{
    public static void main(String[] args) {
        NeuralNetwork n=new NeuralNetwork(new int[]{1,5,1}, 0.0000001f, false);
        int epochs=10000000;
        for(int i=0;i<epochs;i++){
            n.train (new double[]
                  {i/epochs}, 
            new double[]
                  {i/epochs}
                  
                  );
        }
        System.out.println("\n"+(n.feedForward(new double[]{0.9f})[0]));
    }
}