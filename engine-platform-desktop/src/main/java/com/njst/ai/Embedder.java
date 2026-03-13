package com.njst.ai;

import java.io.*;
import java.util.*;

import com.njst.gaming.data;

public class Embedder {

    public static void main(String[] args) {
        try {
            HashMap<String, Integer> m = new HashMap<>();
            ArrayList<String> book = loadStringArrayFromFileList(new File(data.rootDirectory + "/book.txt"));

            String[] words = getUniqueStrings(book.toArray(new String[0]));
            int t = 0;
            for (String word : words) {
                m.put(word, t);
                t++;
                // System.out.println(word);
            }

            // saveStringArrayToFile(book.toArray(new String[0]),new
            // File("/sdcard/book.txt"));

            System.out.println(words.length);
            NeuralNetwork nn = new NeuralNetwork(new int[] { words.length * 2, 1200, words.length * 2, words.length },
                    0.1, false);
            t = 0;
            // double[] test=new double[words.length];
            // test[(int)m.get("blessed")]=1;
            // for(int i=0;i<1000;i++){
            // nn.train(test, test);
            // }
            long start = System.currentTimeMillis();

            while (true) {
                for (String word : book) {
                    t++;
                    double[] context = new double[words.length];
                    if (t == book.size())
                        break;
                    double[] input1 = new double[words.length * 2];
                    input1[(int) m.get(word)] = 1;
                    System.arraycopy(context, 0, input1, context.length, context.length);
                    double[] target1 = new double[words.length];
                    context = nn.feedForward(input1);
                    String next = book.get(t);
                    target1[(int) m.get(next)] = 1;
                    nn.train(input1, target1);
                    if (t % 1000 == 0)
                        System.out.println("training:" + word + "Next:" + next);
                    if (t % 500 == 0) {
                        System.out.println(((double) t / book.size()) * 100);
                        System.out.println(((System.currentTimeMillis() - start) / 1000));

                        int random_word = (int) (Math.random() * words.length);
                        System.out.println("Testing word:" + words[random_word]);
                        double[] test_input = new double[words.length * 2];
                        System.arraycopy(wordtovector(words.length, random_word), 0, test_input, 0,
                                wordtovector(words.length, random_word).length);
                        System.arraycopy(context, 0, input1, context.length, context.length);

                        double[] out = nn.feedForward(test_input);
                        int[] indices = getSortedIndices(out);
                        for (int i = 0; i < 10; i++) {
                            System.out.println("   " + words[indices[i]] + " confidence" + out[indices[i]]);

                        }

                    }
                    //
                }
                System.out.printf("Continue training:");
                nn.saveToFile(data.rootDirectory + "/slm");
                System.out.println("Saved model");
                t = 0;
                // Scanner scan=new Scanner(System.in);
                // String line =scan.next();
                //
                // if(line.equals("stop")){
                // break ;
                // }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static double[] wordtovector(int length, int loc) {
        double[] test = new double[length];
        test[loc] = 1;
        return test;
    }

    public static int indexOfMax(double[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array must not be null or empty");
        }

        int maxIndex = 0;
        double maxValue = array[0];

        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                maxIndex = i;
            }
        }

        return maxIndex;
    }

    public static int[] getSortedIndices(final double[] array) {
        // Create Integer array of indices
        Integer[] indices = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            indices[i] = i;
        }

        // Sort indices using a comparator (ascending order)
        Arrays.sort(indices, new Comparator<Integer>() {
            public int compare(Integer i1, Integer i2) {
                return Double.compare(array[i2], array[i1]);
            }
        });

        // Convert Integer[] to int[]
        int[] result = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            result[i] = indices[i];
        }

        return result;
    }

    public static String[] loadStringArrayFromFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        ArrayList<String> lines = new ArrayList<String>();
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        reader.close();
        return lines.toArray(new String[0]);
    }

    public static ArrayList<String> loadStringArrayFromFileList(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        ArrayList<String> lines = new ArrayList<String>();
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        reader.close();
        return lines;
    }

    public static void saveStringArrayToFile(String[] data, File file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (String line : data) {
            writer.write(line);
            writer.newLine();
        }
        writer.close();
    }

    public static String[] getUniqueStrings(String[] input) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        for (int i = 0; i < input.length; i++) {
            set.add(input[i]);
        }
        return set.toArray(new String[0]);
    }

}
