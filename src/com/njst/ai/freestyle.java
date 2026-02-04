package com.njst.ai;

import static com.njst.ai.Embedder.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class freestyle {
    public static void main(String[] args) {
        try {
            HashMap<String, Integer> word_to_index = new HashMap<>();
            ArrayList<String> book = loadStringArrayFromFileList(new File("/jimmy/book.txt"));
            int[] word_order=new int[book.size()];

            String[] words = getUniqueStrings(book.toArray(new String[0]));
            int t = 0;
            for (String word : words) {
                word_to_index.put(word, t);
                t++;
                // System.out.println(word);
            }
            t=0;
            for(String word:book){
                word_order[t++]=word_to_index.get(word);
            }
            System.out.println(book.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Transformer{
        int model_dim=1000;
        int vocab_size;
        public float[][] hot_to_positional;
        public Transformer(int vocab_size){
            this.vocab_size=vocab_size;
            hot_to_positional=new float[vocab_size][model_dim];
        }
        public float[] train(int[] tokens,int target){
            float[] prediction=new float[vocab_size];
            
            return prediction;
        }
    }
    public static float[][] relu(float[][] in){
        float[][] out=new float[in.length][in[0].length];
        for(int i=0;i<in.length;i++){
            for(int i2=0;i2<in[i].length;i2++){
                out[i][i2]=in[i][i2]>0?in[i][i2]:0;
            }
        }
        return out;
    }
}