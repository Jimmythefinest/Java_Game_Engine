package com.njst.ai;

public class MaxArraySize {
    public static void main(String[] args) {
        int i =100000000;
        try {
        while(true){
            i+=1;
        }
        } catch (OutOfMemoryError e) {
            System.out.println("Out of memory error: Cannot create an array of this size.");
        } catch (NegativeArraySizeException e) {
            System.out.println("Negative array size exception: Cannot create an array of this size.");
        }
        System.out.println("Successfully created an array of size: " + i);
       
    }
}