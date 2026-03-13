package com.njst.ai;

import java.io.*;
import java.util.*;

import javax.imageio.ImageIO;

import com.njst.gaming.data;

import java.awt.image.*;
import java.awt.Color;;

public class DatasetLoader {

    // Method to load the dataset from a file
    public static List<double[]> loadDataset(String filePath) throws IOException {
        List<double[]> data = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line;
        int b = 0;
        while ((line = br.readLine()) != null && b < 1000) {
            // Split the line by commas to get pixel values
            String[] pixelValues = line.split(",");
            double[] image = new double[pixelValues.length - 1];
            b++;
            for (int i = 1; i < pixelValues.length - 1; i++) {
                image[i] = Double.parseDouble(pixelValues[i]); // Convert to double
            }

            data.add(image); // Add the image to the dataset
        }

        br.close();
        return data;
    }

    public static int[] getLabels(int number, String filePath) throws IOException {
        int[] data = new int[number];
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line;
        int b = 0;
        while ((line = br.readLine()) != null && b < data.length) {
            // Split the line by commas to get pixel values
            String[] pixelValues = line.split(",");
            data[b] = Integer.parseInt(pixelValues[0]);
            b++;
        }

        br.close();
        return data;
    }

    // Method to normalize the dataset
    public static List<double[]> normalizeDataset(List<double[]> dataset) {
        List<double[]> normalizedData = new ArrayList<>();

        for (double[] image : dataset) {
            double[] normalizedImage = new double[image.length];
            for (int i = 0; i < image.length; i++) {
                normalizedImage[i] = (image[i] > 0) ? 1 : 0;
            }
            normalizedData.add(normalizedImage);
        }

        return normalizedData;
    }

    public static void main(String[] args) {
        boolean load = true;
        boolean train = true;
        try {
            // Load the dataset from file
            List<double[]> dataset = loadDataset(data.rootDirectory + "/MNIST_CSV/mnist_train.csv");

            // Normalize the dataset
            // List<double[]> normalizedDataset = normalizeDataset(dataset);
            int[] labels = getLabels(1000, data.rootDirectory + "/MNIST_CSV/mnist_train.csv");
            // Print the first image in the normalized dataset
            System.out.println("First Image (Normalized):");
            int[] layers = { 784, 350, 16, 10 };
            int first = 0;
            NeuralNetwork nn = new NeuralNetwork(layers, 0.01, true);
            if (load) {
                nn = NeuralNetwork.loadFromFile(data.rootDirectory + "/nn.dat");//
            }
            int last = 0;
            while (train) {
                int score = 0;

                for (int i1 = 0; i1 < 100; i1++) {
                    for (int i = 0; i < 100; i++) {
                        int ss = (int) (Math.random() * 100) + (int) ((Math.random() * 9) * 100);
                        double[] target = new double[10];
                        target[labels[ss]] = 1;
                        nn.train(dataset.get(ss), target);
                    }
                    if (i1 % 100 == 0)
                        System.out.printf("training %.3f done\n", i1 / 100.0);
                    // System.out.println();
                }

                for (int rr = 0; rr < 100; rr++) {
                    // input[rr]=1;
                    double[] prediction = nn.feedForward(dataset.get(rr));
                    // dat[rr]=prediction;

                    // System.out.printf("label:"+labels[rr]);
                    // System.out.printf(" Predicted:"+max(prediction));
                    // System.out.println();
                    if (labels[rr] == max(prediction))
                        score++;
                    double max = prediction[max(prediction)];
                    for (int t = 0; t < prediction.length; t++) {
                        prediction[t] = prediction[t] / max;
                    }
                    // System.out.println(Arrays.toString(prediction));

                    for (int i = 0; i < prediction.length; i++) {

                    }
                    // saveImage(toBufferedImage(prediction,
                    // 28,28),data.rootDirectory+"/out.png","PNG");

                }
                for (int i = 0; i < 784; i++) {
                    // difference=dat[0][i]-dat[1][i];
                }
                System.out.println(score);
                if (score > 90)
                    train = false;
                if (score <= last)
                    nn.setLeanRate(Math.random() * 0.1);
                if (score > last)
                    nn.setLeanRate(0.01);
                if (score > first + 5) {
                    nn.saveToFile(data.rootDirectory + "/nn.dat");
                    System.err.println("saving");
                    first = score;
                }
                last = score;
            }
            nn.saveToFile(data.rootDirectory + "/nn.dat");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int max(double[] rr) {
        int max = 0;
        for (int i = 1; i < rr.length; i++) {
            if (rr[i] > rr[max]) {
                max = i;
            }
        }
        return max;
    }

    public static void saveImage(BufferedImage image, String filename, String format) {

        try {

            File outputFile = new File(filename);

            ImageIO.write(image, format, outputFile);

            System.out.println("Image saved to " + outputFile.getAbsolutePath());

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    public static BufferedImage toBufferedImage(double[] flattenedImage, int width, int height) {

        if (flattenedImage.length != width * height) {

            throw new IllegalArgumentException("Size of flattenedImage does not match width * height");

        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {

            for (int x = 0; x < width; x++) {

                int i = y * width + x;

                // Clamp the value between 0 and 1, then scale to 0-255

                int gray = (int) (Math.max(0, Math.min(1, flattenedImage[i])) * 255);

                int rgb = new Color(gray, gray, gray).getRGB();

                image.setRGB(x, y, rgb);

            }

        }

        return image;

    }

}
