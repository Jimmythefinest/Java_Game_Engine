package com.njst.gaming.Utils;

public class Matrice_Math {
    /**
     * 
     * @param a1 first Matrix rows are taken from here
     * @param a2 second Matrix columns are taken from here
     * @param x  The elesments in rows/columns
     * @return a
     */
    public static float[] MatrixMultiplication(float[] a1,float[] a2,int x){
        int row_number=a1.length/x, column_number=a2.length/x;
        float[] result=new float[row_number*column_number];
        for(int row_count=0;row_count<row_number;row_count++){
            float[] row=new float[x];
            for(int i=0;i<x;i++){
                row[i]=a1[(row_count*x)+i];
            }    
            for(int column_count=0;column_count<column_number;column_count++){
                float[] column=new float[x];
                for(int i=0;i<x;i++){
                    column[i]=a2[column_count+(i*column_number)];
                }        
                result[(row_count*column_number)+column_count]=rowxcolumn(row, column);
            }
        }
        for(int i=0;i<row_number;i++){
            String rows="{";
            for(int i1=0;i1<column_number;i1++){
                if(i1!=0){
                    rows=rows+",";
                }
                rows=rows+result[(i*column_number)+i1];
            }
           // System.out.println(rows+"}");
        }
        return result;
    }
    public static float rowxcolumn(float[] row,float[] column){
        float result=0f;
        // System.out.println(Arrays.toString(row));
        // System.out.println(Arrays.toString(column));
        for(int i=0;i<row.length;i++){
            result+=row[i]*column[i];
        }
        return result;

    }
}