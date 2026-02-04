package com.njst.ai;

import java.util.*;
import java.io.*;
public class SimpleNeuralNetwork {
    int inputSize = 784;
    int hiddenSize = 198;
    int outputSize = 10;

    double[][] weightsInputHidden;
    double[][] weightsHiddenOutput;
    double[] hiddenBias;
    double[] outputBias;
    double learningRate = 0.01;

    public SimpleNeuralNetwork() {
        weightsInputHidden = randomMatrix(inputSize, hiddenSize);
        weightsHiddenOutput = randomMatrix(hiddenSize, outputSize);
        hiddenBias = new double[hiddenSize];
        outputBias = new double[outputSize];
    }

    public double[] predict(double[] input) {
        double[] hidden = new double[hiddenSize];
        for (int i = 0; i < hiddenSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                hidden[i] += input[j] * weightsInputHidden[j][i];
            }
            hidden[i] += hiddenBias[i];
            hidden[i] = MathUtils.sigmoid(hidden[i]);
        }

        double[] output = new double[outputSize];
        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < hiddenSize; j++) {
                output[i] += hidden[j] * weightsHiddenOutput[j][i];
            }
            output[i] += outputBias[i];
        }

        return MathUtils.softmax(output);
    }

    public void train(double[] input, int label) {
        double[] hidden = new double[hiddenSize];
        for (int i = 0; i < hiddenSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                hidden[i] += input[j] * weightsInputHidden[j][i];
            }
            hidden[i] += hiddenBias[i];
            hidden[i] = MathUtils.sigmoid(hidden[i]);
        }

        double[] output = new double[outputSize];
        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < hiddenSize; j++) {
                output[i] += hidden[j] * weightsHiddenOutput[j][i];
            }
            output[i] += outputBias[i];
        }

        double[] predicted = MathUtils.softmax(output);
        double[] expected = new double[outputSize];
        expected[label] = 1.0;

        // Calculate output layer error
        double[] outputError = new double[outputSize];
        for (int i = 0; i < outputSize; i++) {
            outputError[i] = (expected[i] - predicted[i]);
        }

        // Calculate hidden layer error
        double[] hiddenError = new double[hiddenSize];
        for (int i = 0; i < hiddenSize; i++) {
            for (int j = 0; j < outputSize; j++) {
                hiddenError[i] += outputError[j] * weightsHiddenOutput[i][j];
            }
            hiddenError[i] *= MathUtils.sigmoidDerivative(hidden[i]);
        }

        // Update weightsHiddenOutput
        for (int i = 0; i < hiddenSize; i++) {
            for (int j = 0; j < outputSize; j++) {
                weightsHiddenOutput[i][j] += learningRate * outputError[j] * hidden[i];
            }
        }

        // Update weightsInputHidden
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < hiddenSize; j++) {
                weightsInputHidden[i][j] += learningRate * hiddenError[j] * input[i];
            }
        }

        // Update biases
        for (int i = 0; i < hiddenSize; i++) {
            hiddenBias[i] += learningRate * hiddenError[i];
        }
        for (int i = 0; i < outputSize; i++) {
            outputBias[i] += learningRate * outputError[i];
        }
    }

    private double[][] randomMatrix(int rows, int cols) {
        double[][] mat = new double[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                mat[i][j] = Math.random() * 2 - 1;
        return mat;
    }
    public void saveWeights(String path) throws Exception {
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
        oos.writeObject(weightsInputHidden);
        oos.writeObject(weightsHiddenOutput);
        oos.writeObject(hiddenBias);
        oos.writeObject(outputBias);
    }
}
public void loadWeights(String path) throws Exception {
    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
        weightsInputHidden = (double[][]) ois.readObject();
        weightsHiddenOutput = (double[][]) ois.readObject();
        hiddenBias = (double[]) ois.readObject();
        outputBias = (double[]) ois.readObject();
    }
}

    public static void main(String[] args){
    try{
      SimpleNeuralNetwork net = new SimpleNeuralNetwork();
      // Train with input image and label (e.g. 7)
     List<double[]> dataset =DatasetLoader.loadDataset("/sdcard/Download/MNIST_CSV/mnist_train.csv");
            
            // Normalize the dataset
           // List<double[]> normalizedDataset = normalizeDataset(dataset);
            int[] labels=DatasetLoader.getLabels(dataset.size(),"/sdcard/Download/MNIST_CSV/mnist_train.csv");
     for(int i=0;i<1000;i++){
       net.train (dataset.get(i),labels[i]);
     }
net.saveWeights("/sdcard/mnist-weights.dat");
// Predict
for(int z=0;z<10;z++){

  double[] result = net.predict(dataset.get(z));
System.out.println("Predicted: "+max(result) );
System.out.println("Actual"+labels[z]);

}

}catch(Exception e){
  e.printStackTrace();
}
    }
    
    public static int max(double[] rr){
      int max=0;
      for(int i=1;i<rr.length;i++){
        if(rr[i]>rr[max]){
          max=i;
        }
      }
      return max;
    }
}
