package com.njst.ai;

import static com.njst.ai.Embedder.*;

import java.io.File;
import java.util.ArrayList;

public class SML {
    public static int get_index(String string,String[] array){
        for(int i=0;i<array.length;i++){
            if(array[i].equals(string))return i;
        }
        return -1;
    }
    public static void main(String[] args) {
        try{
          
        ArrayList<String> book=loadStringArrayFromFileList(new File("/jimmy/book.txt"));
        
        String[] word_list=book.toArray(new String[0]);
        String[] unique_word_list=getUniqueStrings(word_list);
        int[] word_indice_list=new int[word_list.length];
        int i=0;
        for(String word:word_list){
            word_indice_list[i++]=get_index(word, unique_word_list);
        }
        double[][] one_hot_encoded_word_list=new double[word_indice_list.length][unique_word_list.length];
        i=0;
        for(int hot_code:word_indice_list){
            one_hot_encoded_word_list[i++]=wordtovector(unique_word_list.length,hot_code);
        }

        NeuralNetwork sml=new NeuralNetwork(new int[]{unique_word_list.length*2,20,unique_word_list.length}, 0.1, false);

        boolean traing_active=true;
        int epochs=1000;
        while(traing_active){

            for(i=1;i<one_hot_encoded_word_list.length-1&&i<100;i++){
                if(unique_word_list[Embedder.indexOfMax(one_hot_encoded_word_list[i+1])].equals("the")){
                }else{
                    double[] input=new double[unique_word_list.length*2];
                    System.arraycopy(one_hot_encoded_word_list[i-1], 0, input, 0, unique_word_list.length);
                    System.arraycopy(one_hot_encoded_word_list[i], 0, input, unique_word_list.length, unique_word_list.length);
                    sml.train(input, one_hot_encoded_word_list[i+1]);
                }
            }
            epochs--;
            traing_active=epochs>0;
            if(epochs%100==0){
                System.out.println(epochs);
            }
        }
        System.out.println("Number of  words:"+word_list.length);
        System.out.println("Number of unique words:"+unique_word_list.length);
        double[] last_word=one_hot_encoded_word_list[0];
        for( i=0;i<10;i++){
            double[] input=new double[unique_word_list.length*2];
                    System.arraycopy(one_hot_encoded_word_list[1-1], 0, input, 0, unique_word_list.length);
                    System.arraycopy(last_word, 0, input, unique_word_list.length, unique_word_list.length);
                    
        int[] indices=Embedder.getSortedIndices(sml.feedForward(input));
        last_word=wordtovector(unique_word_list.length, get_index(unique_word_list[indices[0]], unique_word_list));
       
        // System.out.println(Arrays.toString(last_word));
        System.out.println(unique_word_list[indices[0]]);

        }  
        }catch(Exception e){
            e.printStackTrace();
        }
    }


}
